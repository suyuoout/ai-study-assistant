package com.study.assisiant.document;

import com.study.assisiant.mapper.DocumentChunkMapper;
import com.study.assisiant.mapper.StudyDocumentMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文档解析与切分。
 *
 * <p>解析比较耗时（大 PDF 可能要十几秒），因此默认放在异步线程里执行，
 * 上传接口立即返回 PROCESSING，前端轮询状态。</p>
 */
@Service
public class DocumentParseService {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentParseService.class);

    static final int CHUNK_SIZE = 500;
    static final int CHUNK_OVERLAP = 60;
    private static final int MAX_ERROR_LENGTH = 400;

    private final StudyDocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final DocumentStorage storage;

    public DocumentParseService(StudyDocumentMapper documentMapper,
                                DocumentChunkMapper chunkMapper,
                                DocumentStorage storage) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.storage = storage;
    }

    @Async("documentExecutor")
    @Transactional
    public void parseAsync(Long documentId) {
        parse(documentId);
    }

    /** 解析并写入片段；失败时把原因记录到 error_message */
    @Transactional
    public void parse(Long documentId) {
        StudyDocument document = documentMapper.findById(documentId);
        if (document == null) {
            return;
        }

        documentMapper.updateStatus(documentId, "PROCESSING", null);
        try {
            byte[] data = storage.read(document.getStoragePath());
            String extension = extensionOf(document.getFileName());

            chunkMapper.deleteByDocumentId(documentId);

            List<PageText> pages = extractText(extension, data);
            int index = 0;
            for (PageText page : pages) {
                for (String text : chunkText(page.text,
                        CHUNK_SIZE, CHUNK_OVERLAP)) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocumentId(documentId);
                    chunk.setChunkIndex(index++);
                    chunk.setContent(text);
                    chunk.setPageNumber(page.page > 0 ? page.page : null);
                    chunkMapper.insert(chunk);
                }
            }

            if (index == 0) {
                throw new IllegalStateException("未提取到文本");
            }
            documentMapper.updateStatus(documentId, "READY", null);
            log.info("文档解析完成 id={} 片段数={}", documentId, index);
        } catch (Exception e) {
            String reason = e.getMessage() == null
                    ? e.getClass().getSimpleName() : e.getMessage();
            if (reason.length() > MAX_ERROR_LENGTH) {
                reason = reason.substring(0, MAX_ERROR_LENGTH);
            }
            documentMapper.updateStatus(documentId, "FAILED", reason);
            log.warn("文档解析失败 id={} reason={}", documentId, reason, e);
        }
    }

    /** 按扩展名提取文本 */
    List<PageText> extractText(String extension, byte[] data)
            throws Exception {
        if ("txt".equals(extension)) {
            PageText page = new PageText();
            page.text = new String(data, StandardCharsets.UTF_8);
            return List.of(page);
        }
        if ("pdf".equals(extension)) {
            List<PageText> pages = new ArrayList<>();
            try (PDDocument document = Loader.loadPDF(data)) {
                int count = document.getNumberOfPages();
                for (int i = 1; i <= count; i++) {
                    PDFTextStripper stripper = new PDFTextStripper();
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
        if ("docx".equals(extension)) {
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
        throw new IllegalArgumentException("不支持的文件类型: " + extension);
    }

    /**
     * 滑动窗口切分，并尽量在句子/段落边界处断开，避免把一句话劈成两半。
     */
    static List<String> chunkText(String text, int size, int overlap) {
        List<String> result = new ArrayList<>();
        if (text == null) {
            return result;
        }

        String normalized = text
                .replace('\u0000', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();
        if (normalized.isEmpty()) {
            return result;
        }

        int length = normalized.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + size, length);
            if (end < length) {
                int boundary = findBoundary(normalized, start, end, size);
                if (boundary > 0) {
                    end = boundary;
                }
            }
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
            if (end >= length) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return result;
    }

    /** 在窗口后 40% 范围内向前找标点，找不到就按原位置切 */
    private static int findBoundary(String text, int start, int end,
                                    int size) {
        int searchFrom = start + (int) (size * 0.6);
        for (int i = end; i > searchFrom; i--) {
            char c = text.charAt(i - 1);
            if (c == '\n' || c == '。' || c == '！' || c == '？'
                    || c == '；' || c == '.' || c == '!' || c == '?'
                    || c == ';') {
                return i;
            }
        }
        return -1;
    }

    static String extensionOf(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }

    /** 提取出来的文本；page 为 0 表示没有页码概念（txt/docx） */
    static class PageText {
        private int page;
        private String text;
    }
}
