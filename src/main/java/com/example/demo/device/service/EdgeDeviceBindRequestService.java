package com.example.demo.device.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.dto.ApproveEdgeDeviceBindRequest;
import com.example.demo.device.dto.CreateEdgeDeviceBindRequest;
import com.example.demo.device.dto.DeviceBindingViewData;
import com.example.demo.device.dto.EdgeDeviceBindRequestPageResponseData;
import com.example.demo.device.dto.EdgeDeviceBindRequestResponseData;
import com.example.demo.device.dto.RejectEdgeDeviceBindRequest;
import com.example.demo.device.entity.Device;
import com.example.demo.device.entity.EdgeDeviceBindRequest;
import com.example.demo.device.entity.EdgeDeviceBindRequestHistory;
import com.example.demo.device.model.EdgeDeviceBindRequestHistoryAction;
import com.example.demo.device.model.EdgeDeviceBindRequestStatus;
import com.example.demo.device.model.EdgeDeviceEffectiveStage;
import com.example.demo.device.model.EdgeDeviceEnterpriseBindStatus;
import com.example.demo.device.model.EdgeDeviceLifecycleStatus;
import com.example.demo.device.model.EdgeDeviceSessionStage;
import com.example.demo.device.model.EdgeDeviceStatus;
import com.example.demo.device.model.EdgeDeviceVehicleBindStatus;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindRequestHistoryRepository;
import com.example.demo.device.repository.EdgeDeviceBindRequestRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.session.entity.DrivingSession;
import com.example.demo.session.model.SessionStatus;
import com.example.demo.session.repository.DrivingSessionRepository;
import com.example.demo.system.service.SystemAuditService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EdgeDeviceBindRequestService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long DEFAULT_EXPIRE_DAYS = 7L;
    private static final String SYSTEM_OPERATOR_NAME = "系统";

    private final EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository;
    private final EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final DrivingSessionRepository drivingSessionRepository;
    private final DeviceService deviceService;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;

    public EdgeDeviceBindRequestService(EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository,
                                        EdgeDeviceBindRequestHistoryRepository edgeDeviceBindRequestHistoryRepository,
                                        DeviceRepository deviceRepository,
                                        EnterpriseRepository enterpriseRepository,
                                        FleetRepository fleetRepository,
                                        VehicleRepository vehicleRepository,
                                        DrivingSessionRepository drivingSessionRepository,
                                        DeviceService deviceService,
                                        BusinessAccessService businessAccessService,
                                        SystemAuditService systemAuditService) {
        this.edgeDeviceBindRequestRepository = edgeDeviceBindRequestRepository;
        this.edgeDeviceBindRequestHistoryRepository = edgeDeviceBindRequestHistoryRepository;
        this.deviceRepository = deviceRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.vehicleRepository = vehicleRepository;
        this.drivingSessionRepository = drivingSessionRepository;
        this.deviceService = deviceService;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData create(String deviceCode, String deviceToken, CreateEdgeDeviceBindRequest request) {
        Device device = deviceService.authenticateAndTouch(deviceCode, deviceToken).device();
        if (device.getLastActivatedAt() == null) {
            throw new BusinessException(ApiCode.DEVICE_NOT_ACTIVATED, ApiCode.DEVICE_NOT_ACTIVATED.getMessage());
        }
        if (device.getEnterpriseId() != null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "设备已绑定企业");
        }
        Enterprise enterprise = enterpriseRepository.findById(request.enterpriseId())
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不存在"));
        if (enterprise.getStatus() == null || enterprise.getStatus() == (byte) 0) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "企业已禁用");
        }
        EdgeDeviceBindRequest current = latest(device);
        current = deviceService.refreshBindRequestIfExpired(device, current);
        if (current != null && EdgeDeviceBindRequestStatus.PENDING.name().equals(current.getStatus())) {
            throw new BusinessException(ApiCode.DEVICE_BIND_PENDING, ApiCode.DEVICE_BIND_PENDING.getMessage());
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        EdgeDeviceBindRequest bindRequest = new EdgeDeviceBindRequest();
        bindRequest.setDeviceId(device.getId());
        bindRequest.setDeviceCode(device.getDeviceCode());
        bindRequest.setRequestedEnterpriseId(enterprise.getId());
        bindRequest.setStatus(EdgeDeviceBindRequestStatus.PENDING.name());
        bindRequest.setApplyRemark(normalizeRemark(request.remark()));
        bindRequest.setApproveRemark(null);
        bindRequest.setRejectReason(null);
        bindRequest.setSubmittedAt(now);
        bindRequest.setReviewedAt(null);
        bindRequest.setReviewedBy(null);
        bindRequest.setExpiresAt(now.plusDays(DEFAULT_EXPIRE_DAYS));
        bindRequest.setCreatedAt(now);
        bindRequest.setUpdatedAt(now);
        EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);

        deviceService.syncDeviceStatusAfterBindRequestChange(device);
        appendHistory(saved.getId(),
                current != null && (EdgeDeviceBindRequestStatus.REJECTED.name().equals(current.getStatus())
                        || EdgeDeviceBindRequestStatus.EXPIRED.name().equals(current.getStatus()))
                        ? EdgeDeviceBindRequestHistoryAction.RESUBMITTED
                        : EdgeDeviceBindRequestHistoryAction.SUBMITTED,
                null,
                SYSTEM_OPERATOR_NAME,
                saved.getApplyRemark(),
                now);
        systemAuditService.record(null, "EDGE_DEVICE_BIND_REQUEST", "SUBMIT_EDGE_DEVICE_BIND_REQUEST",
                "EDGE_DEVICE_BIND_REQUEST", String.valueOf(saved.getId()), "SUCCESS", "设备提交企业绑定申请",
                Map.of("deviceId", device.getId(), "deviceCode", device.getDeviceCode(), "requestedEnterpriseId", enterprise.getId()));
        return toResponse(saved, device, false);
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData current(String deviceCode, String deviceToken) {
        Device device = deviceService.authenticateAndTouch(deviceCode, deviceToken).device();
        EdgeDeviceBindRequest current = latest(device);
        if (current == null) {
            return null;
        }
        return toResponse(deviceService.refreshBindRequestIfExpired(device, current), device, false);
    }

    @Transactional
    public EdgeDeviceBindRequestPageResponseData list(AuthenticatedUser operator, Integer page, Integer size, Long enterpriseId, String status, String deviceCode) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        expirePendingRequestsForList(status);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        Specification<EdgeDeviceBindRequest> specification = buildSpecification(readableEnterpriseId, normalizeOptional(status), normalizeOptional(deviceCode));
        Page<EdgeDeviceBindRequest> result = edgeDeviceBindRequestRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new EdgeDeviceBindRequestPageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(bindRequest -> {
                    Device device = deviceService.requireDevice(bindRequest.getDeviceId());
                    return toResponse(deviceService.refreshBindRequestIfExpired(device, bindRequest), device, false);
                }).toList());
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData get(AuthenticatedUser operator, Long id) {
        EdgeDeviceBindRequest bindRequest = getEntity(id);
        businessAccessService.resolveReadableEnterpriseId(operator, bindRequest.getRequestedEnterpriseId());
        Device device = deviceService.requireDevice(bindRequest.getDeviceId());
        return toResponse(deviceService.refreshBindRequestIfExpired(device, bindRequest), device, true);
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData approve(AuthenticatedUser operator, Long id, ApproveEdgeDeviceBindRequest request) {
        EdgeDeviceBindRequest bindRequest = getEntity(id);
        businessAccessService.assertCanManageEnterprise(operator, bindRequest.getRequestedEnterpriseId());
        Device device = deviceService.requireDevice(bindRequest.getDeviceId());
        bindRequest = deviceService.refreshBindRequestIfExpired(device, bindRequest);
        if (!EdgeDeviceBindRequestStatus.PENDING.name().equals(bindRequest.getStatus())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前申请不可审批");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        bindRequest.setStatus(EdgeDeviceBindRequestStatus.APPROVED.name());
        bindRequest.setApproveRemark(normalizeRemark(request == null ? null : request.remark()));
        bindRequest.setRejectReason(null);
        bindRequest.setReviewedAt(now);
        bindRequest.setReviewedBy(operator.getUserId());
        bindRequest.setUpdatedAt(now);

        device.setEnterpriseId(bindRequest.getRequestedEnterpriseId());
        reconcileVehicleBinding(device);
        EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);
        deviceService.syncDeviceStatusAfterBindRequestChange(device);
        appendHistory(saved.getId(), EdgeDeviceBindRequestHistoryAction.APPROVED, operator.getUserId(), operator.getUsername(), saved.getApproveRemark(), now);
        systemAuditService.record(operator, "EDGE_DEVICE_BIND_REQUEST", "APPROVE_EDGE_DEVICE_BIND_REQUEST",
                "EDGE_DEVICE_BIND_REQUEST", String.valueOf(saved.getId()), "SUCCESS", "审批通过设备企业绑定申请",
                auditDetail(operator, saved, device));
                return toResponse(saved, device, false);
    }

    private void expirePendingRequestsForList(String status) {
        if (StringUtils.hasText(status)
                && !EdgeDeviceBindRequestStatus.PENDING.name().equals(status)
                && !EdgeDeviceBindRequestStatus.EXPIRED.name().equals(status)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (EdgeDeviceBindRequest bindRequest : edgeDeviceBindRequestRepository.findByStatusAndExpiresAtBefore(
                EdgeDeviceBindRequestStatus.PENDING.name(), now)) {
            Device device = deviceService.requireDevice(bindRequest.getDeviceId());
            deviceService.refreshBindRequestIfExpired(device, bindRequest);
        }
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData reject(AuthenticatedUser operator, Long id, RejectEdgeDeviceBindRequest request) {
        EdgeDeviceBindRequest bindRequest = getEntity(id);
        businessAccessService.assertCanManageEnterprise(operator, bindRequest.getRequestedEnterpriseId());
        Device device = deviceService.requireDevice(bindRequest.getDeviceId());
        bindRequest = deviceService.refreshBindRequestIfExpired(device, bindRequest);
        if (!EdgeDeviceBindRequestStatus.PENDING.name().equals(bindRequest.getStatus())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前申请不可驳回");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        bindRequest.setStatus(EdgeDeviceBindRequestStatus.REJECTED.name());
        bindRequest.setApproveRemark(null);
        bindRequest.setRejectReason(normalizeReason(request.reason()));
        bindRequest.setReviewedAt(now);
        bindRequest.setReviewedBy(operator.getUserId());
        bindRequest.setUpdatedAt(now);
        EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);

        deviceService.syncDeviceStatusAfterBindRequestChange(device);
        appendHistory(saved.getId(), EdgeDeviceBindRequestHistoryAction.REJECTED, operator.getUserId(), operator.getUsername(), saved.getRejectReason(), now);
        systemAuditService.record(operator, "EDGE_DEVICE_BIND_REQUEST", "REJECT_EDGE_DEVICE_BIND_REQUEST",
                "EDGE_DEVICE_BIND_REQUEST", String.valueOf(saved.getId()), "SUCCESS", "驳回设备企业绑定申请",
                auditDetail(operator, saved, device));
        return toResponse(saved, device, false);
    }

    private EdgeDeviceBindRequest latest(Device device) {
        return edgeDeviceBindRequestRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.getId()).orElse(null);
    }

    private void reconcileVehicleBinding(Device device) {
        if (device.getEnterpriseId() == null) {
            device.setFleetId(null);
            device.setVehicleId(null);
            return;
        }
        if (device.getVehicleId() != null) {
            var vehicle = vehicleRepository.findById(device.getVehicleId()).orElse(null);
            var occupiedBy = deviceRepository.findByVehicleId(device.getVehicleId()).orElse(null);
            if (vehicle == null
                    || !device.getEnterpriseId().equals(vehicle.getEnterpriseId())
                    || (occupiedBy != null && !occupiedBy.getId().equals(device.getId()))) {
                device.setFleetId(null);
                device.setVehicleId(null);
            } else {
                device.setFleetId(vehicle.getFleetId());
                return;
            }
        }
        if (device.getFleetId() != null) {
            var fleet = fleetRepository.findById(device.getFleetId()).orElse(null);
            if (fleet == null || !device.getEnterpriseId().equals(fleet.getEnterpriseId())) {
                device.setFleetId(null);
            }
        }
    }

    private EdgeDeviceBindRequest getEntity(Long id) {
        return edgeDeviceBindRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private Specification<EdgeDeviceBindRequest> buildSpecification(Long enterpriseId, String status, String deviceCode) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("requestedEnterpriseId"), enterpriseId));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(deviceCode)) {
                predicates.add(cb.like(cb.lower(root.get("deviceCode")), "%" + deviceCode.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private EdgeDeviceBindRequestResponseData toResponse(EdgeDeviceBindRequest bindRequest, Device device, boolean includeDetail) {
        Enterprise requestedEnterprise = enterpriseRepository.findById(bindRequest.getRequestedEnterpriseId()).orElse(null);
        String currentRequestStatus = deviceService.currentBindRequestStatus(device);
        DrivingSession activeSession = drivingSessionRepository.findFirstByDeviceIdAndStatusOrderBySignInTimeDesc(device.getId(), SessionStatus.ACTIVE.getCode()).orElse(null);
        EdgeDeviceLifecycleStatus lifecycleStatus = deviceService.resolveLifecycleStatus(device);
        EdgeDeviceEnterpriseBindStatus enterpriseBindStatus = deviceService.resolveEnterpriseBindStatus(device, currentRequestStatus);
        EdgeDeviceVehicleBindStatus vehicleBindStatus = deviceService.resolveVehicleBindStatus(device);
        EdgeDeviceSessionStage sessionStage = deviceService.resolveSessionStage(activeSession);
        EdgeDeviceEffectiveStage effectiveStage = deviceService.resolveEffectiveStage(lifecycleStatus, enterpriseBindStatus, vehicleBindStatus, sessionStage);

        DeviceBindingViewData.BindRequestDeviceData deviceSnapshot = includeDetail ? new DeviceBindingViewData.BindRequestDeviceData(
                device.getId(),
                device.getDeviceCode(),
                device.getDeviceName(),
                device.getEnterpriseId(),
                device.getEnterpriseId() == null ? null : enterpriseRepository.findById(device.getEnterpriseId()).map(Enterprise::getName).orElse(null),
                device.getFleetId(),
                device.getFleetId() == null ? null : fleetRepository.findById(device.getFleetId()).map(fleet -> fleet.getName()).orElse(null),
                device.getVehicleId(),
                device.getVehicleId() == null ? null : vehicleRepository.findById(device.getVehicleId()).map(vehicle -> vehicle.getPlateNumber()).orElse(null),
                lifecycleStatus.name(),
                toOffsetDateTime(device.getLastActivatedAt()),
                toOffsetDateTime(device.getLastSeenAt())) : null;

        List<DeviceBindingViewData.BindRequestHistoryItemData> history = includeDetail
                ? edgeDeviceBindRequestHistoryRepository.findByBindRequestIdOrderByCreatedAtAscIdAsc(bindRequest.getId()).stream()
                .map(this::toHistoryItem)
                .toList()
                : null;

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
                deviceSnapshot,
                history);
    }

    private DeviceBindingViewData.BindRequestHistoryItemData toHistoryItem(EdgeDeviceBindRequestHistory history) {
        return new DeviceBindingViewData.BindRequestHistoryItemData(
                history.getId(),
                history.getAction(),
                history.getOperatorId(),
                history.getOperatorName(),
                history.getRemark(),
                toOffsetDateTime(history.getCreatedAt()));
    }

    private void appendHistory(Long bindRequestId,
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

    private Map<String, Object> auditDetail(AuthenticatedUser operator, EdgeDeviceBindRequest bindRequest, Device device) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator == null ? null : operator.getUserId());
        detail.put("operatorRoles", operator == null ? null : operator.getRoles());
        detail.put("deviceId", device.getId());
        detail.put("deviceCode", device.getDeviceCode());
        detail.put("requestedEnterpriseId", bindRequest.getRequestedEnterpriseId());
        detail.put("status", bindRequest.getStatus());
        detail.put("applyRemark", bindRequest.getApplyRemark());
        detail.put("approveRemark", bindRequest.getApproveRemark());
        detail.put("rejectReason", bindRequest.getRejectReason());
        return detail;
    }

    private String normalizeRemark(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeReason(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "reason不能为空");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
}
