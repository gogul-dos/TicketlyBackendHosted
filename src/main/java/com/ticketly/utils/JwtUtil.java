package com.ticketly.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;

public class JwtUtil {
    private static final String SECRET_KEY = "Ticketly";

    public static Claims verifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
            return claims;
        } catch (SignatureException e) {
            System.out.println("Invalid JWT signature.");
        } catch (Exception e) {
            System.out.println("Token verification failed: " + e.getMessage());
        }
        return null;
    }
}
