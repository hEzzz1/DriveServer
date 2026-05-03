package com.example.demo.device.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class DeviceTelemetryRequest {

    @Min(0)
    private Integer uploadQueueSize;

    @Size(max = 64)
    private String uploadLastFailureClass;

    @Size(max = 255)
    private String uploadLastErrorMessage;

    private OffsetDateTime uploadLastSuccessAt;

    private OffsetDateTime uploadLastFailedAt;

    public Integer getUploadQueueSize() {
        return uploadQueueSize;
    }

    public void setUploadQueueSize(Integer uploadQueueSize) {
        this.uploadQueueSize = uploadQueueSize;
    }

    public String getUploadLastFailureClass() {
        return uploadLastFailureClass;
    }

    public void setUploadLastFailureClass(String uploadLastFailureClass) {
        this.uploadLastFailureClass = uploadLastFailureClass;
    }

    public String getUploadLastErrorMessage() {
        return uploadLastErrorMessage;
    }

    public void setUploadLastErrorMessage(String uploadLastErrorMessage) {
        this.uploadLastErrorMessage = uploadLastErrorMessage;
    }

    public OffsetDateTime getUploadLastSuccessAt() {
        return uploadLastSuccessAt;
    }

    public void setUploadLastSuccessAt(OffsetDateTime uploadLastSuccessAt) {
        this.uploadLastSuccessAt = uploadLastSuccessAt;
    }

    public OffsetDateTime getUploadLastFailedAt() {
        return uploadLastFailedAt;
    }

    public void setUploadLastFailedAt(OffsetDateTime uploadLastFailedAt) {
        this.uploadLastFailedAt = uploadLastFailedAt;
    }
}
