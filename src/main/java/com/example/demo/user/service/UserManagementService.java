package com.example.demo.user.service;

import com.example.demo.auth.entity.Role;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.entity.UserRole;
import com.example.demo.auth.model.RoleCode;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.repository.UserRoleRepository;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.user.dto.RoleItemData;
import com.example.demo.user.dto.UserDetailResponseData;
import com.example.demo.user.dto.UserListItemData;
import com.example.demo.user.dto.UserPageResponseData;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public UserManagementService(UserAccountRepository userAccountRepository,
                                 UserRoleRepository userRoleRepository,
                                 RoleRepository roleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public UserPageResponseData listUsers(Integer page, Integer size, String keyword, Boolean enabled) {
        int pageNo = normalizePage(page);
        int pageSize = normalizeSize(size);
        Specification<UserAccount> specification = buildUserSpecification(keyword, enabled);
        Page<UserAccount> result = userAccountRepository.findAll(
                specification,
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.ASC, "id")));

        Map<Long, List<String>> roleMap = loadRoleMap(result.getContent().stream().map(UserAccount::getId).toList());
        List<UserListItemData> items = result.getContent().stream()
                .map(user -> new UserListItemData(
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        isEnabled(user),
                        roleMap.getOrDefault(user.getId(), List.of()),
                        toOffsetDateTime(user.getCreatedAt()),
                        toOffsetDateTime(user.getUpdatedAt())))
                .toList();
        return new UserPageResponseData(result.getTotalElements(), pageNo, pageSize, items);
    }

    @Transactional(readOnly = true)
    public UserDetailResponseData getUser(Long userId) {
        UserAccount user = getUserAccount(userId);
        return toDetail(user, loadRoleMap(List.of(userId)).getOrDefault(userId, List.of()));
    }

    @Transactional
    public UserDetailResponseData updateRoles(Long userId, List<String> requestedRoles) {
        UserAccount user = getUserAccount(userId);
        List<String> normalizedRoles = normalizeRequestedRoles(requestedRoles);
        Map<String, Role> rolesByCode = roleRepository.findByRoleCodeIn(normalizedRoles).stream()
                .collect(Collectors.toMap(Role::getRoleCode, Function.identity()));
        if (rolesByCode.size() != normalizedRoles.size()) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "存在不支持的角色编码");
        }

        userRoleRepository.deleteByUserId(userId);
        if (!normalizedRoles.isEmpty()) {
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
        return toDetail(user, normalizedRoles);
    }

    @Transactional
    public UserDetailResponseData updateStatus(Long userId, Boolean enabled) {
        if (enabled == null) {
            throw new BusinessException(ApiCode.INVALID_PARAM, "enabled不能为空");
        }
        UserAccount user = getUserAccount(userId);
        user.setStatus(Boolean.TRUE.equals(enabled) ? (byte) 1 : (byte) 0);
        UserAccount saved = userAccountRepository.save(user);
        return toDetail(saved, loadRoleMap(List.of(userId)).getOrDefault(userId, List.of()));
    }

    @Transactional(readOnly = true)
    public List<RoleItemData> listRoles() {
        return roleRepository.findAllByOrderByRoleCodeAsc().stream()
                .filter(role -> RoleCode.from(role.getRoleCode()).isPresent())
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

    private Specification<UserAccount> buildUserSpecification(String keyword, Boolean enabled) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("subjectType"), SubjectType.USER.name()));
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

    private UserDetailResponseData toDetail(UserAccount user, List<String> roles) {
        return new UserDetailResponseData(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                isEnabled(user),
                roles,
                toOffsetDateTime(user.getCreatedAt()),
                toOffsetDateTime(user.getUpdatedAt()));
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
}
