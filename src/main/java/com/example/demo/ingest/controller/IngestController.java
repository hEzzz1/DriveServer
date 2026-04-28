package com.example.demo.ingest.controller;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.ingest.dto.IngestEventRequest;
import com.example.demo.ingest.dto.IngestEventResponseData;
import com.example.demo.ingest.service.IngestService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
