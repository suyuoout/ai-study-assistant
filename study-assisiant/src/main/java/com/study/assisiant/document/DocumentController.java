package com.study.assisiant.document;

import com.study.assisiant.auth.JwtInterceptor;
import com.study.assisiant.common.PageResult;
import com.study.assisiant.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** 上传后立即可见，解析在后台进行（状态 PROCESSING → READY/FAILED） */
    @PostMapping
    public Result<StudyDocument> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return Result.success("上传成功，正在解析",
                documentService.upload(userId(request), file));
    }

    @GetMapping
    public Result<List<StudyDocument>> list(HttpServletRequest request) {
        return Result.success(documentService.list(userId(request)));
    }

    /** 资料详情（前端轮询解析状态用） */
    @GetMapping("/{id}")
    public Result<StudyDocument> detail(@PathVariable Long id,
                                        HttpServletRequest request) {
        return Result.success(
                documentService.detail(userId(request), id));
    }

    /** 片段预览 */
    @GetMapping("/{id}/chunks")
    public Result<PageResult<Map<String, Object>>> chunks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return Result.success(
                documentService.chunks(userId(request), id, page, size));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        documentService.delete(userId(request), id);
        return Result.success("删除成功", null);
    }

    @PostMapping("/{id}/reprocess")
    public Result<StudyDocument> reprocess(@PathVariable Long id,
                                           HttpServletRequest request) {
        return Result.success("已开始重新解析",
                documentService.reprocess(userId(request), id));
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute(JwtInterceptor.ATTR_USER_ID);
    }
}
