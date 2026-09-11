/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author tuanlee
 */
public class CookieSigner {
        private static final String SECRET =
            Config.getString(CookieUtil.class, "session", "secret", "change-me");

    private CookieSigner() { }

    public static String sign(long sessionId) {
        String id = Long.toString(sessionId);
        return id + "." + hmac(id);
    }

    public static long verify(String value) {
        if (value == null) {
            return -1;
        }
        int dot = value.lastIndexOf('.');
        if (dot <= 0 || dot == value.length() - 1) {
            return -1;
        }
        String id  = value.substring(0, dot);
        String sig = value.substring(dot + 1);

        if (!constantTimeEquals(hmac(id), sig)) {       
            return -1;
        }
        try {
            long sessionId = Long.parseLong(id);
            return sessionId > 0 ? sessionId : -1;
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC failed", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); ++i) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
