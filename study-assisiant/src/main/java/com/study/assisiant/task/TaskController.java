package com.study.assisiant.task;

import com.study.assisiant.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping
    public Result list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(taskService.listByDate(userId, date));
    }

    @PutMapping("/{id}")
    public Result update(
            @PathVariable Long id,
            @RequestBody StudyTask task,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(taskService.update(userId, id, task));
    }

    @PatchMapping("/{id}/status")
    public Result updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest body,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(taskService.updateStatus(
                userId, id, body.getStatus(), body.getActualMinutes()));
    }
}
