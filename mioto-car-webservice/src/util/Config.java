
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author tuanlee
 */
public final class Config {

    // "section" -> ("key" -> "value")
    private static final Map<String, Map<String, String>> SECTIONS =
            new HashMap<>();

    static {
        String env  = System.getProperty("appenv", "development");
        String dir  = System.getProperty("conf", "conf");
        String path = dir + File.separator + env + ".ini";
        try {
            load(path);
            //System.out.println("[Config] loaded " + path + ", sections=" + SECTIONS.size());
        } catch (IOException ex) {
            // Fail loudly: running with silently-empty config is worse than not starting.
            throw new IllegalStateException("cannot load config: " + path, ex);
        }
    }

    private Config() { }

    private static void load(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8));
        try {
            Map<String, String> current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    String section = line.substring(1, line.length() - 1).trim();
                    current = SECTIONS.get(section);
                    if (current == null) {
                        current = new HashMap<>();
                        SECTIONS.put(section, current);
                    }
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0 || current == null) {
                    continue;                       // malformed or before any section: skip
                }
                // only split on the FIRST '=' - a value may legitimately contain one
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                current.put(key, val);
            }
        } finally {
            reader.close();
        }
    }

    private static String section(Class<?> clazz, String name) {
        return clazz.getSimpleName() + "@" + name;
    }

    public static String getString(String section, String key, String defVal) {
        Map<String, String> map = SECTIONS.get(section);
        if (map == null) {
            return defVal;
        }
        String val = map.get(key);
        return (val == null || val.isEmpty()) ? defVal : val;
    }

    public static String getString(Class<?> clazz, String name, String key, String defVal) {
        return getString(section(clazz, name), key, defVal);
    }

    public static int getInt(Class<?> clazz, String name, String key, int defVal) {
        String val = getString(section(clazz, name), key, null);
        if (val == null) {
            return defVal;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException ex) {
            return defVal;                          // a typo degrades, it does not crash
        }
    }

    public static long getLong(Class<?> clazz, String name, String key, long defVal) {
        String val = getString(section(clazz, name), key, null);
        if (val == null) {
            return defVal;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException ex) {
            return defVal;
        }
    }

    public static boolean getBoolean(Class<?> clazz, String name, String key, boolean defVal) {
        String val = getString(section(clazz, name), key, null);
        if (val == null) {
            return defVal;
        }
        return "true".equalsIgnoreCase(val) || "1".equals(val) || "yes".equalsIgnoreCase(val);
    }
}
