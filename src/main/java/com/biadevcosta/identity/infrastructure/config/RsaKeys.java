package com.biadevcosta.identity.infrastructure.config;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Loads RSA keys from PEM resources: PKCS#8 for the private key, X.509 SubjectPublicKeyInfo for the public key. */
final class RsaKeys {

    private RsaKeys() {
    }

    static RSAPrivateKey privateKey(Resource pem) {
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(der(pem, "PRIVATE KEY")));
        } catch (Exception e) {
            throw new IllegalStateException("cannot load RSA private key from " + pem.getDescription(), e);
        }
    }

    static RSAPublicKey publicKey(Resource pem) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der(pem, "PUBLIC KEY")));
        } catch (Exception e) {
            throw new IllegalStateException("cannot load RSA public key from " + pem.getDescription(), e);
        }
    }

    private static byte[] der(Resource pem, String label) {
        try {
            String base64 = new String(pem.getContentAsByteArray(), StandardCharsets.US_ASCII)
                    .replaceAll("-----BEGIN (RSA )?" + label + "-----", "")
                    .replaceAll("-----END (RSA )?" + label + "-----", "")
                    .replaceAll("\\s", "");
            return Base64.getDecoder().decode(base64);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read key resource " + pem.getDescription(), e);
        }
    }
}
