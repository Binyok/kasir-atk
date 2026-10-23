package giriatk.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null) {
            return false;
        }
        if (stored.contains(":")) {
            String[] parts = stored.split(":", 2);
            try {
                byte[] salt = Base64.getDecoder().decode(parts[0]);
                byte[] expected = Base64.getDecoder().decode(parts[1]);
                byte[] actual = pbkdf2(plain.toCharArray(), salt, ITERATIONS, expected.length * 8);
                return MessageDigest.isEqual(expected, actual);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return stored.equals(plain);
    }

    public static boolean needsRehash(String stored) {
        return stored != null && !stored.contains(":");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Gagal melakukan hashing password", e);
        }
    }
}
