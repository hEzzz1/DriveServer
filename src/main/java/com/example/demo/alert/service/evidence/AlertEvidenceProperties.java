package com.example.demo.alert.service.evidence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alert.evidence")
public class AlertEvidenceProperties {

    private String storageType = "oss";
    private String clientUrlMode = "proxy";
    private String storageDir = "data/alert-evidence";
    private long maxBytes = 20 * 1024 * 1024L;
    private Oss oss = new Oss();

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getClientUrlMode() {
        return clientUrlMode;
    }

    public void setClientUrlMode(String clientUrlMode) {
        this.clientUrlMode = clientUrlMode;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public Oss getOss() {
        return oss;
    }

    public void setOss(Oss oss) {
        this.oss = oss == null ? new Oss() : oss;
    }

    public static class Oss {
        private String endpoint;
        private String bucket;
        private String accessKeyId;
        private String accessKeySecret;
        private String objectPrefix = "alert-evidence";
        private long signedUrlExpireSeconds = 300L;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getObjectPrefix() {
            return objectPrefix;
        }

        public void setObjectPrefix(String objectPrefix) {
            this.objectPrefix = objectPrefix;
        }

        public long getSignedUrlExpireSeconds() {
            return signedUrlExpireSeconds;
        }

        public void setSignedUrlExpireSeconds(long signedUrlExpireSeconds) {
            this.signedUrlExpireSeconds = signedUrlExpireSeconds;
        }
    }
}
