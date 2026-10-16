package com.study.assisiant.task;

import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final TaskService taskService;
    private final StudyReportService reportService;

    public StatisticsController(TaskService taskService,
                                StudyReportService reportService) {
        this.taskService = taskService;
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(
            HttpServletRequest request) {
        return Result.success(taskService.overview(userId(request)));
    }

    @GetMapping("/daily")
    public Result<List<Map<String, Object>>> daily(
            HttpServletRequest request) {
        return Result.success(taskService.daily(userId(request)));
    }

    /** AI 学习复盘报告 */
    @GetMapping("/report")
    public Result<Map<String, Object>> report(
            @RequestParam(defaultValue = "7") int days,
            HttpServletRequest request) {
        return Result.success(reportService.report(userId(request), days,
                AiCallContext.of(request, "REPORT")));
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
