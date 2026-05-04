package com.example.demo.ingest.dto;

public class IngestEvidenceResponseData {

    private String eventId;
    private String evidenceType;
    private String evidenceUrl;
    private String evidenceMimeType;
    private Long evidenceCapturedAtMs;

    public IngestEvidenceResponseData() {
    }

    public IngestEvidenceResponseData(String eventId,
                                      String evidenceType,
                                      String evidenceUrl,
                                      String evidenceMimeType,
                                      Long evidenceCapturedAtMs) {
        this.eventId = eventId;
        this.evidenceType = evidenceType;
        this.evidenceUrl = evidenceUrl;
        this.evidenceMimeType = evidenceMimeType;
        this.evidenceCapturedAtMs = evidenceCapturedAtMs;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public void setEvidenceType(String evidenceType) {
        this.evidenceType = evidenceType;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public String getEvidenceMimeType() {
        return evidenceMimeType;
    }

    public void setEvidenceMimeType(String evidenceMimeType) {
        this.evidenceMimeType = evidenceMimeType;
    }

    public Long getEvidenceCapturedAtMs() {
        return evidenceCapturedAtMs;
    }

    public void setEvidenceCapturedAtMs(Long evidenceCapturedAtMs) {
        this.evidenceCapturedAtMs = evidenceCapturedAtMs;
    }
}
