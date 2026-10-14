package com.study.assisiant.admin;

import com.study.assisiant.Result;
import com.study.assisiant.SysUser;
import com.study.assisiant.UserVO;
import com.study.assisiant.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/overview")
    public Result overview(HttpServletRequest request) {
        requireAdmin(request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", count("sys_user"));
        data.put("documents", count("study_document"));
        data.put("plans", count("study_plan"));
        data.put("tasks", count("study_task"));
        data.put("chatSessions", count("chat_session"));
        data.put("chatMessages", count("chat_message"));
        return Result.success(data);
    }

    @GetMapping("/users")
    public Result users(HttpServletRequest request) {
        requireAdmin(request);
        List<UserVO> list = sysUserMapper.findAll()
                .stream()
                .map(this::toVO)
                .toList();
        return Result.success(list);
    }

    @PutMapping("/users/{id}/role")
    public Result updateRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest body,
            HttpServletRequest request
    ) {
        requireAdmin(request);

        Long currentUserId = (Long) request.getAttribute("userId");
        if (currentUserId.equals(id)) {
            return Result.error(400, "不能修改自己的角色");
        }

        String role = body.getRole();
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            return Result.error(400, "角色只能是 ADMIN 或 USER");
        }

        sysUserMapper.updateRole(id, role);
        return Result.success("修改成功", null);
    }

    @GetMapping("/documents")
    public Result documents(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(jdbcTemplate.queryForList("""
            SELECT d.id, d.title, d.file_name AS "fileName",
                   d.file_type AS "fileType", d.status,
                   d.create_time AS "createTime", u.username
            FROM study_document d
            LEFT JOIN sys_user u ON d.user_id = u.id
            ORDER BY d.id DESC
            """));
    }

    @DeleteMapping("/documents/{id}")
    public Result deleteDocument(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        requireAdmin(request);

        String storagePath = jdbcTemplate.query(
                "SELECT storage_path FROM study_document WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                id);

        jdbcTemplate.update(
                "DELETE FROM study_document WHERE id = ?", id);

        if (storagePath != null) {
            try {
                Files.deleteIfExists(Paths.get(storagePath));
            } catch (Exception ignored) {
                // 磁盘文件删除失败不影响记录删除
            }
        }
        return Result.success("删除成功", null);
    }

    @GetMapping("/plans")
    public Result plans(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(jdbcTemplate.queryForList("""
            SELECT p.id, p.title, p.goal, p.status,
                   p.create_time AS "createTime", u.username
            FROM study_plan p
            LEFT JOIN sys_user u ON p.user_id = u.id
            ORDER BY p.id DESC
            """));
    }

    @DeleteMapping("/plans/{id}")
    public Result deletePlan(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        requireAdmin(request);
        jdbcTemplate.update(
                "DELETE FROM study_plan WHERE id = ?", id);
        return Result.success("删除成功", null);
    }

    private Long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table, Long.class);
    }

    private void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "需要管理员权限");
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getCreateTime()
        );
        vo.setRole(user.getRole());
        return vo;
    }
}
