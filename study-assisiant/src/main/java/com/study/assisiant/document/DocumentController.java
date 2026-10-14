package com.study.assisiant.document;

import com.study.assisiant.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping
    public Result upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(
                "上传成功",
                documentService.upload(userId, file));
    }

    @GetMapping
    public Result list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(documentService.list(userId));
    }

    @GetMapping("/{id}")
    public Result detail(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(documentService.detail(userId, id));
    }

    @DeleteMapping("/{id}")
    public Result delete(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        documentService.delete(userId, id);
        return Result.success(null);
    }

    @PostMapping("/{id}/reprocess")
    public Result reprocess(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(
                "重新解析完成",
                documentService.reprocess(userId, id));
    }
}
