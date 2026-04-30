package com.example.demo.device.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "edge_device_bind_log")
public class EdgeDeviceBindLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "device_code", nullable = false, length = 64)
    private String deviceCode;

    @Column(name = "enterprise_id", nullable = false)
    private Long enterpriseId;

    @Column(name = "enterprise_name_snapshot", length = 128)
    private String enterpriseNameSnapshot;

    @Column(name = "activation_code_masked", length = 64)
    private String activationCodeMasked;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "operator_type", nullable = false, length = 32)
    private String operatorType;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public Long getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(Long enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getEnterpriseNameSnapshot() {
        return enterpriseNameSnapshot;
    }

    public void setEnterpriseNameSnapshot(String enterpriseNameSnapshot) {
        this.enterpriseNameSnapshot = enterpriseNameSnapshot;
    }

    public String getActivationCodeMasked() {
        return activationCodeMasked;
    }

    public void setActivationCodeMasked(String activationCodeMasked) {
        this.activationCodeMasked = activationCodeMasked;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
