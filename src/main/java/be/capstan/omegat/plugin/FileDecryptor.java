package be.capstan.omegat.plugin;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class FileDecryptor {
    private FileDecryptor() {}

    private static final int NONCE_LEN    = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int KEY_LEN      = 24;  // AES-192, matches 24-char passwords

    public static String decrypt(byte[] cipherBytes, String password) throws Exception {
        byte[] keyBytes = Arrays.copyOf(password.getBytes(StandardCharsets.UTF_8), KEY_LEN);
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

        byte[] nonce = Arrays.copyOfRange(cipherBytes, 0, NONCE_LEN);
        byte[] ct    = Arrays.copyOfRange(cipherBytes, NONCE_LEN, cipherBytes.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
        byte[] plain = cipher.doFinal(ct);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
