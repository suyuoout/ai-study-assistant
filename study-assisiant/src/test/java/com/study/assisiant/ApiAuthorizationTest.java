package com.study.assisiant;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 权限边界回归测试：这几个场景曾经是真实漏洞。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:study_authz;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "jwt.secret=UnitTestSecretKeyForJwtSecurity2026!!",
        "app.upload-dir=${java.io.tmpdir}/study-assisiant-authz-test",
        "app.allow-register=true",
        "app.bootstrap-admin=false"
})
class ApiAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    private String userToken;
    private String username;

    @BeforeEach
    void registerAndLogin() throws Exception {
        username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "Study123456";

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s",
                                 "nickname":"测试用户"}
                                """.formatted(username, password)))
                .andExpect(status().isOk());

        MvcResult login = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JSONObject body = JSONObject.parseObject(
                login.getResponse().getContentAsString());
        userToken = body.getJSONObject("data").getString("token");
    }

    /** 曾经的越权接口：任何登录用户都能拉全站用户列表 */
    @Test
    void userListEndpointIsRemoved() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void normalUserCannotOpenAdminApis() throws Exception {
        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordReturns401InsteadOf200() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"WrongPass123"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void validationErrorsReturn400WithReadableMessage() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"shortpw","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码至少需要8位"));
    }

    @Test
    void otherUsersPlanIsNotAccessible() throws Exception {
        mockMvc.perform(get("/api/plans/999999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void personalAiKeyIsStoredEncrypted() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request
                        .MockMvcRequestBuilders
                        .put("/api/users/me/ai-key")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aiKey\":\"sk-unit-test-key-123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiKeyMasked")
                        .value("sk-un****3456"));
    }
}
