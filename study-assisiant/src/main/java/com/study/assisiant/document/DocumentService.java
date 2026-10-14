package com.study.assisiant.document;

import com.study.assisiant.mapper.DocumentChunkMapper;
import com.study.assisiant.mapper.StudyDocumentMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Autowired
    private StudyDocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    public StudyDocument upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (!"pdf".equals(ext) && !"txt".equals(ext) && !"docx".equals(ext)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "仅支持 PDF、TXT、DOCX 文件");
        }

        String storedName = UUID.randomUUID() + "." + ext;
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            byte[] data = file.getBytes();
            Path target = dir.resolve(storedName);
            Files.write(target, data);

            StudyDocument document = new StudyDocument();
            document.setUserId(userId);
            document.setTitle(titleOf(originalName));
            document.setFileName(originalName);
            document.setStoragePath(target.toString());
            document.setFileType(ext.toUpperCase(Locale.ROOT));
            document.setStatus("PROCESSING");
            documentMapper.insert(document);

            try {
                List<PageText> pages = extractText(ext, data);
                int index = 0;
                for (PageText page : pages) {
                    for (String chunkText : chunkText(page.text)) {
                        DocumentChunk chunk = new DocumentChunk();
                        chunk.setDocumentId(document.getId());
                        chunk.setChunkIndex(index++);
                        chunk.setContent(chunkText);
                        chunk.setPageNumber(
                                page.page > 0 ? page.page : null);
                        chunkMapper.insert(chunk);
                    }
                }
                if (index == 0) {
                    throw new IllegalStateException("未提取到文本");
                }
                documentMapper.updateStatus(document.getId(), "READY");
                document.setStatus("READY");
            } catch (Exception e) {
                documentMapper.updateStatus(document.getId(), "FAILED");
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "文档解析失败：" + e.getMessage());
            }

            return document;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "文件保存失败：" + e.getMessage());
        }
    }

    public List<StudyDocument> list(Long userId) {
        return documentMapper.findByUserId(userId);
    }

    public StudyDocument detail(Long userId, Long id) {
        StudyDocument document = documentMapper.findByIdAndUser(id, userId);
        if (document == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限查看该资料");
        }
        return document;
    }

    public void delete(Long userId, Long id) {
        StudyDocument document = documentMapper.findByIdAndUser(id, userId);
        if (document == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限删除该资料");
        }
        chunkMapper.deleteByDocumentId(id);
        documentMapper.delete(id, userId);
        try {
            if (document.getStoragePath() != null) {
                Files.deleteIfExists(Paths.get(document.getStoragePath()));
            }
        } catch (Exception ignored) {
            // 文件删除失败不影响数据库记录删除
        }
    }

    public StudyDocument reprocess(Long userId, Long id) {
        StudyDocument document = documentMapper.findByIdAndUser(id, userId);
        if (document == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "无权限操作该资料");
        }

        chunkMapper.deleteByDocumentId(id);
        documentMapper.updateStatus(id, "PROCESSING");

        try {
            byte[] data = Files.readAllBytes(
                    Paths.get(document.getStoragePath()));
            String ext = extensionOf(document.getFileName());
            List<PageText> pages = extractText(ext, data);
            int index = 0;
            for (PageText page : pages) {
                for (String chunkText : chunkText(page.text)) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocumentId(document.getId());
                    chunk.setChunkIndex(index++);
                    chunk.setContent(chunkText);
                    chunk.setPageNumber(
                            page.page > 0 ? page.page : null);
                    chunkMapper.insert(chunk);
                }
            }
            if (index == 0) {
                throw new IllegalStateException("未提取到文本");
            }
            documentMapper.updateStatus(id, "READY");
            document.setStatus("READY");
            return document;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            documentMapper.updateStatus(id, "FAILED");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "重新解析失败：" + e.getMessage());
        }
    }

    /**
     * 关键词检索：返回按命中度排序的片段
     */
    public List<Map<String, Object>> retrieve(
            Long userId, String question,
            List<Long> documentIds, int topK) {

        List<StudyDocument> documents = new ArrayList<>();
        if (documentIds != null && !documentIds.isEmpty()) {
            for (Long id : documentIds) {
                StudyDocument d = documentMapper.findByIdAndUser(id, userId);
                if (d != null && "READY".equals(d.getStatus())) {
                    documents.add(d);
                }
            }
        } else {
            for (StudyDocument d : documentMapper.findByUserId(userId)) {
                if ("READY".equals(d.getStatus())) {
                    documents.add(d);
                }
            }
        }

        if (documents.isEmpty()) {
            return List.of();
        }

        List<Long> ids = documents.stream().map(StudyDocument::getId).toList();
        List<DocumentChunk> chunks = chunkMapper.findByDocumentIds(ids);
        Map<Long, String> titleById = new HashMap<>();
        for (StudyDocument d : documents) {
            titleById.put(d.getId(), d.getTitle());
        }

        List<String> terms = tokenize(question);
        if (terms.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String content = chunk.getContent().toLowerCase(Locale.ROOT);
            int score = 0;
            for (String term : terms) {
                if (content.contains(term)) {
                    score++;
                }
            }
            if (score > 0) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("chunk", chunk);
                item.put("documentTitle",
                        titleById.getOrDefault(chunk.getDocumentId(), "未命名资料"));
                item.put("score", score);
                scored.add(item);
            }
        }

        scored.sort((a, b) ->
                Integer.compare(
                        (Integer) b.get("score"),
                        (Integer) a.get("score")));

        return scored.subList(0, Math.min(topK, scored.size()));
    }

    private List<PageText> extractText(String ext, byte[] data)
            throws Exception {
        if ("txt".equals(ext)) {
            PageText page = new PageText();
            page.text = new String(data, StandardCharsets.UTF_8);
            return List.of(page);
        }
        if ("pdf".equals(ext)) {
            List<PageText> pages = new ArrayList<>();
            try (PDDocument document = Loader.loadPDF(data)) {
                PDFTextStripper stripper = new PDFTextStripper();
                int count = document.getNumberOfPages();
                for (int i = 1; i <= count; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    PageText page = new PageText();
                    page.page = i;
                    page.text = stripper.getText(document);
                    pages.add(page);
                }
            }
            return pages;
        }
        if ("docx".equals(ext)) {
            PageText page = new PageText();
            try (XWPFDocument document =
                         new XWPFDocument(new ByteArrayInputStream(data))) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    sb.append(paragraph.getText()).append('\n');
                }
                page.text = sb.toString();
            }
            return List.of(page);
        }
        throw new IllegalArgumentException("不支持的文件类型: " + ext);
    }

    private List<String> chunkText(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return result;
        }
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            result.add(normalized.substring(start, end));
            if (end >= normalized.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }
        return result;
    }

    private List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        String lower = text.toLowerCase(Locale.ROOT);

        Matcher word = Pattern.compile("[a-z0-9]+").matcher(lower);
        while (word.find()) {
            terms.add(word.group());
        }

        Matcher cjk = Pattern.compile("[\\u4e00-\\u9fa5]+").matcher(lower);
        while (cjk.find()) {
            String seg = cjk.group();
            for (int i = 0; i < seg.length() - 1; i++) {
                terms.add(seg.substring(i, i + 2));
            }
        }
        return terms;
    }

    private String extensionOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String titleOf(String name) {
        if (name == null) {
            return "未命名资料";
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static class PageText {
        private int page;
        private String text;
    }
}
