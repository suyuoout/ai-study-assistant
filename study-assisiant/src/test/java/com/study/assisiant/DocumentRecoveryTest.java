package com.study.assisiant;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 「从挂载盘重建资料索引」的回归测试：
 * 模拟"上传目录里有文件、但数据库里没有对应记录"的场景。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:study_recovery;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "jwt.secret=UnitTestSecretKeyForJwtSecurity2026!!",
        "app.upload-dir=${java.io.tmpdir}/study-assisiant-recovery-test",
        "app.bootstrap-admin=true",
        "app.admin-username=recoveryadmin",
        "app.admin-password=Recovery123456",
        "app.allow-register=false"
})
class DocumentRecoveryTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private String adminToken;

    @BeforeEach
    void prepare() throws Exception {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        // 模拟一次历史上传：数据文件 + 元数据文件（元数据里有原始文件名与归属）
        Path data = dir.resolve("11111111-2222-3333-4444-555555555555.txt");
        Files.writeString(data, "数据库索引是一种加快检索的数据结构。",
                StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(
                        "11111111-2222-3333-4444-555555555555.txt.meta.json"),
                "{\"fileName\":\"复习笔记.txt\",\"userId\":1,\"title\":\"复习笔记\"}",
                StandardCharsets.UTF_8);

        JSONObject login = JSONObject.parseObject(
                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"recoveryadmin",
                                         "password":"Recovery123456"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        adminToken = login.getJSONObject("data").getString("token");
    }

    @Test
    void reindexRecoversOrphanFilesAndIsIdempotent() throws Exception {
        JSONObject first = reindex();
        assertEquals(0, first.getIntValue("failed"));
        assertTrue(first.getIntValue("scanned") >= 1,
                "应当扫描到挂载盘上的文件");
        assertTrue(first.getIntValue("recovered") >= 1,
                "孤立文件应当被重新入库");

        // 再执行一次：已经入库的文件应当被跳过，不会产生重复记录
        JSONObject second = reindex();
        assertEquals(0, second.getIntValue("recovered"));
        assertTrue(second.getIntValue("skipped") >= 1);
    }

    private JSONObject reindex() throws Exception {
        String body = mockMvc.perform(
                        post("/api/admin/documents/reindex")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JSONObject.parseObject(body).getJSONObject("data");
    }
}
