package com.study.assisiant.mapper;


import com.study.assisiant.StudyPlan;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface StudyPlanMapper {


    /**
     * 新增学习计划
     */
    @Insert("""
        INSERT INTO study_plan
        (
            user_id,
            title,
            goal,
            content,
            start_date,
            end_date,
            daily_minutes,
            status
        )
        VALUES
        (
            #{userId},
            #{title},
            #{goal},
            #{content},
            #{startDate},
            #{endDate},
            #{dailyMinutes},
            #{status}
        )
        """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id"
    )
    int insert(StudyPlan studyPlan);



    /**
     * 查询当前用户的学习计划
     */
    @Select("""
            SELECT *
            FROM study_plan
            WHERE user_id = #{userId}
            ORDER BY create_time DESC
            """)
    List<StudyPlan> findByUserId(Long userId);



    /**
     * 根据id查询
     */
    @Select("""
            SELECT *
            FROM study_plan
            WHERE id = #{id}
            """)
    StudyPlan findById(Long id);



    /**
     * 修改计划
     */
    @Update("""
UPDATE study_plan
SET
    title=#{title},
    content=#{content},
    start_date=#{startDate},
    end_date=#{endDate},
    status=#{status}
WHERE id=#{id}
AND user_id=#{userId}
""")
    int update(StudyPlan studyPlan);



    /**
     * 删除计划
     */
    @Delete("""
        DELETE FROM study_plan
        WHERE id=#{id}
        AND user_id=#{userId}
        """)
    int delete(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

}


