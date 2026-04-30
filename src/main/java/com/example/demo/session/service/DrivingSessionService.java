package com.example.demo.session.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.service.DeviceAuthContext;
import com.example.demo.device.service.DeviceService;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.rule.service.EdgeConfigVersionResolver;
import com.example.demo.session.dto.AvailableDriverItemData;
import com.example.demo.session.dto.AvailableDriversResponseData;
import com.example.demo.session.dto.SessionAdminDetailResponseData;
import com.example.demo.session.dto.SessionAdminListItemData;
import com.example.demo.session.dto.SessionAdminPageResponseData;
import com.example.demo.session.dto.SessionCurrentResponseData;
import com.example.demo.session.dto.SignInSessionRequest;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.ResolutionStatus;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.vehicle.entity.Vehicle;
import com.example.demo.vehicle.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

@Service
public class DrivingSessionService {

    private static final DateTimeFormatter SESSION_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_REMARK_LENGTH = 255;

    private final DrivingSessionRepository drivingSessionRepository;
    private final DriverRepository driverRepository;
    private final DeviceRepository deviceRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceService deviceService;
    private final BusinessAccessService businessAccessService;
    private final PasswordEncoder passwordEncoder;
    private final SystemAuditService systemAuditService;
    private final EdgeConfigVersionResolver edgeConfigVersionResolver;

    public DrivingSessionService(DrivingSessionRepository drivingSessionRepository,
                                 DriverRepository driverRepository,
                                 DeviceRepository deviceRepository,
                                 EnterpriseRepository enterpriseRepository,
                                 FleetRepository fleetRepository,
                                 VehicleRepository vehicleRepository,
                                 DeviceService deviceService,
                                 BusinessAccessService businessAccessService,
                                 PasswordEncoder passwordEncoder,
                                 SystemAuditService systemAuditService,
                                 EdgeConfigVersionResolver edgeConfigVersionResolver) {
        this.drivingSessionRepository = drivingSessionRepository;
        this.driverRepository = driverRepository;
        this.deviceRepository = deviceRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.vehicleRepository = vehicleRepository;
        this.deviceService = deviceService;
        this.businessAccessService = businessAccessService;
        this.passwordEncoder = passwordEncoder;
        this.systemAuditService = systemAuditService;
        this.edgeConfigVersionResolver = edgeConfigVersionResolver;
    }

    @Transactional(readOnly = true)
    public AvailableDriversResponseData listAvailableDrivers(DeviceAuthContext authContext) {
        Device device = authContext.device();
        deviceService.ensureReadyForSignIn(device);
        List<AvailableDriverItemData> items = driverRepository.findAll().stream()
                .filter(driver -> driver.getEnterpriseId().equals(device.getEnterpriseId()))
                .filter(driver -> driver.getFleetId().equals(device.getFleetId()))
                .filter(driver -> driver.getStatus() != null && driver.getStatus() == (byte) 1)
                .filter(driver -> StringUtils.hasText(driver.getDriverCode()))
                .map(driver -> new AvailableDriverItemData(driver.getId(), driver.getDriverCode(), driver.getName(), driver.getFleetId()))
                .toList();
        return new AvailableDriversResponseData(device.getId(), device.getFleetId(), items);
    }

    @Transactional
    public SessionCurrentResponseData signIn(DeviceAuthContext authContext, SignInSessionRequest request) {
        Device device = authContext.device();
        deviceService.ensureReadyForSignIn(device);
        if (drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode()).isPresent()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前设备已有进行中的会话");
        }
        Driver driver = driverRepository.findByEnterpriseIdAndDriverCode(device.getEnterpriseId(), request.driverCode().trim())
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "driverCode不存在"));
        if (driver.getStatus() == null || driver.getStatus() == (byte) 0) {
            throw new BusinessException(ApiCode.FORBIDDEN, "驾驶员已禁用");
        }
        if (!driver.getFleetId().equals(device.getFleetId())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "驾驶员不属于当前设备所在车队");
        }
        if (!StringUtils.hasText(driver.getPinHash()) || !passwordEncoder.matches(request.pin().trim(), driver.getPinHash())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "PIN错误");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        DrivingSession session = new DrivingSession();
        session.setSessionNo(generateSessionNo(now));
        session.setEnterpriseId(device.getEnterpriseId());
        session.setFleetId(device.getFleetId());
        session.setVehicleId(device.getVehicleId());
        session.setDriverId(driver.getId());
        session.setDeviceId(device.getId());
        session.setSignInTime(now);
        session.setLastHeartbeatAt(now);
        session.setStatus(SessionStatus.ACTIVE.getCode());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        DrivingSession saved = drivingSessionRepository.save(session);
        systemAuditService.record(null, "SESSION", "SIGN_IN_DRIVER", "SESSION", String.valueOf(saved.getId()),
                "SUCCESS", "司机签到", Map.of("sessionId", saved.getId(), "driverId", driver.getId(), "deviceId", device.getId()));
        return toCurrentResponse(saved);
    }

    @Transactional
    public SessionCurrentResponseData signOut(DeviceAuthContext authContext, String remark) {
        DrivingSession session = requireActiveSession(authContext.device().getId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        session.setStatus(SessionStatus.CLOSED.getCode());
        session.setClosedReason("SIGN_OUT");
        session.setRemark(normalizeRemark(remark, null));
        session.setLastHeartbeatAt(now);
        session.setSignOutTime(now);
        session.setUpdatedAt(now);
        DrivingSession saved = drivingSessionRepository.save(session);
        systemAuditService.record(null, "SESSION", "SIGN_OUT_DRIVER", "SESSION", String.valueOf(saved.getId()),
                "SUCCESS", "司机签退", Map.of("sessionId", saved.getId(), "deviceId", saved.getDeviceId()));
        return toCurrentResponse(saved);
    }

    @Transactional(readOnly = true)
    public SessionCurrentResponseData current(DeviceAuthContext authContext) {
        return drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(authContext.device().getId(), SessionStatus.ACTIVE.getCode())
                .map(this::toCurrentResponse)
                .orElseGet(() -> emptyCurrentResponse(authContext.device()));
    }

    @Transactional(readOnly = true)
    public SessionAdminPageResponseData listSessions(AuthenticatedUser operator,
                                                     Integer page,
                                                     Integer size,
                                                     Long enterpriseId,
                                                     Long fleetId,
                                                     Byte status,
                                                     String keyword) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        Specification<DrivingSession> specification = buildSpecification(readableEnterpriseId, fleetId, normalizeStatus(status, true), keyword);
        Page<DrivingSession> result = drivingSessionRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "signInTime")));
        ReferenceMaps refs = loadReferenceMaps(result.getContent());
        return new SessionAdminPageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(session -> toAdminListItem(session, refs)).toList());
    }

    @Transactional(readOnly = true)
    public SessionAdminDetailResponseData getSession(AuthenticatedUser operator, Long sessionId) {
        DrivingSession session = getSessionEntity(sessionId);
        businessAccessService.resolveReadableEnterpriseId(operator, session.getEnterpriseId());
        return toAdminDetail(session, loadReferenceMaps(List.of(session)));
    }

    @Transactional
    public SessionAdminDetailResponseData forceSignOut(AuthenticatedUser operator, Long sessionId, String remark) {
        DrivingSession session = getSessionEntity(sessionId);
        businessAccessService.assertCanManageEnterprise(operator, session.getEnterpriseId());
        if (session.getStatus() == null || session.getStatus() != SessionStatus.ACTIVE.getCode()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前会话已结束，无需强制签退");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Map<String, Object> before = snapshot(session);
        session.setStatus(SessionStatus.CLOSED.getCode());
        session.setClosedReason("FORCE_SIGN_OUT");
        session.setRemark(normalizeRemark(remark, "后台强制签退"));
        session.setSignOutTime(now);
        session.setUpdatedAt(now);
        DrivingSession saved = drivingSessionRepository.save(session);
        systemAuditService.record(operator, "SESSION", "FORCE_SIGN_OUT_SESSION", "SESSION", String.valueOf(saved.getId()),
                "SUCCESS", "强制签退驾驶会话", auditDetail(operator, saved, before, snapshot(saved)));
        return toAdminDetail(saved, loadReferenceMaps(List.of(saved)));
    }

    @Transactional(readOnly = true)
    public EventOwnershipResolution resolveOwnership(Device device, Long reportedEnterpriseId, Long reportedFleetId, Long reportedVehicleId, Long reportedDriverId, Long requestedSessionId) {
        DrivingSession activeSession = drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode()).orElse(null);
        if (requestedSessionId != null && (activeSession == null || !requestedSessionId.equals(activeSession.getId()))) {
            return new EventOwnershipResolution(
                    device.getId(),
                    requestedSessionId,
                    reportedEnterpriseId,
                    reportedFleetId,
                    reportedVehicleId,
                    reportedDriverId,
                    device.getEnterpriseId(),
                    device.getFleetId(),
                    device.getVehicleId(),
                    null,
                    ResolutionStatus.SESSION_INVALID,
                    null);
        }
        if (activeSession != null) {
            return new EventOwnershipResolution(
                    device.getId(),
                    activeSession.getId(),
                    reportedEnterpriseId,
                    reportedFleetId,
                    reportedVehicleId,
                    reportedDriverId,
                    activeSession.getEnterpriseId(),
                    activeSession.getFleetId(),
                    activeSession.getVehicleId(),
                    activeSession.getDriverId(),
                    ResolutionStatus.RESOLVED_BY_SESSION,
                    activeSession);
        }
        return new EventOwnershipResolution(
                device.getId(),
                null,
                reportedEnterpriseId,
                reportedFleetId,
                reportedVehicleId,
                reportedDriverId,
                device.getEnterpriseId(),
                device.getFleetId(),
                device.getVehicleId(),
                null,
                ResolutionStatus.RESOLVED_BY_DEVICE_BINDING,
                null);
    }

    private DrivingSession requireActiveSession(Long deviceId) {
        return drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(deviceId, SessionStatus.ACTIVE.getCode())
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, "当前无进行中的会话"));
    }

    private DrivingSession getSessionEntity(Long sessionId) {
        return drivingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private SessionCurrentResponseData toCurrentResponse(DrivingSession session) {
        ReferenceMaps refs = loadReferenceMaps(List.of(session));
        Enterprise enterprise = refs.enterprises.get(session.getEnterpriseId());
        Fleet fleet = refs.fleets.get(session.getFleetId());
        Vehicle vehicle = refs.vehicles.get(session.getVehicleId());
        Driver driver = refs.drivers.get(session.getDriverId());
        Device device = refs.devices.get(session.getDeviceId());
        return new SessionCurrentResponseData(
                session.getId(),
                session.getSessionNo(),
                session.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                session.getFleetId(),
                fleet == null ? null : fleet.getName(),
                session.getVehicleId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                session.getDriverId(),
                driver == null ? null : driver.getDriverCode(),
                driver == null ? null : driver.getName(),
                session.getDeviceId(),
                device == null ? null : device.getDeviceCode(),
                toOffsetDateTime(session.getSignInTime()),
                toOffsetDateTime(session.getSignOutTime()),
                session.getStatus(),
                session.getClosedReason(),
                session.getRemark(),
                edgeConfigVersionResolver.resolveCurrentVersion());
    }

    private SessionCurrentResponseData emptyCurrentResponse(Device device) {
        Enterprise enterprise = enterpriseRepository.findById(device.getEnterpriseId()).orElse(null);
        Fleet fleet = fleetRepository.findById(device.getFleetId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(device.getVehicleId()).orElse(null);
        return new SessionCurrentResponseData(
                null,
                null,
                device.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                device.getFleetId(),
                fleet == null ? null : fleet.getName(),
                device.getVehicleId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                null,
                null,
                null,
                device.getId(),
                device.getDeviceCode(),
                null,
                null,
                null,
                null,
                null,
                edgeConfigVersionResolver.resolveCurrentVersion());
    }

    private SessionAdminListItemData toAdminListItem(DrivingSession session, ReferenceMaps refs) {
        Enterprise enterprise = refs.enterprises.get(session.getEnterpriseId());
        Fleet fleet = refs.fleets.get(session.getFleetId());
        Vehicle vehicle = refs.vehicles.get(session.getVehicleId());
        Driver driver = refs.drivers.get(session.getDriverId());
        Device device = refs.devices.get(session.getDeviceId());
        return new SessionAdminListItemData(
                session.getId(),
                session.getSessionNo(),
                session.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                session.getFleetId(),
                fleet == null ? null : fleet.getName(),
                session.getVehicleId(),
                vehicle == null ? null : vehicle.getPlateNumber(),
                session.getDriverId(),
                driver == null ? null : driver.getDriverCode(),
                driver == null ? null : driver.getName(),
                session.getDeviceId(),
                device == null ? null : device.getDeviceCode(),
                toOffsetDateTime(session.getSignInTime()),
                toOffsetDateTime(session.getSignOutTime()),
                session.getStatus(),
                session.getClosedReason(),
                session.getRemark(),
                toOffsetDateTime(session.getLastHeartbeatAt()));
    }

    private SessionAdminDetailResponseData toAdminDetail(DrivingSession session, ReferenceMaps refs) {
        SessionAdminListItemData item = toAdminListItem(session, refs);
        return new SessionAdminDetailResponseData(
                item.id(),
                item.sessionNo(),
                item.enterpriseId(),
                item.enterpriseName(),
                item.fleetId(),
                item.fleetName(),
                item.vehicleId(),
                item.vehiclePlateNumber(),
                item.driverId(),
                item.driverCode(),
                item.driverName(),
                item.deviceId(),
                item.deviceCode(),
                item.signInTime(),
                item.signOutTime(),
                item.status(),
                item.closedReason(),
                item.remark(),
                item.lastHeartbeatAt(),
                toOffsetDateTime(session.getCreatedAt()),
                toOffsetDateTime(session.getUpdatedAt()));
    }

    private Specification<DrivingSession> buildSpecification(Long enterpriseId, Long fleetId, Byte status, String keyword) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("enterpriseId"), enterpriseId));
            }
            if (fleetId != null) {
                predicates.add(cb.equal(root.get("fleetId"), fleetId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("sessionNo"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private ReferenceMaps loadReferenceMaps(Collection<DrivingSession> sessions) {
        return new ReferenceMaps(
                indexById(enterpriseRepository.findAllById(sessions.stream().map(DrivingSession::getEnterpriseId).distinct().toList()), Enterprise::getId),
                indexById(fleetRepository.findAllById(sessions.stream().map(DrivingSession::getFleetId).distinct().toList()), Fleet::getId),
                indexById(vehicleRepository.findAllById(sessions.stream().map(DrivingSession::getVehicleId).distinct().toList()), Vehicle::getId),
                indexById(driverRepository.findAllById(sessions.stream().map(DrivingSession::getDriverId).distinct().toList()), Driver::getId),
                indexById(deviceRepository.findAllById(sessions.stream().map(DrivingSession::getDeviceId).distinct().toList()), Device::getId));
    }

    private <T> Map<Long, T> indexById(List<T> items, Function<T, Long> idGetter) {
        Map<Long, T> map = new HashMap<>();
        for (T item : items) {
            Long id = idGetter.apply(item);
            if (id != null) {
                map.put(id, item);
            }
        }
        return map;
    }

    private Map<String, Object> snapshot(DrivingSession session) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", session.getId());
        snapshot.put("sessionNo", session.getSessionNo());
        snapshot.put("enterpriseId", session.getEnterpriseId());
        snapshot.put("fleetId", session.getFleetId());
        snapshot.put("vehicleId", session.getVehicleId());
        snapshot.put("driverId", session.getDriverId());
        snapshot.put("deviceId", session.getDeviceId());
        snapshot.put("status", session.getStatus());
        snapshot.put("signInTime", toOffsetDateTime(session.getSignInTime()));
        snapshot.put("lastHeartbeatAt", toOffsetDateTime(session.getLastHeartbeatAt()));
        snapshot.put("signOutTime", toOffsetDateTime(session.getSignOutTime()));
        snapshot.put("closedReason", session.getClosedReason());
        snapshot.put("remark", session.getRemark());
        return snapshot;
    }

    private Map<String, Object> auditDetail(AuthenticatedUser operator,
                                            DrivingSession session,
                                            Map<String, Object> before,
                                            Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator.getUserId());
        detail.put("operatorRoles", operator.getRoles());
        detail.put("targetType", "SESSION");
        detail.put("targetId", session.getId());
        detail.put("targetEnterpriseId", session.getEnterpriseId());
        detail.put("before", before);
        detail.put("after", after);
        return detail;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String generateSessionNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "SES" + now.format(SESSION_NO_TIME_FORMATTER) + random;
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

    private Byte normalizeStatus(Byte status, boolean allowNull) {
        if (status == null) {
            return allowNull ? null : SessionStatus.ACTIVE.getCode();
        }
        if (status != SessionStatus.ACTIVE.getCode() && status != SessionStatus.CLOSED.getCode()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status仅支持1或2");
        }
        return status;
    }

    private String normalizeRemark(String remark, String defaultValue) {
        String normalized = StringUtils.hasText(remark) ? remark.trim() : defaultValue;
        if (normalized != null && normalized.length() > MAX_REMARK_LENGTH) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "remark长度不能超过255");
        }
        return normalized;
    }

    private record ReferenceMaps(
            Map<Long, Enterprise> enterprises,
            Map<Long, Fleet> fleets,
            Map<Long, Vehicle> vehicles,
            Map<Long, Driver> drivers,
            Map<Long, Device> devices
    ) {
    }
}
