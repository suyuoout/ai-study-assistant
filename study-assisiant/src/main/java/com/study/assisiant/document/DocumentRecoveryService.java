package com.study.assisiant.document;

import com.study.assisiant.mapper.StudyDocumentMapper;
import com.study.assisiant.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从挂载盘重建资料索引。
 *
 * <p>用于「上传目录挂在持久化存储上、但数据库每次重新部署会清零」的场景
 * （例如云托管 + 对象存储挂载 + 容器内 H2）：文件其实还在，
 * 但数据库记录没了，前端就再也看不到它们。这个服务把盘上的文件重新写回数据库，
 * 并触发重新解析。</p>
 *
 * <p>归属用户优先取上传时写入的元数据里的 userId；元数据缺失或用户已不存在时，
 * 归给执行恢复动作的管理员。</p>
 */
@Service
public class DocumentRecoveryService {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentRecoveryService.class);

    private final StudyDocumentMapper documentMapper;
    private final SysUserMapper sysUserMapper;
    private final DocumentStorage storage;
    private final DocumentParseService parseService;

    public DocumentRecoveryService(StudyDocumentMapper documentMapper,
                                   SysUserMapper sysUserMapper,
                                   DocumentStorage storage,
                                   DocumentParseService parseService) {
        this.documentMapper = documentMapper;
        this.sysUserMapper = sysUserMapper;
        this.storage = storage;
        this.parseService = parseService;
    }

    public Map<String, Object> recover(Long fallbackUserId) {
        List<Path> files = storage.scanStoredFiles();

        // 数据库里已有的文件不再重复入库
        Set<String> known = new HashSet<>();
        for (String path : documentMapper.findAllStoragePaths()) {
            if (path != null) {
                known.add(normalize(path));
            }
        }

        int recovered = 0;
        int skipped = 0;
        int failed = 0;

        for (Path file : files) {
            String storagePath = file.toAbsolutePath().normalize().toString();
            if (known.contains(normalize(storagePath))) {
                skipped++;
                continue;
            }

            try {
                DocumentStorage.Meta meta = storage.readMetadata(storagePath);
                Long ownerId = fallbackUserId;
                String fileName = file.getFileName().toString();
                String title = null;

                if (meta != null) {
                    if (meta.userId() != null
                            && sysUserMapper.findById(meta.userId()) != null) {
                        ownerId = meta.userId();
                    }
                    if (meta.fileName() != null && !meta.fileName().isBlank()) {
                        fileName = meta.fileName();
                    }
                    title = meta.title();
                }
                if (title == null || title.isBlank()) {
                    title = fileName.contains(".")
                            ? fileName.substring(0, fileName.lastIndexOf('.'))
                            : fileName;
                }

                StudyDocument document = new StudyDocument();
                document.setUserId(ownerId);
                document.setTitle(title.length() > 200
                        ? title.substring(0, 200) : title);
                document.setFileName(fileName);
                document.setStoragePath(storagePath);
                document.setFileType(DocumentParseService
                        .extensionOf(fileName).toUpperCase(java.util.Locale.ROOT));
                document.setStatus("PROCESSING");
                documentMapper.insert(document);
                parseService.parseAsync(document.getId());
                recovered++;
            } catch (Exception e) {
                failed++;
                log.warn("重建资料索引失败 path={}", storagePath, e);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanned", files.size());
        result.put("recovered", recovered);
        result.put("skipped", skipped);
        result.put("failed", failed);

        log.info("从挂载盘重建资料索引: 扫描 {} 个文件，恢复 {}，跳过 {}，失败 {}",
                files.size(), recovered, skipped, failed);
        return result;
    }

    private String normalize(String path) {
        return path.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
    }
}
