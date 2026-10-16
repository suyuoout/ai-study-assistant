package com.study.assisiant.mapper;

import com.study.assisiant.chat.ChatSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    @Insert("""
        INSERT INTO chat_session (user_id, title, chat_type)
        VALUES (#{userId}, #{title}, #{chatType})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    @Select("SELECT * FROM chat_session WHERE id = #{id}")
    ChatSession findById(Long id);

    @Select("""
        SELECT * FROM chat_session
        WHERE id = #{id} AND user_id = #{userId}
        """)
    ChatSession findByIdAndUser(@Param("id") Long id,
                                @Param("userId") Long userId);

    @Select("""
        SELECT * FROM chat_session
        WHERE user_id = #{userId}
        ORDER BY update_time DESC, id DESC
        """)
    List<ChatSession> findByUserId(Long userId);

    @Update("""
        UPDATE chat_session SET title = #{title}
        WHERE id = #{id} AND user_id = #{userId}
        """)
    int updateTitle(@Param("id") Long id,
                    @Param("userId") Long userId,
                    @Param("title") String title);

    /** 新消息进来时刷新会话排序时间（H2/MySQL 行为一致） */
    @Update("""
        UPDATE chat_session SET update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int touch(@Param("id") Long id);

    @Delete("""
        DELETE FROM chat_session
        WHERE id = #{id} AND user_id = #{userId}
        """)
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
