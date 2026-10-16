package com.study.assisiant.task;

import com.study.assisiant.ai.AIService;
import com.study.assisiant.ai.AiCallContext;
import com.study.assisiant.mapper.StudyTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习报告：把区间内的任务数据交给 AI 汇总成一段可读的复盘。
 */
@Service
public class StudyReportService {

    private final StudyTaskMapper taskMapper;
    private final AIService aiService;

    public StudyReportService(StudyTaskMapper taskMapper,
                              AIService aiService) {
        this.taskMapper = taskMapper;
        this.aiService = aiService;
    }

    public Map<String, Object> report(Long userId, int days,
                                      AiCallContext ctx) {

        int span = Math.max(1, Math.min(days, 90));
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(span - 1L);

        List<StudyTask> tasks = taskMapper.findByDateRange(userId, from, to);
        Map<String, Object> summary = buildSummary(tasks);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("from", from);
        data.put("to", to);
        data.put("summary", summary);

        long total = ((Number) summary.get("total")).longValue();
        if (total == 0) {
            data.put("report", "这段时间还没有任务记录，"
                    + "先在「学习计划」里生成一份计划，"
                    + "把目标拆成每天能做完的小任务吧。");
            data.put("aiGenerated", false);
            return data;
        }

        String report = aiService.chatWithSystem(
                "你是一名耐心、具体的学业教练，用中文输出简洁的学习复盘，"
                        + "不要使用 Markdown 表格，控制在 300 字以内。",
                buildPrompt(from, to, summary),
                ctx);

        data.put("report", report);
        data.put("aiGenerated", true);
        return data;
    }

    private Map<String, Object> buildSummary(List<StudyTask> tasks) {
        int total = tasks.size();
        int done = 0;
        int minutes = 0;
        Map<String, Integer> perDay = new LinkedHashMap<>();
        List<String> unfinished = new ArrayList<>();

        for (StudyTask task : tasks) {
            if ("DONE".equals(task.getStatus())) {
                done++;
                if (task.getActualMinutes() != null) {
                    minutes += task.getActualMinutes();
                }
            } else if (unfinished.size() < 10) {
                unfinished.add((task.getTaskDate() == null ? ""
                        : task.getTaskDate() + " ") + task.getTitle());
            }
            perDay.merge(String.valueOf(task.getTaskDate()), 1,
                    Integer::sum);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", total);
        summary.put("done", done);
        summary.put("minutes", minutes);
        summary.put("completionRate",
                total == 0 ? 0.0
                        : Math.round(done * 1000.0 / total) / 10.0);
        summary.put("perDay", perDay);
        summary.put("unfinished", unfinished);
        return summary;
    }

    @SuppressWarnings("unchecked")
    private String buildPrompt(LocalDate from, LocalDate to,
                               Map<String, Object> summary) {
        Map<String, Integer> perDay =
                (Map<String, Integer>) summary.get("perDay");
        List<String> unfinished =
                (List<String>) summary.get("unfinished");

        StringBuilder days = new StringBuilder();
        perDay.forEach((day, count) -> days.append(day).append("：")
                .append(count).append(" 个任务\n"));

        return """
                请根据以下真实数据写一段学习复盘（中文，300 字以内）：

                统计区间：%s ~ %s
                任务总数：%s
                已完成：%s
                完成率：%s%%
                有效学习时长：%s 分钟

                每日任务量：
                %s
                尚未完成的任务（最多10条）：
                %s

                要求：先肯定做得好的地方，再指出可改进之处，
                最后给 2 条具体、可执行的建议。
                只依据上面的数据，不要编造没有的信息。
                """.formatted(
                from, to,
                summary.get("total"), summary.get("done"),
                summary.get("completionRate"), summary.get("minutes"),
                days.toString(),
                unfinished.isEmpty()
                        ? "（无）" : String.join("\n", unfinished));
    }
}
