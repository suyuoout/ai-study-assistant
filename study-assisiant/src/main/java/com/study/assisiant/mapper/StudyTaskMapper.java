package com.study.assisiant.mapper;

import com.study.assisiant.task.StudyTask;
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
        WHERE p.user_id = #{userId} AND t.plan_id = #{planId}
        ORDER BY t.task_date, t.id
        """)
    List<StudyTask> findByPlan(
            @Param("userId") Long userId,
            @Param("planId") Long planId);

    @Insert("""
        INSERT INTO study_task
        (plan_id, title, description, task_date, estimated_minutes, status)
        VALUES
        (#{planId}, #{title}, #{description}, #{taskDate}, #{estimatedMinutes}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyTask task);

    @Update("""
        UPDATE study_task
        SET title = #{title},
            description = #{description},
            task_date = #{taskDate},
            estimated_minutes = #{estimatedMinutes}
        WHERE id = #{id}
        """)
    int update(StudyTask task);

    @Update("""
        UPDATE study_task
        SET status = #{status},
            actual_minutes = #{actualMinutes},
            completed_time = #{completedTime}
        WHERE id = #{id}
        """)
    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("actualMinutes") Integer actualMinutes,
            @Param("completedTime") LocalDateTime completedTime);

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
