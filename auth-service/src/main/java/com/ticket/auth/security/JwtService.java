package com.ticket.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final String SECRET =
            "mysecretkeymysecretkeymysecretkey";

    public String generateToken(String userId) {

        return Jwts.builder()

                .setSubject(userId)

                .setIssuedAt(new Date())

                .setExpiration(
                        new Date(System.currentTimeMillis() + 86400000)
                )

                .signWith(
                        SignatureAlgorithm.HS256,
                        SECRET
                )

                .compact();

    }

}
