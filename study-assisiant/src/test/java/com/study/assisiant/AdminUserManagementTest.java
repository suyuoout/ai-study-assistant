package com.study.assisiant;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册关闭时的账号管理回归测试：
 * 前端能拿到「是否开放注册」、管理员可以在后台建号、新账号首次登录需改密。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:study_admin;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "jwt.secret=UnitTestSecretKeyForJwtSecurity2026!!",
        "app.upload-dir=${java.io.tmpdir}/study-assisiant-admin-test",
        "app.bootstrap-admin=true",
        "app.admin-username=siteadmin",
        "app.admin-password=SiteAdmin123456",
        "app.allow-register=false"
})
class AdminUserManagementTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken() throws Exception {
        JSONObject body = JSONObject.parseObject(
                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"siteadmin",
                                         "password":"SiteAdmin123456"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        return body.getJSONObject("data").getString("token");
    }

    @Test
    void publicConfigIsReadableWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowRegister").value(false));
    }

    @Test
    void registerIsRejectedWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newstudent",
                                 "password":"Study123456"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUserWhoMustChangePassword() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"classmate",
                                 "password":"Classmate123",
                                 "nickname":"同学",
                                 "role":"USER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("classmate"));

        JSONObject login = JSONObject.parseObject(
                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"classmate",
                                         "password":"Classmate123"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        JSONObject user = login.getJSONObject("data").getJSONObject("user");
        assertTrue(user.getBooleanValue("mustChangePassword"),
                "管理员创建的账号首次登录应当被要求改密");
        assertEquals("USER", user.getString("role"));
    }

    @Test
    void createdUserCannotReachAdminApis() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"normaluser",
                                 "password":"NormalUser123",
                                 "role":"USER"}
                                """))
                .andExpect(status().isOk());

        JSONObject login = JSONObject.parseObject(
                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"normaluser",
                                         "password":"NormalUser123"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        String token = login.getJSONObject("data").getString("token");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // 普通用户改名、改密等接口仍然可用
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("normaluser"));

        assertFalse(token.isBlank());
    }
}
