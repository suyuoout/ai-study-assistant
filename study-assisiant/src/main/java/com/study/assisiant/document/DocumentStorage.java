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

    private void requireInsideRoot(Path path, Path root) {
        if (!path.startsWith(root)) {
            log.error("检测到越界的文件路径 path={} root={}", path, root);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "非法的文件路径");
        }
    }
}
