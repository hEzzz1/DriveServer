package com.example.demo.driver.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.driver.dto.CreateDriverRequest;
import com.example.demo.driver.dto.DriverDetailResponseData;
import com.example.demo.driver.dto.DriverPageResponseData;
import com.example.demo.driver.dto.ReassignDriverFleetRequest;
import com.example.demo.driver.dto.ResetDriverPinRequest;
import com.example.demo.driver.dto.UpdateDriverRequest;
import com.example.demo.driver.dto.UpdateDriverStatusRequest;
import com.example.demo.driver.service.DriverManagementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverManagementService driverManagementService;

    public DriverController(DriverManagementService driverManagementService) {
        this.driverManagementService = driverManagementService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.read')")
    public ApiResponse<DriverPageResponseData> listDrivers(@RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size,
                                                           @RequestParam(required = false) Long enterpriseId,
                                                           @RequestParam(required = false) Long fleetId,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) Byte status,
                                                           Authentication authentication) {
        return ApiResponse.success(driverManagementService.listDrivers(
                currentUser(authentication), page, size, enterpriseId, fleetId, keyword, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.read')")
    public ApiResponse<DriverDetailResponseData> getDriver(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(driverManagementService.getDriver(currentUser(authentication), id));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.manage')")
    public ApiResponse<DriverDetailResponseData> createDriver(@Valid @RequestBody CreateDriverRequest request,
                                                              Authentication authentication) {
        return ApiResponse.success(driverManagementService.createDriver(currentUser(authentication), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.manage')")
    public ApiResponse<DriverDetailResponseData> updateDriver(@PathVariable Long id,
                                                              @Valid @RequestBody UpdateDriverRequest request,
                                                              Authentication authentication) {
        return ApiResponse.success(driverManagementService.updateDriver(currentUser(authentication), id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.manage')")
    public ApiResponse<DriverDetailResponseData> updateDriverStatus(@PathVariable Long id,
                                                                    @Valid @RequestBody UpdateDriverStatusRequest request,
                                                                    Authentication authentication) {
        return ApiResponse.success(driverManagementService.updateStatus(currentUser(authentication), id, request.status()));
    }

    @PutMapping("/{id}/fleet")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.manage')")
    public ApiResponse<DriverDetailResponseData> reassignDriverFleet(@PathVariable Long id,
                                                                     @Valid @RequestBody ReassignDriverFleetRequest request,
                                                                     Authentication authentication) {
        return ApiResponse.success(driverManagementService.reassignFleet(currentUser(authentication), id, request));
    }

    @PostMapping("/{id}/reset-pin")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'driver.manage')")
    public ApiResponse<DriverDetailResponseData> resetDriverPin(@PathVariable Long id,
                                                                @Valid @RequestBody ResetDriverPinRequest request,
                                                                Authentication authentication) {
        return ApiResponse.success(driverManagementService.resetPin(currentUser(authentication), id, request.pin()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
