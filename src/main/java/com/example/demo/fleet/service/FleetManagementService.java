package com.example.demo.fleet.service;

import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.fleet.dto.CreateFleetRequest;
import com.example.demo.fleet.dto.FleetDetailResponseData;
import com.example.demo.fleet.dto.FleetListItemData;
import com.example.demo.fleet.dto.FleetPageResponseData;
import com.example.demo.fleet.dto.UpdateFleetRequest;
import com.example.demo.fleet.entity.Fleet;
import com.example.demo.fleet.repository.FleetRepository;
import com.example.demo.system.service.SystemAuditService;
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
public class FleetManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final FleetRepository fleetRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;

    public FleetManagementService(FleetRepository fleetRepository,
                                  EnterpriseRepository enterpriseRepository,
                                  BusinessAccessService businessAccessService,
                                  SystemAuditService systemAuditService) {
        this.fleetRepository = fleetRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
    }

    @Transactional(readOnly = true)
    public FleetPageResponseData listFleets(AuthenticatedUser operator,
                                            Integer page,
                                            Integer size,
                                            Long enterpriseId) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        Specification<Fleet> specification = buildSpecification(readableEnterpriseId);
        Page<Fleet> result = fleetRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));
        return new FleetPageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(this::toListItem).toList());
    }

    @Transactional(readOnly = true)
    public FleetDetailResponseData getFleet(AuthenticatedUser operator, Long fleetId) {
        Fleet fleet = getFleetEntity(fleetId);
        businessAccessService.resolveReadableEnterpriseId(operator, fleet.getEnterpriseId());
        return toDetail(fleet);
    }

    @Transactional
    public FleetDetailResponseData createFleet(AuthenticatedUser operator, CreateFleetRequest request) {
        Long enterpriseId = request.enterpriseId();
        businessAccessService.assertCanManageEnterprise(operator, enterpriseId);
        validateEnterpriseExists(enterpriseId);
        String name = normalizeRequired(request.name(), "name不能为空");
        if (fleetRepository.existsByEnterpriseIdAndName(enterpriseId, name)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "车队名称已存在");
        }

        Fleet fleet = new Fleet();
        fleet.setEnterpriseId(enterpriseId);
        fleet.setName(name);
        fleet.setStatus((byte) 1);
        fleet.setRemark(normalizeOptional(request.remark()));
        fleet.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        fleet.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        Fleet saved = fleetRepository.save(fleet);

        systemAuditService.record(operator, "FLEET", "CREATE_FLEET", "FLEET", String.valueOf(saved.getId()),
                "SUCCESS", "创建车队", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), null, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public FleetDetailResponseData updateFleet(AuthenticatedUser operator, Long fleetId, UpdateFleetRequest request) {
        Fleet fleet = getFleetEntity(fleetId);
        businessAccessService.assertCanManageEnterprise(operator, fleet.getEnterpriseId());
        Map<String, Object> before = snapshot(fleet);
        String name = normalizeRequired(request.name(), "name不能为空");
        if (fleetRepository.existsByEnterpriseIdAndNameAndIdNot(fleet.getEnterpriseId(), name, fleetId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "车队名称已存在");
        }
        fleet.setName(name);
        fleet.setRemark(normalizeOptional(request.remark()));
        Fleet saved = fleetRepository.save(fleet);

        systemAuditService.record(operator, "FLEET", "UPDATE_FLEET", "FLEET", String.valueOf(saved.getId()),
                "SUCCESS", "更新车队", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public FleetDetailResponseData updateStatus(AuthenticatedUser operator, Long fleetId, Byte status) {
        Fleet fleet = getFleetEntity(fleetId);
        businessAccessService.assertCanManageEnterprise(operator, fleet.getEnterpriseId());
        Map<String, Object> before = snapshot(fleet);
        fleet.setStatus(normalizeStatus(status));
        Fleet saved = fleetRepository.save(fleet);

        systemAuditService.record(operator, "FLEET", "UPDATE_FLEET_STATUS", "FLEET", String.valueOf(saved.getId()),
                "SUCCESS", "更新车队状态", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    private Specification<Fleet> buildSpecification(Long enterpriseId) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("enterpriseId"), enterpriseId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Fleet getFleetEntity(Long fleetId) {
        return fleetRepository.findById(fleetId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private void validateEnterpriseExists(Long enterpriseId) {
        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不存在");
        }
    }

    private FleetListItemData toListItem(Fleet fleet) {
        return new FleetListItemData(
                fleet.getId(),
                fleet.getEnterpriseId(),
                fleet.getName(),
                fleet.getStatus(),
                fleet.getRemark(),
                toOffsetDateTime(fleet.getCreatedAt()),
                toOffsetDateTime(fleet.getUpdatedAt()));
    }

    private FleetDetailResponseData toDetail(Fleet fleet) {
        return new FleetDetailResponseData(
                fleet.getId(),
                fleet.getEnterpriseId(),
                fleet.getName(),
                fleet.getStatus(),
                fleet.getRemark(),
                toOffsetDateTime(fleet.getCreatedAt()),
                toOffsetDateTime(fleet.getUpdatedAt()));
    }

    private Map<String, Object> snapshot(Fleet fleet) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", fleet.getId());
        snapshot.put("enterpriseId", fleet.getEnterpriseId());
        snapshot.put("name", fleet.getName());
        snapshot.put("status", fleet.getStatus());
        snapshot.put("remark", fleet.getRemark());
        return snapshot;
    }

    private Map<String, Object> auditDetail(AuthenticatedUser operator,
                                            Long targetId,
                                            Long targetEnterpriseId,
                                            Map<String, Object> before,
                                            Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator.getUserId());
        detail.put("operatorRoles", operator.getRoles());
        detail.put("operatorEnterpriseId", resolveOperatorEnterpriseId(operator));
        detail.put("targetType", "FLEET");
        detail.put("targetId", targetId);
        detail.put("targetEnterpriseId", targetEnterpriseId);
        detail.put("before", before);
        detail.put("after", after);
        return detail;
    }

    private Long resolveOperatorEnterpriseId(AuthenticatedUser operator) {
        try {
            return businessAccessService.getOperatorAccount(operator).getEnterpriseId();
        } catch (BusinessException ex) {
            return null;
        }
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

    private Byte normalizeStatus(Byte status) {
        if (status == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status不能为空");
        }
        if (status != (byte) 0 && status != (byte) 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "status仅支持0或1");
        }
        return status;
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

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
