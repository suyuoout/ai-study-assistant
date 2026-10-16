package com.study.assisiant.admin;

import com.study.assisiant.ai.AiUsageService;
import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.PageResult;
import com.study.assisiant.common.Result;
import com.study.assisiant.document.DocumentStorage;
import com.study.assisiant.mapper.SysUserMapper;
import com.study.assisiant.user.SysUser;
import com.study.assisiant.user.UserCache;
import com.study.assisiant.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口，全部需要管理员角色。
 *
 * <p>角色来自数据库（拦截器注入），而不是令牌声明，
 * 因此管理员被降权后立即失效。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log =
            LoggerFactory.getLogger(AdminController.class);

    private static final int MAX_PAGE_SIZE = 100;

    private final SysUserMapper sysUserMapper;
    private final UserCache userCache;
    private final AiUsageService aiUsageService;
    private final DocumentStorage documentStorage;
    private final JdbcTemplate jdbcTemplate;

    public AdminController(SysUserMapper sysUserMapper,
                           UserCache userCache,
                           AiUsageService aiUsageService,
                           DocumentStorage documentStorage,
                           JdbcTemplate jdbcTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.userCache = userCache;
        this.aiUsageService = aiUsageService;
        this.documentStorage = documentStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(
            HttpServletRequest request) {
        requireAdmin(request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", count("SELECT COUNT(*) FROM sys_user"));
        data.put("documents", count("SELECT COUNT(*) FROM study_document"));
        data.put("plans", count("SELECT COUNT(*) FROM study_plan"));
        data.put("tasks", count("SELECT COUNT(*) FROM study_task"));
        data.put("chatSessions", count("SELECT COUNT(*) FROM chat_session"));
        data.put("chatMessages", count("SELECT COUNT(*) FROM chat_message"));
        data.put("processingDocuments", count(
                "SELECT COUNT(*) FROM study_document WHERE status = 'PROCESSING'"));
        data.put("failedDocuments", count(
                "SELECT COUNT(*) FROM study_document WHERE status = 'FAILED'"));
        data.put("aiCallsToday", aiUsageService.countTotal(1));
        return Result.success(data);
    }

    /** AI 用量统计：近 N 天的每日调用量 + 调用最多的用户 */
    @GetMapping("/ai-usage")
    public Result<Map<String, Object>> aiUsage(
            @RequestParam(defaultValue = "14") int days,
            HttpServletRequest request) {
        requireAdmin(request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("daily", aiUsageService.dailyTotals(days));
        data.put("topUsers", aiUsageService.topUsers(days, 10));
        data.put("totalCalls", aiUsageService.countTotal(days));
        return Result.success(data);
    }

    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        requireAdmin(request);

        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        String trimmed = keyword == null || keyword.isBlank()
                ? null : keyword.trim();

        List<UserVO> list = sysUserMapper
                .findPage(trimmed, (pageNo - 1) * pageSize, pageSize)
                .stream()
                .map(this::toVO)
                .toList();
        long total = sysUserMapper.countByKeyword(trimmed);
        return Result.success(new PageResult<>(list, total, pageNo, pageSize));
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest body,
            HttpServletRequest request) {
        requireAdmin(request);

        if (currentUserId(request).equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不能修改自己的角色");
        }
        if (sysUserMapper.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "用户不存在");
        }

        sysUserMapper.updateRole(id, body.role());
        userCache.evict(id);
        log.info("管理员 {} 将用户 {} 的角色改为 {}",
                currentUserId(request), id, body.role());
        return Result.success("修改成功", null);
    }

    /** 禁用 / 启用账号；禁用后该账号的请求会被拦截器直接拒绝 */
    @PutMapping("/users/{id}/enabled")
    public Result<Void> updateEnabled(
            @PathVariable Long id,
            @Valid @RequestBody UserEnabledRequest body,
            HttpServletRequest request) {
        requireAdmin(request);

        if (currentUserId(request).equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "不能禁用自己的账号");
        }

        SysUser user = sysUserMapper.findById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "用户不存在");
        }
        if (Boolean.TRUE.equals(user.getEnabled()) && !body.enabled()
                && "ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "请先取消该账号的管理员角色再禁用");
        }

        sysUserMapper.updateEnabled(id, body.enabled());
        userCache.evict(id);
        log.info("管理员 {} 将用户 {} 设为 {}",
                currentUserId(request), id,
                body.enabled() ? "启用" : "禁用");
        return Result.success(body.enabled() ? "已启用" : "已禁用", null);
    }

    @GetMapping("/documents")
    public Result<PageResult<Map<String, Object>>> documents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        requireAdmin(request);

        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);

        List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                SELECT d.id, d.title, d.file_name AS "fileName",
                       d.file_type AS "fileType", d.status,
                       d.error_message AS "errorMessage",
                       d.create_time AS "createTime", u.username
                FROM study_document d
                LEFT JOIN sys_user u ON d.user_id = u.id
                ORDER BY d.id DESC
                LIMIT ? OFFSET ?
                """, pageSize, (pageNo - 1) * pageSize);
        long total = count("SELECT COUNT(*) FROM study_document");
        return Result.success(new PageResult<>(list, total, pageNo, pageSize));
    }

    @DeleteMapping("/documents/{id}")
    public Result<Void> deleteDocument(
            @PathVariable Long id,
            HttpServletRequest request) {
        requireAdmin(request);

        String storagePath = jdbcTemplate.query(
                "SELECT storage_path FROM study_document WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null,
                id);

        jdbcTemplate.update("DELETE FROM study_document WHERE id = ?", id);
        documentStorage.delete(storagePath);
        log.info("管理员 {} 删除了资料 {}", currentUserId(request), id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/plans")
    public Result<PageResult<Map<String, Object>>> plans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        requireAdmin(request);

        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);

        List<Map<String, Object>> list = jdbcTemplate.queryForList("""
                SELECT p.id, p.title, p.goal, p.status,
                       p.create_time AS "createTime", u.username,
                       (SELECT COUNT(*) FROM study_task t
                        WHERE t.plan_id = p.id) AS "taskCount"
                FROM study_plan p
                LEFT JOIN sys_user u ON p.user_id = u.id
                ORDER BY p.id DESC
                LIMIT ? OFFSET ?
                """, pageSize, (pageNo - 1) * pageSize);
        long total = count("SELECT COUNT(*) FROM study_plan");
        return Result.success(new PageResult<>(list, total, pageNo, pageSize));
    }

    @DeleteMapping("/plans/{id}")
    public Result<Void> deletePlan(
            @PathVariable Long id,
            HttpServletRequest request) {
        requireAdmin(request);
        jdbcTemplate.update("DELETE FROM study_plan WHERE id = ?", id);
        log.info("管理员 {} 删除了学习计划 {}",
                currentUserId(request), id);
        return Result.success("删除成功", null);
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private int normalizePage(int page) {
        return Math.max(1, page);
    }

    private int normalizeSize(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }

    private void requireAdmin(HttpServletRequest request) {
        if (!"ADMIN".equals(
                request.getAttribute(JwtInterceptor.ATTR_ROLE))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "需要管理员权限");
        }
    }

    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = UserVO.of(user, null);
        vo.setAiKeyMasked(user.getAiApiKey() == null
                || user.getAiApiKey().isBlank() ? null : "已配置");
        return vo;
    }
}
