package com.example.demo.driver.service;

import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.driver.dto.CreateDriverRequest;
import com.example.demo.driver.dto.DriverDetailResponseData;
import com.example.demo.driver.dto.DriverListItemData;
import com.example.demo.driver.dto.DriverPageResponseData;
import com.example.demo.driver.dto.ReassignDriverFleetRequest;
import com.example.demo.driver.dto.UpdateDriverRequest;
import com.example.demo.driver.entity.Driver;
import com.example.demo.driver.repository.DriverRepository;
import com.example.demo.enterprise.repository.EnterpriseRepository;
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
public class DriverManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final DriverRepository driverRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final FleetRepository fleetRepository;
    private final BusinessAccessService businessAccessService;
    private final SystemAuditService systemAuditService;

    public DriverManagementService(DriverRepository driverRepository,
                                   EnterpriseRepository enterpriseRepository,
                                   FleetRepository fleetRepository,
                                   BusinessAccessService businessAccessService,
                                   SystemAuditService systemAuditService) {
        this.driverRepository = driverRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.fleetRepository = fleetRepository;
        this.businessAccessService = businessAccessService;
        this.systemAuditService = systemAuditService;
    }

    @Transactional(readOnly = true)
    public DriverPageResponseData listDrivers(AuthenticatedUser operator,
                                              Integer page,
                                              Integer size,
                                              Long enterpriseId,
                                              Long fleetId,
                                              String keyword,
                                              Byte status) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Long readableEnterpriseId = businessAccessService.resolveReadableEnterpriseId(operator, enterpriseId);
        validateFleetScope(fleetId, readableEnterpriseId);
        Specification<Driver> specification = buildSpecification(readableEnterpriseId, fleetId, keyword, normalizeStatus(status, true));
        Page<Driver> result = driverRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));
        return new DriverPageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(this::toListItem).toList());
    }

    @Transactional(readOnly = true)
    public DriverDetailResponseData getDriver(AuthenticatedUser operator, Long driverId) {
        Driver driver = getDriverEntity(driverId);
        businessAccessService.resolveReadableEnterpriseId(operator, driver.getEnterpriseId());
        return toDetail(driver);
    }

    @Transactional
    public DriverDetailResponseData createDriver(AuthenticatedUser operator, CreateDriverRequest request) {
        Long enterpriseId = request.enterpriseId();
        businessAccessService.assertCanManageEnterprise(operator, enterpriseId);
        validateEnterpriseExists(enterpriseId);
        validateFleetBelongsToEnterprise(request.fleetId(), enterpriseId);

        Driver driver = new Driver();
        driver.setEnterpriseId(enterpriseId);
        driver.setFleetId(request.fleetId());
        driver.setName(normalizeRequired(request.name(), "name不能为空"));
        driver.setPhone(normalizeOptional(request.phone()));
        driver.setLicenseNo(normalizeOptional(request.licenseNo()));
        driver.setStatus((byte) 1);
        driver.setRemark(normalizeOptional(request.remark()));
        driver.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        driver.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        Driver saved = driverRepository.save(driver);

        systemAuditService.record(operator, "DRIVER", "CREATE_DRIVER", "DRIVER", String.valueOf(saved.getId()),
                "SUCCESS", "创建驾驶员", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), null, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public DriverDetailResponseData updateDriver(AuthenticatedUser operator, Long driverId, UpdateDriverRequest request) {
        Driver driver = getDriverEntity(driverId);
        businessAccessService.assertCanManageEnterprise(operator, driver.getEnterpriseId());
        Map<String, Object> before = snapshot(driver);
        driver.setName(normalizeRequired(request.name(), "name不能为空"));
        driver.setPhone(normalizeOptional(request.phone()));
        driver.setLicenseNo(normalizeOptional(request.licenseNo()));
        driver.setRemark(normalizeOptional(request.remark()));
        Driver saved = driverRepository.save(driver);

        systemAuditService.record(operator, "DRIVER", "UPDATE_DRIVER", "DRIVER", String.valueOf(saved.getId()),
                "SUCCESS", "更新驾驶员", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public DriverDetailResponseData updateStatus(AuthenticatedUser operator, Long driverId, Byte status) {
        Driver driver = getDriverEntity(driverId);
        businessAccessService.assertCanManageEnterprise(operator, driver.getEnterpriseId());
        Map<String, Object> before = snapshot(driver);
        driver.setStatus(normalizeStatus(status, false));
        Driver saved = driverRepository.save(driver);

        systemAuditService.record(operator, "DRIVER", "UPDATE_DRIVER_STATUS", "DRIVER", String.valueOf(saved.getId()),
                "SUCCESS", "更新驾驶员状态", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public DriverDetailResponseData reassignFleet(AuthenticatedUser operator, Long driverId, ReassignDriverFleetRequest request) {
        Driver driver = getDriverEntity(driverId);
        businessAccessService.assertCanManageEnterprise(operator, driver.getEnterpriseId());
        validateFleetBelongsToEnterprise(request.fleetId(), driver.getEnterpriseId());
        Map<String, Object> before = snapshot(driver);
        driver.setFleetId(request.fleetId());
        Driver saved = driverRepository.save(driver);

        systemAuditService.record(operator, "DRIVER", "REASSIGN_DRIVER_FLEET", "DRIVER", String.valueOf(saved.getId()),
                "SUCCESS", "调整驾驶员所属车队", auditDetail(operator, saved.getId(), saved.getEnterpriseId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    private Specification<Driver> buildSpecification(Long enterpriseId, Long fleetId, String keyword, Byte status) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("enterpriseId"), enterpriseId));
            }
            if (fleetId != null) {
                predicates.add(cb.equal(root.get("fleetId"), fleetId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), pattern),
                        cb.like(root.get("phone"), pattern),
                        cb.like(root.get("licenseNo"), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Driver getDriverEntity(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private void validateEnterpriseExists(Long enterpriseId) {
        if (!enterpriseRepository.existsById(enterpriseId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不存在");
        }
    }

    private void validateFleetScope(Long fleetId, Long enterpriseId) {
        if (fleetId == null) {
            return;
        }
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "fleetId不存在"));
        if (enterpriseId != null && !enterpriseId.equals(fleet.getEnterpriseId())) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    private void validateFleetBelongsToEnterprise(Long fleetId, Long enterpriseId) {
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "fleetId不存在"));
        if (!fleet.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "fleetId不属于当前enterpriseId");
        }
    }

    private DriverListItemData toListItem(Driver driver) {
        return new DriverListItemData(
                driver.getId(),
                driver.getEnterpriseId(),
                driver.getFleetId(),
                driver.getName(),
                driver.getPhone(),
                driver.getLicenseNo(),
                driver.getStatus(),
                driver.getRemark(),
                toOffsetDateTime(driver.getCreatedAt()),
                toOffsetDateTime(driver.getUpdatedAt()));
    }

    private DriverDetailResponseData toDetail(Driver driver) {
        return new DriverDetailResponseData(
                driver.getId(),
                driver.getEnterpriseId(),
                driver.getFleetId(),
                driver.getName(),
                driver.getPhone(),
                driver.getLicenseNo(),
                driver.getStatus(),
                driver.getRemark(),
                toOffsetDateTime(driver.getCreatedAt()),
                toOffsetDateTime(driver.getUpdatedAt()));
    }

    private Map<String, Object> snapshot(Driver driver) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", driver.getId());
        snapshot.put("enterpriseId", driver.getEnterpriseId());
        snapshot.put("fleetId", driver.getFleetId());
        snapshot.put("name", driver.getName());
        snapshot.put("phone", driver.getPhone());
        snapshot.put("licenseNo", driver.getLicenseNo());
        snapshot.put("status", driver.getStatus());
        snapshot.put("remark", driver.getRemark());
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
        detail.put("targetType", "DRIVER");
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

    private Byte normalizeStatus(Byte status, boolean allowNull) {
        if (status == null) {
            if (allowNull) {
                return null;
            }
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
