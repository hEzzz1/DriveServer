package com.example.demo.device.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.EnterpriseAdminOrSuperAdmin;
import com.example.demo.auth.security.EnterpriseReadRole;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.device.dto.CreateDeviceRequest;
import com.example.demo.device.dto.DeviceDetailResponseData;
import com.example.demo.device.dto.DevicePageResponseData;
import com.example.demo.device.dto.ReassignDeviceVehicleRequest;
import com.example.demo.device.dto.RotateDeviceTokenResponseData;
import com.example.demo.device.dto.UpdateDeviceRequest;
import com.example.demo.device.dto.UpdateDeviceStatusRequest;
import com.example.demo.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    @EnterpriseReadRole
    public ApiResponse<DevicePageResponseData> listDevices(@RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size,
                                                           @RequestParam(required = false) Long enterpriseId,
                                                           @RequestParam(required = false) Long fleetId,
                                                           @RequestParam(required = false) Long vehicleId,
                                                           Authentication authentication) {
        return ApiResponse.success(deviceService.listDevices(currentUser(authentication), page, size, enterpriseId, fleetId, vehicleId));
    }

    @GetMapping("/{id}")
    @EnterpriseReadRole
    public ApiResponse<DeviceDetailResponseData> getDevice(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(deviceService.getDevice(currentUser(authentication), id));
    }

    @PostMapping
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<DeviceDetailResponseData> createDevice(@Valid @RequestBody CreateDeviceRequest request, Authentication authentication) {
        return ApiResponse.success(deviceService.createDevice(currentUser(authentication), request));
    }

    @PutMapping("/{id}")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<DeviceDetailResponseData> updateDevice(@PathVariable Long id,
                                                              @Valid @RequestBody UpdateDeviceRequest request,
                                                              Authentication authentication) {
        return ApiResponse.success(deviceService.updateDevice(currentUser(authentication), id, request));
    }

    @PutMapping("/{id}/status")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<DeviceDetailResponseData> updateDeviceStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody UpdateDeviceStatusRequest request,
                                                                    Authentication authentication) {
        return ApiResponse.success(deviceService.updateStatus(currentUser(authentication), id, request.status()));
    }

    @PutMapping("/{id}/vehicle")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<DeviceDetailResponseData> reassignVehicle(@PathVariable Long id,
                                                                 @Valid @RequestBody ReassignDeviceVehicleRequest request,
                                                                 Authentication authentication) {
        return ApiResponse.success(deviceService.reassignVehicle(currentUser(authentication), id, request));
    }

    @DeleteMapping("/{id}/vehicle")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<DeviceDetailResponseData> unassignVehicle(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(deviceService.unassignVehicle(currentUser(authentication), id));
    }

    @PostMapping("/{id}/rotate-token")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<RotateDeviceTokenResponseData> rotateToken(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(deviceService.rotateToken(currentUser(authentication), id));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
