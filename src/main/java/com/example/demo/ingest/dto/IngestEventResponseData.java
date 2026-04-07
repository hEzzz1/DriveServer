package com.example.demo.ingest.dto;

public class IngestEventResponseData {
    private boolean accepted;

    public IngestEventResponseData() {
    }

    public IngestEventResponseData(boolean accepted) {
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
