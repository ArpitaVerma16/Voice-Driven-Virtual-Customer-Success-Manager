package com.vcsm.security.hmac;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@Component
public class SignatureValidator {

    @Value("${security.hmac.secret:dev-secret}")
    private String secret;

    public String generateSignature(
            String method,
            String path,
            String body,
            String timestamp,
            String nonce) {

        try {

            String payload =
                    method +
                    path +
                    body +
                    timestamp +
                    nonce;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            secret.getBytes(),
                            "HmacSHA256");

            mac.init(secretKey);

            byte[] hash =
                    mac.doFinal(payload.getBytes());

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new CustomDomainException(e);
        }
    }
}