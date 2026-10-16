package com.study.assisiant.document;

import com.study.assisiant.common.PageResult;
import com.study.assisiant.mapper.DocumentChunkMapper;
import com.study.assisiant.mapper.StudyDocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentService {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentService.class);

    /** 允许上传的扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "txt", "docx");

    /** 单文件大小上限（与 spring.servlet.multipart 配置保持一致） */
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    /** 单次检索最多考虑的片段数，避免知识库变大后拖垮内存 */
    private static final int MAX_CANDIDATE_CHUNKS = 4000;

    /** 单个问题的检索词上限 */
    private static final int MAX_TERMS = 40;

    private static final Pattern ASCII_WORD =
            Pattern.compile("[a-z0-9]{2,}");
    private static final Pattern CJK =
            Pattern.compile("[\\u4e00-\\u9fa5]+");

    private final StudyDocumentMapper documentMapper;
    private final DocumentChunkMapper chunkMapper;
    private final DocumentStorage storage;
    private final DocumentParseService parseService;

    @Value("${app.async-parse:true}")
    private boolean asyncParse;

    public DocumentService(StudyDocumentMapper documentMapper,
                           DocumentChunkMapper chunkMapper,
                           DocumentStorage storage,
                           DocumentParseService parseService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.storage = storage;
        this.parseService = parseService;
    }

    /** 上传：落盘 + 建记录，解析默认异步执行 */
    public StudyDocument upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "文件过大，请上传 20MB 以内的文件");
        }

        String originalName = file.getOriginalFilename();
        String extension = DocumentParseService.extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "仅支持 PDF、TXT、DOCX 文件");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "读取上传文件失败");
        }

        String storagePath = storage.store(extension, data);

        StudyDocument document = new StudyDocument();
        document.setUserId(userId);
        document.setTitle(titleOf(originalName));
        document.setFileName(originalName);
        document.setStoragePath(storagePath);
        document.setFileType(extension.toUpperCase(Locale.ROOT));
        document.setStatus("PROCESSING");
        documentMapper.insert(document);

        // 同步写一份元数据，便于日后从挂载盘重建索引时还原原始文件名
        storage.writeMetadata(storagePath, originalName, userId,
                document.getTitle());

        if (asyncParse) {
            parseService.parseAsync(document.getId());
        } else {
            parseService.parse(document.getId());
        }

        return documentMapper.findById(document.getId());
    }

    public List<StudyDocument> list(Long userId) {
        return documentMapper.findByUserId(userId);
    }

    public StudyDocument detail(Long userId, Long id) {
        return requireOwned(userId, id);
    }

    /** 片段预览（分页），用于资料详情页 */
    public PageResult<Map<String, Object>> chunks(Long userId, Long id,
                                                  int page, int size) {
        requireOwned(userId, id);

        int pageNo = Math.max(1, page);
        int pageSize = Math.max(1, Math.min(size, 100));
        long total = chunkMapper.countByDocumentId(id);
        List<DocumentChunk> chunks = chunkMapper.findPageByDocumentId(
                id, (pageNo - 1) * pageSize, pageSize);

        List<Map<String, Object>> list = new ArrayList<>(chunks.size());
        for (DocumentChunk chunk : chunks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", chunk.getId());
            item.put("chunkIndex", chunk.getChunkIndex());
            item.put("pageNumber", chunk.getPageNumber());
            String content = chunk.getContent() == null
                    ? "" : chunk.getContent();
            item.put("content", content.length() > 600
                    ? content.substring(0, 600) + "…" : content);
            item.put("length", content.length());
            list.add(item);
        }
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        StudyDocument document = requireOwned(userId, id);
        chunkMapper.deleteByDocumentId(id);
        documentMapper.delete(id, userId);
        storage.delete(document.getStoragePath());
    }

    /** 重新解析（状态置为 PROCESSING 后异步执行） */
    public StudyDocument reprocess(Long userId, Long id) {
        requireOwned(userId, id);
        documentMapper.updateStatus(id, "PROCESSING", null);

        if (asyncParse) {
            parseService.parseAsync(id);
        } else {
            parseService.parse(id);
        }
        return documentMapper.findById(id);
    }

    /**
     * 关键词检索：在候选片段里按命中度排序。
     *
     * <p>为了控制内存，只取有限条候选片段；若知识库继续变大，
     * 下一步应该换成向量检索（embedding + 向量索引）。</p>
     */
    public List<Map<String, Object>> retrieve(Long userId, String question,
                                              List<Long> documentIds,
                                              int topK) {

        List<StudyDocument> documents = searchableDocuments(
                userId, documentIds);
        if (documents.isEmpty()) {
            return List.of();
        }

        List<String> terms = tokenize(question);
        if (terms.isEmpty()) {
            return List.of();
        }

        Map<Long, String> titleById = new HashMap<>();
        Map<Long, String> lowerTitleById = new HashMap<>();
        List<Long> ids = new ArrayList<>(documents.size());
        for (StudyDocument document : documents) {
            ids.add(document.getId());
            String title = document.getTitle() == null
                    ? "未命名资料" : document.getTitle();
            titleById.put(document.getId(), title);
            lowerTitleById.put(document.getId(),
                    title.toLowerCase(Locale.ROOT));
        }

        List<DocumentChunk> chunks = chunkMapper.findCandidates(
                ids, MAX_CANDIDATE_CHUNKS);

        List<Map<String, Object>> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String content = chunk.getContent() == null
                    ? "" : chunk.getContent().toLowerCase(Locale.ROOT);
            String title = lowerTitleById.getOrDefault(
                    chunk.getDocumentId(), "");

            int score = 0;
            for (String term : terms) {
                int hits = countOccurrences(content, term);
                if (hits > 0) {
                    score += Math.min(hits, 3);
                }
                if (!title.isEmpty() && title.contains(term)) {
                    score += 2;
                }
            }
            if (score <= 0) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("chunk", chunk);
            item.put("documentTitle", titleById.getOrDefault(
                    chunk.getDocumentId(), "未命名资料"));
            item.put("score", score);
            scored.add(item);
        }

        scored.sort((a, b) -> Integer.compare(
                (Integer) b.get("score"), (Integer) a.get("score")));
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    private List<StudyDocument> searchableDocuments(Long userId,
                                                    List<Long> documentIds) {
        List<StudyDocument> documents = new ArrayList<>();
        if (documentIds != null && !documentIds.isEmpty()) {
            for (Long id : documentIds) {
                StudyDocument document = documentMapper.findByIdAndUser(
                        id, userId);
                if (document != null && "READY".equals(document.getStatus())) {
                    documents.add(document);
                }
            }
            return documents;
        }
        for (StudyDocument document
                : documentMapper.findByUserIdAndStatus(userId, "READY")) {
            documents.add(document);
        }
        return documents;
    }

    /** 中文按二字切分、英文按词切分，去重后限制数量 */
    List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Set<String> terms = new LinkedHashSet<>();

        Matcher word = ASCII_WORD.matcher(lower);
        while (word.find() && terms.size() < MAX_TERMS) {
            terms.add(word.group());
        }

        Matcher cjk = CJK.matcher(lower);
        while (cjk.find() && terms.size() < MAX_TERMS) {
            String segment = cjk.group();
            if (segment.length() == 1) {
                terms.add(segment);
                continue;
            }
            for (int i = 0; i < segment.length() - 1
                    && terms.size() < MAX_TERMS; i++) {
                terms.add(segment.substring(i, i + 2));
            }
        }
        return new ArrayList<>(terms);
    }

    private int countOccurrences(String content, String term) {
        int count = 0;
        int from = 0;
        while (true) {
            int index = content.indexOf(term, from);
            if (index < 0) {
                return count;
            }
            count++;
            from = index + term.length();
        }
    }

    private StudyDocument requireOwned(Long userId, Long id) {
        StudyDocument document = documentMapper.findByIdAndUser(id, userId);
        if (document == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "无权限访问该资料");
        }
        return document;
    }

    private String titleOf(String name) {
        if (name == null) {
            return "未命名资料";
        }
        int dot = name.lastIndexOf('.');
        String title = dot > 0 ? name.substring(0, dot) : name;
        return title.length() > 200 ? title.substring(0, 200) : title;
    }
}
