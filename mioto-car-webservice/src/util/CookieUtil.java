/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author tuanlee
 */
public class CookieUtil {
    
    private static final String  NAME    = Config.getString(CookieUtil.class, "session", "name", "sid");
    private static final String  DOMAIN  = Config.getString(CookieUtil.class, "session", "domain", "");
    private static final String  PATH    = Config.getString(CookieUtil.class, "session", "path", "/");
    private static final int     MAX_AGE = Config.getInt(CookieUtil.class, "session", "max-age", 86400);
    private static final boolean SECURE  = Config.getBoolean(CookieUtil.class, "session", "secure", false);

     private static String build(String value, int maxAgeSeconds) {
        StringBuilder sb = new StringBuilder(160);
        sb.append(NAME).append('=').append(value == null ? "" : value);
        sb.append("; Path=").append(PATH);
        if (DOMAIN != null && !DOMAIN.isEmpty()) {
            sb.append("; Domain=").append(DOMAIN);
        }
        sb.append("; Max-Age=").append(maxAgeSeconds);
        sb.append("; HttpOnly");                       
        if (SECURE) {
            sb.append("; Secure");                    
        }
        sb.append("; SameSite=Lax");
        return sb.toString();
    }

    public static void set(HttpServletResponse resp, String signedValue) {
        resp.addHeader("Set-Cookie", build(signedValue, MAX_AGE));
    }

    public static void clear(HttpServletResponse resp) {
        resp.addHeader("Set-Cookie", build("", 0));
    }

    public static String read(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {                          
            return null;
        }
        for (int i = 0; i < cookies.length; ++i) {
            if (NAME.equals(cookies[i].getName())) {
                String v = cookies[i].getValue();
                return (v == null || v.isEmpty()) ? null : v;
            }
        }
        return null;
    }
}
