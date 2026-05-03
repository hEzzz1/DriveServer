package com.example.demo.rule.controller;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.service.DeviceService;
import com.example.demo.rule.dto.EdgeConfigResponseData;
import com.example.demo.rule.service.EdgeRuntimeConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edge")
public class EdgeConfigController {

    private final DeviceService deviceService;
    private final EdgeRuntimeConfigService edgeRuntimeConfigService;

    public EdgeConfigController(DeviceService deviceService, EdgeRuntimeConfigService edgeRuntimeConfigService) {
        this.deviceService = deviceService;
        this.edgeRuntimeConfigService = edgeRuntimeConfigService;
    }

    @GetMapping("/config")
    public ApiResponse<EdgeConfigResponseData> config(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                      @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        deviceService.authenticateAndTouch(deviceCode, deviceToken);
        return ApiResponse.success(edgeRuntimeConfigService.currentConfig());
    }
}
