package com.study.assisiant.mapper;

import com.study.assisiant.plan.StudyPlan;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StudyPlanMapper {

    @Insert("""
        INSERT INTO study_plan
        (user_id, title, goal, content, start_date, end_date,
         daily_minutes, status)
        VALUES
        (#{userId}, #{title}, #{goal}, #{content}, #{startDate}, #{endDate},
         #{dailyMinutes}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyPlan plan);

    @Select("""
        SELECT * FROM study_plan
        WHERE user_id = #{userId}
        ORDER BY id DESC
        """)
    List<StudyPlan> findByUserId(Long userId);

    @Select("SELECT * FROM study_plan WHERE id = #{id}")
    StudyPlan findById(Long id);

    @Select("""
        SELECT * FROM study_plan
        WHERE id = #{id} AND user_id = #{userId}
        """)
    StudyPlan findByIdAndUser(@Param("id") Long id,
                              @Param("userId") Long userId);

    /** 只允许改自己的计划：条件里带 user_id */
    @Update("""
        UPDATE study_plan
        SET title = #{title}, status = #{status}
        WHERE id = #{id} AND user_id = #{userId}
        """)
    int updateTitle(@Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("title") String title,
                    @Param("status") String status);

    @Delete("""
        DELETE FROM study_plan
        WHERE id = #{id} AND user_id = #{userId}
        """)
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM study_plan WHERE user_id = #{userId}")
    long countByUser(Long userId);
}
