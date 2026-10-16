package com.study.assisiant.plan;

import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.Result;
import com.study.assisiant.task.TaskService;
import com.study.assisiant.task.StudyTask;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final StudyPlanService planService;
    private final TaskService taskService;

    public PlanController(StudyPlanService planService,
                          TaskService taskService) {
        this.planService = planService;
        this.taskService = taskService;
    }

    /** AI 生成学习计划 */
    @PostMapping("/generate")
    public Result<Object> generate(
            @Valid @RequestBody PlanGenerateRequest request,
            HttpServletRequest httpRequest) {

        AiCallContext ctx = AiCallContext.of(httpRequest, "PLAN");
        return Result.success("生成成功",
                planService.generatePlan(request, ctx));
    }

    @GetMapping
    public Result<List<StudyPlan>> list(HttpServletRequest request) {
        return Result.success(planService.findByUserId(userId(request)));
    }

    @GetMapping("/{id}")
    public Result<StudyPlan> detail(@PathVariable Long id,
                                    HttpServletRequest request) {
        return Result.success(
                planService.requireOwned(userId(request), id));
    }

    @GetMapping("/{id}/tasks")
    public Result<List<StudyTask>> tasks(@PathVariable Long id,
                                         HttpServletRequest request) {
        Long userId = userId(request);
        planService.requireOwned(userId, id);
        return Result.success(taskService.listByPlan(userId, id));
    }

    /** 重命名计划 / 修改状态 */
    @PutMapping("/{id}")
    public Result<StudyPlan> update(
            @PathVariable Long id,
            @Valid @RequestBody PlanUpdateRequest body,
            HttpServletRequest request) {

        return Result.success("修改成功",
                planService.updatePlan(userId(request), id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        planService.deletePlan(userId(request), id);
        return Result.success("删除成功", null);
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
