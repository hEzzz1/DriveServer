package com.example.demo.enterprise.service;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.model.RoleCode;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.dto.CreateEnterpriseRequest;
import com.example.demo.enterprise.dto.EnterpriseDetailResponseData;
import com.example.demo.enterprise.dto.EnterpriseListItemData;
import com.example.demo.enterprise.dto.EnterprisePageResponseData;
import com.example.demo.enterprise.dto.UpdateEnterpriseRequest;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
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
public class EnterpriseManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final EnterpriseRepository enterpriseRepository;
    private final UserAccountRepository userAccountRepository;
    private final SystemAuditService systemAuditService;

    public EnterpriseManagementService(EnterpriseRepository enterpriseRepository,
                                       UserAccountRepository userAccountRepository,
                                       SystemAuditService systemAuditService) {
        this.enterpriseRepository = enterpriseRepository;
        this.userAccountRepository = userAccountRepository;
        this.systemAuditService = systemAuditService;
    }

    @Transactional(readOnly = true)
    public EnterprisePageResponseData listEnterprises(AuthenticatedUser operator,
                                                      Integer page,
                                                      Integer size,
                                                      String keyword,
                                                      Boolean enabled) {
        UserAccount currentUser = getOperatorAccount(operator);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Specification<Enterprise> specification = buildSpecification(operator, currentUser, keyword, enabled);
        Page<Enterprise> result = enterpriseRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));

        return new EnterprisePageResponseData(
                result.getTotalElements(),
                pageNo,
                pageSize,
                result.getContent().stream().map(this::toListItem).toList());
    }

    @Transactional(readOnly = true)
    public EnterpriseDetailResponseData getEnterprise(AuthenticatedUser operator, Long enterpriseId) {
        UserAccount currentUser = getOperatorAccount(operator);
        Enterprise enterprise = getEnterpriseEntity(enterpriseId);
        assertCanRead(operator, currentUser, enterprise);
        return toDetail(enterprise);
    }

    @Transactional
    public EnterpriseDetailResponseData createEnterprise(AuthenticatedUser operator, CreateEnterpriseRequest request) {
        assertSuperAdmin(operator);
        String code = normalizeRequired(request.code(), "code不能为空");
        if (enterpriseRepository.existsByCode(code)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "企业编码已存在");
        }

        Enterprise enterprise = new Enterprise();
        enterprise.setCode(code);
        enterprise.setName(normalizeRequired(request.name(), "name不能为空"));
        enterprise.setStatus((byte) 1);
        enterprise.setContactName(normalizeOptional(request.contactName()));
        enterprise.setContactPhone(normalizeOptional(request.contactPhone()));
        enterprise.setRemark(normalizeOptional(request.remark()));
        enterprise.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        enterprise.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        Enterprise saved = enterpriseRepository.save(enterprise);

        systemAuditService.record(operator, "ENTERPRISE", "CREATE_ENTERPRISE", "ENTERPRISE", String.valueOf(saved.getId()),
                "SUCCESS", "创建企业", auditDetail(operator, saved.getId(), null, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public EnterpriseDetailResponseData updateEnterprise(AuthenticatedUser operator, Long enterpriseId, UpdateEnterpriseRequest request) {
        assertSuperAdmin(operator);
        Enterprise enterprise = getEnterpriseEntity(enterpriseId);
        Map<String, Object> before = snapshot(enterprise);
        String code = normalizeRequired(request.code(), "code不能为空");
        if (enterpriseRepository.existsByCodeAndIdNot(code, enterpriseId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "企业编码已存在");
        }

        enterprise.setCode(code);
        enterprise.setName(normalizeRequired(request.name(), "name不能为空"));
        enterprise.setContactName(normalizeOptional(request.contactName()));
        enterprise.setContactPhone(normalizeOptional(request.contactPhone()));
        enterprise.setRemark(normalizeOptional(request.remark()));
        Enterprise saved = enterpriseRepository.save(enterprise);

        systemAuditService.record(operator, "ENTERPRISE", "UPDATE_ENTERPRISE", "ENTERPRISE", String.valueOf(saved.getId()),
                "SUCCESS", "更新企业信息", auditDetail(operator, saved.getId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    @Transactional
    public EnterpriseDetailResponseData updateStatus(AuthenticatedUser operator, Long enterpriseId, Boolean enabled) {
        assertSuperAdmin(operator);
        if (enabled == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enabled不能为空");
        }
        Enterprise enterprise = getEnterpriseEntity(enterpriseId);
        Map<String, Object> before = snapshot(enterprise);
        enterprise.setStatus(Boolean.TRUE.equals(enabled) ? (byte) 1 : (byte) 0);
        Enterprise saved = enterpriseRepository.save(enterprise);

        systemAuditService.record(operator, "ENTERPRISE", "UPDATE_ENTERPRISE_STATUS", "ENTERPRISE", String.valueOf(saved.getId()),
                "SUCCESS", "更新企业状态", auditDetail(operator, saved.getId(), before, snapshot(saved)));
        return toDetail(saved);
    }

    private Specification<Enterprise> buildSpecification(AuthenticatedUser operator,
                                                         UserAccount currentUser,
                                                         String keyword,
                                                         Boolean enabled) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (isEnterpriseAdmin(operator)) {
                predicates.add(cb.equal(root.get("id"), requireEnterpriseId(currentUser)));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("code"), pattern),
                        cb.like(root.get("name"), pattern)));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("status"), Boolean.TRUE.equals(enabled) ? (byte) 1 : (byte) 0));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Enterprise getEnterpriseEntity(Long enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
    }

    private UserAccount getOperatorAccount(AuthenticatedUser operator) {
        return userAccountRepository.findById(operator.getUserId())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
    }

    private void assertCanRead(AuthenticatedUser operator, UserAccount currentUser, Enterprise enterprise) {
        if (!isEnterpriseAdmin(operator)) {
            return;
        }
        if (!enterprise.getId().equals(requireEnterpriseId(currentUser))) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    private void assertSuperAdmin(AuthenticatedUser operator) {
        if (!operator.getRoles().contains(RoleCode.SUPER_ADMIN.name())) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    private boolean isEnterpriseAdmin(AuthenticatedUser operator) {
        return operator.getRoles().contains(RoleCode.ENTERPRISE_ADMIN.name())
                && !operator.getRoles().contains(RoleCode.SUPER_ADMIN.name())
                && !operator.getRoles().contains(RoleCode.SYS_ADMIN.name());
    }

    private Long requireEnterpriseId(UserAccount user) {
        if (user.getEnterpriseId() == null) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return user.getEnterpriseId();
    }

    private EnterpriseListItemData toListItem(Enterprise enterprise) {
        return new EnterpriseListItemData(
                enterprise.getId(),
                enterprise.getCode(),
                enterprise.getName(),
                isEnabled(enterprise),
                enterprise.getContactName(),
                enterprise.getContactPhone(),
                enterprise.getRemark(),
                toOffsetDateTime(enterprise.getCreatedAt()),
                toOffsetDateTime(enterprise.getUpdatedAt()));
    }

    private EnterpriseDetailResponseData toDetail(Enterprise enterprise) {
        return new EnterpriseDetailResponseData(
                enterprise.getId(),
                enterprise.getCode(),
                enterprise.getName(),
                isEnabled(enterprise),
                enterprise.getContactName(),
                enterprise.getContactPhone(),
                enterprise.getRemark(),
                toOffsetDateTime(enterprise.getCreatedAt()),
                toOffsetDateTime(enterprise.getUpdatedAt()));
    }

    private boolean isEnabled(Enterprise enterprise) {
        return enterprise.getStatus() != null && enterprise.getStatus() == (byte) 1;
    }

    private Map<String, Object> snapshot(Enterprise enterprise) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", enterprise.getId());
        snapshot.put("code", enterprise.getCode());
        snapshot.put("name", enterprise.getName());
        snapshot.put("enabled", isEnabled(enterprise));
        snapshot.put("contactName", enterprise.getContactName());
        snapshot.put("contactPhone", enterprise.getContactPhone());
        snapshot.put("remark", enterprise.getRemark());
        return snapshot;
    }

    private Map<String, Object> auditDetail(AuthenticatedUser operator,
                                            Long targetEnterpriseId,
                                            Map<String, Object> before,
                                            Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operator.getUserId());
        detail.put("operatorRoles", operator.getRoles());
        detail.put("operatorEnterpriseId", resolveOperatorEnterpriseId(operator));
        detail.put("targetType", "ENTERPRISE");
        detail.put("targetId", targetEnterpriseId);
        detail.put("targetEnterpriseId", targetEnterpriseId);
        detail.put("before", before);
        detail.put("after", after);
        return detail;
    }

    private Long resolveOperatorEnterpriseId(AuthenticatedUser operator) {
        return userAccountRepository.findById(operator.getUserId())
                .map(UserAccount::getEnterpriseId)
                .orElse(null);
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
