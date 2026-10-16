package com.study.assisiant;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.study.assisiant.mapper.StudyPlanMapper;
import com.study.assisiant.mapper.StudyTaskMapper;
import com.study.assisiant.mapper.SysUserMapper;
import com.study.assisiant.plan.StudyPlan;
import com.study.assisiant.task.StudyTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定统计接口的返回结构。
 *
 * <p>前端「学习趋势」按日期聚合，日期字段的序列化格式一旦变化就会导致图表全为 0，
 * 因此这里把它固定成契约来测。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:study_stats;"
                + "MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "jwt.secret=UnitTestSecretKeyForJwtSecurity2026!!",
        "app.upload-dir=${java.io.tmpdir}/study-assisiant-stats-test",
        "app.bootstrap-admin=true",
        "app.admin-username=statsadmin",
        "app.admin-password=StatsAdmin123456"
})
class StatisticsPayloadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private StudyPlanMapper planMapper;

    @Autowired
    private StudyTaskMapper taskMapper;

    private Long userId;
    private LocalDate today;

    @BeforeEach
    void prepareData() {
        userId = sysUserMapper.findByUsername("statsadmin").getId();
        today = LocalDate.now();

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setTitle("统计测试计划");
        plan.setStatus("进行中");
        planMapper.insert(plan);

        StudyTask done = new StudyTask();
        done.setPlanId(plan.getId());
        done.setTitle("已完成任务");
        done.setTaskDate(today);
        done.setEstimatedMinutes(60);
        done.setStatus("DONE");
        taskMapper.insert(done);
        taskMapper.updateStatus(done.getId(), userId, "DONE", 90,
                LocalDateTime.now());

        StudyTask todo = new StudyTask();
        todo.setPlanId(plan.getId());
        todo.setTitle("未完成任务");
        todo.setTaskDate(today);
        todo.setEstimatedMinutes(30);
        todo.setStatus("TODO");
        taskMapper.insert(todo);
    }

    private String adminToken() throws Exception {
        JSONObject body = JSONObject.parseObject(
                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"username":"statsadmin",
                                         "password":"StatsAdmin123456"}
                                        """))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());
        return body.getJSONObject("data").getString("token");
    }

    @Test
    void dailyStatsExposeIsoDateKeysAndNumbers() throws Exception {
        String raw = mockMvc.perform(get("/api/statistics/daily")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        System.out.println("[统计接口返回] " + raw);

        JSONArray data = JSONObject.parseObject(raw).getJSONArray("data");
        assertNotNull(data, "daily 接口应当返回数组");
        assertTrue(data.size() >= 1, "应当至少有一天的统计");

        JSONObject first = data.getJSONObject(0);
        assertTrue(first.containsKey("taskDate"), "必须包含 taskDate 字段");
        assertTrue(first.containsKey("total"));
        assertTrue(first.containsKey("done"));
        assertTrue(first.containsKey("minutes"));

        String dateValue = String.valueOf(first.get("taskDate"));
        assertEquals(today.toString(), dateValue.substring(0, 10),
                "taskDate 前 10 位必须是 yyyy-MM-dd");
        assertEquals(2, first.getIntValue("total"));
        assertEquals(1, first.getIntValue("done"));
        assertEquals(90, first.getIntValue("minutes"));
    }
}
