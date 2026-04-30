package com.example.demo.user.service;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserScopeRole;
import com.example.demo.auth.model.RoleTemplateCode;
import com.example.demo.auth.model.ScopeType;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserScopeRoleRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import com.example.demo.system.service.SystemAuditService;
import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.ResetUserPasswordRequest;
import com.example.demo.user.dto.UpdateUserRequest;
import com.example.demo.user.dto.UserDetailResponseData;
import com.example.demo.user.dto.UserListItemData;
import com.example.demo.user.dto.UserPageResponseData;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PlatformEnterpriseAdminService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final byte ACTIVE_SCOPE_ROLE_STATUS = 1;
    private static final Set<String> PLATFORM_MANAGED_ROLES = Set.of(
            RoleTemplateCode.PLATFORM_SUPER_ADMIN.name(),
            RoleTemplateCode.PLATFORM_SYS_ADMIN.name(),
            RoleTemplateCode.PLATFORM_RISK_ADMIN.name(),
            RoleTemplateCode.ORG_ADMIN.name()
    );

    private final UserAccountRepository userAccountRepository;
    private final UserScopeRoleRepository userScopeRoleRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemAuditService systemAuditService;
    private final BusinessAccessService businessAccessService;

    public PlatformEnterpriseAdminService(UserAccountRepository userAccountRepository,
                                          UserScopeRoleRepository userScopeRoleRepository,
                                          EnterpriseRepository enterpriseRepository,
                                          PasswordEncoder passwordEncoder,
                                          SystemAuditService systemAuditService,
                                          BusinessAccessService businessAccessService) {
        this.userAccountRepository = userAccountRepository;
        this.userScopeRoleRepository = userScopeRoleRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemAuditService = systemAuditService;
        this.businessAccessService = businessAccessService;
    }

    @Transactional
    public UserDetailResponseData createUser(AuthenticatedUser operator, CreateUserRequest request) {
        assertPlatformAdmin(operator);
        UserAccount currentUser = getOperatorAccount(operator);
        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "用户名已存在");
        }

        List<String> normalizedRoles = validateRequestedRoles(request.roles());
        Long enterpriseId = resolveTargetEnterpriseId(request.enterpriseId(), normalizedRoles);
        String nickname = StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username;

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        user.setNickname(nickname);
        user.setSubjectType(SubjectType.USER.name());
        user.setEnterpriseId(enterpriseId);
        user.setStatus(Boolean.FALSE.equals(request.enabled()) ? (byte) 0 : (byte) 1);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        UserAccount saved = userAccountRepository.save(user);

        syncScopeRoles(saved, normalizedRoles);
        systemAuditService.record(operator, "USER", "CREATE_ENTERPRISE_ADMIN", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "创建平台域用户", buildAuditDetail(currentUser, resolveOperatorRoles(operator), saved, null, snapshot(saved, normalizedRoles)));
        return toDetail(saved, normalizedRoles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData updateUser(AuthenticatedUser operator, Long userId, UpdateUserRequest request) {
        assertPlatformAdmin(operator);
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        List<String> currentRoles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        assertCanAccessTarget(user, currentRoles);
        Map<String, Object> before = snapshot(user, currentRoles);

        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "用户名已存在");
        }

        Long enterpriseId = resolveUpdatedEnterpriseId(user, request.enterpriseId(), currentRoles);
        user.setUsername(username);
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username);
        user.setEnterpriseId(enterpriseId);
        UserAccount saved = userAccountRepository.save(user);
        syncScopeRoles(saved, currentRoles);

        systemAuditService.record(operator, "USER", "UPDATE_ENTERPRISE_ADMIN_PROFILE", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "更新平台域用户信息", buildAuditDetail(currentUser, resolveOperatorRoles(operator), saved, before, snapshot(saved, currentRoles)));
        return toDetail(saved, currentRoles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional(readOnly = true)
    public UserPageResponseData listUsers(AuthenticatedUser operator,
                                          Integer page,
                                          Integer size,
                                          String keyword,
                                          Boolean enabled,
                                          Long enterpriseId) {
        assertPlatformAdmin(operator);
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Specification<UserAccount> specification = buildUserSpecification(keyword, enabled, enterpriseId);
        Page<UserAccount> result = userAccountRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));

        Map<Long, List<String>> roleMap = loadRoleMap(result.getContent().stream().map(UserAccount::getId).toList());
        List<UserListItemData> items = result.getContent().stream()
                .filter(user -> isPlatformManagedUser(roleMap.getOrDefault(user.getId(), List.of())))
                .map(user -> new UserListItemData(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getEnterpriseId(),
                        resolveEnterpriseName(user.getEnterpriseId()),
                        isEnabled(user),
                        roleMap.getOrDefault(user.getId(), List.of()),
                        toOffsetDateTime(user.getCreatedAt()),
                        toOffsetDateTime(user.getUpdatedAt())))
                .toList();
        return new UserPageResponseData(result.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public UserDetailResponseData getUser(AuthenticatedUser operator, Long userId) {
        assertPlatformAdmin(operator);
        UserAccount user = getUserAccount(userId);
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        assertCanAccessTarget(user, roles);
        return toDetail(user, roles, resolveEnterpriseName(user.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData updateRoles(AuthenticatedUser operator, Long userId, List<String> requestedRoles) {
        assertPlatformAdmin(operator);
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        List<String> beforeRoles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        assertCanAccessTarget(user, beforeRoles);
        Map<String, Object> before = snapshot(user, beforeRoles);
        List<String> normalizedRoles = validateRequestedRoles(requestedRoles);
        ensureSuperAdminRetained(beforeRoles, normalizedRoles);
        user.setEnterpriseId(resolveTargetEnterpriseId(user.getEnterpriseId(), normalizedRoles));
        syncScopeRoles(user, normalizedRoles);
        systemAuditService.record(operator, "USER", "UPDATE_ENTERPRISE_ADMIN_ROLES", "USER", String.valueOf(user.getId()),
                "SUCCESS", "更新平台域用户角色", buildAuditDetail(currentUser, resolveOperatorRoles(operator), user, before, snapshot(user, normalizedRoles)));
        return toDetail(user, normalizedRoles, resolveEnterpriseName(user.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData updateStatus(AuthenticatedUser operator, Long userId, Boolean enabled) {
        assertPlatformAdmin(operator);
        if (enabled == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enabled不能为空");
        }
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        assertCanAccessTarget(user, roles);
        Map<String, Object> before = snapshot(user, roles);
        ensureSuperAdminStatusAllowed(user, roles, enabled);
        user.setStatus(Boolean.TRUE.equals(enabled) ? (byte) 1 : (byte) 0);
        UserAccount saved = userAccountRepository.save(user);
        systemAuditService.record(operator, "USER", "UPDATE_ENTERPRISE_ADMIN_STATUS", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "更新平台域用户状态", buildAuditDetail(currentUser, resolveOperatorRoles(operator), saved, before, snapshot(saved, roles)));
        return toDetail(saved, roles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData resetPassword(AuthenticatedUser operator, Long userId, ResetUserPasswordRequest request) {
        assertPlatformAdmin(operator);
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        assertCanAccessTarget(user, roles);
        if (!StringUtils.hasText(request.newPassword())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "newPassword不能为空");
        }
        Map<String, Object> before = snapshot(user, roles);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        UserAccount saved = userAccountRepository.save(user);
        Map<String, Object> after = snapshot(saved, roles);
        after.put("passwordReset", true);
        systemAuditService.record(operator, "USER", "RESET_ENTERPRISE_ADMIN_PASSWORD", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "重置平台域用户密码", buildAuditDetail(currentUser, resolveOperatorRoles(operator), saved, before, after));
        return toDetail(saved, roles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional(readOnly = true)
    public SystemAuditPageResponseData listUserAudits(AuthenticatedUser operator, Long userId, Integer page, Integer size) {
        assertPlatformAdmin(operator);
        UserAccount targetUser = getUserAccount(userId);
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        assertCanAccessTarget(targetUser, roles);
        return systemAuditService.list("USER", null, "USER", String.valueOf(userId), null, null, null, page, size);
    }

    private Specification<UserAccount> buildUserSpecification(String keyword, Boolean enabled, Long enterpriseId) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subjectType"), SubjectType.USER.name()));

            var includedRoles = query.subquery(Long.class);
            var includedRoleRoot = includedRoles.from(UserScopeRole.class);
            includedRoles.select(includedRoleRoot.get("userId"))
                    .where(
                            cb.equal(includedRoleRoot.get("status"), ACTIVE_SCOPE_ROLE_STATUS),
                            includedRoleRoot.get("roleCode").in(PLATFORM_MANAGED_ROLES));
            predicates.add(root.get("id").in(includedRoles));

            var excludedRoles = query.subquery(Long.class);
            var excludedRoleRoot = excludedRoles.from(UserScopeRole.class);
            excludedRoles.select(excludedRoleRoot.get("userId"))
                    .where(
                            cb.equal(excludedRoleRoot.get("status"), ACTIVE_SCOPE_ROLE_STATUS),
                            cb.not(excludedRoleRoot.get("roleCode").in(PLATFORM_MANAGED_ROLES)));
            predicates.add(cb.not(root.get("id").in(excludedRoles)));

            if (enterpriseId != null) {
                predicates.add(cb.equal(root.get("enterpriseId"), enterpriseId));
            }
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), pattern),
                        cb.like(root.get("nickname"), pattern)));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("status"), Boolean.TRUE.equals(enabled) ? (byte) 1 : (byte) 0));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private UserAccount getUserAccount(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
        if (!SubjectType.USER.name().equals(user.getSubjectType())) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
        return user;
    }

    private UserAccount getOperatorAccount(AuthenticatedUser operator) {
        return getUserAccount(operator.getUserId());
    }

    private List<String> resolveOperatorRoles(AuthenticatedUser operator) {
        return businessAccessService.getAuthorizationProfile(operator).roles();
    }

    private Map<Long, List<String>> loadRoleMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, LinkedHashSet<String>> roleSets = new LinkedHashMap<>();
        for (UserScopeRole assignment : userScopeRoleRepository.findByUserIdInAndStatusOrderByIdAsc(userIds, ACTIVE_SCOPE_ROLE_STATUS)) {
            RoleTemplateCode.from(assignment.getRoleCode()).ifPresent(role ->
                    roleSets.computeIfAbsent(assignment.getUserId(), key -> new LinkedHashSet<>()).add(role.name()));
        }
        Map<Long, List<String>> roleMap = new LinkedHashMap<>();
        roleSets.forEach((userId, roles) -> roleMap.put(userId, List.copyOf(roles)));
        return roleMap;
    }

    private List<String> normalizeRequestedRoles(List<String> requestedRoles) {
        if (requestedRoles == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "roles不能为空");
        }
        List<String> normalizedRoles = RoleTemplateCode.normalizeAll(requestedRoles);
        long distinctInputCount = requestedRoles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .count();
        if (normalizedRoles.size() != distinctInputCount) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "存在不支持的角色编码");
        }
        return normalizedRoles;
    }

    private List<String> validateRequestedRoles(List<String> requestedRoles) {
        List<String> normalizedRoles = normalizeRequestedRoles(requestedRoles);
        if (normalizedRoles.isEmpty() || !normalizedRoles.stream().allMatch(PLATFORM_MANAGED_ROLES::contains)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        boolean hasPlatformRole = containsPlatformRole(normalizedRoles);
        boolean hasOrgAdminRole = normalizedRoles.contains(RoleTemplateCode.ORG_ADMIN.name());
        if (hasPlatformRole && hasOrgAdminRole) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "平台角色和企业管理员角色不能混配");
        }
        return normalizedRoles;
    }

    private Long resolveTargetEnterpriseId(Long requestedEnterpriseId, List<String> requestedRoles) {
        if (containsPlatformRole(requestedRoles)) {
            if (requestedEnterpriseId != null) {
                throw new BusinessException(ApiCode.INVALID_PARAM, "平台角色不能绑定enterpriseId");
            }
            return null;
        }
        if (requestedEnterpriseId == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
        }
        return requireEnterpriseExists(requestedEnterpriseId).getId();
    }

    private Long resolveUpdatedEnterpriseId(UserAccount targetUser, Long requestedEnterpriseId, List<String> currentRoles) {
        if (containsPlatformRole(currentRoles)) {
            if (requestedEnterpriseId != null) {
                throw new BusinessException(ApiCode.INVALID_PARAM, "平台角色不能绑定enterpriseId");
            }
            return null;
        }
        Long effectiveEnterpriseId = requestedEnterpriseId == null ? targetUser.getEnterpriseId() : requestedEnterpriseId;
        if (effectiveEnterpriseId == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
        }
        return requireEnterpriseExists(effectiveEnterpriseId).getId();
    }

    private void assertCanAccessTarget(UserAccount targetUser, List<String> roles) {
        if (!isPlatformManagedUser(roles)) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
    }

    private boolean isPlatformManagedUser(List<String> roles) {
        return roles != null && !roles.isEmpty() && roles.stream().allMatch(PLATFORM_MANAGED_ROLES::contains);
    }

    private void syncScopeRoles(UserAccount user, List<String> normalizedRoles) {
        userScopeRoleRepository.deleteByUserId(user.getId());
        if (normalizedRoles == null || normalizedRoles.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<UserScopeRole> assignments = new ArrayList<>();
        for (String roleCode : normalizedRoles) {
            RoleTemplateCode templateCode = RoleTemplateCode.from(roleCode)
                    .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "存在不支持的角色编码"));
            UserScopeRole assignment = new UserScopeRole();
            assignment.setUserId(user.getId());
            assignment.setRoleCode(templateCode.name());
            assignment.setStatus(ACTIVE_SCOPE_ROLE_STATUS);
            assignment.setCreatedAt(now);
            assignment.setUpdatedAt(now);
            if (templateCode.isPlatformRole()) {
                assignment.setScopeType(ScopeType.PLATFORM.name());
                assignment.setEnterpriseId(null);
            } else {
                assignment.setScopeType(ScopeType.ENTERPRISE.name());
                assignment.setEnterpriseId(user.getEnterpriseId());
            }
            assignments.add(assignment);
        }
        userScopeRoleRepository.saveAll(assignments);
    }

    private void ensureSuperAdminRetained(List<String> beforeRoles, List<String> afterRoles) {
        if (!beforeRoles.contains(RoleTemplateCode.PLATFORM_SUPER_ADMIN.name())
                || afterRoles.contains(RoleTemplateCode.PLATFORM_SUPER_ADMIN.name())) {
            return;
        }
        if (userScopeRoleRepository.countEnabledUsersByRoleCode(
                SubjectType.USER.name(),
                ACTIVE_SCOPE_ROLE_STATUS,
                RoleTemplateCode.PLATFORM_SUPER_ADMIN.name()) <= 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "至少保留一个平台超级管理员");
        }
    }

    private void ensureSuperAdminStatusAllowed(UserAccount user, List<String> roles, Boolean enabled) {
        if (!roles.contains(RoleTemplateCode.PLATFORM_SUPER_ADMIN.name()) || Boolean.TRUE.equals(enabled) || !isEnabled(user)) {
            return;
        }
        if (userScopeRoleRepository.countEnabledUsersByRoleCode(
                SubjectType.USER.name(),
                ACTIVE_SCOPE_ROLE_STATUS,
                RoleTemplateCode.PLATFORM_SUPER_ADMIN.name()) <= 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "至少保留一个平台超级管理员");
        }
    }

    private UserDetailResponseData toDetail(UserAccount user, List<String> roles, String enterpriseName) {
        return new UserDetailResponseData(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEnterpriseId(),
                enterpriseName,
                isEnabled(user),
                roles,
                toOffsetDateTime(user.getCreatedAt()),
                toOffsetDateTime(user.getUpdatedAt()));
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "username不能为空");
        }
        return username.trim();
    }

    private boolean containsPlatformRole(List<String> roles) {
        return roles.stream()
                .map(RoleTemplateCode::from)
                .flatMap(java.util.Optional::stream)
                .anyMatch(RoleTemplateCode::isPlatformRole);
    }

    private Enterprise requireEnterpriseExists(Long enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_PARAM, "企业不存在"));
    }

    private String resolveEnterpriseName(Long enterpriseId) {
        if (enterpriseId == null) {
            return null;
        }
        return enterpriseRepository.findById(enterpriseId).map(Enterprise::getName).orElse(null);
    }

    private Map<String, Object> snapshot(UserAccount user, List<String> roles) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", user.getId());
        snapshot.put("username", user.getUsername());
        snapshot.put("nickname", user.getNickname());
        snapshot.put("enterpriseId", user.getEnterpriseId());
        snapshot.put("enterpriseName", resolveEnterpriseName(user.getEnterpriseId()));
        snapshot.put("enabled", isEnabled(user));
        snapshot.put("roles", roles);
        snapshot.put("scopeRoles", buildScopeRoleSnapshot(user, roles));
        return snapshot;
    }

    private Map<String, Object> buildAuditDetail(UserAccount operatorUser,
                                                 List<String> operatorRoles,
                                                 UserAccount targetUser,
                                                 Map<String, Object> before,
                                                 Map<String, Object> after) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("operatorUserId", operatorUser.getId());
        detail.put("operatorRoles", operatorRoles);
        detail.put("operatorEnterpriseId", operatorUser.getEnterpriseId());
        detail.put("targetType", "USER");
        detail.put("targetId", targetUser.getId());
        detail.put("targetEnterpriseId", targetUser.getEnterpriseId());
        detail.put("before", before);
        detail.put("after", after);
        return detail;
    }

    private List<String> buildScopeRoleSnapshot(UserAccount user, List<String> roles) {
        List<String> snapshots = new ArrayList<>();
        for (String role : roles) {
            RoleTemplateCode templateCode = RoleTemplateCode.from(role).orElse(null);
            if (templateCode == null) {
                continue;
            }
            if (templateCode.isPlatformRole()) {
                snapshots.add(templateCode.name() + "@PLATFORM");
            } else if (user.getEnterpriseId() != null) {
                snapshots.add(templateCode.name() + "@ENTERPRISE(" + user.getEnterpriseId() + ")");
            }
        }
        return snapshots;
    }

    private boolean isEnabled(UserAccount user) {
        return user.getStatus() != null && user.getStatus() == (byte) 1;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        return time == null ? null : time.atOffset(ZoneOffset.UTC);
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

    private void assertPlatformAdmin(AuthenticatedUser operator) {
        businessAccessService.assertPlatformAdmin(operator);
    }
}
