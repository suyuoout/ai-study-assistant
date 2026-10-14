package com.study.assisiant.task;

import com.study.assisiant.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/overview")
    public Result overview(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(taskService.overview(userId));
    }

    @GetMapping("/daily")
    public Result daily(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(taskService.daily(userId));
    }
}
