package com.study.assisiant.task;

import com.study.assisiant.mapper.StudyTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    @Autowired
    private StudyTaskMapper taskMapper;

    public List<StudyTask> listByDate(Long userId, LocalDate date) {
        return taskMapper.findByDate(userId, date);
    }

    public List<StudyTask> listByPlan(Long userId, Long planId) {
        return taskMapper.findByPlan(userId, planId);
    }

    public StudyTask update(Long userId, Long id, StudyTask task) {
        StudyTask existing = requireOwned(userId, id);
        task.setId(existing.getId());
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "任务标题不能为空");
        }
        taskMapper.update(task);
        return taskMapper.findById(id, userId);
    }

    public StudyTask updateStatus(
            Long userId, Long id, String status, Integer actualMinutes) {
        StudyTask task = requireOwned(userId, id);

        if (!"TODO".equals(status)
                && !"DOING".equals(status)
                && !"DONE".equals(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "无效的任务状态");
        }

        LocalDateTime completedTime =
                "DONE".equals(status) ? LocalDateTime.now() : null;
        if (!"DONE".equals(status)) {
            actualMinutes = null;
        }

        taskMapper.updateStatus(id, status, actualMinutes, completedTime);

        task.setStatus(status);
        task.setActualMinutes(actualMinutes);
        task.setCompletedTime(completedTime);
        return task;
    }

    public Map<String, Object> overview(Long userId) {
        Long total = taskMapper.countByUser(userId);
        Long done = taskMapper.countDone(userId);
        Long actual = taskMapper.sumActualMinutes(userId);
        Long estimated = taskMapper.sumEstimatedMinutes(userId);

        double rate = (total != null && total > 0)
                ? done.doubleValue() / total.doubleValue() : 0.0;

        Map<String, Object> data = new HashMap<>();
        data.put("totalTasks", total);
        data.put("doneTasks", done);
        data.put("completionRate", rate);
        data.put("actualMinutes", actual);
        data.put("estimatedMinutes", estimated);
        return data;
    }

    public List<Map<String, Object>> daily(Long userId) {
        return taskMapper.dailyStats(userId);
    }

    private StudyTask requireOwned(Long userId, Long id) {
        StudyTask task = taskMapper.findById(id, userId);
        if (task == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限操作该任务");
        }
        return task;
    }
}
