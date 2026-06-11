package com.project.security.jwt;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.response.ErrorResponse;
import com.project.security.user.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

import io.jsonwebtoken.JwtException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwt;
    private final CustomUserDetailsService users;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(7);
        try {
            jwt.valid(token);
            var details = users.loadUserById(jwt.userId(token));
            if (!details.isEnabled())
                throw new UsernameNotFoundException("User is disabled");
            var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } catch (JwtException | IllegalArgumentException
                | org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            write(res, req, HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }

    private void write(HttpServletResponse res, HttpServletRequest req, HttpStatus status, String message)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getWriter(), new ErrorResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), message, req.getRequestURI()));
    }
}
