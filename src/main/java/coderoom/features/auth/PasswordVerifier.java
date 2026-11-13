package coderoom.features.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 담당: B. JDK의 PBKDF2-HMAC-SHA256으로 비밀번호 해시를 생성하고 검증한다.
 * 저장 형식: v1$pbkdf2-sha256$600000$Base64(salt 16바이트)$Base64(해시 32바이트).
 * v1은 이 설정만 허용한다. 설정 변경은 새 버전과 이전 형식 지원을 함께 추가한다.
 * 호출자의 비밀번호 배열은 수정하지 않는다. 호출자가 사용 후 지워야 한다.
 */
public final class PasswordVerifier {
    // OWASP Password Storage Cheat Sheet의 PBKDF2-HMAC-SHA256 권고값(2026-11-03 확인).
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final String PREFIX = "v1$pbkdf2-sha256$" + ITERATIONS + "$";
    private final SecureRandom random = new SecureRandom();

    /** 시연 계정의 password_hash에도 이 메서드의 반환값 전체를 저장한다. */
    public String hash(char[] password) {
        Objects.requireNonNull(password, "비밀번호가 필요합니다.");
        if (password.length == 0) {
            throw new IllegalArgumentException("비밀번호는 비어 있을 수 없습니다.");
        }
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] derived = derive(password, salt);
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            return PREFIX + encoder.encodeToString(salt) + "$" + encoder.encodeToString(derived);
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    /** 오답·빈 입력·지원하지 않거나 손상된 저장 형식은 false로 처리한다. */
    public boolean verify(char[] password, String storedHash) {
        if (password == null || password.length == 0 || storedHash == null
                || storedHash.length() > 160 || !storedHash.startsWith(PREFIX)) {
            return false;
        }
        String[] fields = storedHash.split("\\$", -1);
        if (fields.length != 5 || fields[3].length() != 24 || fields[4].length() != 44) {
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(fields[3]);
            expected = Base64.getDecoder().decode(fields[4]);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (salt.length != SALT_BYTES || expected.length != HASH_BYTES) {
            return false;
        }
        byte[] actual = derive(password, salt);
        try {
            return MessageDigest.isEqual(expected, actual);
        } finally {
            Arrays.fill(actual, (byte) 0);
            Arrays.fill(expected, (byte) 0);
        }
    }

    private static byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("비밀번호 검증 기능을 준비할 수 없습니다.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
