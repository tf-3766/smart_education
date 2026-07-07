package com.zhongruan.edu.biz.shared.security;

import com.zhongruan.edu.common.error.CommonErrorCode;
import com.zhongruan.edu.common.security.JwtClaims;
import com.zhongruan.edu.common.security.JwtTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final SecurityErrorResponseWriter errorWriter;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, SecurityErrorResponseWriter errorWriter) {
        this.jwtTokenService = jwtTokenService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = jwtTokenService.parse(header.substring(BEARER_PREFIX.length()));
            AuthenticatedUser principal = new AuthenticatedUser(
                    claims.userId(), claims.username(), claims.activeRole(), claims.roles(), claims.permissions());
            Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
            claims.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            claims.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.write(request, response, CommonErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            errorWriter.write(request, response, CommonErrorCode.UNAUTHORIZED);
        }
    }
}
