package com.study.assisiant.mapper;

import com.study.assisiant.chat.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("""
        INSERT INTO chat_message (session_id, role, content, sources_json)
        VALUES (#{sessionId}, #{role}, #{content}, #{sourcesJson})
        """)
    int insert(ChatMessage message);

    @Select("""
        SELECT * FROM chat_message
        WHERE session_id = #{sessionId}
        ORDER BY id ASC
        """)
    List<ChatMessage> findBySessionId(Long sessionId);
}
