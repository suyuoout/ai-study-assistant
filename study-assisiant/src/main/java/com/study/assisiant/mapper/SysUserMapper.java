
package com.study.assisiant.mapper;

import com.study.assisiant.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SysUserMapper {

    @Select("""
        SELECT id, username, password, nickname, email, role,
               create_time, update_time
        FROM sys_user
        ORDER BY id DESC
        """)
    List<SysUser> findAll();

    @Select("""
        SELECT id, username, password, nickname, email, role,
               create_time, update_time
        FROM sys_user
        WHERE username = #{username}
        LIMIT 1
        """)
    SysUser findByUsername(String username);

    @Select("""
        SELECT id, username, password, nickname, email, role,
               create_time, update_time
        FROM sys_user
        WHERE id = #{id}
        """)
    SysUser findById(Long id);


    @Insert("""
        INSERT INTO sys_user (username, password, nickname, email, role)
        VALUES (#{username}, #{password}, #{nickname}, #{email}, #{role})
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
        UPDATE sys_user SET role = #{role}
        WHERE id = #{id}
        """)
    int updateRole(
            @Param("id") Long id,
            @Param("role") String role);
}
