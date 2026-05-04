package com.example.demo.alert.service.evidence;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public interface EvidenceStorage extends AutoCloseable {

    String evidenceUrlPrefix();

    StoredObject put(String objectKey, byte[] bytes, String mimeType, boolean replaceExisting);

    EvidenceObject load(String objectKey, String fallbackMimeType);

    @Override
    default void close() {
    }

    record StoredObject(String objectKey, String evidenceUrl, String filename) {
    }

    record EvidenceObject(Resource resource, MediaType mediaType, long contentLength, String filename) {
    }
}
