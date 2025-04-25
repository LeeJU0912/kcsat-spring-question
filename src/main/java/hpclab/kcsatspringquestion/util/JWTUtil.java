package hpclab.kcsatspringquestion.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * JWT 토큰 설정 관련 클래스입니다.
 */
@Component
public class JWTUtil {

    private static final Long expiredMs = 3600000L;

    @Value("${jwt.secret}")
    private String secretKey;
    private final SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

    public static final String USER_EMAIL = "userEmail";
    public static final String USER_NAME = "userName";
    public static final String ROLE = "role";

    // claim 반환 메서드
    public Claims getClaims(String token) {
        String tokenWithoutHeader = token.replace("Bearer ", "");
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(tokenWithoutHeader)
                .getPayload();
    }
}
