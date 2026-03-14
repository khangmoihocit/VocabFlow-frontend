package com.khangmoihocit.VocabFlow.core.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
@Slf4j
public class JwtService {
    @NonFinal
    @Value("${spring.jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${spring.jwt.issuer}")
    protected String ISSUER;

    @NonFinal
    @Value("${spring.jwt.valid-duration}")
    protected long ACCESS_TOKEN_VALID_DURATION;

    @NonFinal
    @Value("${spring.jwt.refreshable-duration}")
    protected long REFRESH_TOKEN_VALID_DURATION;

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SIGNER_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email){
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(Instant.now().plus(ACCESS_TOKEN_VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .signWith(getSignInKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(String email){
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(Instant.now().plus(REFRESH_TOKEN_VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .signWith(getSignInKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public LocalDateTime extractExpired(String token) {
        Claims claims = extractAllClaims(token);
        LocalDateTime expired = claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return expired;
    }
/*
    1. token có đúng định dạng không
    2. chữ ký của token có đúng không
    3. kiểm tra xem token có hết hạn hay chuưa
    4.  user_id của token có khớp với userdetail không
    5. kiểm tra xem token có trong blacklist không
    6. kiểm tra quyền
    */


    public boolean isTokenFormatValid(String token){
        try{
            String[] tokenParts = token.split("\\.");
            return tokenParts.length == 3;
        }catch (Exception e){
            return false;
        }
    }

    public boolean isSignatureValid(String token){
        try{
            Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractClaims(token, Claims::getExpiration);
            if (expiration == null) {
                return true;
            }
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isIssuerToken(String token){
        String tokenIssuer = extractClaims(token, Claims::getIssuer);
        return tokenIssuer.equals(ISSUER);
    }


    // Lấy theo nhiều claim khác nhau
    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Giải mã token và lấy tất cả claims
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
