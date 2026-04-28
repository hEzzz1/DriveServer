package com.example.demo.device.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.dto.CreateDeviceRequest;
import com.example.demo.device.dto.DeviceActivateRequest;
import com.example.demo.device.dto.DeviceActivateResponseData;
import com.example.demo.device.dto.DeviceContextResponseData;
import com.example.demo.device.dto.DeviceDetailResponseData;
import com.example.demo.device.dto.DeviceListItemData;
import com.example.demo.device.dto.DevicePageResponseData;
import com.example.demo.device.dto.ReassignDeviceVehicleRequest;
import com.example.demo.device.dto.RotateDeviceTokenResponseData;
import com.example.demo.device.dto.UpdateDeviceRequest;
import com.example.demo.device.entity.Device;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.rule.service.EdgeConfigVersionResolver;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.session.model.SessionStatus;
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
import java.util.UUID;

@Service
public class DeviceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final DeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final DriverRepository driverRepository;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;
    private final EdgeConfigVersionResolver edgeConfigVersionResolver;

    public DeviceService(DeviceRepository deviceRepository,
                         VehicleRepository vehicleRepository,
                         DrivingSessionRepository drivingSessionRepository,
                         EnterpriseRepository enterpriseRepository,
                         FleetRepository fleetRepository,
                         DriverRepository driverRepository,
                         BusinessAccessService businessAccessService,
                         SystemAuditService systemAuditService,
                         EdgeConfigVersionResolver edgeConfigVersionResolver) {
        this.deviceRepository = deviceRepository;
        this.vehicleRepository = vehicleRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.driverRepository = driverRepository;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
        this.edgeConfigVersionResolver = edgeConfigVersionResolver;
    }

    @Transactional(readOnly = true)
    public DevicePageResponseData listDevices(AuthenticatedUser operator, Integer page, Integer size, Long enterpriseId, Long fleetId, Long vehicleId) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        Specification<Device> specification = buildSpecification(readableEnterpriseId, fleetId, vehicleId);
        Page<Device> result = deviceRepository.findAll(specification, PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));
        DeviceViewRefs refs = loadDeviceViewRefs(result.getContent());
        return new DevicePageResponseData(result.getTotalElements(), pageNo, pageSize,
                result.getContent().stream().map(device -> toListItem(device, refs)).toList());
    }

    @Transactional(readOnly = true)
    public DeviceDetailResponseData getDevice(AuthenticatedUser operator, Long deviceId) {
        Device device = getDeviceEntity(deviceId);
        businessAccessService.resolveReadableEnterpriseId(operator, device.getEnterpriseId());
        return toDetail(device, loadDeviceViewRefs(List.of(device)));
    }

    @Transactional
    public DeviceDetailResponseData createDevice(AuthenticatedUser operator, CreateDeviceRequest request) {
        Vehicle vehicle = getVehicleEntity(request.vehicleId());
        businessAccessService.assertCanManageEnterprise(operator, vehicle.getEnterpriseId());
        String deviceCode = normalizeRequired(request.deviceCode(), "deviceCode不能为空");
        if (deviceRepository.existsByDeviceCode(deviceCode)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "deviceCode已存在");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Device device = new Device();
        device.setEnterpriseId(vehicle.getEnterpriseId());
        device.setFleetId(vehicle.getFleetId());
        device.setVehicleId(vehicle.getId());
        device.setDeviceCode(deviceCode);
        device.setDeviceName(normalizeRequired(request.deviceName(), "deviceName不能为空"));
        device.setActivationCode(normalizeOptional(request.activationCode()));
        device.setStatus((byte) 1);
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
        businessAccessService.assertCanManageEnterprise(operator, device.getEnterpriseId());
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
        businessAccessService.assertCanManageEnterprise(operator, device.getEnterpriseId());
        Map<String, Object> before = snapshot(device);
        device.setStatus(normalizeStatus(status));
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "UPDATE_DEVICE_STATUS", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "更新设备状态", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public DeviceDetailResponseData reassignVehicle(AuthenticatedUser operator, Long deviceId, ReassignDeviceVehicleRequest request) {
        Device device = getDeviceEntity(deviceId);
        businessAccessService.assertCanManageEnterprise(operator, device.getEnterpriseId());
        Vehicle vehicle = getVehicleEntity(request.vehicleId());
        if (!vehicle.getEnterpriseId().equals(device.getEnterpriseId())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "vehicleId不属于当前enterprise");
        }
        Map<String, Object> before = snapshot(device);
        device.setFleetId(vehicle.getFleetId());
        device.setVehicleId(vehicle.getId());
        Device saved = deviceRepository.save(device);
        systemAuditService.record(operator, "DEVICE", "REASSIGN_DEVICE_VEHICLE", "DEVICE", String.valueOf(saved.getId()),
                "SUCCESS", "调整设备绑定车辆", auditDetail(operator, saved, before, snapshot(saved)));
        return toDetail(saved, loadDeviceViewRefs(List.of(saved)));
    }

    @Transactional
    public RotateDeviceTokenResponseData rotateToken(AuthenticatedUser operator, Long deviceId) {
        Device device = getDeviceEntity(deviceId);
        businessAccessService.assertCanManageEnterprise(operator, device.getEnterpriseId());
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
        if (device.getStatus() == null || device.getStatus() == (byte) 0) {
            throw new BusinessException(ApiCode.FORBIDDEN, "设备已禁用");
        }
        if (!StringUtils.hasText(device.getActivationCode()) || !device.getActivationCode().equals(request.activationCode().trim())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "激活码错误");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        device.setDeviceToken(generateSecretToken());
        device.setLastActivatedAt(now);
        device.setTokenRotatedAt(now);
        deviceRepository.save(device);
        systemAuditService.record(null, "DEVICE", "DEVICE_ACTIVATE", "DEVICE", String.valueOf(device.getId()),
                "SUCCESS", "设备激活", Map.of("deviceId", device.getId(), "deviceCode", device.getDeviceCode()));
        return new DeviceActivateResponseData(device.getId(), device.getDeviceCode(), device.getDeviceName(), device.getDeviceToken(),
                device.getEnterpriseId(), device.getFleetId(), device.getVehicleId(), toOffsetDateTime(now));
    }

    @Transactional
    public DeviceContextResponseData getContext(String deviceCode, String deviceToken) {
        Device device = authenticateAndTouch(deviceCode, deviceToken).device();
        Enterprise enterprise = enterpriseRepository.findById(device.getEnterpriseId()).orElse(null);
        Fleet fleet = fleetRepository.findById(device.getFleetId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(device.getVehicleId()).orElse(null);
        DrivingSession session = drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode()).orElse(null);
        Driver driver = session == null ? null : driverRepository.findById(session.getDriverId()).orElse(null);
        return new DeviceContextResponseData(
                device.getId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                device.getFleetId(),
                fleet == null ? null : fleet.getName(),
                device.getVehicleId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                driver == null ? null : driver.getId(),
                driver == null ? null : driver.getDriverCode(),
                driver == null ? null : driver.getName(),
                session == null ? null : session.getId(),
                session == null ? null : session.getSessionNo(),
                session == null ? null : toOffsetDateTime(session.getSignInTime()),
                session == null ? null : session.getStatus(),
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

    private Device authenticateDevice(String deviceCode, String deviceToken) {
        if (!StringUtils.hasText(deviceCode) || !StringUtils.hasText(deviceToken)) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "设备认证失败");
        }
        Device device = deviceRepository.findByDeviceCodeAndDeviceToken(deviceCode.trim(), deviceToken.trim())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, "设备认证失败"));
        if (device.getStatus() == null || device.getStatus() == (byte) 0) {
            throw new BusinessException(ApiCode.FORBIDDEN, "设备已禁用");
        }
        return device;
    }

    private void touchOnlineAndActiveSession(Device device) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        device.setLastOnlineAt(now);
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

    private DeviceListItemData toListItem(Device device, DeviceViewRefs refs) {
        DrivingSession activeSession = refs.activeSessionsByDeviceId().get(device.getId());
        Driver currentDriver = activeSession == null ? null : refs.driversById().get(activeSession.getDriverId());
        return new DeviceListItemData(device.getId(), device.getEnterpriseId(), device.getFleetId(), device.getVehicleId(),
                device.getDeviceCode(), device.getDeviceName(), device.getActivationCode(), device.getStatus(),
                toOffsetDateTime(device.getLastActivatedAt()), toOffsetDateTime(device.getLastOnlineAt()), toOffsetDateTime(device.getTokenRotatedAt()),
                currentDriver == null ? null : currentDriver.getId(),
                currentDriver == null ? null : currentDriver.getDriverCode(),
                currentDriver == null ? null : currentDriver.getName(),
                activeSession == null ? null : activeSession.getId(),
                device.getRemark(),
                toOffsetDateTime(device.getCreatedAt()), toOffsetDateTime(device.getUpdatedAt()));
    }

    private DeviceDetailResponseData toDetail(Device device, DeviceViewRefs refs) {
        DrivingSession activeSession = refs.activeSessionsByDeviceId().get(device.getId());
        Driver currentDriver = activeSession == null ? null : refs.driversById().get(activeSession.getDriverId());
        return new DeviceDetailResponseData(device.getId(), device.getEnterpriseId(), device.getFleetId(), device.getVehicleId(),
                device.getDeviceCode(), device.getDeviceName(), device.getActivationCode(), device.getStatus(),
                toOffsetDateTime(device.getLastActivatedAt()), toOffsetDateTime(device.getLastOnlineAt()), toOffsetDateTime(device.getTokenRotatedAt()),
                currentDriver == null ? null : currentDriver.getId(),
                currentDriver == null ? null : currentDriver.getDriverCode(),
                currentDriver == null ? null : currentDriver.getName(),
                activeSession == null ? null : activeSession.getId(),
                device.getRemark(),
                toOffsetDateTime(device.getCreatedAt()), toOffsetDateTime(device.getUpdatedAt()));
    }

    private DeviceViewRefs loadDeviceViewRefs(Collection<Device> devices) {
        if (devices.isEmpty()) {
            return new DeviceViewRefs(Map.of(), Map.of());
        }
        List<Long> deviceIds = devices.stream().map(Device::getId).toList();
        Map<Long, DrivingSession> activeSessionsByDeviceId = new HashMap<>();
        for (DrivingSession session : drivingSessionRepository.findByStatusAndDeviceIdInOrderBySignInTimeDesc(SessionStatus.ACTIVE.getCode(), deviceIds)) {
            if (session.getDeviceId() != null) {
                activeSessionsByDeviceId.putIfAbsent(session.getDeviceId(), session);
            }
        }

        List<Long> driverIds = activeSessionsByDeviceId.values().stream().map(DrivingSession::getDriverId).toList();
        Map<Long, Driver> driversById = new HashMap<>();
        for (Driver driver : driverRepository.findAllById(driverIds)) {
            if (driver.getId() != null) {
                driversById.put(driver.getId(), driver);
            }
        }
        return new DeviceViewRefs(activeSessionsByDeviceId, driversById);
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
        snapshot.put("lastOnlineAt", toOffsetDateTime(device.getLastOnlineAt()));
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

    private Byte normalizeStatus(Byte status) {
        if (status == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status不能为空");
        }
        if (status != (byte) 0 && status != (byte) 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status仅支持0或1");
        }
        return status;
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
            Map<Long, DrivingSession> activeSessionsByDeviceId,
            Map<Long, Driver> driversById
    ) {
    }
}
