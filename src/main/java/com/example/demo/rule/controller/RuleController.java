package com.example.demo.rule.controller;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.RiskAdminOrSuperAdmin;
import com.example.demo.common.api.ApiResponse;
import com.example.demo.rule.dto.RuleConfigDetailData;
import com.example.demo.rule.dto.RuleConfigPageResponseData;
import com.example.demo.rule.dto.RuleConfigVersionItemData;
import com.example.demo.rule.dto.RuleOperationResponseData;
import com.example.demo.rule.dto.RulePublishRequest;
import com.example.demo.rule.dto.RuleRollbackRequest;
import com.example.demo.rule.dto.RuleUpsertRequest;
import com.example.demo.rule.service.RuleConfigService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleConfigService ruleConfigService;

    public RuleController(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @GetMapping
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleConfigPageResponseData> list(@RequestParam(required = false) Integer page,
                                                        @RequestParam(required = false) Integer size,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) Boolean enabled,
                                                        @RequestParam(required = false) String keyword) {
        return ApiResponse.success(ruleConfigService.listRules(page, size, status, enabled, keyword));
    }

    @GetMapping("/{id}")
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleConfigDetailData> detail(@PathVariable Long id) {
        return ApiResponse.success(ruleConfigService.getRule(id));
    }

    @PostMapping
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleOperationResponseData> create(@Valid @RequestBody RuleUpsertRequest request,
                                                         Authentication authentication) {
        return ApiResponse.success(ruleConfigService.createRule(request, currentUser(authentication)));
    }

    @PutMapping("/{id}")
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleOperationResponseData> update(@PathVariable Long id,
                                                         @Valid @RequestBody RuleUpsertRequest request,
                                                         Authentication authentication) {
        return ApiResponse.success(ruleConfigService.updateRule(id, request, currentUser(authentication)));
    }

    @PostMapping("/{id}/publish")
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleOperationResponseData> publish(@PathVariable Long id,
                                                          @Valid @RequestBody(required = false) RulePublishRequest request,
                                                          Authentication authentication) {
        RulePublishRequest normalized = request == null ? new RulePublishRequest(null) : request;
        return ApiResponse.success(ruleConfigService.publishRule(id, normalized, currentUser(authentication)));
    }

    @PostMapping("/{id}/toggle")
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleOperationResponseData> toggle(@PathVariable Long id,
                                                         Authentication authentication) {
        return ApiResponse.success(ruleConfigService.toggleRule(id, currentUser(authentication)));
    }

    @GetMapping("/{id}/versions")
    @RiskAdminOrSuperAdmin
    public ApiResponse<List<RuleConfigVersionItemData>> versions(@PathVariable Long id) {
        return ApiResponse.success(ruleConfigService.listVersions(id));
    }

    @PostMapping("/{id}/rollback")
    @RiskAdminOrSuperAdmin
    public ApiResponse<RuleOperationResponseData> rollback(@PathVariable Long id,
                                                           @Valid @RequestBody RuleRollbackRequest request,
                                                           Authentication authentication) {
        return ApiResponse.success(ruleConfigService.rollbackRule(id, request, currentUser(authentication)));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
