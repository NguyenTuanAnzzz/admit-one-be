package com.an.admit_one.security;

import com.an.admit_one.service.CustomerUserDetailsService;
import com.an.admit_one.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomerUserDetailsService userDetailsService;

    public JwtFilter(
            JwtService jwtService,
            CustomerUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
            throws ServletException {

        String path = request.getServletPath();

        return path.equals("/api/auth/refresh");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy Authorization header
        String authHeader = request.getHeader("Authorization");

        // Không có access token → cho request đi tiếp
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 2. Lấy JWT
        String token = authHeader.substring(7);

        try {
            // 3. Lấy email từ JWT
            String email = jwtService.extractUsername(token);

            // 4. Kiểm tra token type
            String tokenType = jwtService.extractTokenType(token);

            // Chỉ ACCESS token được xác thực tại filter này
            if (!"ACCESS".equals(tokenType)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 5. Chưa có Authentication trong SecurityContext
            if (email != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                // 6. Lấy user từ database
                UserPrincipal userPrincipal =
                        (UserPrincipal) userDetailsService
                                .loadUserByUsername(email);

                if (jwtService.isTokenValid(
                        token,
                        userPrincipal
                )) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal,
                                    null,
                                    userPrincipal.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}