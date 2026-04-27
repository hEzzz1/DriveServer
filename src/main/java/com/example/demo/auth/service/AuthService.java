package com.example.demo.auth.service;

import com.example.demo.auth.dto.LoginResponseData;
import com.example.demo.auth.entity.UserAccount;
import com.example.demo.auth.model.RoleCode;
import com.example.demo.auth.model.SubjectType;
import com.example.demo.auth.repository.RoleRepository;
import com.example.demo.auth.repository.UserAccountRepository;
import com.example.demo.auth.security.AuthenticatedUser;
import com.example.demo.auth.security.JwtTokenResult;
import com.example.demo.auth.security.JwtTokenService;
import com.example.demo.common.api.ApiCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.enterprise.entity.Enterprise;
import com.example.demo.enterprise.repository.EnterpriseRepository;
import com.example.demo.auth.dto.CurrentUserResponseData;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserAccountRepository userAccountRepository,
                       RoleRepository roleRepository,
                       EnterpriseRepository enterpriseRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
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

        List<String> roles = RoleCode.normalizeAll(roleRepository.findRoleCodesByUserId(user.getId()));
        if (roles.isEmpty()) {
            throw new BusinessException(ApiCode.FORBIDDEN, "账号未分配角色");
        }

        JwtTokenResult tokenResult = jwtTokenService.issueToken(user.getId(), user.getUsername(), roles);
        return new LoginResponseData(tokenResult.token(), tokenResult.expireAt(), roles);
    }

    public CurrentUserResponseData getCurrentUser(AuthenticatedUser authenticatedUser) {
        UserAccount user = userAccountRepository.findById(authenticatedUser.getUserId())
                .orElseThrow(() -> new BusinessException(ApiCode.UNAUTHORIZED, ApiCode.UNAUTHORIZED.getMessage()));
        Enterprise enterprise = user.getEnterpriseId() == null ? null : enterpriseRepository.findById(user.getEnterpriseId()).orElse(null);
        return new CurrentUserResponseData(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                authenticatedUser.getRoles(),
                user.getEnterpriseId(),
                enterprise == null ? null : enterprise.getName(),
                user.getSubjectType(),
                user.getStatus() != null && user.getStatus() == (byte) 1);
    }
}
