package be.capstan.omegat.plugin;

import org.omegat.core.team2.TeamSettings;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CredentialStorage {

    /**
     * Gets the plaintext password for UI display.
     * Expects full key (e.g. url + "!password").
     */
    public static String getPlainPassword(String key) {
        String raw = TeamSettings.get(key);
        if (raw == null || raw.isEmpty()) return raw;

        String result = null;
        try {
            Class<?> tuClass = Class.forName("org.omegat.core.team2.impl.TeamUtils");
            Method decodeMethod = tuClass.getMethod("decodePassword", String.class);
            result = (String) decodeMethod.invoke(null, raw);
        } catch (Exception e) {
            // Ignore reflection errors and drop to fallback
        }

        // If reflection successfully returned a different decrypted string, return it
        if (result != null && !result.equals(raw)) {
            return result;
        }

        // Fallback: If not encrypted with M118's "***" marker, assume Base64 and decode
        if (!raw.startsWith("***")) {
            try {
                return new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ex) {
                return raw; // Already plaintext fallback
            }
        }
        
        return raw; // Return raw as last resort
    }

    /**
     * Saves the password in TeamSettings.
     * In M118, encrypts immediately. In Mainstream, encodes to Base64.
     * Expects full key (e.g. url + "!password").
     */
    public static void setPassword(String key, String plaintext) {
        if (plaintext == null) {
            TeamSettings.set(key, null);
            return;
        }

        // Try M118 immediate encryption first
        try {
            Class<?> tuClass = Class.forName("org.omegat.core.team2.impl.TeamUtils");
            Method encodeMethod = tuClass.getMethod("encodePassword", String.class);
            String encrypted = (String) encodeMethod.invoke(null, plaintext);
            
            if (encrypted != null && encrypted.startsWith("***")) {
                TeamSettings.set(key, encrypted);
                return; // Successfully encrypted and saved for M118
            }
        } catch (Exception e) {
            // Encode method doesn't exist, we are running on standard OmegaT. Proceed to fallback.
        }

        // Fallback: Encode to Base64 for standard OmegaT
        String b64 = Base64.getEncoder().encodeToString(plaintext.getBytes(StandardCharsets.UTF_8));
        TeamSettings.set(key, b64);
    }

    /**
     * Returns Base64 format of the password for standard OmegaT file exports.
     * Expects full key (e.g. url + "!password").
     */
    public static String getBase64ForExport(String key) {
        String raw = TeamSettings.get(key);
        if (raw == null || raw.isEmpty()) return null;

        if (raw.startsWith("***")) {
            String plain = getPlainPassword(key);
            if (plain != null) {
                return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        try {
            Base64.getDecoder().decode(raw);
            return raw; // Safely already base64
        } catch (IllegalArgumentException e) {
            return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }
    }
}
