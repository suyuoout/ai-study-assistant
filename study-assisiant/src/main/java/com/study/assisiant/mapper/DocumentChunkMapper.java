package com.study.assisiant.mapper;

import com.study.assisiant.document.DocumentChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentChunkMapper {

    @Insert("""
        INSERT INTO document_chunk
        (document_id, chunk_index, content, page_number, vector_id)
        VALUES
        (#{documentId}, #{chunkIndex}, #{content}, #{pageNumber}, #{vectorId})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DocumentChunk chunk);

    @Select("""
        SELECT * FROM document_chunk
        WHERE document_id = #{documentId}
        ORDER BY chunk_index
        """)
    List<DocumentChunk> findByDocumentId(Long documentId);

    @Delete("DELETE FROM document_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);

    @Select("""
        <script>
        SELECT * FROM document_chunk
        WHERE document_id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
        </script>
        """)
    List<DocumentChunk> findByDocumentIds(@Param("ids") List<Long> ids);
}
