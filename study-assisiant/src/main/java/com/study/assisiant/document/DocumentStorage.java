package com.study.assisiant.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 上传文件读写。
 *
 * <p>所有基于数据库里 storage_path 的操作都会先校验
 * 「规范化后的绝对路径必须位于上传根目录内」，
 * 避免记录被污染时删除/读取到系统上的任意文件。</p>
 */
@Component
public class DocumentStorage {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentStorage.class);

    /** 与文件同目录的元数据文件后缀，用于挂载盘重建索引时还原原始文件名 */
    public static final String META_SUFFIX = ".meta.json";

    /** 允许作为资料参与重建的扩展名 */
    private static final Set<String> RECOVERABLE_EXTENSIONS =
            Set.of("pdf", "txt", "docx");

    @Value("${app.upload-dir}")
    private String uploadDir;

    public Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 保存文件，返回落盘的绝对路径字符串 */
    public String store(String extension, byte[] data) {
        Path root = root();
        try {
            Files.createDirectories(root);
            Path target = root.resolve(
                    UUID.randomUUID() + "." + extension).normalize();
            requireInsideRoot(target, root);
            Files.write(target, data);
            return target.toString();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "文件保存失败：" + e.getMessage());
        }
    }

    public byte[] read(String storagePath) {
        Path path = resolveSafe(storagePath);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "文件已不存在或无法读取，请重新上传");
        }
    }

    /** 删除文件；失败只记录日志，不影响数据库记录清理 */
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(storagePath));
        } catch (Exception e) {
            log.warn("删除上传文件失败 path={}", storagePath, e);
        }
    }

    /** 把数据库里的路径解析成安全的绝对路径 */
    public Path resolveSafe(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "文件路径为空");
        }
        Path root = root();
        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        requireInsideRoot(path, root);
        return path;
    }

    /**
     * 写一份与上传文件同名的元数据（原始文件名、归属用户、标题）。
     *
     * <p>文件本身是 UUID 命名，元数据损坏或缺失时重建索引只能用 UUID 当标题。</p>
     */
    public void writeMetadata(String storagePath, String fileName,
                              Long userId, String title) {
        try {
            Path metaPath = Paths.get(
                    resolveSafe(storagePath).toString() + META_SUFFIX);
            String json = "{\"fileName\":" + quote(fileName)
                    + ",\"userId\":" + userId
                    + ",\"title\":" + quote(title) + "}";
            Files.writeString(metaPath, json, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 元数据只是辅助信息，写失败不影响上传
            log.warn("写入上传元数据失败 path={}", storagePath, e);
        }
    }

    /** 读取元数据；不存在或损坏时返回 null */
    public Meta readMetadata(String storagePath) {
        try {
            Path metaPath = Paths.get(
                    resolveSafe(storagePath).toString() + META_SUFFIX);
            if (!Files.exists(metaPath)) {
                return null;
            }
            String json = Files.readString(metaPath, StandardCharsets.UTF_8);
            return Meta.parse(json);
        } catch (Exception e) {
            log.warn("读取上传元数据失败 path={}", storagePath, e);
            return null;
        }
    }

    /** 扫描上传目录里已落盘、可作为资料恢复的文件 */
    public List<Path> scanStoredFiles() {
        Path root = root();
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return files;
        }
        try (var stream = Files.list(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> RECOVERABLE_EXTENSIONS.contains(
                            extensionOf(path.getFileName().toString())))
                    .forEach(files::add);
        } catch (IOException e) {
            log.warn("扫描上传目录失败 root={}", root, e);
        }
        return files;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    /** 上传文件的辅助元数据 */
    public record Meta(String fileName, Long userId, String title) {

        static Meta parse(String json) {
            try {
                com.alibaba.fastjson2.JSONObject object =
                        com.alibaba.fastjson2.JSONObject.parseObject(json);
                if (object == null) {
                    return null;
                }
                return new Meta(object.getString("fileName"),
                        object.getLong("userId"), object.getString("title"));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void requireInsideRoot(Path path, Path root) {
        if (!path.startsWith(root)) {
            log.error("检测到越界的文件路径 path={} root={}", path, root);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "非法的文件路径");
        }
    }
}
