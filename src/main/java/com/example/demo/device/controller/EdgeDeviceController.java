package com.example.demo.device.controller;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.dto.CreateEdgeDeviceBindRequest;
import com.example.demo.device.dto.DeviceActivateRequest;
import com.example.demo.device.dto.DeviceActivateResponseData;
import com.example.demo.device.dto.DeviceContextResponseData;
import com.example.demo.device.dto.EdgeDeviceBindRequestResponseData;
import com.example.demo.device.service.EdgeDeviceBindRequestService;
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
    private final EdgeDeviceBindRequestService edgeDeviceBindRequestService;

    public EdgeDeviceController(DeviceService deviceService,
                                EdgeDeviceBindRequestService edgeDeviceBindRequestService) {
        this.deviceService = deviceService;
        this.edgeDeviceBindRequestService = edgeDeviceBindRequestService;
    }

    @PostMapping("/activate")
    public ApiResponse<DeviceActivateResponseData> activate(@Valid @RequestBody DeviceActivateRequest request) {
        return ApiResponse.success(deviceService.activate(request));
    }

    @GetMapping("/context")
    public ApiResponse<DeviceContextResponseData> context(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                          @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ApiResponse.success(deviceService.getContext(deviceCode, deviceToken));
    }

    @PostMapping("/bind-requests")
    public ApiResponse<EdgeDeviceBindRequestResponseData> createBindRequest(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                                            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
                                                                            @Valid @RequestBody CreateEdgeDeviceBindRequest request) {
        return ApiResponse.success(edgeDeviceBindRequestService.create(deviceCode, deviceToken, request));
    }

    @GetMapping("/bind-requests/current")
    public ApiResponse<EdgeDeviceBindRequestResponseData> currentBindRequest(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                                             @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ApiResponse.success(edgeDeviceBindRequestService.current(deviceCode, deviceToken));
    }
}
