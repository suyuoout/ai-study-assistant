package com.study.assisiant.mapper;

import com.study.assisiant.user.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("""
        SELECT id, username, password, nickname, email, role, ai_api_key,
               enabled, must_change_password, create_time, update_time
        FROM sys_user
        ORDER BY id DESC
        """)
    List<SysUser> findAll();

    @Select("""
        SELECT id, username, password, nickname, email, role, ai_api_key,
               enabled, must_change_password, create_time, update_time
        FROM sys_user
        WHERE username = #{username}
        LIMIT 1
        """)
    SysUser findByUsername(String username);

    @Select("""
        SELECT id, username, password, nickname, email, role, ai_api_key,
               enabled, must_change_password, create_time, update_time
        FROM sys_user
        WHERE id = #{id}
        """)
    SysUser findById(Long id);

    @Insert("""
        INSERT INTO sys_user
        (username, password, nickname, email, role, enabled, must_change_password)
        VALUES
        (#{username}, #{password}, #{nickname}, #{email}, #{role},
         #{enabled}, #{mustChangePassword})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    @Update("""
        UPDATE sys_user
        SET nickname = #{nickname}, email = #{email}
        WHERE id = #{id}
        """)
    int updateProfile(
            @Param("id") Long id,
            @Param("nickname") String nickname,
            @Param("email") String email);

    @Update("""
        UPDATE sys_user SET password = #{password}
        WHERE id = #{id}
        """)
    int updatePassword(@Param("id") Long id,
                       @Param("password") String password);

    @Update("""
        UPDATE sys_user SET must_change_password = FALSE
        WHERE id = #{id}
        """)
    int clearMustChangePassword(@Param("id") Long id);

    @Update("""
        UPDATE sys_user SET role = #{role}
        WHERE id = #{id}
        """)
    int updateRole(
            @Param("id") Long id,
            @Param("role") String role);

    @Update("""
        UPDATE sys_user SET enabled = #{enabled}
        WHERE id = #{id}
        """)
    int updateEnabled(
            @Param("id") Long id,
            @Param("enabled") boolean enabled);

    @Update("""
        UPDATE sys_user SET ai_api_key = #{aiApiKey}
        WHERE id = #{id}
        """)
    int updateAiKey(
            @Param("id") Long id,
            @Param("aiApiKey") String aiApiKey);

    @Select("""
        <script>
        SELECT COUNT(*) FROM sys_user
        <where>
          <if test="keyword != null and keyword != ''">
            (username LIKE CONCAT('%', #{keyword}, '%')
             OR nickname LIKE CONCAT('%', #{keyword}, '%')
             OR email LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        </script>
        """)
    long countByKeyword(@Param("keyword") String keyword);

    @Select("""
        <script>
        SELECT id, username, nickname, email, role, enabled,
               must_change_password, create_time, update_time
        FROM sys_user
        <where>
          <if test="keyword != null and keyword != ''">
            (username LIKE CONCAT('%', #{keyword}, '%')
             OR nickname LIKE CONCAT('%', #{keyword}, '%')
             OR email LIKE CONCAT('%', #{keyword}, '%'))
          </if>
        </where>
        ORDER BY id DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """)
    List<SysUser> findPage(@Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("size") int size);

    @Select("SELECT COUNT(*) FROM sys_user")
    long countAll();
}
