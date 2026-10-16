package com.study.assisiant.task;

import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 任务列表：支持按单日（date）或区间（from/to）查询。
     */
    @GetMapping
    public Result<List<StudyTask>> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long planId,
            HttpServletRequest request) {

        Long userId = userId(request);
        if (planId != null) {
            return Result.success(taskService.listByPlan(userId, planId));
        }
        if (from != null || to != null) {
            return Result.success(
                    taskService.listByDateRange(userId, from, to));
        }
        return Result.success(taskService.listByDate(userId,
                date == null ? LocalDate.now() : date));
    }

    /** 待办：今天及之前累计未完成的任务 */
    @GetMapping("/upcoming")
    public Result<List<StudyTask>> upcoming(
            @RequestParam(defaultValue = "0") int days,
            HttpServletRequest request) {
        return Result.success(
                taskService.listUpcoming(userId(request), days));
    }

    @PostMapping
    public Result<StudyTask> create(
            @Valid @RequestBody TaskCreateRequest body,
            HttpServletRequest request) {
        return Result.success("创建成功",
                taskService.create(userId(request), body));
    }

    @PutMapping("/{id}")
    public Result<StudyTask> update(
            @PathVariable Long id,
            @RequestBody StudyTask task,
            HttpServletRequest request) {
        return Result.success("修改成功",
                taskService.update(userId(request), id, task));
    }

    @PatchMapping("/{id}/status")
    public Result<StudyTask> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest body,
            HttpServletRequest request) {
        return Result.success("已更新",
                taskService.updateStatus(userId(request), id,
                        body.status(), body.actualMinutes()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        taskService.delete(userId(request), id);
        return Result.success("删除成功", null);
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
