package com.khangmoihocit.VocabFlow.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khangmoihocit.VocabFlow.core.exception.ValidTokenException;
import com.khangmoihocit.VocabFlow.modules.user.services.Impl.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import io.jsonwebtoken.security.SignatureException;

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
            final String userEmail = jwtService.extractUsername(jwt);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetailsCustom userDetails = (UserDetailsCustom) userDetailService.loadUserByUsername(userEmail);
                if (!userEmail.equals(userDetails.getUsername())) {
                    throw new ValidTokenException("User token không chính xác");
                }

                if (!userDetails.isEnabled()) {
                    throw new ValidTokenException("Tài khoản của bạn hiện đang bị khóa");
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            throw new ValidTokenException("Token đã hết hạn, vui lòng đăng nhập lại");
        } catch (SignatureException ex) {
            throw new ValidTokenException("Chữ ký token không hợp lệ hoặc đã bị giả mạo");
        } catch (MalformedJwtException ex) {
            throw new ValidTokenException("Token không đúng định dạng");
        } catch (UnsupportedJwtException ex) {
            throw new ValidTokenException("Định dạng Token không được hệ thống hỗ trợ");
        } catch (IllegalArgumentException ex) {
            throw new ValidTokenException("Token không hợp lệ hoặc bị trống");
        } catch (Exception ex) {
            throw new ValidTokenException("Đã xảy ra lỗi trong quá trình xác thực token");
        }
    }

    //401 Unauthorized	Chưa xác thực hoặc token không hợp lệ / hết hạn
    //403 Forbidden	Đã xác thực thành công, nhưng không có quyền truy cập
}