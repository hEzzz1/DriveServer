package com.example.demo.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_audit_log")
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_enterprise_id")
    private Long operatorEnterpriseId;

    @Column(name = "operator_name", nullable = false, length = 64)
    private String operatorName;

    @Column(nullable = false, length = 64)
    private String module;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Lob
    @Column(name = "detail_json", columnDefinition = "json")
    private String detailJson;

    @Column(length = 64)
    private String ip;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType;

    @Column(name = "action_by")
    private Long actionBy;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "action_target_type", nullable = false, length = 64)
    private String actionTargetType;

    @Column(name = "action_target_id", length = 64)
    private String actionTargetId;

    @Column(name = "target_enterprise_id")
    private Long targetEnterpriseId;

    @Column(name = "action_result", nullable = false, length = 32)
    private String actionResult;

    @Column(name = "action_remark", length = 255)
    private String actionRemark;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public Long getOperatorEnterpriseId() {
        return operatorEnterpriseId;
    }

    public void setOperatorEnterpriseId(Long operatorEnterpriseId) {
        this.operatorEnterpriseId = operatorEnterpriseId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getActionBy() {
        return actionBy;
    }

    public void setActionBy(Long actionBy) {
        this.actionBy = actionBy;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public String getActionTargetType() {
        return actionTargetType;
    }

    public void setActionTargetType(String actionTargetType) {
        this.actionTargetType = actionTargetType;
    }

    public String getActionTargetId() {
        return actionTargetId;
    }

    public void setActionTargetId(String actionTargetId) {
        this.actionTargetId = actionTargetId;
    }

    public Long getTargetEnterpriseId() {
        return targetEnterpriseId;
    }

    public void setTargetEnterpriseId(Long targetEnterpriseId) {
        this.targetEnterpriseId = targetEnterpriseId;
    }

    public String getActionResult() {
        return actionResult;
    }

    public void setActionResult(String actionResult) {
        this.actionResult = actionResult;
    }

    public String getActionRemark() {
        return actionRemark;
    }

    public void setActionRemark(String actionRemark) {
        this.actionRemark = actionRemark;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
