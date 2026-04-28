package com.example.demo.session.controller;

import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.service.DeviceService;
import com.example.demo.session.dto.AvailableDriversResponseData;
import com.example.demo.session.dto.SessionCurrentResponseData;
import com.example.demo.session.dto.SignInSessionRequest;
import com.example.demo.session.dto.SignOutSessionRequest;
import com.example.demo.session.service.DrivingSessionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/edge")
public class EdgeSessionController {

    private final DeviceService deviceService;
    private final DrivingSessionService drivingSessionService;

    public EdgeSessionController(DeviceService deviceService, DrivingSessionService drivingSessionService) {
        this.deviceService = deviceService;
        this.drivingSessionService = drivingSessionService;
    }

    @GetMapping("/drivers/available")
    public ApiResponse<AvailableDriversResponseData> availableDrivers(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                                      @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ApiResponse.success(drivingSessionService.listAvailableDrivers(deviceService.authenticateAndTouch(deviceCode, deviceToken)));
    }

    @PostMapping("/sessions/sign-in")
    public ApiResponse<SessionCurrentResponseData> signIn(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                          @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
                                                          @Valid @RequestBody SignInSessionRequest request) {
        return ApiResponse.success(drivingSessionService.signIn(deviceService.authenticateAndTouch(deviceCode, deviceToken), request));
    }

    @PostMapping("/sessions/sign-out")
    public ApiResponse<SessionCurrentResponseData> signOut(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                           @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
                                                           @RequestBody(required = false) SignOutSessionRequest request) {
        return ApiResponse.success(drivingSessionService.signOut(deviceService.authenticateAndTouch(deviceCode, deviceToken), request == null ? null : request.remark()));
    }

    @GetMapping("/sessions/current")
    public ApiResponse<SessionCurrentResponseData> current(@RequestHeader(value = "X-Device-Code", required = false) String deviceCode,
                                                           @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        return ApiResponse.success(drivingSessionService.current(deviceService.authenticateAndTouch(deviceCode, deviceToken)));
    }
}
