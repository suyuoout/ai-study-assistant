package com.study.assisiant.plan;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.study.assisiant.ai.AIService;
import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.mapper.StudyPlanMapper;
import com.study.assisiant.mapper.StudyTaskMapper;
import com.study.assisiant.task.StudyTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudyPlanServiceImpl implements StudyPlanService {

    private static final Logger log =
            LoggerFactory.getLogger(StudyPlanServiceImpl.class);

    /** 计划最长跨度，避免生成上千条任务 */
    private static final long MAX_PLAN_DAYS = 366;
    /** 单个计划最多保存的任务数 */
    private static final int MAX_TASKS = 200;

    private static final Set<String> ALLOWED_STATUS =
            Set.of("进行中", "已完成", "已归档");

    private final StudyPlanMapper planMapper;
    private final StudyTaskMapper taskMapper;
    private final AIService aiService;

    public StudyPlanServiceImpl(StudyPlanMapper planMapper,
                                StudyTaskMapper taskMapper,
                                AIService aiService) {
        this.planMapper = planMapper;
        this.taskMapper = taskMapper;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public Map<String, Object> generatePlan(PlanGenerateRequest request,
                                            AiCallContext ctx) {

        validate(request);

        String raw = aiService.chatJson(
                "你是学习计划生成助手，只输出合法的 JSON，不要输出任何解释。",
                buildPlanPrompt(request), ctx);

        JSONObject json = parseJson(raw);
        if (json == null || json.getJSONArray("tasks") == null
                || json.getJSONArray("tasks").isEmpty()) {
            log.warn("AI 返回的计划无法解析: {}",
                    raw == null ? "null" : raw.substring(0,
                            Math.min(300, raw.length())));
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI 返回的计划格式不正确，请重试");
        }

        StudyPlan plan = new StudyPlan();
        plan.setUserId(ctx.userId());
        String title = json.getString("title");
        plan.setTitle(title != null && !title.isBlank()
                ? trimTo(title, 200)
                : trimTo(request.goal() + "学习计划", 200));
        plan.setGoal(request.goal());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        plan.setDailyMinutes(request.dailyMinutes());
        plan.setContent(raw);
        plan.setStatus("进行中");
        planMapper.insert(plan);

        JSONArray tasks = json.getJSONArray("tasks");
        int saved = 0;
        for (int i = 0; i < tasks.size() && saved < MAX_TASKS; i++) {
            JSONObject item = tasks.getJSONObject(i);
            if (item == null) {
                continue;
            }
            String taskTitle = item.getString("title");
            if (taskTitle == null || taskTitle.isBlank()) {
                continue;
            }

            LocalDate taskDate = parseDate(item, request.startDate());
            if (taskDate.isBefore(request.startDate())
                    || taskDate.isAfter(request.endDate())) {
                taskDate = request.startDate();
            }
            Integer estimated = item.getInteger("estimatedMinutes");
            if (estimated == null || estimated <= 0) {
                estimated = request.dailyMinutes();
            }
            estimated = Math.min(estimated, request.dailyMinutes());

            StudyTask task = new StudyTask();
            task.setPlanId(plan.getId());
            task.setTitle(trimTo(taskTitle, 200));
            task.setDescription(item.getString("description"));
            task.setTaskDate(taskDate);
            task.setEstimatedMinutes(estimated);
            task.setStatus("TODO");
            taskMapper.insert(task);
            saved++;
        }

        return Map.of("plan", plan, "taskCount", saved);
    }

    @Override
    public List<StudyPlan> findByUserId(Long userId) {
        return planMapper.findByUserId(userId);
    }

    @Override
    public StudyPlan requireOwned(Long userId, Long planId) {
        StudyPlan plan = planMapper.findByIdAndUser(planId, userId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "无权限访问该学习计划");
        }
        return plan;
    }

    @Override
    public StudyPlan updatePlan(Long userId, Long planId,
                               PlanUpdateRequest request) {
        StudyPlan plan = requireOwned(userId, planId);

        String title = request.title() == null || request.title().isBlank()
                ? plan.getTitle() : request.title().trim();
        String status = request.status() == null || request.status().isBlank()
                ? plan.getStatus() : request.status().trim();

        if (!ALLOWED_STATUS.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "计划状态只能是：进行中 / 已完成 / 已归档");
        }

        planMapper.updateTitle(planId, userId, trimTo(title, 200), status);
        return planMapper.findByIdAndUser(planId, userId);
    }

    @Override
    @Transactional
    public void deletePlan(Long userId, Long planId) {
        requireOwned(userId, planId);
        // study_task 有 ON DELETE CASCADE，会一起清理
        planMapper.delete(planId, userId);
    }

    private void validate(PlanGenerateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "截止日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(
                request.startDate(), request.endDate()) + 1;
        if (days > MAX_PLAN_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "计划周期过长，请控制在 " + MAX_PLAN_DAYS + " 天以内");
        }
    }

    private String buildPlanPrompt(PlanGenerateRequest request) {
        String days = (request.availableDays() == null
                || request.availableDays().isEmpty())
                ? "每天"
                : String.join(",", request.availableDays());
        String level = (request.currentLevel() == null
                || request.currentLevel().isBlank())
                ? "未填写" : request.currentLevel();

        return """
                请根据以下信息生成一份结构化学习计划，输出 JSON，格式如下：
                {
                  "title": "计划标题",
                  "tasks": [
                    {"title":"任务标题","taskDate":"yyyy-MM-dd","estimatedMinutes":60,"description":"任务说明"}
                  ]
                }

                学习目标：%s
                当前水平：%s
                开始日期：%s
                截止日期：%s
                每日可用分钟：%d
                可学习日：%s

                要求：任务日期必须在开始和截止日期之间；单个任务预计用时不超过每日可用分钟；
                任务数量合理（不超过 %d 条），title 简洁明确，description 写清具体做什么。
                """.formatted(
                request.goal(),
                level,
                request.startDate(),
                request.endDate(),
                request.dailyMinutes(),
                days,
                MAX_TASKS);
    }

    private JSONObject parseJson(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        // 兼容模型偶尔包裹 ```json 代码块的情况
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            return null;
        }
        try {
            return JSONObject.parseObject(text.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(JSONObject task, LocalDate fallback) {
        try {
            Object value = task.get("taskDate");
            if (value != null) {
                String text = value.toString().trim();
                if (text.length() >= 10) {
                    return LocalDate.parse(text.substring(0, 10));
                }
            }
        } catch (Exception e) {
            log.debug("任务日期解析失败，使用计划开始日期兜底");
        }
        return fallback;
    }

    private String trimTo(String text, int max) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.length() > max ? value.substring(0, max) : value;
    }
}
