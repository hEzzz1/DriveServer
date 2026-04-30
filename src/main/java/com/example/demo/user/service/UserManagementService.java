package com.example.demo.user.service;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.entity.UserScopeRole;
import com.example.demo.auth.model.RoleCode;
import com.example.demo.auth.model.RoleTemplateCode;
import com.example.demo.auth.model.ScopeType;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.auth.repository.UserScopeRoleRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.service.BusinessAccessService;
import com.example.demo.auth.service.BusinessDataScope;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.dto.ResetUserPasswordRequest;
import com.example.demo.user.dto.RoleItemData;
import com.example.demo.user.dto.UpdateUserRequest;
import com.example.demo.user.dto.UserDetailResponseData;
import com.example.demo.user.dto.UserListItemData;
import com.example.demo.user.dto.UserPageResponseData;
import com.example.demo.system.dto.SystemAuditPageResponseData;
import com.example.demo.system.service.SystemAuditService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ENTERPRISE_ADMIN_ASSIGNABLE_ROLES = Set.of(
            RoleCode.RISK_ADMIN.name(),
            RoleCode.OPERATOR.name(),
            RoleCode.ANALYST.name(),
            RoleCode.VIEWER.name()
    );

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserScopeRoleRepository userScopeRoleRepository;
    private final RoleRepository roleRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemAuditService systemAuditService;
    private final BusinessAccessService businessAccessService;

    public UserManagementService(UserAccountRepository userAccountRepository,
                                 UserRoleRepository userRoleRepository,
                                 UserScopeRoleRepository userScopeRoleRepository,
                                 RoleRepository roleRepository,
                                 EnterpriseRepository enterpriseRepository,
                                 PasswordEncoder passwordEncoder,
                                 SystemAuditService systemAuditService,
                                 BusinessAccessService businessAccessService) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.userScopeRoleRepository = userScopeRoleRepository;
        this.roleRepository = roleRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemAuditService = systemAuditService;
        this.businessAccessService = businessAccessService;
    }

    @Transactional
    public UserDetailResponseData createUser(AuthenticatedUser operator, CreateUserRequest request) {
        UserAccount currentUser = getOperatorAccount(operator);
        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "用户名已存在");
        }

        List<String> normalizedRoles = validateRequestedRoles(operator, currentUser, request.roles());
        Long enterpriseId = resolveTargetEnterpriseId(operator, currentUser, request.enterpriseId(), normalizedRoles);
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

        assignRoles(saved.getId(), normalizedRoles);
        syncScopeRoles(saved, normalizedRoles);
        systemAuditService.record(operator, "USER", "CREATE_USER", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "创建用户", buildAuditDetail(currentUser, operator.getRoles(), saved, null, snapshot(saved, normalizedRoles)));
        return toDetail(saved, normalizedRoles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData updateUser(AuthenticatedUser operator, Long userId, UpdateUserRequest request) {
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        assertCanAccessTarget(operator, currentUser, user);
        Map<String, Object> before = snapshot(user, loadRoleMap(List.of(userId)).getOrDefault(userId, List.of()));

        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "用户名已存在");
        }

        List<String> currentRoles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        Long enterpriseId = resolveUpdatedEnterpriseId(operator, currentUser, user, request.enterpriseId(), currentRoles);
        user.setUsername(username);
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username);
        user.setEnterpriseId(enterpriseId);
        UserAccount saved = userAccountRepository.save(user);
        syncScopeRoles(saved, currentRoles);

        systemAuditService.record(operator, "USER", "UPDATE_USER_PROFILE", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "更新用户信息", buildAuditDetail(currentUser, operator.getRoles(), saved, before, snapshot(saved, currentRoles)));
        return toDetail(saved, currentRoles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional(readOnly = true)
    public UserPageResponseData listUsers(AuthenticatedUser operator,
                                          Integer page,
                                          Integer size,
                                          String keyword,
                                          Boolean enabled,
                                          Long enterpriseId) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Specification<UserAccount> specification = buildUserSpecification(
                keyword,
                enabled,
                resolveUserEnterpriseScope(operator, enterpriseId));
        Page<UserAccount> result = userAccountRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));

        Map<Long, List<String>> roleMap = loadRoleMap(result.getContent().stream().map(UserAccount::getId).toList());
        List<UserListItemData> items = result.getContent().stream()
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
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        assertCanAccessTarget(operator, currentUser, user);
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        return toDetail(user, roles, resolveEnterpriseName(user.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData updateRoles(AuthenticatedUser operator, Long userId, List<String> requestedRoles) {
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        assertCanAccessTarget(operator, currentUser, user);
        List<String> beforeRoles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        Map<String, Object> before = snapshot(user, beforeRoles);
        List<String> normalizedRoles = validateRequestedRoles(operator, currentUser, requestedRoles);
        ensureSuperAdminRetained(user, beforeRoles, normalizedRoles);
        assignRoles(userId, normalizedRoles);
        syncScopeRoles(user, normalizedRoles);
        systemAuditService.record(operator, "USER", "UPDATE_USER_ROLES", "USER", String.valueOf(user.getId()),
                "SUCCESS", "更新用户角色", buildAuditDetail(currentUser, operator.getRoles(), user, before, snapshot(user, normalizedRoles)));
        return toDetail(user, normalizedRoles, resolveEnterpriseName(user.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData updateStatus(AuthenticatedUser operator, Long userId, Boolean enabled) {
        if (enabled == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enabled不能为空");
        }
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        assertCanAccessTarget(operator, currentUser, user);
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        Map<String, Object> before = snapshot(user, roles);
        ensureSuperAdminStatusAllowed(user, roles, enabled);
        user.setStatus(Boolean.TRUE.equals(enabled) ? (byte) 1 : (byte) 0);
        UserAccount saved = userAccountRepository.save(user);
        systemAuditService.record(operator, "USER", "UPDATE_USER_STATUS", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "更新用户状态", buildAuditDetail(currentUser, operator.getRoles(), saved, before, snapshot(saved, roles)));
        return toDetail(saved, roles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional
    public UserDetailResponseData resetPassword(AuthenticatedUser operator, Long userId, ResetUserPasswordRequest request) {
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount user = getUserAccount(userId);
        assertCanAccessTarget(operator, currentUser, user);
        if (!StringUtils.hasText(request.newPassword())) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "newPassword不能为空");
        }
        List<String> roles = loadRoleMap(List.of(userId)).getOrDefault(userId, List.of());
        Map<String, Object> before = snapshot(user, roles);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword().trim()));
        UserAccount saved = userAccountRepository.save(user);
        Map<String, Object> after = snapshot(saved, roles);
        after.put("passwordReset", true);
        systemAuditService.record(operator, "USER", "RESET_USER_PASSWORD", "USER", String.valueOf(saved.getId()),
                "SUCCESS", "重置用户密码", buildAuditDetail(currentUser, operator.getRoles(), saved, before, after));
        return toDetail(saved, roles, resolveEnterpriseName(saved.getEnterpriseId()));
    }

    @Transactional(readOnly = true)
    public SystemAuditPageResponseData listUserAudits(AuthenticatedUser operator, Long userId, Integer page, Integer size) {
        UserAccount currentUser = getOperatorAccount(operator);
        UserAccount targetUser = getUserAccount(userId);
        assertCanAccessTarget(operator, currentUser, targetUser);
        return systemAuditService.list("USER", null, "USER", String.valueOf(userId), null, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public List<RoleItemData> listRoles(AuthenticatedUser operator) {
        return roleRepository.findAllByOrderByRoleCodeAsc().stream()
                .filter(role -> RoleCode.from(role.getRoleCode()).isPresent())
                .filter(role -> canAssignRole(operator, role.getRoleCode()))
                .map(role -> new RoleItemData(role.getId(), role.getRoleCode(), role.getRoleName()))
                .toList();
    }

    private UserAccount getUserAccount(Long userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage()));
        if (!SubjectType.USER.name().equals(user.getSubjectType())) {
            throw new BusinessException(ApiCode.NOT_FOUND, ApiCode.NOT_FOUND.getMessage());
        }
        return user;
    }

    private Specification<UserAccount> buildUserSpecification(String keyword, Boolean enabled, BusinessDataScope dataScope) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subjectType"), SubjectType.USER.name()));
            predicates.add(dataScope.toPredicate(root, cb, "enterpriseId", null));
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

    private Map<Long, List<String>> loadRoleMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> roleCodeById = roleRepository.findAllByOrderByRoleCodeAsc().stream()
                .collect(Collectors.toMap(Role::getId, Role::getRoleCode));

        Map<Long, List<String>> roleMap = new LinkedHashMap<>();
        for (UserRole assignment : userRoleRepository.findByUserIdIn(userIds)) {
            roleMap.computeIfAbsent(assignment.getUserId(), key -> new ArrayList<>())
                    .add(roleCodeById.get(assignment.getRoleId()));
        }
        roleMap.replaceAll((key, value) -> RoleCode.normalizeAll(value));
        return roleMap;
    }

    private List<String> normalizeRequestedRoles(List<String> requestedRoles) {
        if (requestedRoles == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "roles不能为空");
        }
        List<String> normalizedRoles = RoleCode.normalizeAll(requestedRoles);
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

    private UserAccount getOperatorAccount(AuthenticatedUser operator) {
        return getUserAccount(operator.getUserId());
    }

    private BusinessDataScope resolveUserEnterpriseScope(AuthenticatedUser operator, Long requestedEnterpriseId) {
        BusinessDataScope scope = businessAccessService.getAuthorizationProfile(operator).dataScope();
        if (scope.platformWide()) {
            return requestedEnterpriseId == null
                    ? scope
                    : new BusinessDataScope(false, Set.of(requestedEnterpriseId), Map.of());
        }
        if (scope.enterpriseIds().isEmpty()) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        if (requestedEnterpriseId != null) {
            if (scope.enterpriseIds().contains(requestedEnterpriseId)) {
                return new BusinessDataScope(false, Set.of(requestedEnterpriseId), Map.of());
            }
            return new BusinessDataScope(false, scope.enterpriseIds(), Map.of());
        }
        return new BusinessDataScope(false, scope.enterpriseIds(), Map.of());
    }

    private Long resolveTargetEnterpriseId(AuthenticatedUser operator,
                                           UserAccount currentUser,
                                           Long requestedEnterpriseId,
                                           List<String> requestedRoles) {
        if (isSuperAdmin(operator)) {
            if (containsPlatformRole(requestedRoles)) {
                return requestedEnterpriseId == null ? null : requireEnterpriseExists(requestedEnterpriseId).getId();
            }
            if (requestedEnterpriseId == null) {
                Set<Long> manageableEnterpriseIds = resolveManageableEnterpriseIds(operator);
                if (manageableEnterpriseIds.size() != 1) {
                    throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
                }
                return manageableEnterpriseIds.iterator().next();
            }
            return requireEnterpriseExists(requestedEnterpriseId).getId();
        }
        if (requestedEnterpriseId != null) {
            businessAccessService.assertCanManageEnterprise(operator, requestedEnterpriseId);
            return requestedEnterpriseId;
        }
        Set<Long> manageableEnterpriseIds = resolveManageableEnterpriseIds(operator);
        if (manageableEnterpriseIds.size() != 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
        }
        return manageableEnterpriseIds.iterator().next();
    }

    private Long resolveUpdatedEnterpriseId(AuthenticatedUser operator,
                                            UserAccount currentUser,
                                            UserAccount targetUser,
                                            Long requestedEnterpriseId,
                                            List<String> currentRoles) {
        if (!isSuperAdmin(operator)) {
            if (requestedEnterpriseId != null && !requestedEnterpriseId.equals(targetUser.getEnterpriseId())) {
                throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
            }
            return targetUser.getEnterpriseId();
        }
        if (containsPlatformRole(currentRoles)) {
            return requestedEnterpriseId == null ? null : requireEnterpriseExists(requestedEnterpriseId).getId();
        }
        if (requestedEnterpriseId == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enterpriseId不能为空");
        }
        return requireEnterpriseExists(requestedEnterpriseId).getId();
    }

    private List<String> validateRequestedRoles(AuthenticatedUser operator, UserAccount currentUser, List<String> requestedRoles) {
        List<String> normalizedRoles = normalizeRequestedRoles(requestedRoles);
        if (!normalizedRoles.stream().allMatch(role -> canAssignRole(operator, role))) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        if (!isSuperAdmin(operator) && resolveManageableEnterpriseIds(operator).isEmpty()) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return normalizedRoles;
    }

    private void assertCanAccessTarget(AuthenticatedUser operator, UserAccount currentUser, UserAccount targetUser) {
        if (isSuperAdmin(operator)) {
            return;
        }
        if (targetUser.getEnterpriseId() == null) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        businessAccessService.assertCanManageEnterprise(operator, targetUser.getEnterpriseId());
    }

    private void assignRoles(Long userId, List<String> normalizedRoles) {
        Map<String, Role> rolesByCode = roleRepository.findByRoleCodeIn(normalizedRoles).stream()
                .collect(Collectors.toMap(Role::getRoleCode, Function.identity()));
        if (rolesByCode.size() != normalizedRoles.size()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "存在不支持的角色编码");
        }

        userRoleRepository.deleteByUserId(userId);
        if (normalizedRoles.isEmpty()) {
            return;
        }

        List<UserRole> assignments = new ArrayList<>();
        for (String roleCode : normalizedRoles) {
            UserRole assignment = new UserRole();
            assignment.setUserId(userId);
            assignment.setRoleId(rolesByCode.get(roleCode).getId());
            assignment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            assignments.add(assignment);
        }
        userRoleRepository.saveAll(assignments);
    }

    private void syncScopeRoles(UserAccount user, List<String> normalizedRoles) {
        userScopeRoleRepository.deleteByUserId(user.getId());
        if (normalizedRoles == null || normalizedRoles.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        List<UserScopeRole> assignments = new ArrayList<>();
        for (String roleCode : normalizedRoles) {
            RoleTemplateCode templateCode = toTemplateCode(roleCode);
            if (templateCode == null) {
                continue;
            }
            UserScopeRole assignment = new UserScopeRole();
            assignment.setUserId(user.getId());
            assignment.setRoleCode(templateCode.name());
            assignment.setStatus((byte) 1);
            assignment.setCreatedAt(now);
            assignment.setUpdatedAt(now);
            if (templateCode.isPlatformRole()) {
                assignment.setScopeType(ScopeType.PLATFORM.name());
            } else {
                if (user.getEnterpriseId() == null) {
                    continue;
                }
                assignment.setScopeType(ScopeType.ENTERPRISE.name());
                assignment.setEnterpriseId(user.getEnterpriseId());
            }
            assignments.add(assignment);
        }
        userScopeRoleRepository.saveAll(assignments);
    }

    private void ensureSuperAdminRetained(UserAccount user, List<String> beforeRoles, List<String> afterRoles) {
        if (!beforeRoles.contains(RoleCode.SUPER_ADMIN.name()) || afterRoles.contains(RoleCode.SUPER_ADMIN.name())) {
            return;
        }
        if (userAccountRepository.countEnabledUsersByRoleCode(SubjectType.USER.name(), RoleCode.SUPER_ADMIN.name()) <= 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "至少保留一个SUPER_ADMIN");
        }
    }

    private void ensureSuperAdminStatusAllowed(UserAccount user, List<String> roles, Boolean enabled) {
        if (!roles.contains(RoleCode.SUPER_ADMIN.name()) || Boolean.TRUE.equals(enabled) || !isEnabled(user)) {
            return;
        }
        if (userAccountRepository.countEnabledUsersByRoleCode(SubjectType.USER.name(), RoleCode.SUPER_ADMIN.name()) <= 1) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "至少保留一个SUPER_ADMIN");
        }
    }

    private boolean canAssignRole(AuthenticatedUser operator, String roleCode) {
        return isSuperAdmin(operator) || ENTERPRISE_ADMIN_ASSIGNABLE_ROLES.contains(roleCode);
    }

    private boolean isSuperAdmin(AuthenticatedUser operator) {
        return operator.getRoles().contains(RoleCode.SUPER_ADMIN.name());
    }

    private Long requireEnterpriseId(UserAccount user) {
        if (user.getEnterpriseId() == null) {
            throw new BusinessException(ApiCode.FORBIDDEN, "无权限访问");
        }
        return user.getEnterpriseId();
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "username不能为空");
        }
        return username.trim();
    }

    private boolean containsPlatformRole(List<String> roles) {
        return roles.contains(RoleCode.SUPER_ADMIN.name())
                || roles.contains(RoleCode.SYS_ADMIN.name())
                || roles.contains(RoleCode.RISK_ADMIN.name());
    }

    private Set<Long> resolveManageableEnterpriseIds(AuthenticatedUser operator) {
        return businessAccessService.getAuthorizationProfile(operator).dataScope().enterpriseIds();
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

    private boolean isEnabled(UserAccount user) {
        return user.getStatus() != null && user.getStatus() == (byte) 1;
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime time) {
        return time == null ? null : time.atOffset(ZoneOffset.UTC);
    }

    private List<String> buildScopeRoleSnapshot(UserAccount user, List<String> roles) {
        List<String> snapshots = new ArrayList<>();
        for (String role : roles) {
            RoleTemplateCode templateCode = toTemplateCode(role);
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

    private RoleTemplateCode toTemplateCode(String roleCode) {
        RoleCode normalized = RoleCode.from(roleCode).orElse(null);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case SUPER_ADMIN -> RoleTemplateCode.PLATFORM_SUPER_ADMIN;
            case SYS_ADMIN -> RoleTemplateCode.PLATFORM_SYS_ADMIN;
            case RISK_ADMIN -> RoleTemplateCode.PLATFORM_RISK_ADMIN;
            case ENTERPRISE_ADMIN -> RoleTemplateCode.ORG_ADMIN;
            case OPERATOR -> RoleTemplateCode.ORG_OPERATOR;
            case ANALYST -> RoleTemplateCode.ORG_ANALYST;
            case VIEWER -> RoleTemplateCode.ORG_VIEWER;
        };
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
