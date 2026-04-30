package com.example.demo.fleet.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.fleet.dto.CreateFleetRequest;
import com.example.demo.fleet.dto.FleetDetailResponseData;
import com.example.demo.fleet.dto.FleetPageResponseData;
import com.example.demo.fleet.dto.UpdateFleetRequest;
import com.example.demo.fleet.dto.UpdateFleetStatusRequest;
import com.example.demo.fleet.service.FleetManagementService;
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
@RequestMapping({"/api/v1/fleets", "/api/v1/org/fleets"})
public class FleetController {

    private final FleetManagementService fleetManagementService;

    public FleetController(FleetManagementService fleetManagementService) {
        this.fleetManagementService = fleetManagementService;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'fleet.read')")
    public ApiResponse<FleetPageResponseData> listFleets(@RequestParam(required = false) Integer page,
                                                         @RequestParam(required = false) Integer size,
                                                         @RequestParam(required = false) Long enterpriseId,
                                                         @RequestParam(required = false) String keyword,
                                                         Authentication authentication) {
        return ApiResponse.success(fleetManagementService.listFleets(currentUser(authentication), page, size, enterpriseId, keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'fleet.read')")
    public ApiResponse<FleetDetailResponseData> getFleet(@PathVariable Long id, Authentication authentication) {
        return ApiResponse.success(fleetManagementService.getFleet(currentUser(authentication), id));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'fleet.manage')")
    public ApiResponse<FleetDetailResponseData> createFleet(@Valid @RequestBody CreateFleetRequest request,
                                                            Authentication authentication) {
        return ApiResponse.success(fleetManagementService.createFleet(currentUser(authentication), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'fleet.manage')")
    public ApiResponse<FleetDetailResponseData> updateFleet(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateFleetRequest request,
                                                            Authentication authentication) {
        return ApiResponse.success(fleetManagementService.updateFleet(currentUser(authentication), id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionAuthorizationService.hasPermission(authentication, 'fleet.manage')")
    public ApiResponse<FleetDetailResponseData> updateFleetStatus(@PathVariable Long id,
                                                                  @Valid @RequestBody UpdateFleetStatusRequest request,
                                                                  Authentication authentication) {
        return ApiResponse.success(fleetManagementService.updateStatus(currentUser(authentication), id, request.status()));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
