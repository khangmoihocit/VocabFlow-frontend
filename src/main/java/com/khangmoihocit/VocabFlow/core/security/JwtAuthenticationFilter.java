package com.khangmoihocit.VocabFlow.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khangmoihocit.VocabFlow.core.utils.JwtUtil;
import com.khangmoihocit.VocabFlow.modules.auth.dtos.UserDetailsCustom;
import com.khangmoihocit.VocabFlow.modules.auth.services.Impl.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "JWT AUTHENTICATION  FILTER")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    JwtUtil jwtUtil;
    UserDetailsServiceImpl userDetailService;
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    //trước khi vào controller sẽ vào filter này để check token, check đầu -> security config
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("request không có token: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            if (jwtUtil.isTokenExpired(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "token đã hết hạn");
                return;
            }

            if (!jwtUtil.isTokenFormatValid(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "token không đúng định dạng");
                return;
            }

            if (!jwtUtil.isSignatureValid(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "chữ ký token không hợp lệ");
                return;
            }

            if (!jwtUtil.isIssuerToken(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "token có nguồn gốc không hợp lệ");
                return;
            }

            final String userEmail = jwtUtil.extractUsername(jwt);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetailsCustom userDetails = (UserDetailsCustom) userDetailService.loadUserByUsername(userEmail);
                if (!userEmail.equals(userDetails.getUsername())) {
                    sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                            "Xác thực không thành công",
                            "User token không chính xác");
                    return;
                }

                if (!userDetails.isEnabled()) {
                    sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                            "Xác thực không thành công",
                            "Tài khoản của bạn hiện đang bị khóa");
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException ex) {
            sendErrorResponse(response, request,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Network Error!",
                    ex.getMessage());
        }
    }

    private void sendErrorResponse(
            @NotNull HttpServletResponse response,
            @NotNull HttpServletRequest request,
            int statusCode, String error, String message) throws IOException {

        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("Timestamp", System.currentTimeMillis());
        errorResponse.put("status", statusCode);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", request.getRequestURL());

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}