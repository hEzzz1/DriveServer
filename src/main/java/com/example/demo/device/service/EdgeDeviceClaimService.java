package com.example.demo.device.service;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.dto.ClaimEdgeDeviceRequest;
import com.example.demo.device.dto.DeviceBindingViewData;
import com.example.demo.device.dto.DeviceClaimResponseData;
import com.example.demo.device.entity.Device;
import com.example.demo.device.entity.EdgeDeviceBindLog;
import com.example.demo.device.model.EdgeDeviceBindLogAction;
import com.example.demo.device.model.EdgeDeviceEffectiveStage;
import com.example.demo.device.model.EdgeDeviceEnterpriseBindStatus;
import com.example.demo.device.model.EdgeDeviceLifecycleStatus;
import com.example.demo.device.model.EdgeDeviceSessionStage;
import com.example.demo.device.model.EdgeDeviceStatus;
import com.example.demo.device.model.EdgeDeviceVehicleBindStatus;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindLogRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.service.EnterpriseActivationCodeService;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class EdgeDeviceClaimService {

    private static final String OPERATOR_TYPE_EDGE = "EDGE_DEVICE";

    private final DeviceRepository deviceRepository;
    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final EdgeDeviceBindLogRepository edgeDeviceBindLogRepository;
    private final EnterpriseActivationCodeService enterpriseActivationCodeService;
    private final DeviceService deviceService;

    public EdgeDeviceClaimService(DeviceRepository deviceRepository,
                                  FleetRepository fleetRepository,
                                  VehicleRepository vehicleRepository,
                                  DrivingSessionRepository drivingSessionRepository,
                                  EdgeDeviceBindLogRepository edgeDeviceBindLogRepository,
                                  EnterpriseActivationCodeService enterpriseActivationCodeService,
                                  DeviceService deviceService) {
        this.deviceRepository = deviceRepository;
        this.fleetRepository = fleetRepository;
        this.vehicleRepository = vehicleRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.edgeDeviceBindLogRepository = edgeDeviceBindLogRepository;
        this.enterpriseActivationCodeService = enterpriseActivationCodeService;
        this.deviceService = deviceService;
    }

    @Transactional
    public DeviceClaimResponseData claim(ClaimEdgeDeviceRequest request) {
        Enterprise enterprise = enterpriseActivationCodeService.resolveActiveEnterprise(request.enterpriseActivationCode());
        String deviceCode = normalizeRequired(request.deviceCode(), "deviceCode不能为空");
        String deviceName = normalizeOptional(request.deviceName());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Device device = deviceRepository.findByDeviceCode(deviceCode).orElse(null);
        EdgeDeviceBindLogAction action = EdgeDeviceBindLogAction.CLAIMED;
        if (device == null) {
            device = new Device();
            device.setDeviceCode(deviceCode);
            device.setCreatedAt(now);
            device.setStatus(EdgeDeviceStatus.NEW.name());
        } else if (device.getEnterpriseId() != null && !device.getEnterpriseId().equals(enterprise.getId())) {
            throw new BusinessException(ApiCode.DEVICE_BOUND_TO_OTHER_ENTERPRISE, ApiCode.DEVICE_BOUND_TO_OTHER_ENTERPRISE.getMessage());
        } else if (device.getEnterpriseId() != null) {
            action = EdgeDeviceBindLogAction.AUTO_RECOVERED;
        }

        device.setDeviceName(StringUtils.hasText(deviceName) ? deviceName : defaultDeviceName(device));
        if (device.getEnterpriseId() == null) {
            device.setEnterpriseId(enterprise.getId());
            if (device.getVehicleId() != null) {
                Vehicle vehicle = vehicleRepository.findById(device.getVehicleId()).orElse(null);
                if (vehicle == null || !enterprise.getId().equals(vehicle.getEnterpriseId())) {
                    device.setFleetId(null);
                    device.setVehicleId(null);
                }
            }
            if (device.getFleetId() != null && device.getVehicleId() == null) {
                Fleet fleet = fleetRepository.findById(device.getFleetId()).orElse(null);
                if (fleet == null || !enterprise.getId().equals(fleet.getEnterpriseId())) {
                    device.setFleetId(null);
                }
            }
        }

        device.setDeviceToken(generateSecretToken());
        device.setTokenRotatedAt(now);
        device.setLastActivatedAt(now);
        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        if (!EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            device.setStatus(EdgeDeviceStatus.BOUND.name());
        }
        Device saved = deviceRepository.save(device);
        appendBindLog(saved, enterprise, request.enterpriseActivationCode(), action, now);

        Fleet fleet = saved.getFleetId() == null ? null : fleetRepository.findById(saved.getFleetId()).orElse(null);
        Vehicle vehicle = saved.getVehicleId() == null ? null : vehicleRepository.findById(saved.getVehicleId()).orElse(null);
        DrivingSession activeSession = drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(saved.getId(), SessionStatus.ACTIVE.getCode()).orElse(null);

        EdgeDeviceLifecycleStatus lifecycleStatus = deviceService.resolveLifecycleStatus(saved);
        EdgeDeviceEnterpriseBindStatus enterpriseBindStatus = deviceService.resolveEnterpriseBindStatus(saved, null);
        EdgeDeviceVehicleBindStatus vehicleBindStatus = deviceService.resolveVehicleBindStatus(saved);
        EdgeDeviceSessionStage sessionStage = deviceService.resolveSessionStage(activeSession);
        EdgeDeviceEffectiveStage effectiveStage = deviceService.resolveEffectiveStage(lifecycleStatus, enterpriseBindStatus, vehicleBindStatus, sessionStage);

        return new DeviceClaimResponseData(
                new DeviceBindingViewData.ClaimedDeviceData(saved.getId(), saved.getDeviceCode(), saved.getDeviceName(), saved.getDeviceToken(), lifecycleStatus.name()),
                new DeviceBindingViewData.NamedResourceData(enterprise.getId(), enterprise.getName()),
                fleet == null ? null : new DeviceBindingViewData.NamedResourceData(fleet.getId(), fleet.getName()),
                vehicle == null ? null : new DeviceBindingViewData.VehicleData(vehicle.getId(), vehicle.getPlateNumber()),
                vehicleBindStatus.name(),
                sessionStage.name(),
                effectiveStage.name(),
                toOffsetDateTime(now));
    }

    private void appendBindLog(Device device,
                               Enterprise enterprise,
                               String activationCode,
                               EdgeDeviceBindLogAction action,
                               LocalDateTime createdAt) {
        EdgeDeviceBindLog bindLog = new EdgeDeviceBindLog();
        bindLog.setDeviceId(device.getId());
        bindLog.setDeviceCode(device.getDeviceCode());
        bindLog.setEnterpriseId(enterprise.getId());
        bindLog.setEnterpriseNameSnapshot(enterprise.getName());
        bindLog.setActivationCodeMasked(enterpriseActivationCodeService.maskActivationCode(activationCode));
        bindLog.setAction(action.name());
        bindLog.setOperatorType(OPERATOR_TYPE_EDGE);
        bindLog.setOperatorId(null);
        bindLog.setRemark(action == EdgeDeviceBindLogAction.AUTO_RECOVERED ? "同企业重复认领，按幂等恢复" : "企业激活码认领设备");
        bindLog.setCreatedAt(createdAt);
        edgeDeviceBindLogRepository.save(bindLog);
    }

    private String defaultDeviceName(Device device) {
        if (StringUtils.hasText(device.getDeviceName())) {
            return device.getDeviceName().trim();
        }
        return device.getDeviceCode();
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateSecretToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
