package com.khangmoihocit.VocabFlow.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khangmoihocit.VocabFlow.modules.user.services.Impl.UserDetailsServiceImpl;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "JWT AUTHENTICATION  FILTER")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    JwtService jwtService;
    UserDetailsServiceImpl userDetailService;

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

            if (!jwtService.isTokenFormatValid(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "token không đúng định dạng");
                return;
            }

            if (!jwtService.isSignatureValid(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "chữ ký token không hợp lệ");
                return;
            }

            if (!jwtService.isIssuerToken(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "token có nguồn gốc không hợp lệ");
                return;
            }

            if (jwtService.isTokenExpired(jwt)) {
                sendErrorResponse(response, request, HttpServletResponse.SC_UNAUTHORIZED,
                        "Xác thực không thành công",
                        "token đã hết hạn");
                return;
            }

            final String userEmail = jwtService.extractUsername(jwt);
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
        } catch (Exception ex) {
            log.error("JWT error: ", ex);

            sendErrorResponse(response, request,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Xác thực không thành công",
                    ex.getMessage());
        }
    }

    //401 Unauthorized	Chưa xác thực hoặc token không hợp lệ / hết hạn
    //403 Forbidden	Đã xác thực thành công, nhưng không có quyền truy cập

    private void sendErrorResponse(
            @NotNull HttpServletResponse response,
            @NotNull HttpServletRequest request,
            int statusCode, String error, String message) throws IOException {

        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("status", statusCode);
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("path", request.getRequestURL());

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}