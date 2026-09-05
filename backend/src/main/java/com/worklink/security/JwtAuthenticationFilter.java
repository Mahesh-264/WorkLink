package com.worklink.security;

import com.worklink.entity.User;
import com.worklink.entity.enums.AccountStatus;
import com.worklink.entity.enums.Role;
import com.worklink.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository,
                                   RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!header.startsWith("Bearer ")) {
            authenticationEntryPoint.write(response, request, "Invalid access token");
            return;
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            authenticationEntryPoint.write(response, request, "Invalid access token");
            return;
        }

        try {
            String email = jwtService.extractEmail(token);
            Role tokenRole = jwtService.extractRole(token);
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isEmpty() || user.get().getAccountStatus() != AccountStatus.ACTIVE) {
                authenticationEntryPoint.write(response, request, "Account is not available");
                return;
            }
            if (user.get().getRole() != tokenRole) {
                authenticationEntryPoint.write(response, request, "Invalid access token");
                return;
            }

            AuthenticatedUser principal = new AuthenticatedUser(user.get().getId(), user.get().getEmail(), user.get().getRole());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.get().getRole().name())));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (ExpiredJwtException exception) {
            authenticationEntryPoint.write(response, request, "Access token has expired");
            return;
        } catch (JwtException | IllegalArgumentException exception) {
            authenticationEntryPoint.write(response, request, "Invalid access token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/health") || path.startsWith("/api/auth/") || path.startsWith("/actuator/");
    }
}
