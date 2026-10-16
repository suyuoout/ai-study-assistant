package com.study.assisiant.task;

import com.study.assisiant.mapper.StudyPlanMapper;
import com.study.assisiant.mapper.StudyTaskMapper;
import com.study.assisiant.plan.StudyPlan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private static final int MAX_UPCOMING = 100;

    private final StudyTaskMapper taskMapper;
    private final StudyPlanMapper planMapper;

    public TaskService(StudyTaskMapper taskMapper,
                       StudyPlanMapper planMapper) {
        this.taskMapper = taskMapper;
        this.planMapper = planMapper;
    }

    public List<StudyTask> listByDate(Long userId, LocalDate date) {
        return taskMapper.findByDate(userId, date);
    }

    public List<StudyTask> listByDateRange(Long userId, LocalDate from,
                                           LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "请提供 from 与 to 两个日期");
        }
        if (to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "结束日期不能早于开始日期");
        }
        if (from.plusDays(400).isBefore(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "查询区间过大，请控制在 400 天以内");
        }
        return taskMapper.findByDateRange(userId, from, to);
    }

    public List<StudyTask> listByPlan(Long userId, Long planId) {
        return taskMapper.findByPlan(userId, planId);
    }

    /** 待办清单：今天及之前未完成的任务 */
    public List<StudyTask> listUpcoming(Long userId, int days) {
        int span = Math.max(0, Math.min(days, 90));
        return taskMapper.findUpcoming(userId,
                LocalDate.now().plusDays(span), MAX_UPCOMING);
    }

    @Transactional
    public StudyTask create(Long userId, TaskCreateRequest request) {
        StudyPlan plan = planMapper.findByIdAndUser(
                request.planId(), userId);
        if (plan == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "无权限在该计划下创建任务");
        }

        LocalDate date = request.taskDate() == null
                ? LocalDate.now() : request.taskDate();
        if (plan.getStartDate() != null && date.isBefore(plan.getStartDate())) {
            date = plan.getStartDate();
        }
        if (plan.getEndDate() != null && date.isAfter(plan.getEndDate())) {
            date = plan.getEndDate();
        }

        StudyTask task = new StudyTask();
        task.setPlanId(plan.getId());
        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setTaskDate(date);
        task.setEstimatedMinutes(request.estimatedMinutes() == null
                ? defaultMinutes(plan) : request.estimatedMinutes());
        task.setStatus("TODO");
        taskMapper.insert(task);
        return task;
    }

    public StudyTask update(Long userId, Long id, StudyTask task) {
        StudyTask existing = requireOwned(userId, id);

        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "任务标题不能为空");
        }
        if (task.getEstimatedMinutes() != null
                && (task.getEstimatedMinutes() < 0
                || task.getEstimatedMinutes() > 1440)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "预计用时需在 0-1440 分钟之间");
        }

        existing.setTitle(task.getTitle().trim());
        existing.setDescription(task.getDescription());
        if (task.getTaskDate() != null) {
            existing.setTaskDate(task.getTaskDate());
        }
        if (task.getEstimatedMinutes() != null) {
            existing.setEstimatedMinutes(task.getEstimatedMinutes());
        }

        taskMapper.update(existing, userId);
        return taskMapper.findById(id, userId);
    }

    public StudyTask updateStatus(Long userId, Long id, String status,
                                  Integer actualMinutes) {
        StudyTask task = requireOwned(userId, id);

        LocalDateTime completedTime =
                "DONE".equals(status) ? LocalDateTime.now() : null;
        Integer minutes = "DONE".equals(status) ? actualMinutes : null;

        taskMapper.updateStatus(id, userId, status, minutes, completedTime);

        task.setStatus(status);
        task.setActualMinutes(minutes);
        task.setCompletedTime(completedTime);
        return task;
    }

    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        taskMapper.delete(id, userId);
    }

    public Map<String, Object> overview(Long userId) {
        Long total = taskMapper.countByUser(userId);
        Long done = taskMapper.countDone(userId);
        Long actual = taskMapper.sumActualMinutes(userId);
        Long estimated = taskMapper.sumEstimatedMinutes(userId);

        long totalValue = total == null ? 0 : total;
        long doneValue = done == null ? 0 : done;
        double rate = totalValue > 0
                ? (double) doneValue / totalValue : 0.0;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalTasks", totalValue);
        data.put("doneTasks", doneValue);
        data.put("completionRate", rate);
        data.put("actualMinutes", actual == null ? 0 : actual);
        data.put("estimatedMinutes", estimated == null ? 0 : estimated);
        return data;
    }

    public List<Map<String, Object>> daily(Long userId) {
        return taskMapper.dailyStats(userId);
    }

    private int defaultMinutes(StudyPlan plan) {
        Integer daily = plan.getDailyMinutes();
        return daily == null || daily <= 0 ? 60 : daily;
    }

    private StudyTask requireOwned(Long userId, Long id) {
        StudyTask task = taskMapper.findById(id, userId);
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "无权限操作该任务");
        }
        return task;
    }
}
