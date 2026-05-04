package com.example.demo.ingest.controller;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.ingest.dto.IngestEvidenceResponseData;
import com.example.demo.ingest.dto.IngestEventRequest;
import com.example.demo.ingest.dto.IngestEventResponseData;
import com.example.demo.ingest.service.IngestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/events")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping
    public ApiResponse<IngestEventResponseData> ingest(
            @RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody IngestEventRequest request) {
        return ApiResponse.success(ingestService.ingest(deviceCode, deviceToken, request));
    }

    @PostMapping("/evidence")
    public ApiResponse<IngestEvidenceResponseData> uploadEvidence(
            @RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestParam("eventId") @NotBlank String eventId,
            @RequestParam(value = "evidenceType", required = false) @Size(max = 32) String evidenceType,
            @RequestParam(value = "evidenceMimeType", required = false) @Size(max = 64) String evidenceMimeType,
            @RequestParam(value = "evidenceCapturedAtMs", required = false) Long evidenceCapturedAtMs,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(ingestService.uploadEvidence(
                deviceCode,
                deviceToken,
                eventId,
                evidenceType,
                evidenceMimeType,
                evidenceCapturedAtMs,
                file));
    }
}
