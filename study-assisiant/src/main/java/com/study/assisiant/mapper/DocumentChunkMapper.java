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

    @Select("""
        SELECT * FROM document_chunk
        WHERE document_id = #{documentId}
        ORDER BY chunk_index
        LIMIT #{size} OFFSET #{offset}
        """)
    List<DocumentChunk> findPageByDocumentId(
            @Param("documentId") Long documentId,
            @Param("offset") int offset,
            @Param("size") int size);

    @Select("""
        SELECT COUNT(*) FROM document_chunk
        WHERE document_id = #{documentId}
        """)
    long countByDocumentId(Long documentId);

    /**
     * 检索候选集：限制条数，避免知识库变大后一次性把全部片段读进内存。
     */
    @Select("""
        <script>
        SELECT * FROM document_chunk
        WHERE document_id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
          #{id}
        </foreach>
        ORDER BY id
        LIMIT #{limit}
        </script>
        """)
    List<DocumentChunk> findCandidates(@Param("ids") List<Long> ids,
                                       @Param("limit") int limit);

    @Delete("DELETE FROM document_chunk WHERE document_id = #{documentId}")
    int deleteByDocumentId(Long documentId);
}
