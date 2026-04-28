package com.example.demo.vehicle.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.EnterpriseAdminOrSuperAdmin;
import com.example.demo.auth.security.EnterpriseReadRole;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.vehicle.dto.CreateVehicleRequest;
import com.example.demo.vehicle.dto.UpdateVehicleRequest;
import com.example.demo.vehicle.dto.UpdateVehicleStatusRequest;
import com.example.demo.vehicle.dto.VehicleDetailResponseData;
import com.example.demo.vehicle.dto.VehiclePageResponseData;
import com.example.demo.vehicle.service.VehicleManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    private final VehicleManagementService vehicleManagementService;

    public VehicleController(VehicleManagementService vehicleManagementService) {
        this.vehicleManagementService = vehicleManagementService;
    }

    @GetMapping
    @EnterpriseReadRole
    public ApiResponse<VehiclePageResponseData> listVehicles(@RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size,
                                                             @RequestParam(required = false) Long enterpriseId,
                                                             @RequestParam(required = false) Long fleetId,
                                                             @RequestParam(required = false) Byte status,
                                                             Authentication authentication) {
        return ApiResponse.success(vehicleManagementService.listVehicles(currentUser(authentication), page, size, enterpriseId, fleetId, status));
    }

    @GetMapping("/{id}")
    @EnterpriseReadRole
    public ApiResponse<VehicleDetailResponseData> getVehicle(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(vehicleManagementService.getVehicle(currentUser(authentication), id));
    }

    @PostMapping
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<VehicleDetailResponseData> createVehicle(@Valid @RequestBody CreateVehicleRequest request, Authentication authentication) {
        return ApiResponse.success(vehicleManagementService.createVehicle(currentUser(authentication), request));
    }

    @PutMapping("/{id}")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<VehicleDetailResponseData> updateVehicle(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateVehicleRequest request,
                                                                Authentication authentication) {
        return ApiResponse.success(vehicleManagementService.updateVehicle(currentUser(authentication), id, request));
    }

    @PutMapping("/{id}/status")
    @EnterpriseAdminOrSuperAdmin
    public ApiResponse<VehicleDetailResponseData> updateVehicleStatus(@PathVariable Long id,
                                                                      @Valid @RequestBody UpdateVehicleStatusRequest request,
                                                                      Authentication authentication) {
        return ApiResponse.success(vehicleManagementService.updateStatus(currentUser(authentication), id, request.status()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
