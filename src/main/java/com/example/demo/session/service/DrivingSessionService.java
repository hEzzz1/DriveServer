package com.example.demo.session.service;

import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.entity.Device;
import com.example.demo.device.service.DeviceAuthContext;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.session.dto.AvailableDriverItemData;
import com.example.demo.session.dto.AvailableDriversResponseData;
import com.example.demo.session.dto.SessionCurrentResponseData;
import com.example.demo.session.dto.SignInSessionRequest;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.ResolutionStatus;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.system.service.SystemAuditService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class DrivingSessionService {

    private static final DateTimeFormatter SESSION_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final DrivingSessionRepository drivingSessionRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemAuditService systemAuditService;

    public DrivingSessionService(DrivingSessionRepository drivingSessionRepository,
                                 DriverRepository driverRepository,
                                 PasswordEncoder passwordEncoder,
                                 SystemAuditService systemAuditService) {
        this.drivingSessionRepository = drivingSessionRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemAuditService = systemAuditService;
    }

    @Transactional(readOnly = true)
    public AvailableDriversResponseData listAvailableDrivers(DeviceAuthContext authContext) {
        Device device = authContext.device();
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
        session.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
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
                .orElse(new SessionCurrentResponseData(null, null, authContext.device().getEnterpriseId(), authContext.device().getFleetId(),
                        authContext.device().getVehicleId(), null, authContext.device().getId(), null, null, null, null, null));
    }

    @Transactional(readOnly = true)
    public EventOwnershipResolution resolveOwnership(Device device, Long reportedFleetId, Long reportedVehicleId, Long reportedDriverId, Long requestedSessionId) {
        DrivingSession activeSession = drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode()).orElse(null);
        if (requestedSessionId != null && (activeSession == null || !requestedSessionId.equals(activeSession.getId()))) {
            return new EventOwnershipResolution(
                    device.getId(),
                    requestedSessionId,
                    device.getEnterpriseId(),
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
                    device.getEnterpriseId(),
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
                device.getEnterpriseId(),
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

    private SessionCurrentResponseData toCurrentResponse(DrivingSession session) {
        return new SessionCurrentResponseData(session.getId(), session.getSessionNo(), session.getEnterpriseId(), session.getFleetId(),
                session.getVehicleId(), session.getDriverId(), session.getDeviceId(), toOffsetDateTime(session.getSignInTime()),
                toOffsetDateTime(session.getSignOutTime()), session.getStatus(), session.getClosedReason(), session.getRemark());
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String generateSessionNo(LocalDateTime now) {
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "SES" + now.format(SESSION_NO_TIME_FORMATTER) + random;
    }
}
