package com.example.demo.device.service;

import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.device.dto.CreateEdgeDeviceBindRequest;
import com.example.demo.device.dto.EdgeDeviceBindRequestPageResponseData;
import com.example.demo.device.dto.EdgeDeviceBindRequestResponseData;
import com.example.demo.device.dto.ReviewEdgeDeviceBindRequest;
import com.example.demo.device.entity.Device;
import com.example.demo.device.entity.EdgeDeviceBindRequest;
import com.example.demo.device.model.EdgeDeviceBindRequestStatus;
import com.example.demo.device.model.EdgeDeviceStatus;
import com.example.demo.device.repository.DeviceRepository;
import com.example.demo.device.repository.EdgeDeviceBindRequestRepository;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.repository.FleetRepository;
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
import java.util.Map;

@Service
public class EdgeDeviceBindRequestService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final long DEFAULT_EXPIRE_DAYS = 7L;

    private final EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository;
    private final DeviceRepository deviceRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final DeviceService deviceService;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;

    public EdgeDeviceBindRequestService(EdgeDeviceBindRequestRepository edgeDeviceBindRequestRepository,
                                        DeviceRepository deviceRepository,
                                        EnterpriseRepository enterpriseRepository,
                                        FleetRepository fleetRepository,
                                        VehicleRepository vehicleRepository,
                                        DeviceService deviceService,
                                        BusinessAccessService businessAccessService,
                                        SystemAuditService systemAuditService) {
        this.edgeDeviceBindRequestRepository = edgeDeviceBindRequestRepository;
        this.deviceRepository = deviceRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.vehicleRepository = vehicleRepository;
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
        if (current != null && EdgeDeviceBindRequestStatus.PENDING.name().equals(refreshStatusIfExpired(current, device).getStatus())) {
            throw new BusinessException(ApiCode.DEVICE_BIND_PENDING, ApiCode.DEVICE_BIND_PENDING.getMessage());
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        EdgeDeviceBindRequest bindRequest = new EdgeDeviceBindRequest();
        bindRequest.setDeviceId(device.getId());
        bindRequest.setDeviceCode(device.getDeviceCode());
        bindRequest.setRequestedEnterpriseId(enterprise.getId());
        bindRequest.setStatus(EdgeDeviceBindRequestStatus.PENDING.name());
        bindRequest.setApplyRemark(normalizeRemark(request.remark()));
        bindRequest.setSubmittedAt(now);
        bindRequest.setExpiresAt(now.plusDays(DEFAULT_EXPIRE_DAYS));
        bindRequest.setCreatedAt(now);
        bindRequest.setUpdatedAt(now);
        EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);

        device.setStatus(EdgeDeviceStatus.PENDING_ENTERPRISE_APPROVAL.name());
        deviceRepository.save(device);
        systemAuditService.record(null, "EDGE_DEVICE_BIND_REQUEST", "SUBMIT_EDGE_DEVICE_BIND_REQUEST",
                "EDGE_DEVICE_BIND_REQUEST", String.valueOf(saved.getId()), "SUCCESS", "设备提交企业绑定申请",
                Map.of("deviceId", device.getId(), "deviceCode", device.getDeviceCode(), "requestedEnterpriseId", enterprise.getId()));
        return toResponse(saved);
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData current(String deviceCode, String deviceToken) {
        Device device = deviceService.authenticateAndTouch(deviceCode, deviceToken).device();
        EdgeDeviceBindRequest current = latest(device);
        if (current == null) {
            return null;
        }
        return toResponse(refreshStatusIfExpired(current, device));
    }

    @Transactional(readOnly = true)
    public EdgeDeviceBindRequestPageResponseData list(AuthenticatedUser operator, Integer page, Integer size, Long enterpriseId, String status, String deviceCode) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        Specification<EdgeDeviceBindRequest> specification = buildSpecification(readableEnterpriseId, normalizeOptional(status), normalizeOptional(deviceCode));
        Page<EdgeDeviceBindRequest> result = edgeDeviceBindRequestRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new EdgeDeviceBindRequestPageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(this::toResponse).toList());
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData get(AuthenticatedUser operator, Long id) {
        EdgeDeviceBindRequest bindRequest = getEntity(id);
        businessAccessService.resolveReadableEnterpriseId(operator, bindRequest.getRequestedEnterpriseId());
        Device device = deviceService.requireDevice(bindRequest.getDeviceId());
        return toResponse(refreshStatusIfExpired(bindRequest, device));
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData approve(AuthenticatedUser operator, Long id, ReviewEdgeDeviceBindRequest request) {
        EdgeDeviceBindRequest bindRequest = getEntity(id);
        businessAccessService.assertCanManageEnterprise(operator, bindRequest.getRequestedEnterpriseId());
        Device device = deviceService.requireDevice(bindRequest.getDeviceId());
        bindRequest = refreshStatusIfExpired(bindRequest, device);
        if (!EdgeDeviceBindRequestStatus.PENDING.name().equals(bindRequest.getStatus())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前申请不可审批");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        bindRequest.setStatus(EdgeDeviceBindRequestStatus.APPROVED.name());
        bindRequest.setReviewRemark(normalizeRemark(request == null ? null : request.remark()));
        bindRequest.setReviewedAt(now);
        bindRequest.setReviewedBy(operator.getUserId());
        bindRequest.setUpdatedAt(now);

        device.setEnterpriseId(bindRequest.getRequestedEnterpriseId());
        reconcileVehicleBinding(device);
        deviceService.syncDeviceStatusAfterBindRequestChange(device);
        EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);
        systemAuditService.record(operator, "EDGE_DEVICE_BIND_REQUEST", "APPROVE_EDGE_DEVICE_BIND_REQUEST",
                "EDGE_DEVICE_BIND_REQUEST", String.valueOf(saved.getId()), "SUCCESS", "审批通过设备企业绑定申请",
                auditDetail(operator, saved, device));
        return toResponse(saved);
    }

    @Transactional
    public EdgeDeviceBindRequestResponseData reject(AuthenticatedUser operator, Long id, ReviewEdgeDeviceBindRequest request) {
        EdgeDeviceBindRequest bindRequest = getEntity(id);
        businessAccessService.assertCanManageEnterprise(operator, bindRequest.getRequestedEnterpriseId());
        Device device = deviceService.requireDevice(bindRequest.getDeviceId());
        bindRequest = refreshStatusIfExpired(bindRequest, device);
        if (!EdgeDeviceBindRequestStatus.PENDING.name().equals(bindRequest.getStatus())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "当前申请不可驳回");
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        bindRequest.setStatus(EdgeDeviceBindRequestStatus.REJECTED.name());
        bindRequest.setReviewRemark(normalizeRemark(request == null ? null : request.remark()));
        bindRequest.setReviewedAt(now);
        bindRequest.setReviewedBy(operator.getUserId());
        bindRequest.setUpdatedAt(now);
        EdgeDeviceBindRequest saved = edgeDeviceBindRequestRepository.save(bindRequest);

        deviceService.syncDeviceStatusAfterBindRequestChange(device);
        systemAuditService.record(operator, "EDGE_DEVICE_BIND_REQUEST", "REJECT_EDGE_DEVICE_BIND_REQUEST",
                "EDGE_DEVICE_BIND_REQUEST", String.valueOf(saved.getId()), "SUCCESS", "驳回设备企业绑定申请",
                auditDetail(operator, saved, device));
        return toResponse(saved);
    }

    private EdgeDeviceBindRequest latest(Device device) {
        return edgeDeviceBindRequestRepository.findTopByDeviceIdOrderByCreatedAtDesc(device.getId()).orElse(null);
    }

    private EdgeDeviceBindRequest refreshStatusIfExpired(EdgeDeviceBindRequest bindRequest, Device device) {
        if (EdgeDeviceBindRequestStatus.PENDING.name().equals(bindRequest.getStatus())
                && bindRequest.getExpiresAt() != null
                && bindRequest.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            bindRequest.setStatus(EdgeDeviceBindRequestStatus.EXPIRED.name());
            bindRequest.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
            edgeDeviceBindRequestRepository.save(bindRequest);
            deviceService.syncDeviceStatusAfterBindRequestChange(device);
        }
        return bindRequest;
    }

    private void reconcileVehicleBinding(Device device) {
        if (device.getEnterpriseId() == null) {
            device.setFleetId(null);
            device.setVehicleId(null);
            return;
        }
        if (device.getVehicleId() != null) {
            var vehicle = vehicleRepository.findById(device.getVehicleId()).orElse(null);
            if (vehicle == null || !device.getEnterpriseId().equals(vehicle.getEnterpriseId())) {
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

    private EdgeDeviceBindRequestResponseData toResponse(EdgeDeviceBindRequest bindRequest) {
        return new EdgeDeviceBindRequestResponseData(
                bindRequest.getId(),
                bindRequest.getDeviceId(),
                bindRequest.getDeviceCode(),
                bindRequest.getRequestedEnterpriseId(),
                bindRequest.getStatus(),
                bindRequest.getApplyRemark(),
                bindRequest.getReviewRemark(),
                toOffsetDateTime(bindRequest.getSubmittedAt()),
                toOffsetDateTime(bindRequest.getReviewedAt()),
                bindRequest.getReviewedBy(),
                toOffsetDateTime(bindRequest.getExpiresAt()),
                toOffsetDateTime(bindRequest.getCreatedAt()),
                toOffsetDateTime(bindRequest.getUpdatedAt()));
    }

    private Map<String, Object> auditDetail(AuthenticatedUser operator, EdgeDeviceBindRequest bindRequest, Device device) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator.getUserId());
        detail.put("operatorRoles", operator.getRoles());
        detail.put("bindRequestId", bindRequest.getId());
        detail.put("deviceId", device.getId());
        detail.put("deviceCode", device.getDeviceCode());
        detail.put("requestedEnterpriseId", bindRequest.getRequestedEnterpriseId());
        detail.put("status", bindRequest.getStatus());
        return detail;
    }

    private String normalizeRemark(String remark) {
        return StringUtils.hasText(remark) ? remark.trim() : null;
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
