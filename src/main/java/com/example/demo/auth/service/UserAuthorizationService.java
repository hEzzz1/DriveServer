package com.example.demo.auth.service;

import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserScopeRole;
import com.example.demo.auth.model.RoleTemplateCode;
import com.example.demo.auth.model.ScopeType;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserScopeRoleRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserAuthorizationService {

    private static final byte ACTIVE_STATUS = 1;

    private final UserAccountRepository userAccountRepository;
    private final UserScopeRoleRepository userScopeRoleRepository;

    public UserAuthorizationService(UserAccountRepository userAccountRepository,
                                    UserScopeRoleRepository userScopeRoleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userScopeRoleRepository = userScopeRoleRepository;
    }

    public UserAuthorizationProfile loadProfile(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getSubjectType() != SubjectType.USER) {
            return emptyProfile();
        }
        return userAccountRepository.findById(authenticatedUser.getUserId())
                .map(this::loadProfile)
                .orElseGet(this::emptyProfile);
    }

    public UserAuthorizationProfile loadProfile(UserAccount user) {
        if (user == null || !SubjectType.USER.name().equals(user.getSubjectType())) {
            return emptyProfile();
        }

        List<ResolvedAssignment> resolvedAssignments = resolveExplicitAssignments(user.getId());
        List<String> roles = RoleTemplateCode.normalizeAll(resolvedAssignments.stream()
                .map(assignment -> assignment.roleTemplate().name())
                .toList());

        List<String> platformRoles = RoleTemplateCode.normalizeAll(resolvedAssignments.stream()
                .filter(assignment -> assignment.roleTemplate().isPlatformRole())
                .map(assignment -> assignment.roleTemplate().name())
                .toList());

        List<AccessMembership> memberships = resolvedAssignments.stream()
                .filter(assignment -> !assignment.roleTemplate().isPlatformRole())
                .map(assignment -> new AccessMembership(
                        assignment.roleTemplate().name(),
                        assignment.scopeType(),
                        assignment.enterpriseId(),
                        assignment.fleetId()))
                .toList();

        Set<String> permissionSet = new LinkedHashSet<>();
        for (ResolvedAssignment assignment : resolvedAssignments) {
            permissionSet.addAll(assignment.roleTemplate().defaultPermissions());
        }

        return new UserAuthorizationProfile(
                roles,
                platformRoles,
                memberships,
                com.example.demo.auth.model.PermissionCode.sortCodes(permissionSet),
                resolveDefaultScope(platformRoles, memberships),
                resolveDataScope(resolvedAssignments));
    }

    private List<ResolvedAssignment> resolveExplicitAssignments(Long userId) {
        Map<String, ResolvedAssignment> deduplicated = new LinkedHashMap<>();
        for (UserScopeRole assignment : userScopeRoleRepository.findByUserIdAndStatusOrderByIdAsc(userId, ACTIVE_STATUS)) {
            toResolvedAssignment(assignment).ifPresent(resolved ->
                    deduplicated.putIfAbsent(keyOf(resolved), resolved));
        }
        return List.copyOf(deduplicated.values());
    }

    private Optional<ResolvedAssignment> toResolvedAssignment(UserScopeRole assignment) {
        RoleTemplateCode roleTemplate = RoleTemplateCode.from(assignment.getRoleCode()).orElse(null);
        ScopeType scopeType = ScopeType.from(assignment.getScopeType()).orElse(null);
        if (roleTemplate == null || scopeType == null || !roleTemplate.supportsScope(scopeType)) {
            return Optional.empty();
        }
        if (scopeType == ScopeType.PLATFORM) {
            return Optional.of(new ResolvedAssignment(roleTemplate, scopeType, null, null));
        }
        if (scopeType == ScopeType.ENTERPRISE) {
            if (assignment.getEnterpriseId() == null || assignment.getFleetId() != null) {
                return Optional.empty();
            }
            return Optional.of(new ResolvedAssignment(roleTemplate, scopeType, assignment.getEnterpriseId(), null));
        }
        if (assignment.getEnterpriseId() == null || assignment.getFleetId() == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedAssignment(roleTemplate, scopeType, assignment.getEnterpriseId(), assignment.getFleetId()));
    }

    private AccessScopeData resolveDefaultScope(List<String> platformRoles, List<AccessMembership> memberships) {
        for (AccessMembership membership : memberships) {
            if (membership.scopeType() == ScopeType.ENTERPRISE) {
                return new AccessScopeData(membership.scopeType(), membership.enterpriseId(), membership.fleetId());
            }
        }
        for (AccessMembership membership : memberships) {
            if (membership.scopeType() == ScopeType.FLEET) {
                return new AccessScopeData(membership.scopeType(), membership.enterpriseId(), membership.fleetId());
            }
        }
        if (!platformRoles.isEmpty()) {
            return new AccessScopeData(ScopeType.PLATFORM, null, null);
        }
        return null;
    }

    private BusinessDataScope resolveDataScope(List<ResolvedAssignment> assignments) {
        if (assignments.stream().anyMatch(assignment -> assignment.scopeType() == ScopeType.PLATFORM)) {
            return BusinessDataScope.globalScope();
        }

        Set<Long> enterpriseIds = new LinkedHashSet<>();
        Map<Long, Long> fleetEnterpriseIds = new LinkedHashMap<>();
        for (ResolvedAssignment assignment : assignments) {
            if (assignment.scopeType() == ScopeType.ENTERPRISE && assignment.enterpriseId() != null) {
                enterpriseIds.add(assignment.enterpriseId());
            }
            if (assignment.scopeType() == ScopeType.FLEET
                    && assignment.enterpriseId() != null
                    && assignment.fleetId() != null) {
                fleetEnterpriseIds.putIfAbsent(assignment.fleetId(), assignment.enterpriseId());
            }
        }
        if (enterpriseIds.isEmpty() && fleetEnterpriseIds.isEmpty()) {
            return BusinessDataScope.empty();
        }
        return new BusinessDataScope(false, enterpriseIds, fleetEnterpriseIds);
    }

    private UserAuthorizationProfile emptyProfile() {
        return new UserAuthorizationProfile(List.of(), List.of(), List.of(), List.of(), null, BusinessDataScope.empty());
    }

    private String keyOf(ResolvedAssignment assignment) {
        return assignment.roleTemplate().name() + "|" + assignment.scopeType().name() + "|" + assignment.enterpriseId() + "|" + assignment.fleetId();
    }

    private record ResolvedAssignment(
            RoleTemplateCode roleTemplate,
            ScopeType scopeType,
            Long enterpriseId,
            Long fleetId
    ) {
    }
}
