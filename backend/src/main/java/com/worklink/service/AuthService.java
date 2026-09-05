package com.worklink.service;

import com.worklink.dto.auth.AuthUserResponse;
import com.worklink.dto.auth.LoginRequest;
import com.worklink.dto.auth.LoginResponse;
import com.worklink.dto.auth.RegisterRequest;
import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.exception.EmailAlreadyRegisteredException;
import com.worklink.exception.InvalidCredentialsException;
import com.worklink.exception.InvalidRegistrationRoleException;
import com.worklink.repository.UserRepository;
import com.worklink.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthUserResponse register(RegisterRequest request) {
        Role role = request.role();
        if (role != Role.CLIENT && role != Role.FREELANCER) {
            throw new InvalidRegistrationRoleException();
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role,
                AccountStatus.ACTIVE
        );
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (user.getAccountStatus() != AccountStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return new LoginResponse(jwtService.generateAccessToken(user), "Bearer", user.getId(), user.getFirstName(),
                user.getLastName(), user.getEmail(), user.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthUserResponse toResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getRole());
    }
}
