package com.example.demo.device.controller;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.dto.ClaimEdgeDeviceRequest;
import com.example.demo.device.dto.DeviceActivateRequest;
import com.example.demo.device.dto.DeviceActivateResponseData;
import com.example.demo.device.dto.DeviceClaimResponseData;
import com.example.demo.device.dto.DeviceContextResponseData;
import com.example.demo.device.dto.DeviceTelemetryRequest;
import com.example.demo.device.service.EdgeDeviceClaimService;
import com.example.demo.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edge/device")
public class EdgeDeviceController {

    private final DeviceService deviceService;
    private final EdgeDeviceClaimService edgeDeviceClaimService;

    public EdgeDeviceController(DeviceService deviceService,
                                EdgeDeviceClaimService edgeDeviceClaimService) {
        this.deviceService = deviceService;
        this.edgeDeviceClaimService = edgeDeviceClaimService;
    }

    @PostMapping("/claim")
    public ApiResponse<DeviceClaimResponseData> claim(@Valid @RequestBody ClaimEdgeDeviceRequest request) {
        return ApiResponse.success(edgeDeviceClaimService.claim(request));
    }

    @Deprecated(since = "0.1.0", forRemoval = false)
    @PostMapping("/activate")
    public ApiResponse<DeviceActivateResponseData> activate(@Valid @RequestBody DeviceActivateRequest request) {
        return ApiResponse.success(deviceService.activate(request));
    }

    @GetMapping("/context")
    public ApiResponse<DeviceContextResponseData> context(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                          @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ApiResponse.success(deviceService.getContext(deviceCode, deviceToken));
    }

    @PostMapping("/telemetry")
    public ApiResponse<Boolean> telemetry(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                          @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
                                          @Valid @RequestBody DeviceTelemetryRequest request) {
        deviceService.recordEdgeTelemetry(deviceCode, deviceToken, request);
        return ApiResponse.success(true);
    }
}
