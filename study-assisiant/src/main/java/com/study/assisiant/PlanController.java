package com.study.assisiant;

import com.study.assisiant.service.StudyPlanService;
import com.study.assisiant.task.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    @Autowired
    private StudyPlanService studyPlanService;

    @Autowired
    private TaskService taskService;

    @PostMapping("/generate")
    public Result generate(
            @RequestBody PlanGenerateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return Result.success(
                studyPlanService.generatePlan(userId, request));
    }

    @GetMapping
    public Result list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(studyPlanService.findByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result detail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        StudyPlan plan = studyPlanService.findDetail(id, userId);
        if (plan == null) {
            return Result.error(403, "无权限查看");
        }
        return Result.success(plan);
    }

    @GetMapping("/{id}/tasks")
    public Result tasks(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        StudyPlan plan = studyPlanService.findDetail(id, userId);
        if (plan == null) {
            return Result.error(403, "无权限查看");
        }
        return Result.success(taskService.listByPlan(userId, id));
    }
}
