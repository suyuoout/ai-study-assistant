package com.study.assisiant.mapper;

import com.study.assisiant.chat.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("""
        INSERT INTO chat_message
        (session_id, role, content, sources_json)
        VALUES
        (#{sessionId}, #{role}, #{content}, #{sourcesJson})
        """)
    int insert(ChatMessage message);

    @Select("""
        SELECT * FROM chat_message
        WHERE session_id = #{sessionId}
        ORDER BY id ASC
        """)
    List<ChatMessage> findBySessionId(Long sessionId);

    /**
     * 只取最近 N 条（数据库层截断），再按时间正序返回。
     * 避免会话很长时把整段历史都读进内存。
     */
    @Select("""
        SELECT * FROM (
            SELECT * FROM chat_message
            WHERE session_id = #{sessionId}
            ORDER BY id DESC
            LIMIT #{limit}
        ) recent
        ORDER BY id ASC
        """)
    List<ChatMessage> findRecent(@Param("sessionId") Long sessionId,
                                 @Param("limit") int limit);

    /** 分页倒序读取（前端上滑加载更早的消息） */
    @Select("""
        SELECT * FROM (
            SELECT * FROM chat_message
            WHERE session_id = #{sessionId}
            ORDER BY id DESC
            LIMIT #{size} OFFSET #{offset}
        ) recent
        ORDER BY id ASC
        """)
    List<ChatMessage> findPageDesc(@Param("sessionId") Long sessionId,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    long countBySession(Long sessionId);
}
