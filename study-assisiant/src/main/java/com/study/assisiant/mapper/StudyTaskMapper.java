package com.study.assisiant.mapper;

import com.study.assisiant.task.StudyTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudyTaskMapper {

    @Select("""
        SELECT t.* FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE t.id = #{id} AND p.user_id = #{userId}
        """)
    StudyTask findById(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
        SELECT t.* FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId} AND t.task_date = #{date}
        ORDER BY t.id
        """)
    List<StudyTask> findByDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    @Select("""
        SELECT t.* FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
          AND t.task_date BETWEEN #{from} AND #{to}
        ORDER BY t.task_date, t.id
        """)
    List<StudyTask> findByDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Select("""
        SELECT t.* FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId} AND t.plan_id = #{planId}
        ORDER BY t.task_date, t.id
        """)
    List<StudyTask> findByPlan(
            @Param("userId") Long userId,
            @Param("planId") Long planId);

    /** 待办清单：未来若干天未完成的任务 */
    @Select("""
        SELECT t.* FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
          AND t.status <> 'DONE'
          AND t.task_date <= #{until}
        ORDER BY t.task_date, t.id
        LIMIT #{limit}
        """)
    List<StudyTask> findUpcoming(
            @Param("userId") Long userId,
            @Param("until") LocalDate until,
            @Param("limit") int limit);

    @Insert("""
        INSERT INTO study_task
        (plan_id, title, description, task_date, estimated_minutes, status)
        VALUES
        (#{planId}, #{title}, #{description}, #{taskDate},
         #{estimatedMinutes}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyTask task);

    /** 更新时带上用户归属条件，避免越权改他人任务 */
    @Update("""
        UPDATE study_task
        SET title = #{task.title},
            description = #{task.description},
            task_date = #{task.taskDate},
            estimated_minutes = #{task.estimatedMinutes}
        WHERE id = #{task.id}
          AND plan_id IN (SELECT id FROM study_plan WHERE user_id = #{userId})
        """)
    int update(@Param("task") StudyTask task,
               @Param("userId") Long userId);

    @Update("""
        UPDATE study_task
        SET status = #{status},
            actual_minutes = #{actualMinutes},
            completed_time = #{completedTime}
        WHERE id = #{id}
          AND plan_id IN (SELECT id FROM study_plan WHERE user_id = #{userId})
        """)
    int updateStatus(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("actualMinutes") Integer actualMinutes,
            @Param("completedTime") LocalDateTime completedTime);

    @Delete("""
        DELETE FROM study_task
        WHERE id = #{id}
          AND plan_id IN (SELECT id FROM study_plan WHERE user_id = #{userId})
        """)
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*) FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
        """)
    Long countByUser(Long userId);

    @Select("""
        SELECT COUNT(*) FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId} AND t.status = 'DONE'
        """)
    Long countDone(Long userId);

    @Select("""
        SELECT COALESCE(SUM(t.actual_minutes), 0) FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
        """)
    Long sumActualMinutes(Long userId);

    @Select("""
        SELECT COALESCE(SUM(t.estimated_minutes), 0) FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
        """)
    Long sumEstimatedMinutes(Long userId);

    /** 指定区间的完成情况（用于周报/月报） */
    @Select("""
        SELECT COUNT(*) AS total,
               SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS done,
               COALESCE(SUM(t.actual_minutes), 0) AS minutes
        FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
          AND t.task_date BETWEEN #{from} AND #{to}
        """)
    Map<String, Object> rangeSummary(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Select("""
        SELECT t.task_date AS "taskDate",
               COUNT(*) AS total,
               SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) AS done,
               COALESCE(SUM(t.actual_minutes), 0) AS minutes
        FROM study_task t
        JOIN study_plan p ON t.plan_id = p.id
        WHERE p.user_id = #{userId}
        GROUP BY t.task_date
        ORDER BY t.task_date
        """)
    List<Map<String, Object>> dailyStats(Long userId);
}
