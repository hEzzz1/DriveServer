package com.example.demo.device.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.dto.CreateDeviceRequest;
import com.example.demo.device.dto.DeviceActivateRequest;
import com.example.demo.device.dto.DeviceActivateResponseData;
import com.example.demo.device.dto.DeviceBindingViewData;
import com.example.demo.device.dto.DeviceContextResponseData;
import com.example.demo.device.dto.DeviceDetailResponseData;
import com.example.demo.device.dto.DeviceListItemData;
import com.example.demo.device.dto.DevicePageResponseData;
import com.example.demo.device.dto.EdgeDeviceBindRequestResponseData;
import com.example.demo.device.dto.ReassignDeviceVehicleRequest;
import com.example.demo.device.dto.RotateDeviceTokenResponseData;
import com.example.demo.device.dto.UpdateDeviceRequest;
import com.example.demo.device.entity.Device;
import com.example.demo.device.entity.EdgeDeviceBindRequest;
import com.example.demo.device.entity.EdgeDeviceBindRequestHistory;
import com.example.demo.device.model.EdgeDeviceBindRequestHistoryAction;
import com.example.demo.device.model.EdgeDeviceBindRequestStatus;
import com.example.demo.device.model.EdgeDeviceBindStatus;
import com.example.demo.device.model.EdgeDeviceEffectiveStage;
import com.example.demo.device.model.EdgeDeviceEnterpriseBindStatus;
import com.example.demo.device.model.EdgeDeviceLifecycleStatus;
import com.example.demo.device.model.EdgeDeviceSessionStage;
import com.example.demo.device.model.EdgeDeviceStatus;
import com.example.demo.device.model.EdgeDeviceVehicleBindStatus;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindRequestHistoryRepository;
import com.example.demo.device.repository.EdgeDeviceBindRequestRepository;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.rule.service.EdgeConfigVersionResolver;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final String SYSTEM_OPERATOR_NAME = "系统";

    private final DeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;
    private final EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository;
    private final EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;
    private final EdgeConfigVersionResolver edgeConfigVersionResolver;

    public DeviceService(DeviceRepository deviceRepository,
                         VehicleRepository vehicleRepository,
                         DrivingSessionRepository drivingSessionRepository,
                         EnterpriseRepository enterpriseRepository,
                         FleetRepository fleetRepository,
                         DriverRepository driverRepository,
                         EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository,
                         EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository,
                         BusinessAccessService businessAccessService,
                         SystemAuditService systemAuditService,
                         EdgeConfigVersionResolver edgeConfigVersionResolver) {
        this.deviceRepository = deviceRepository;
        this.vehicleRepository = vehicleRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.driverRepository = driverRepository;
        this.edgeDeviceBindRequestRepository = edgeDeviceBindRequestRepository;
        this.edgeDeviceBindRequestHistoryRepository = edgeDeviceBindRequestHistoryRepository;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
        this.edgeConfigVersionResolver = edgeConfigVersionResolver;
    }

    @Transactional
    public DevicePageResponseData listDevices(AuthenticatedUser operator, Integer page, Integer size, Long enterpriseId, Long fleetId, Long vehicleId) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = enterpriseId == null ? null : businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        if (readableEnterpriseId == null && !businessAccessService.isSuperAdmin(operator)) {
            readableEnterpriseId = businessAccessService.requireOperatorEnterpriseId(operator);
        }
        Specification<Device> specification = buildSpecification(readableEnterpriseId, fleetId, vehicleId);
        Page<Device> result = deviceRepository.findAll(specification, PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));
        DeviceViewRefs refs = loadDeviceViewRefs(result.getContent());
        return new DevicePageResponseData(result.getTotalElements(), pageNo, pageSize,
                result.getContent().stream().map(device -> toListItem(device, refs)).toList());
    }

    @Transactional
    public DeviceDetailResponseData getDevice(AuthenticatedUser operator, Long deviceId) {
        Device device = getDeviceEntity(deviceId);
        assertCanAccessDevice(operator, device);
        return toDetail(device, loadDeviceViewRefs(List.of(device)));
    }

    @Transactional
    public DeviceDetailResponseData createDevice(AuthenticatedUser operator, CreateDeviceRequest request) {
        String deviceCode = normalizeRequired(request.deviceCode(), "deviceCode不能为空");
        if (deviceRepository.existsByDeviceCode(deviceCode)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "deviceCode已存在");
        }

        BindingResolution resolution = resolveBindingForCreate(operator, request.enterpriseId(), request.fleetId(), request.vehicleId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Device device = new Device();
        device.setEnterpriseId(resolution.enterpriseId());
        device.setFleetId(resolution.fleetId());
        device.setVehicleId(resolution.vehicleId());
        device.setDeviceCode(deviceCode);
        device.setDeviceName(normalizeRequired(request.deviceName(), "deviceName不能为空"));
        device.setActivationCode(normalizeOptional(request.activationCode()));
        device.setStatus(resolution.enterpriseId() == null ? EdgeDeviceStatus.NEW.name() : EdgeDeviceStatus.BOUND.name());
        device.setRemark(normalizeOptional(request.remark()));
        device.setCreatedAt(now);
        device.setUpdatedAt(now);
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "CREATE_DEVICE", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "创建设备", auditDetail(operator, saved, null, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public DeviceDetailResponseData updateDevice(AuthenticatedUser operator, Long deviceId, UpdateDeviceRequest request) {
        Device device = getDeviceEntity(deviceId);
        assertCanAccessDevice(operator, device);
        Map<String, Object> before = snapshot(device);
        device.setDeviceName(normalizeRequired(request.deviceName(), "deviceName不能为空"));
        device.setActivationCode(normalizeOptional(request.activationCode()));
        device.setRemark(normalizeOptional(request.remark()));
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "UPDATE_DEVICE", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "更新设备", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public DeviceDetailResponseData updateStatus(AuthenticatedUser operator, Long deviceId, Byte status) {
        Device device = getDeviceEntity(deviceId);
        assertCanAccessDevice(operator, device);
        Map<String, Object> before = snapshot(device);
        device.setStatus(normalizeStatus(status, device));
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "UPDATE_DEVICE_STATUS", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "更新设备状态", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public DeviceDetailResponseData reassignVehicle(AuthenticatedUser operator, Long deviceId, ReassignDeviceVehicleRequest request) {
        Device device = getDeviceEntity(deviceId);
        assertCanAccessDevice(operator, device);
        DeviceViewRefs refs = loadDeviceViewRefs(List.of(device));
        DeviceState state = buildDeviceState(device, refs);
        assertEnterpriseApprovedOrThrow(state.enterpriseBindStatus());
        assertNoActiveSession(device);

        Vehicle vehicle = getVehicleEntity(request.vehicleId());
        if (!vehicle.getEnterpriseId().equals(device.getEnterpriseId())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "vehicleId不属于当前enterprise");
        }
        validateVehicleAvailability(vehicle.getId(), device.getId());

        Map<String, Object> before = snapshot(device);
        device.setFleetId(vehicle.getFleetId());
        device.setVehicleId(vehicle.getId());
        if (!EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            device.setStatus(deriveBoundStatus(device).name());
        }
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "REASSIGN_DEVICE_VEHICLE", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "调整设备绑定车辆", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public DeviceDetailResponseData unassignVehicle(AuthenticatedUser operator, Long deviceId) {
        Device device = getDeviceEntity(deviceId);
        assertCanAccessDevice(operator, device);
        DeviceViewRefs refs = loadDeviceViewRefs(List.of(device));
        DeviceState state = buildDeviceState(device, refs);
        assertEnterpriseApprovedOrThrow(state.enterpriseBindStatus());
        assertNoActiveSession(device);

        Map<String, Object> before = snapshot(device);
        device.setFleetId(null);
        device.setVehicleId(null);
        if (!EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            device.setStatus(deriveBoundStatus(device).name());
        }
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "UNASSIGN_DEVICE_VEHICLE", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "取消设备车辆分配", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public RotateDeviceTokenResponseData rotateToken(AuthenticatedUser operator, Long deviceId) {
        Device device = getDeviceEntity(deviceId);
        assertCanAccessDevice(operator, device);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        device.setDeviceToken(generateSecretToken());
        device.setTokenRotatedAt(now);
        deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "ROTATE_DEVICE_TOKEN", "DEVICE", String.valueOf(device.getId()),
                "SUCCESS", "轮换设备token", auditDetail(operator, device, null, snapshot(device)));
        return new RotateDeviceTokenResponseData(device.getId(), device.getDeviceCode(), device.getDeviceToken(), toOffsetDateTime(now));
    }

    @Transactional
    public DeviceActivateResponseData activate(DeviceActivateRequest request) {
        Device device = deviceRepository.findByDeviceCode(normalizeRequired(request.deviceCode(), "deviceCode不能为空"))
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, "设备不存在"));
        if (EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            throw new BusinessException(ApiCode.FORBIDDEN, "设备已禁用");
        }
        if (!StringUtils.hasText(device.getActivationCode()) || !device.getActivationCode().equals(request.activationCode().trim())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "激活码错误");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        device.setDeviceToken(generateSecretToken());
        device.setLastActivatedAt(now);
        device.setTokenRotatedAt(now);
        syncDeviceStatusAfterBindRequestChange(device, currentBindRequestStatus(device));
        systemAuditService.record(null, "DEVICE", "DEVICE_ACTIVATE", "DEVICE", String.valueOf(device.getId()),
                "SUCCESS", "设备激活", Map.of("deviceId", device.getId(), "deviceCode", device.getDeviceCode()));
        return new DeviceActivateResponseData(device.getId(), device.getDeviceCode(), device.getDeviceName(), device.getDeviceToken(),
                device.getEnterpriseId(), device.getFleetId(), device.getVehicleId(), toOffsetDateTime(now));
    }

    @Transactional
    public DeviceContextResponseData getContext(String deviceCode, String deviceToken) {
        Device device = authenticateAndTouch(deviceCode, deviceToken).device();
        DeviceViewRefs refs = loadDeviceViewRefs(List.of(device));
        DeviceState state = buildDeviceState(device, refs);
        Enterprise enterprise = refs.enterprisesById().get(device.getEnterpriseId());
        Fleet fleet = refs.fleetsById().get(device.getFleetId());
        Vehicle vehicle = refs.vehiclesById().get(device.getVehicleId());
        DrivingSession activeSession = refs.activeSessionsByDeviceId().get(device.getId());
        Driver currentDriver = activeSession == null ? null : refs.driversById().get(activeSession.getDriverId());
        return new DeviceContextResponseData(
                device.getId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getStatus(),
                resolveBindStatus(state.enterpriseBindStatus()).name(),
                null,
                new DeviceBindingViewData.ContextDeviceData(
                        device.getId(),
                        device.getDeviceCode(),
                        device.getDeviceName(),
                        state.lifecycleStatus().name(),
                        toOffsetDateTime(device.getLastActivatedAt()),
                        toOffsetDateTime(device.getLastSeenAt())),
                state.lifecycleStatus().name(),
                state.enterpriseBindStatus().name(),
                state.vehicleBindStatus().name(),
                state.sessionStage().name(),
                state.effectiveStage().name(),
                toNamedResourceData(enterprise),
                toNamedResourceData(fleet),
                toVehicleData(vehicle),
                null,
                toSessionData(activeSession),
                device.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                device.getFleetId(),
                fleet == null ? null : fleet.getName(),
                device.getVehicleId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                currentDriver == null ? null : currentDriver.getId(),
                currentDriver == null ? null : currentDriver.getDriverCode(),
                currentDriver == null ? null : currentDriver.getName(),
                activeSession == null ? null : activeSession.getId(),
                activeSession == null ? null : activeSession.getSessionNo(),
                activeSession == null ? null : toOffsetDateTime(activeSession.getSignInTime()),
                activeSession == null ? null : activeSession.getStatus(),
                edgeConfigVersionResolver.resolveCurrentVersion());
    }

    @Transactional(readOnly = true)
    public DeviceAuthContext authenticate(String deviceCode, String deviceToken) {
        return new DeviceAuthContext(authenticateDevice(deviceCode, deviceToken));
    }

    @Transactional
    public DeviceAuthContext authenticateAndTouch(String deviceCode, String deviceToken) {
        Device device = authenticateDevice(deviceCode, deviceToken);
        touchOnlineAndActiveSession(device);
        return new DeviceAuthContext(device);
    }

    @Transactional(readOnly = true)
    public Device requireDevice(Long deviceId) {
        return getDeviceEntity(deviceId);
    }

    @Transactional
    public void ensureReadyForSignIn(Device device) {
        EdgeDeviceEnterpriseBindStatus enterpriseBindStatus = resolveEnterpriseBindStatus(device, currentBindRequestStatus(device));
        assertEnterpriseApprovedOrThrow(enterpriseBindStatus);
        if (device.getVehicleId() == null) {
            throw new BusinessException(ApiCode.DEVICE_NOT_BOUND_VEHICLE, ApiCode.DEVICE_NOT_BOUND_VEHICLE.getMessage());
        }
    }

    @Transactional
    public void syncDeviceStatusAfterBindRequestChange(Device device) {
        syncDeviceStatusAfterBindRequestChange(device, currentBindRequestStatus(device));
    }

    @Transactional
    public String currentBindRequestStatus(Device device) {
        EdgeDeviceBindRequest request = edgeDeviceBindRequestRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.getId()).orElse(null);
        EdgeDeviceBindRequest refreshed = refreshBindRequestIfExpired(device, request);
        return refreshed == null ? null : refreshed.getStatus();
    }

    @Transactional
    public EdgeDeviceBindRequest refreshBindRequestIfExpired(Device device, EdgeDeviceBindRequest bindRequest) {
        if (bindRequest == null) {
            return null;
        }
        if (EdgeDeviceBindRequestStatus.PENDING.name().equals(bindRequest.getStatus())
                && bindRequest.getExpiresAt() != null
                && bindRequest.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            bindRequest.setStatus(EdgeDeviceBindRequestStatus.EXPIRED.name());
            bindRequest.setReviewedAt(now);
            bindRequest.setReviewedBy(null);
            bindRequest.setUpdatedAt(now);
            EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);
            appendBindRequestHistory(saved.getId(), EdgeDeviceBindRequestHistoryAction.EXPIRED, null, SYSTEM_OPERATOR_NAME, "绑定申请已过期", now);
            syncDeviceStatusAfterBindRequestChange(device, saved.getStatus());
            return saved;
        }
        return bindRequest;
    }

    public EdgeDeviceBindStatus resolveBindStatus(EdgeDeviceEnterpriseBindStatus enterpriseBindStatus) {
        return switch (enterpriseBindStatus) {
            case APPROVED -> EdgeDeviceBindStatus.BOUND;
            case PENDING -> EdgeDeviceBindStatus.PENDING;
            case REJECTED -> EdgeDeviceBindStatus.REJECTED;
            case UNBOUND, EXPIRED -> EdgeDeviceBindStatus.UNBOUND;
        };
    }

    public EdgeDeviceLifecycleStatus resolveLifecycleStatus(Device device) {
        if (EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            return EdgeDeviceLifecycleStatus.DISABLED;
        }
        if (device.getEnterpriseId() != null) {
            return EdgeDeviceLifecycleStatus.BOUND;
        }
        return EdgeDeviceLifecycleStatus.NEW;
    }

    public EdgeDeviceEnterpriseBindStatus resolveEnterpriseBindStatus(Device device, String currentBindRequestStatus) {
        if (device.getEnterpriseId() != null) {
            return EdgeDeviceEnterpriseBindStatus.APPROVED;
        }
        return EdgeDeviceEnterpriseBindStatus.UNBOUND;
    }

    public EdgeDeviceVehicleBindStatus resolveVehicleBindStatus(Device device) {
        return device.getVehicleId() == null ? EdgeDeviceVehicleBindStatus.UNASSIGNED : EdgeDeviceVehicleBindStatus.ASSIGNED;
    }

    public EdgeDeviceSessionStage resolveSessionStage(DrivingSession activeSession) {
        return activeSession == null ? EdgeDeviceSessionStage.IDLE : EdgeDeviceSessionStage.ACTIVE;
    }

    public EdgeDeviceEffectiveStage resolveEffectiveStage(EdgeDeviceLifecycleStatus lifecycleStatus,
                                                          EdgeDeviceEnterpriseBindStatus enterpriseBindStatus,
                                                          EdgeDeviceVehicleBindStatus vehicleBindStatus,
                                                          EdgeDeviceSessionStage sessionStage) {
        if (lifecycleStatus == EdgeDeviceLifecycleStatus.DISABLED) {
            return EdgeDeviceEffectiveStage.DISABLED;
        }
        if (sessionStage == EdgeDeviceSessionStage.ACTIVE) {
            return EdgeDeviceEffectiveStage.IN_SESSION;
        }
        return switch (enterpriseBindStatus) {
            case UNBOUND, PENDING, REJECTED, EXPIRED -> EdgeDeviceEffectiveStage.CLAIM_ENTERPRISE;
            case APPROVED -> vehicleBindStatus == EdgeDeviceVehicleBindStatus.ASSIGNED
                    ? EdgeDeviceEffectiveStage.READY_SIGN_IN
                    : EdgeDeviceEffectiveStage.WAITING_VEHICLE;
        };
    }

    private Device authenticateDevice(String deviceCode, String deviceToken) {
        if (!StringUtils.hasText(deviceCode) || !StringUtils.hasText(deviceToken)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "设备认证失败");
        }
        Device device = deviceRepository.findByDeviceCodeAndDeviceToken(deviceCode.trim(), deviceToken.trim())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, "设备认证失败"));
        if (EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            throw new BusinessException(ApiCode.FORBIDDEN, "设备已禁用");
        }
        return device;
    }

    private void touchOnlineAndActiveSession(Device device) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        device.setLastSeenAt(now);
        deviceRepository.save(device);
        drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode())
                .ifPresent(session -> {
                    session.setLastHeartbeatAt(now);
                    drivingSessionRepository.save(session);
                });
    }

    private Specification<Device> buildSpecification(Long enterpriseId, Long fleetId, Long vehicleId) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("enterpriseId"), enterpriseId));
            }
            if (fleetId != null) {
                predicates.add(cb.equal(root.get("fleetId"), fleetId));
            }
            if (vehicleId != null) {
                predicates.add(cb.equal(root.get("vehicleId"), vehicleId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Device getDeviceEntity(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private Vehicle getVehicleEntity(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "vehicleId不存在"));
    }

    private Fleet getFleetEntity(Long fleetId) {
        return fleetRepository.findById(fleetId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "fleetId不存在"));
    }

    private Enterprise getEnterpriseEntity(Long enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不存在"));
    }

    private void assertCanAccessDevice(AuthenticatedUser operator, Device device) {
        if (device.getEnterpriseId() != null) {
            businessAccessService.assertCanManageEnterprise(operator, device.getEnterpriseId());
            return;
        }
        if (!businessAccessService.isSuperAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    private BindingResolution resolveBindingForCreate(AuthenticatedUser operator, Long enterpriseId, Long fleetId, Long vehicleId) {
        if (vehicleId != null) {
            Vehicle vehicle = getVehicleEntity(vehicleId);
            businessAccessService.assertCanManageEnterprise(operator, vehicle.getEnterpriseId());
            if (enterpriseId != null && !enterpriseId.equals(vehicle.getEnterpriseId())) {
                throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId与vehicleId不匹配");
            }
            if (fleetId != null && !fleetId.equals(vehicle.getFleetId())) {
                throw new BusinessException(ApiCode.INVALID_PARAM, "fleetId与vehicleId不匹配");
            }
            validateVehicleAvailability(vehicle.getId(), null);
            return new BindingResolution(vehicle.getEnterpriseId(), vehicle.getFleetId(), vehicle.getId());
        }
        if (fleetId != null) {
            Fleet fleet = getFleetEntity(fleetId);
            businessAccessService.assertCanManageEnterprise(operator, fleet.getEnterpriseId());
            if (enterpriseId != null && !enterpriseId.equals(fleet.getEnterpriseId())) {
                throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId与fleetId不匹配");
            }
            return new BindingResolution(fleet.getEnterpriseId(), fleet.getId(), null);
        }
        if (enterpriseId != null) {
            Enterprise enterprise = getEnterpriseEntity(enterpriseId);
            businessAccessService.assertCanManageEnterprise(operator, enterprise.getId());
            return new BindingResolution(enterprise.getId(), null, null);
        }
        if (!businessAccessService.isSuperAdmin(operator)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return new BindingResolution(null, null, null);
    }

    private void validateVehicleAvailability(Long vehicleId, Long currentDeviceId) {
        Optional<Device> occupiedBy = deviceRepository.findByVehicleId(vehicleId);
        if (occupiedBy.isPresent() && !occupiedBy.get().getId().equals(currentDeviceId)) {
            throw new BusinessException(ApiCode.VEHICLE_ALREADY_BOUND, ApiCode.VEHICLE_ALREADY_BOUND.getMessage());
        }
    }

    private void assertNoActiveSession(Device device) {
        if (drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode()).isPresent()) {
            throw new BusinessException(ApiCode.DEVICE_ACTIVE_SESSION_CONFLICT, ApiCode.DEVICE_ACTIVE_SESSION_CONFLICT.getMessage());
        }
    }

    private void assertEnterpriseApprovedOrThrow(EdgeDeviceEnterpriseBindStatus enterpriseBindStatus) {
        switch (enterpriseBindStatus) {
            case APPROVED -> {
                return;
            }
            case PENDING, REJECTED, EXPIRED, UNBOUND ->
                    throw new BusinessException(ApiCode.DEVICE_NOT_BOUND_ENTERPRISE, ApiCode.DEVICE_NOT_BOUND_ENTERPRISE.getMessage());
        }
    }

    private DeviceListItemData toListItem(Device device, DeviceViewRefs refs) {
        DrivingSession activeSession = refs.activeSessionsByDeviceId().get(device.getId());
        Driver currentDriver = activeSession == null ? null : refs.driversById().get(activeSession.getDriverId());
        DeviceState state = buildDeviceState(device, refs);
        return new DeviceListItemData(device.getId(), device.getEnterpriseId(), device.getFleetId(), device.getVehicleId(),
                device.getDeviceCode(), device.getDeviceName(), device.getActivationCode(), device.getStatus(),
                state.lifecycleStatus().name(), state.enterpriseBindStatus().name(), state.vehicleBindStatus().name(),
                state.sessionStage().name(), state.effectiveStage().name(),
                toOffsetDateTime(device.getLastActivatedAt()), toOffsetDateTime(device.getLastSeenAt()), toOffsetDateTime(device.getTokenRotatedAt()),
                currentDriver == null ? null : currentDriver.getId(),
                currentDriver == null ? null : currentDriver.getDriverCode(),
                currentDriver == null ? null : currentDriver.getName(),
                activeSession == null ? null : activeSession.getId(),
                device.getRemark(),
                toOffsetDateTime(device.getCreatedAt()), toOffsetDateTime(device.getUpdatedAt()));
    }

    private DeviceDetailResponseData toDetail(Device device, DeviceViewRefs refs) {
        Enterprise enterprise = refs.enterprisesById().get(device.getEnterpriseId());
        Fleet fleet = refs.fleetsById().get(device.getFleetId());
        Vehicle vehicle = refs.vehiclesById().get(device.getVehicleId());
        DrivingSession activeSession = refs.activeSessionsByDeviceId().get(device.getId());
        Driver currentDriver = activeSession == null ? null : refs.driversById().get(activeSession.getDriverId());
        DeviceState state = buildDeviceState(device, refs);
        return new DeviceDetailResponseData(device.getId(), device.getEnterpriseId(), device.getFleetId(), device.getVehicleId(),
                device.getDeviceCode(), device.getDeviceName(), device.getActivationCode(), device.getStatus(),
                state.lifecycleStatus().name(), state.enterpriseBindStatus().name(), state.vehicleBindStatus().name(),
                state.sessionStage().name(), state.effectiveStage().name(),
                toNamedResourceData(enterprise), toNamedResourceData(fleet), toVehicleData(vehicle),
                toOffsetDateTime(device.getLastActivatedAt()), toOffsetDateTime(device.getLastSeenAt()), toOffsetDateTime(device.getTokenRotatedAt()),
                currentDriver == null ? null : currentDriver.getId(),
                currentDriver == null ? null : currentDriver.getDriverCode(),
                currentDriver == null ? null : currentDriver.getName(),
                activeSession == null ? null : activeSession.getId(),
                currentDriver == null ? null : new DeviceBindingViewData.DriverData(currentDriver.getId(), currentDriver.getDriverCode(), currentDriver.getName()),
                toSessionData(activeSession),
                device.getRemark(),
                toOffsetDateTime(device.getCreatedAt()), toOffsetDateTime(device.getUpdatedAt()));
    }

    private DeviceViewRefs loadDeviceViewRefs(Collection<Device> devices) {
        if (devices.isEmpty()) {
            return new DeviceViewRefs(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();

        Map<Long, DrivingSession> activeSessionsByDeviceId = new HashMap<>();
        for (DrivingSession session : drivingSessionRepository.findByStatusAndDeviceIdInOrderBySignInTimeDesc(SessionStatus.ACTIVE.getCode(), deviceIds)) {
            if (session.getDeviceId() != null) {
                activeSessionsByDeviceId.putIfAbsent(session.getDeviceId(), session);
            }
        }

        List<Long> driverIds = activeSessionsByDeviceId.values().stream().map(DrivingSession::getDriverId).filter(id -> id != null).toList();
        Map<Long, Driver> driversById = new HashMap<>();
        for (Driver driver : driverRepository.findAllById(driverIds)) {
            if (driver.getId() != null) {
                driversById.put(driver.getId(), driver);
            }
        }

        List<Long> enterpriseIds = devices.stream().map(Device::getEnterpriseId).filter(id -> id != null).distinct().toList();
        Map<Long, Enterprise> enterprisesById = new HashMap<>();
        for (Enterprise enterprise : enterpriseRepository.findAllById(enterpriseIds)) {
            if (enterprise.getId() != null) {
                enterprisesById.put(enterprise.getId(), enterprise);
            }
        }

        List<Long> fleetIds = devices.stream().map(Device::getFleetId).filter(id -> id != null).distinct().toList();
        Map<Long, Fleet> fleetsById = new HashMap<>();
        for (Fleet fleet : fleetRepository.findAllById(fleetIds)) {
            if (fleet.getId() != null) {
                fleetsById.put(fleet.getId(), fleet);
            }
        }

        List<Long> vehicleIds = devices.stream().map(Device::getVehicleId).filter(id -> id != null).distinct().toList();
        Map<Long, Vehicle> vehiclesById = new HashMap<>();
        for (Vehicle vehicle : vehicleRepository.findAllById(vehicleIds)) {
            if (vehicle.getId() != null) {
                vehiclesById.put(vehicle.getId(), vehicle);
            }
        }

        return new DeviceViewRefs(enterprisesById, fleetsById, vehiclesById, activeSessionsByDeviceId, driversById);
    }

    private DeviceState buildDeviceState(Device device, DeviceViewRefs refs) {
        DrivingSession activeSession = refs.activeSessionsByDeviceId().get(device.getId());
        EdgeDeviceLifecycleStatus lifecycleStatus = resolveLifecycleStatus(device);
        EdgeDeviceEnterpriseBindStatus enterpriseBindStatus = resolveEnterpriseBindStatus(device, null);
        EdgeDeviceVehicleBindStatus vehicleBindStatus = resolveVehicleBindStatus(device);
        EdgeDeviceSessionStage sessionStage = resolveSessionStage(activeSession);
        EdgeDeviceEffectiveStage effectiveStage = resolveEffectiveStage(lifecycleStatus, enterpriseBindStatus, vehicleBindStatus, sessionStage);
        return new DeviceState(lifecycleStatus, enterpriseBindStatus, vehicleBindStatus, sessionStage, effectiveStage);
    }

    private EdgeDeviceStatus deriveBoundStatus(Device device) {
        return EdgeDeviceStatus.BOUND;
    }

    private void syncDeviceStatusAfterBindRequestChange(Device device, String requestStatus) {
        if (EdgeDeviceStatus.DISABLED.name().equals(device.getStatus())) {
            deviceRepository.save(device);
            return;
        }
        if (device.getEnterpriseId() != null) {
            device.setStatus(deriveBoundStatus(device).name());
        } else {
            device.setStatus(EdgeDeviceStatus.NEW.name());
        }
        deviceRepository.save(device);
    }

    private DeviceBindingViewData.NamedResourceData toNamedResourceData(Enterprise enterprise) {
        return enterprise == null ? null : new DeviceBindingViewData.NamedResourceData(enterprise.getId(), enterprise.getName());
    }

    private DeviceBindingViewData.NamedResourceData toNamedResourceData(Fleet fleet) {
        return fleet == null ? null : new DeviceBindingViewData.NamedResourceData(fleet.getId(), fleet.getName());
    }

    private DeviceBindingViewData.VehicleData toVehicleData(Vehicle vehicle) {
        return vehicle == null ? null : new DeviceBindingViewData.VehicleData(vehicle.getId(), vehicle.getPlateNumber());
    }

    private DeviceBindingViewData.SessionData toSessionData(DrivingSession activeSession) {
        return activeSession == null ? null : new DeviceBindingViewData.SessionData(
                activeSession.getId(),
                activeSession.getSessionNo(),
                toOffsetDateTime(activeSession.getSignInTime()),
                activeSession.getStatus());
    }

    private EdgeDeviceBindRequestResponseData toBindRequestSummary(EdgeDeviceBindRequest bindRequest,
                                                                  Device device,
                                                                  Enterprise requestedEnterprise,
                                                                  EdgeDeviceEffectiveStage effectiveStage) {
        if (bindRequest == null) {
            return null;
        }
        return new EdgeDeviceBindRequestResponseData(
                bindRequest.getId(),
                bindRequest.getDeviceId(),
                bindRequest.getDeviceCode(),
                device.getDeviceName(),
                device.getActivationCode(),
                bindRequest.getRequestedEnterpriseId(),
                bindRequest.getRequestedEnterpriseId(),
                requestedEnterprise == null ? null : requestedEnterprise.getName(),
                bindRequest.getStatus(),
                bindRequest.getApplyRemark(),
                bindRequest.getApproveRemark(),
                bindRequest.getRejectReason(),
                bindRequest.getApproveRemark() != null ? bindRequest.getApproveRemark() : bindRequest.getRejectReason(),
                toOffsetDateTime(bindRequest.getSubmittedAt()),
                toOffsetDateTime(bindRequest.getReviewedAt()),
                bindRequest.getReviewedBy(),
                toOffsetDateTime(bindRequest.getExpiresAt()),
                toOffsetDateTime(bindRequest.getCreatedAt()),
                toOffsetDateTime(bindRequest.getUpdatedAt()),
                toOffsetDateTime(device.getLastSeenAt()),
                effectiveStage.name(),
                null,
                null);
    }

    private void appendBindRequestHistory(Long bindRequestId,
                                          EdgeDeviceBindRequestHistoryAction action,
                                          Long operatorId,
                                          String operatorName,
                                          String remark,
                                          LocalDateTime createdAt) {
        EdgeDeviceBindRequestHistory history = new EdgeDeviceBindRequestHistory();
        history.setBindRequestId(bindRequestId);
        history.setAction(action.name());
        history.setOperatorId(operatorId);
        history.setOperatorName(operatorName);
        history.setRemark(remark);
        history.setCreatedAt(createdAt);
        edgeDeviceBindRequestHistoryRepository.save(history);
    }

    private Map<String, Object> snapshot(Device device) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", device.getId());
        snapshot.put("enterpriseId", device.getEnterpriseId());
        snapshot.put("fleetId", device.getFleetId());
        snapshot.put("vehicleId", device.getVehicleId());
        snapshot.put("deviceCode", device.getDeviceCode());
        snapshot.put("deviceName", device.getDeviceName());
        snapshot.put("activationCode", device.getActivationCode());
        snapshot.put("lastSeenAt", toOffsetDateTime(device.getLastSeenAt()));
        snapshot.put("status", device.getStatus());
        return snapshot;
    }

    private Map<String, Object> auditDetail(AuthenticatedUser operator, Device device, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator == null ? null : operator.getUserId());
        detail.put("operatorRoles", operator == null ? null : operator.getRoles());
        detail.put("targetType", "DEVICE");
        detail.put("targetId", device.getId());
        detail.put("targetEnterpriseId", device.getEnterpriseId());
        detail.put("before", before);
        detail.put("after", after);
        return detail;
    }

    private String generateSecretToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
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

    private String normalizeStatus(Byte status, Device device) {
        if (status == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status不能为空");
        }
        if (status == (byte) 0) {
            return EdgeDeviceStatus.DISABLED.name();
        }
        if (status == (byte) 1) {
            if (device.getEnterpriseId() == null) {
                return EdgeDeviceStatus.NEW.name();
            }
            return deriveBoundStatus(device).name();
        }
        throw new BusinessException(ApiCode.INVALID_PARAM, "status仅支持0或1");
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "page必须大于等于1");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "size必须大于等于1");
        }
        return Math.min(size, MAX_SIZE);
    }

    private record DeviceViewRefs(
            Map<Long, Enterprise> enterprisesById,
            Map<Long, Fleet> fleetsById,
            Map<Long, Vehicle> vehiclesById,
            Map<Long, DrivingSession> activeSessionsByDeviceId,
            Map<Long, Driver> driversById
    ) {
    }

    private record BindingResolution(
            Long enterpriseId,
            Long fleetId,
            Long vehicleId
    ) {
    }

    private record DeviceState(
            EdgeDeviceLifecycleStatus lifecycleStatus,
            EdgeDeviceEnterpriseBindStatus enterpriseBindStatus,
            EdgeDeviceVehicleBindStatus vehicleBindStatus,
            EdgeDeviceSessionStage sessionStage,
            EdgeDeviceEffectiveStage effectiveStage
    ) {
    }
}
