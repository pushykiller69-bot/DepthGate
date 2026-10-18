package com.depthgate.engine;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PerformanceTier {
    POTATO, LOW, MEDIUM, HIGH;

    private static final Pattern NVIDIA = Pattern.compile("geforce\\s+(gt|gts|gtx|rtx)?\\s*(\\d{3,4})");

    public static PerformanceTier parse(String s) {
        if (s == null) return null;
        try {
            return valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isIntegrated(String glRenderer) {
        if (glRenderer == null) return false;
        String s = glRenderer.toLowerCase(Locale.ROOT);
        if (s.contains("intel")) {
            return !s.contains("arc");
        }
        if (s.contains("radeon")) {
            if (s.contains(" rx ") || s.contains("rx ") || s.contains("pro w") || s.contains("radeon vii")) return false;
            return s.contains("vega") || s.contains("radeon(tm) graphics") || s.contains("radeon graphics");
        }
        return false;
    }

    public static boolean isLegacyDiscrete(String glRenderer) {
        if (glRenderer == null) return false;
        String s = glRenderer.toLowerCase(Locale.ROOT);
        if (s.contains("radeon hd") || s.contains("radeon r5") || s.contains("radeon r7 2")) return true;
        Matcher m = NVIDIA.matcher(s);
        if (m.find()) {
            String prefix = m.group(1) == null ? "" : m.group(1);
            int n = Integer.parseInt(m.group(2));
            if (prefix.equals("rtx")) return false;
            if (n >= 8000 && n <= 9999) return true;
            if (prefix.equals("gtx")) return n < 650;
            return n < 1000 && n <= 740;
        }
        return false;
    }

    public static PerformanceTier compute(String glRenderer, int cores, long maxHeapMb) {
        int score = 0;
        if (isIntegrated(glRenderer)) score -= 2;
        else if (isLegacyDiscrete(glRenderer)) score -= 2;
        else if (glRenderer != null) score += 1;
        if (cores <= 2) score -= 2;
        else if (cores <= 4) score -= 1;
        else if (cores >= 8) score += 1;
        if (maxHeapMb < 2048) score -= 1;
        if (score <= -3) return POTATO;
        if (score <= -1) return LOW;
        if (score <= 1) return MEDIUM;
        return HIGH;
    }
}
