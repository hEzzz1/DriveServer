package com.example.demo.auth.service;

import com.example.demo.auth.dto.LoginResponseData;
import com.example.demo.auth.dto.CurrentUserMembershipData;
import com.example.demo.auth.dto.CurrentUserScopeData;
import com.example.demo.auth.dto.CurrentUserResponseData;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.JwtTokenResult;
import com.example.demo.auth.security.JwtTokenService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final UserAuthorizationService userAuthorizationService;

    public AuthService(UserAccountRepository userAccountRepository,
                       EnterpriseRepository enterpriseRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       UserAuthorizationService userAuthorizationService) {
        this.userAccountRepository = userAccountRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.userAuthorizationService = userAuthorizationService;
    }

    public LoginResponseData login(String username, String password) {
        String normalizedUsername = username == null ? null : username.trim();
        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(password)) {
            throw new BusinessException(ApiCode.INVALID_PARAM, ApiCode.INVALID_PARAM.getMessage());
        }

        UserAccount user = userAccountRepository.findByUsernameAndSubjectType(normalizedUsername, SubjectType.USER.name())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, "用户名或密码错误"));

        if (user.getStatus() == null || user.getStatus() == (byte) 0) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "用户名或密码错误");
        }

        UserAuthorizationProfile profile = userAuthorizationService.loadProfile(user);
        if (profile.permissions().isEmpty()) {
            throw new BusinessException(ApiCode.FORBIDDEN, "账号未分配角色");
        }

        JwtTokenResult tokenResult = jwtTokenService.issueToken(user.getId(), user.getUsername(), profile.roles());
        return new LoginResponseData(tokenResult.token(), tokenResult.expireAt(), profile.roles());
    }

    public CurrentUserResponseData getCurrentUser(AuthenticatedUser authenticatedUser) {
        UserAccount user = userAccountRepository.findById(authenticatedUser.getUserId())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
        UserAuthorizationProfile profile = userAuthorizationService.loadProfile(user);
        Long enterpriseId = user.getEnterpriseId();
        if (enterpriseId == null && profile.defaultScope() != null && profile.defaultScope().scopeType() != null) {
            enterpriseId = profile.defaultScope().enterpriseId();
        }
        Enterprise enterprise = enterpriseId == null ? null : enterpriseRepository.findById(enterpriseId).orElse(null);
        return new CurrentUserResponseData(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                profile.roles(),
                profile.platformRoles(),
                profile.memberships().stream()
                        .map(membership -> new CurrentUserMembershipData(
                                membership.role(),
                                membership.scopeType().name(),
                                membership.enterpriseId(),
                                membership.fleetId()))
                        .toList(),
                profile.permissions(),
                profile.defaultScope() == null ? null : new CurrentUserScopeData(
                        profile.defaultScope().scopeType().name(),
                        profile.defaultScope().enterpriseId(),
                        profile.defaultScope().fleetId()),
                enterpriseId,
                enterprise == null ? null : enterprise.getName(),
                user.getSubjectType(),
                user.getStatus() != null && user.getStatus() == (byte) 1);
    }
}
