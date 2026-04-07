package com.example.demo.alert.dto;

import jakarta.validation.constraints.Size;

public class AlertActionRequest {

    @Size(max = 255)
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
