package com.example.demo.alert.service.evidence;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

public final class AliyunOssEvidenceStorage implements EvidenceStorage {

    public static final String EVIDENCE_URL_PREFIX = "oss-evidence:";

    private final String bucket;
    private final String objectPrefix;
    private final long signedUrlExpireSeconds;
    private final OSS ossClient;

    public AliyunOssEvidenceStorage(AlertEvidenceProperties.Oss properties) {
        if (properties == null
                || !StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getBucket())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "阿里云OSS配置不完整");
        }
        this.bucket = properties.getBucket().trim();
        this.objectPrefix = normalizeObjectPrefix(properties.getObjectPrefix());
        this.signedUrlExpireSeconds = Math.max(60L, properties.getSignedUrlExpireSeconds());
        this.ossClient = new OSSClientBuilder().build(
                properties.getEndpoint().trim(),
                properties.getAccessKeyId().trim(),
                properties.getAccessKeySecret().trim());
    }

    @Override
    public String evidenceUrlPrefix() {
        return EVIDENCE_URL_PREFIX;
    }

    @Override
    public StoredObject put(String objectKey, byte[] bytes, String mimeType, boolean replaceExisting) {
        String resolvedObjectKey = resolveObjectKey(objectKey);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        if (StringUtils.hasText(mimeType)) {
            metadata.setContentType(mimeType);
        }

        try {
            ossClient.putObject(new PutObjectRequest(
                    bucket,
                    resolvedObjectKey,
                    new ByteArrayInputStream(bytes),
                    metadata));
        } catch (OSSException | ClientException ex) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "证据文件上传OSS失败");
        }

        return new StoredObject(
                resolvedObjectKey,
                evidenceUrlPrefix() + resolvedObjectKey,
                filenameFor(resolvedObjectKey));
    }

    @Override
    public EvidenceObject load(String objectKey, String fallbackMimeType) {
        String resolvedObjectKey = validateStoredObjectKey(objectKey);
        try {
            OSSObject object = ossClient.getObject(bucket, resolvedObjectKey);
            ObjectMetadata metadata = object.getObjectMetadata();
            byte[] bytes;
            try (InputStream inputStream = object.getObjectContent()) {
                bytes = inputStream.readAllBytes();
            }
            String mediaTypeText = normalizeMimeType(firstNonBlank(metadata == null ? null : metadata.getContentType(), fallbackMimeType));
            MediaType mediaType = StringUtils.hasText(mediaTypeText)
                    ? toMediaType(mediaTypeText)
                    : MediaTypeFactory.getMediaType(resolvedObjectKey).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return new EvidenceObject(
                    new NamedByteArrayResource(bytes, filenameFor(resolvedObjectKey)),
                    mediaType,
                    bytes.length,
                    filenameFor(resolvedObjectKey));
        } catch (OSSException ex) {
            if ("NoSuchKey".equalsIgnoreCase(ex.getErrorCode())) {
                throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
            }
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "证据文件读取OSS失败");
        } catch (ClientException | IOException ex) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "证据文件读取OSS失败");
        }
    }

    public URL generateSignedUrl(String objectKey) {
        String resolvedObjectKey = validateStoredObjectKey(objectKey);
        Date expiration = Date.from(Instant.now().plusSeconds(signedUrlExpireSeconds));
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, resolvedObjectKey, HttpMethod.GET);
        request.setExpiration(expiration);
        return ossClient.generatePresignedUrl(request);
    }

    @Override
    public void close() {
        ossClient.shutdown();
    }

    private String resolveObjectKey(String objectKey) {
        String normalized = validateObjectKey(objectKey);
        if (!StringUtils.hasText(objectPrefix)) {
            return normalized;
        }
        return objectPrefix + "/" + normalized;
    }

    private String validateStoredObjectKey(String objectKey) {
        return validateObjectKey(objectKey);
    }

    private String validateObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据地址非法");
        }
        String normalized = objectKey.trim().replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains("/..")) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "证据地址非法");
        }
        return normalized;
    }

    private String normalizeObjectPrefix(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("../") || normalized.contains("/..")) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "OSS对象前缀非法");
        }
        return normalized;
    }

    private String filenameFor(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        String filename = index >= 0 ? objectKey.substring(index + 1) : objectKey;
        return StringUtils.hasText(filename) ? filename : "evidence";
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return null;
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
}
