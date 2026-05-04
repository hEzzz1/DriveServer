package com.example.demo.alert.service.evidence;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class LocalEvidenceStorage implements EvidenceStorage {

    public static final String EVIDENCE_URL_PREFIX = "alert-evidence:";

    private final Path storageRoot;

    public LocalEvidenceStorage(String storageDir) {
        this.storageRoot = Paths.get(StringUtils.hasText(storageDir) ? storageDir : "data/alert-evidence")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public String evidenceUrlPrefix() {
        return EVIDENCE_URL_PREFIX;
    }

    @Override
    public StoredObject put(String objectKey, byte[] bytes, String mimeType, boolean replaceExisting) {
        Path target = resolveObjectPath(objectKey);
        try {
            Files.createDirectories(target.getParent());
            if (replaceExisting) {
                Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } else {
                Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        } catch (IOException ex) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "证据文件保存失败");
        }
        return new StoredObject(objectKey, evidenceUrlPrefix() + objectKey, target.getFileName().toString());
    }

    @Override
    public EvidenceObject load(String objectKey, String fallbackMimeType) {
        Path file = resolveObjectPath(objectKey);
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
        try {
            Resource resource = new UrlResource(file.toUri());
            String mediaTypeText = normalizeMimeType(fallbackMimeType);
            if (!StringUtils.hasText(mediaTypeText)) {
                mediaTypeText = Files.probeContentType(file);
            }
            MediaType mediaType = StringUtils.hasText(mediaTypeText)
                    ? toMediaType(mediaTypeText)
                    : MediaTypeFactory.getMediaType(file.getFileName().toString()).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return new EvidenceObject(resource, mediaType, Files.size(file), file.getFileName().toString());
        } catch (IOException ex) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
    }

    private Path resolveObjectPath(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据地址非法");
        }
        Path resolved = storageRoot.resolve(objectKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据地址非法");
        }
        return resolved;
    }

    private String normalizeMimeType(String mimeType) {
        return StringUtils.hasText(mimeType) ? mimeType.trim().toLowerCase(Locale.ROOT) : null;
    }

    private MediaType toMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
