package com.study.assisiant.mapper;

import com.study.assisiant.document.StudyDocument;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface StudyDocumentMapper {

    @Insert("""
        INSERT INTO study_document
        (user_id, title, file_name, storage_path, file_type, status)
        VALUES
        (#{userId}, #{title}, #{fileName}, #{storagePath}, #{fileType},
         #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyDocument document);

    @Select("SELECT * FROM study_document WHERE id = #{id}")
    StudyDocument findById(Long id);

    @Select("""
        SELECT * FROM study_document
        WHERE user_id = #{userId}
        ORDER BY id DESC
        """)
    List<StudyDocument> findByUserId(Long userId);

    @Select("""
        SELECT * FROM study_document
        WHERE id = #{id} AND user_id = #{userId}
        """)
    StudyDocument findByIdAndUser(
            @Param("id") Long id, @Param("userId") Long userId);

    @Select("""
        SELECT * FROM study_document
        WHERE user_id = #{userId} AND status = #{status}
        ORDER BY id DESC
        """)
    List<StudyDocument> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") String status);

    @Update("""
        UPDATE study_document
        SET status = #{status}, error_message = #{errorMessage}
        WHERE id = #{id}
        """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    @Delete("""
        DELETE FROM study_document
        WHERE id = #{id} AND user_id = #{userId}
        """)
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Delete("DELETE FROM study_document WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM study_document WHERE user_id = #{userId}")
    long countByUser(Long userId);

    @Select("""
        SELECT COUNT(*) FROM study_document WHERE status = #{status}
        """)
    long countByStatus(String status);
}
