package com.study.assisiant.service.impl;


import com.study.assisiant.ai.AIRequest;
import com.study.assisiant.ai.AIService;
import com.study.assisiant.StudyPlan;
import com.study.assisiant.PlanGenerateRequest;
import com.study.assisiant.mapper.StudyTaskMapper;
import com.study.assisiant.mapper.StudyPlanMapper;
import com.study.assisiant.service.StudyPlanService;
import com.study.assisiant.task.StudyTask;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;



@Service
public class StudyPlanServiceImpl
        implements StudyPlanService {

    @Override
    public List<StudyPlan> findMyPlans(Long userId){

        return studyPlanMapper.findByUserId(userId);

    }


    @Autowired
    private StudyPlanMapper studyPlanMapper;

    @Autowired
    private AIService aiService;

    @Autowired
    private StudyTaskMapper studyTaskMapper;

    @Override
    public boolean deletePlan(Long id, Long userId){


        int result =
                studyPlanMapper.delete(id,userId);


        return result > 0;

    }

    @Override
    public List<StudyPlan> findByUserId(Long userId){

        return studyPlanMapper.findByUserId(userId);

    }

    @Override
    public StudyPlan findDetail(Long id,Long userId){

        StudyPlan plan =
                studyPlanMapper.findById(id);


        if(plan==null){
            return null;
        }


        if(!plan.getUserId().equals(userId)){
            return null;
        }


        return plan;
    }



    @Override
    public Long saveAIPlan(
            Long userId,
            AIRequest request,
            String content
    ){


        StudyPlan plan =
                new StudyPlan();


        // 当前登录用户
        plan.setUserId(userId);



        // 标题
        plan.setTitle(
                request.getGoal()
                        +"学习计划"
        );



        // AI生成内容
        plan.setContent(content);



        // 状态
        plan.setStatus("进行中");



        studyPlanMapper.insert(plan);



        return plan.getId();

    }

    @Override
    public Map<String, Object> generatePlan(
            Long userId,
            PlanGenerateRequest req
    ){

        validateRequest(req);

        String raw;
        try {
            raw = aiService.chatWithSystem(
                    "你是学习计划生成助手，只输出合法的 JSON，不要输出任何解释。",
                    buildPlanPrompt(req));
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI生成计划失败，请稍后重试");
        }

        JSONObject json = parseJson(raw);
        if (json == null
                || json.getJSONArray("tasks") == null
                || json.getJSONArray("tasks").isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI返回的计划格式不正确");
        }

        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        String title = json.getString("title");
        plan.setTitle(
                (title != null && !title.isBlank())
                        ? title
                        : req.getGoal() + "学习计划");
        plan.setGoal(req.getGoal());
        plan.setStartDate(req.getStartDate());
        plan.setEndDate(req.getEndDate());
        plan.setDailyMinutes(req.getDailyMinutes());
        plan.setContent(raw);
        plan.setStatus("进行中");
        studyPlanMapper.insert(plan);

        JSONArray tasks = json.getJSONArray("tasks");
        int saved = 0;
        for (int i = 0; i < tasks.size() && saved < 200; i++) {
            JSONObject t = tasks.getJSONObject(i);
            if (t == null) {
                continue;
            }
            String taskTitle = t.getString("title");
            if (taskTitle == null || taskTitle.isBlank()) {
                continue;
            }

            LocalDate taskDate = parseDate(t, req.getStartDate());
            if (taskDate.isBefore(req.getStartDate())
                    || taskDate.isAfter(req.getEndDate())) {
                taskDate = req.getStartDate();
            }
            Integer estimated = t.getInteger("estimatedMinutes");
            if (estimated == null || estimated <= 0) {
                estimated = 60;
            }

            StudyTask task = new StudyTask();
            task.setPlanId(plan.getId());
            task.setTitle(taskTitle);
            task.setDescription(t.getString("description"));
            task.setTaskDate(taskDate);
            task.setEstimatedMinutes(estimated);
            task.setStatus("TODO");
            studyTaskMapper.insert(task);
            saved++;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("plan", plan);
        data.put("taskCount", saved);
        return data;
    }

    private void validateRequest(PlanGenerateRequest req) {
        if (req.getGoal() == null || req.getGoal().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "学习目标不能为空");
        }
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "请填写开始和截止日期");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "截止日期不能早于开始日期");
        }
        if (req.getDailyMinutes() == null || req.getDailyMinutes() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "每日可用时间必须大于0");
        }
    }

    private String buildPlanPrompt(PlanGenerateRequest req) {
        String days = (req.getAvailableDays() == null
                || req.getAvailableDays().isEmpty())
                ? "每天"
                : String.join(",", req.getAvailableDays());

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

                要求：任务日期必须在开始和截止日期之间；单个任务预计用时不超过每日可用分钟；任务数量合理。
                """.formatted(
                req.getGoal(),
                req.getCurrentLevel(),
                req.getStartDate(),
                req.getEndDate(),
                req.getDailyMinutes(),
                days);
    }

    private JSONObject parseJson(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end < start) {
            return null;
        }
        try {
            return JSONObject.parseObject(s.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDate(JSONObject t, LocalDate fallback) {
        try {
            Object d = t.get("taskDate");
            if (d != null) {
                String text = d.toString();
                if (text.length() >= 10) {
                    return LocalDate.parse(text.substring(0, 10));
                }
            }
        } catch (Exception e) {
            // 忽略日期解析失败
        }
        return fallback;
    }

}
