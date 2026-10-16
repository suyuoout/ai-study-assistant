package com.study.assisiant.mapper;

import com.study.assisiant.ai.AiUsage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiUsageMapper {

    @Insert("""
        INSERT INTO ai_usage
        (user_id, kind, model, prompt_tokens, completion_tokens, success, own_key)
        VALUES
        (#{userId}, #{kind}, #{model}, #{promptTokens}, #{completionTokens},
         #{success}, #{ownKey})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiUsage usage);

    @Select("""
        SELECT COUNT(*) FROM ai_usage
        WHERE user_id = #{userId}
          AND own_key = #{ownKey}
          AND success = TRUE
          AND create_time >= #{from}
        """)
    long countSince(@Param("userId") Long userId,
                    @Param("ownKey") boolean ownKey,
                    @Param("from") LocalDateTime from);

    @Select("""
        SELECT COUNT(*) FROM ai_usage
        WHERE user_id = #{userId}
          AND create_time >= #{from}
        """)
    long countAllSince(@Param("userId") Long userId,
                       @Param("from") LocalDateTime from);

    @Select("""
        SELECT kind AS "kind", COUNT(*) AS "count"
        FROM ai_usage
        WHERE user_id = #{userId} AND create_time >= #{from}
        GROUP BY kind
        """)
    List<Map<String, Object>> countByKindSince(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from);

    @Select("""
        SELECT DATE(create_time) AS "day",
               COUNT(*) AS "calls",
               COALESCE(SUM(prompt_tokens), 0) AS "promptTokens",
               COALESCE(SUM(completion_tokens), 0) AS "completionTokens"
        FROM ai_usage
        WHERE create_time >= #{from}
        GROUP BY DATE(create_time)
        ORDER BY DATE(create_time)
        """)
    List<Map<String, Object>> dailyTotals(@Param("from") LocalDateTime from);

    @Select("""
        SELECT u.id AS "userId", u.username AS "username",
               COUNT(*) AS "calls"
        FROM ai_usage a
        JOIN sys_user u ON a.user_id = u.id
        WHERE a.create_time >= #{from}
        GROUP BY u.id, u.username
        ORDER BY COUNT(*) DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> topUsers(@Param("from") LocalDateTime from,
                                       @Param("limit") int limit);

    @Select("""
        SELECT COUNT(*) FROM ai_usage WHERE create_time >= #{from}
        """)
    long countTotalSince(@Param("from") LocalDateTime from);
}
