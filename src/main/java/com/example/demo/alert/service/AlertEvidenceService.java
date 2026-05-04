package com.example.demo.alert.service;

import com.example.demo.alert.entity.AlertEvent;
import com.example.demo.alert.service.evidence.AliyunOssEvidenceStorage;
import com.example.demo.alert.service.evidence.AlertEvidenceProperties;
import com.example.demo.alert.service.evidence.EvidenceStorage;
import com.example.demo.alert.service.evidence.LocalEvidenceStorage;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AlertEvidenceService {

    private static final String LOCAL_EVIDENCE_PREFIX = "alert-evidence:";
    private static final String OSS_EVIDENCE_PREFIX = "oss-evidence:";
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "application/zip",
            "application/x-zip-compressed"
    );

    private final long maxBytes;
    private final EvidenceStorage localStorage;
    private final EvidenceStorage ossStorage;
    private final EvidenceStorage activeStorage;
    private final String clientUrlMode;

    public AlertEvidenceService(AlertEvidenceProperties properties) {
        this.maxBytes = Math.max(1L, properties.getMaxBytes());
        this.clientUrlMode = normalizeOptionalText(properties.getClientUrlMode());
        this.localStorage = new LocalEvidenceStorage(properties.getStorageDir());
        this.ossStorage = isConfigured(properties.getOss()) ? new AliyunOssEvidenceStorage(properties.getOss()) : null;
        String storageType = normalizeOptionalText(properties.getStorageType());
        if ("oss".equalsIgnoreCase(storageType)) {
            if (ossStorage == null) {
                throw new BusinessException(ApiCode.INTERNAL_ERROR, "阿里云OSS配置不完整");
            }
            this.activeStorage = ossStorage;
        } else {
            this.activeStorage = localStorage;
        }
    }

    public StoredEvidence storeAlertEvidence(String alertNo,
                                             String evidenceType,
                                             String evidenceUrl,
                                             String evidenceMimeType,
                                             Long evidenceCapturedAtMs,
                                             LocalDateTime now) {
        String normalizedUrl = normalizeOptionalText(evidenceUrl);
        String normalizedMimeType = normalizeMimeType(evidenceMimeType);
        String normalizedType = normalizeEvidenceType(evidenceType, normalizedMimeType);
        if (!StringUtils.hasText(normalizedUrl) || isLocalEvidenceUrl(normalizedUrl)) {
            return new StoredEvidence(normalizedType, normalizedUrl, normalizedMimeType, evidenceCapturedAtMs);
        }
        if (isStoredEvidenceUrl(normalizedUrl)) {
            return new StoredEvidence(normalizedType, normalizedUrl, normalizedMimeType, evidenceCapturedAtMs);
        }
        if (!isDataUri(normalizedUrl)) {
            return new StoredEvidence(normalizedType, normalizedUrl, normalizedMimeType, evidenceCapturedAtMs);
        }

        DecodedEvidence decoded = decodeDataUri(normalizedUrl, normalizedMimeType);
        String finalMimeType = normalizeMimeType(decoded.mimeType());
        assertSupportedMimeType(finalMimeType);
        String finalType = normalizeEvidenceType(normalizedType, finalMimeType);
        String objectKey = buildAlertObjectKey(now, alertNo, finalMimeType);
        EvidenceStorage.StoredObject storedObject = activeStorage.put(objectKey, decoded.bytes(), finalMimeType, false);
        return new StoredEvidence(finalType, storedObject.evidenceUrl(), finalMimeType, evidenceCapturedAtMs);
    }

    public StoredEvidence storeUploadedEvidence(String ownerKey,
                                                String evidenceType,
                                                String evidenceMimeType,
                                                Long evidenceCapturedAtMs,
                                                MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据文件不能为空");
        }
        String finalMimeType = normalizeMimeType(firstNonBlank(evidenceMimeType, file.getContentType(), null));
        assertSupportedMimeType(finalMimeType);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据文件读取失败");
        }
        assertAllowedSize(bytes.length);
        String objectKey = buildEventObjectKey(ownerKey, finalMimeType);
        EvidenceStorage.StoredObject storedObject = activeStorage.put(objectKey, bytes, finalMimeType, true);
        return new StoredEvidence(
                normalizeEvidenceType(evidenceType, finalMimeType),
                storedObject.evidenceUrl(),
                finalMimeType,
                evidenceCapturedAtMs);
    }

    public String toClientEvidenceUrl(AlertEvent alert) {
        if (alert == null || !StringUtils.hasText(alert.getEvidenceUrl())) {
            return null;
        }
        String evidenceUrl = alert.getEvidenceUrl().trim();
        if (isOssEvidenceUrl(evidenceUrl) && shouldUseSignedUrl()) {
            return signedUrlFor(evidenceUrl);
        }
        if (isStoredEvidenceUrl(evidenceUrl) || isDataUri(evidenceUrl)) {
            return "/api/v1/org/alerts/" + alert.getId() + "/evidence";
        }
        return evidenceUrl;
    }

    public EvidenceResource loadEvidence(AlertEvent alert) {
        if (alert == null || !StringUtils.hasText(alert.getEvidenceUrl())) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
        String evidenceUrl = alert.getEvidenceUrl().trim();
        if (isStoredEvidenceUrl(evidenceUrl)) {
            EvidenceStorage storage = storageForUrl(evidenceUrl);
            String objectKey = evidenceUrl.startsWith(LOCAL_EVIDENCE_PREFIX)
                    ? evidenceUrl.substring(LOCAL_EVIDENCE_PREFIX.length())
                    : evidenceUrl.substring(OSS_EVIDENCE_PREFIX.length());
            EvidenceStorage.EvidenceObject object = storage.load(objectKey, alert.getEvidenceMimeType());
            return new EvidenceResource(object.resource(), object.mediaType(), object.contentLength(), object.filename());
        }
        if (isDataUri(evidenceUrl)) {
            DecodedEvidence decoded = decodeDataUri(evidenceUrl, alert.getEvidenceMimeType());
            return new EvidenceResource(
                    new NamedByteArrayResource(decoded.bytes(), "alert-" + alert.getId() + "." + extensionFor(decoded.mimeType())),
                    toMediaType(decoded.mimeType()),
                    decoded.bytes().length,
                    "alert-" + alert.getId() + "." + extensionFor(decoded.mimeType()));
        }
        throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
    }

    public boolean isLocalEvidenceUrl(String evidenceUrl) {
        return evidenceUrl != null && evidenceUrl.trim().startsWith(LOCAL_EVIDENCE_PREFIX);
    }

    @PreDestroy
    public void close() {
        try {
            activeStorage.close();
        } catch (Exception ignored) {
        }
        if (ossStorage != null && ossStorage != activeStorage) {
            try {
                ossStorage.close();
            } catch (Exception ignored) {
            }
        }
    }

    private EvidenceStorage storageForUrl(String evidenceUrl) {
        if (evidenceUrl.startsWith(LOCAL_EVIDENCE_PREFIX)) {
            return localStorage;
        }
        if (isOssEvidenceUrl(evidenceUrl)) {
            if (ossStorage == null) {
                throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
            }
            return ossStorage;
        }
        throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
    }

    private boolean isStoredEvidenceUrl(String evidenceUrl) {
        return evidenceUrl != null
                && (evidenceUrl.trim().startsWith(LOCAL_EVIDENCE_PREFIX)
                || evidenceUrl.trim().startsWith(OSS_EVIDENCE_PREFIX));
    }

    private boolean isOssEvidenceUrl(String evidenceUrl) {
        return evidenceUrl != null && evidenceUrl.trim().startsWith(OSS_EVIDENCE_PREFIX);
    }

    private boolean shouldUseSignedUrl() {
        return "signed-url".equalsIgnoreCase(clientUrlMode) || "signed_url".equalsIgnoreCase(clientUrlMode);
    }

    private String signedUrlFor(String evidenceUrl) {
        if (!(ossStorage instanceof AliyunOssEvidenceStorage aliyunOssEvidenceStorage)) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
        String objectKey = evidenceUrl.substring(OSS_EVIDENCE_PREFIX.length());
        return aliyunOssEvidenceStorage.generateSignedUrl(objectKey).toString();
    }

    private DecodedEvidence decodeDataUri(String dataUri, String fallbackMimeType) {
        int commaIndex = dataUri.indexOf(',');
        if (commaIndex <= 5) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据Data URI格式非法");
        }
        String metadata = dataUri.substring(5, commaIndex);
        String data = dataUri.substring(commaIndex + 1);
        boolean base64 = false;
        String mimeType = null;
        for (String token : metadata.split(";")) {
            if ("base64".equalsIgnoreCase(token)) {
                base64 = true;
            } else if (StringUtils.hasText(token) && token.contains("/")) {
                mimeType = token.trim().toLowerCase(Locale.ROOT);
            }
        }
        String finalMimeType = normalizeMimeType(firstNonBlank(mimeType, fallbackMimeType, null));
        assertSupportedMimeType(finalMimeType);
        byte[] bytes;
        try {
            bytes = base64
                    ? Base64.getDecoder().decode(data)
                    : URLDecoder.decode(data, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据Data URI解码失败");
        }
        assertAllowedSize(bytes.length);
        return new DecodedEvidence(finalMimeType, bytes);
    }

    private String buildAlertObjectKey(LocalDateTime now, String alertNo, String mimeType) {
        String datePath = (now == null ? LocalDateTime.now() : now).format(DATE_PATH_FORMATTER);
        String safeAlertNo = sanitizeOwnerKey(alertNo);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return datePath + "/" + safeAlertNo + "-" + suffix + "." + extensionFor(mimeType);
    }

    private String buildEventObjectKey(String ownerKey, String mimeType) {
        return "events/" + sanitizeOwnerKey(ownerKey) + "." + extensionFor(mimeType);
    }

    private String sanitizeOwnerKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "evidence";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (char ch : value.trim().toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_') {
                builder.append(ch);
            } else {
                builder.append('_');
            }
        }
        return builder.length() == 0 ? "evidence" : builder.toString();
    }

    private void assertSupportedMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType) || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据媒体类型不支持");
        }
    }

    private void assertAllowedSize(long size) {
        if (size <= 0L) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据文件不能为空");
        }
        if (size > maxBytes) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据文件超过大小限制");
        }
    }

    private boolean isDataUri(String value) {
        return value != null && value.regionMatches(true, 0, "data:", 0, 5);
    }

    private String normalizeEvidenceType(String evidenceType, String mimeType) {
        String normalized = normalizeOptionalText(evidenceType);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (mimeType != null && mimeType.startsWith("video/")) {
            return "VIDEO_CLIP";
        }
        if (mimeType != null && mimeType.startsWith("image/")) {
            return "KEY_FRAME";
        }
        if ("application/zip".equals(mimeType) || "application/x-zip-compressed".equals(mimeType)) {
            return "FRAME_SEQUENCE";
        }
        return null;
    }

    private String normalizeMimeType(String mimeType) {
        String normalized = normalizeOptionalText(mimeType);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return fallback;
    }

    private String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            case "video/quicktime" -> "mov";
            case "application/zip", "application/x-zip-compressed" -> "zip";
            default -> "bin";
        };
    }

    private MediaType toMediaType(String mimeType) {
        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public record StoredEvidence(
            String evidenceType,
            String evidenceUrl,
            String evidenceMimeType,
            Long evidenceCapturedAtMs
    ) {
    }

    public record EvidenceResource(
            Resource resource,
            MediaType mediaType,
            long contentLength,
            String filename
    ) {
    }

    private record DecodedEvidence(String mimeType, byte[] bytes) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private boolean isConfigured(AlertEvidenceProperties.Oss oss) {
        return oss != null
                && StringUtils.hasText(oss.getEndpoint())
                && StringUtils.hasText(oss.getBucket())
                && StringUtils.hasText(oss.getAccessKeyId())
                && StringUtils.hasText(oss.getAccessKeySecret());
    }
}
