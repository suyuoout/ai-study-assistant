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
        (#{userId}, #{title}, #{fileName}, #{storagePath}, #{fileType}, #{status})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StudyDocument document);

    @Select("SELECT * FROM study_document WHERE id = #{id}")
    StudyDocument findById(Long id);

    @Select("""
        SELECT * FROM study_document
        WHERE user_id = #{userId}
        ORDER BY create_time DESC
        """)
    List<StudyDocument> findByUserId(Long userId);

    @Select("""
        SELECT * FROM study_document
        WHERE id = #{id} AND user_id = #{userId}
        """)
    StudyDocument findByIdAndUser(
            @Param("id") Long id, @Param("userId") Long userId);

    @Update("""
        UPDATE study_document SET status = #{status}
        WHERE id = #{id}
        """)
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Delete("""
        DELETE FROM study_document
        WHERE id = #{id} AND user_id = #{userId}
        """)
    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
