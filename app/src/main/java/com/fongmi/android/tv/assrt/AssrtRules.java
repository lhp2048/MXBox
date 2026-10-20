package com.fongmi.android.tv.assrt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AssrtRules {

    private static final Pattern CN_EPISODE = Pattern.compile("第\\s*(\\d+)\\s*集");
    private static final Pattern SEASON_EPISODE = Pattern.compile("[Ss](\\d+)[Ee](\\d+)");

    private AssrtRules() {
    }

    public static boolean validToken(String token) {
        return token != null && token.matches("[A-Za-z0-9]{32}");
    }

    public static boolean validId(String id) {
        return id != null && id.matches("\\d{6}");
    }

    public static String keywordError(String keyword) {
        String text = keyword == null ? "" : keyword.trim();
        if (text.length() <= 3) return "关键字至少 4 个字符";
        return "";
    }

    public static String statusMessage(int status) {
        return switch (status) {
            case 1, 20001 -> "密钥无效";
            case 101 -> "关键字至少 4 个字符";
            case 30900 -> "请求过于频繁，请稍后再试";
            case 20900 -> "没有找到字幕";
            default -> "搜索失败";
        };
    }

    public static boolean textSubtitle(String name) {
        String lower = bareName(name);
        return lower.endsWith(".srt") || lower.endsWith(".ssa") || lower.endsWith(".ass");
    }

    public static boolean disabled(boolean hasFileList, List<String> names, String subtype) {
        if (hasFileList) {
            for (String name : names) if (textSubtitle(name)) return false;
            return true;
        }
        return !textSubtype(subtype);
    }

    public static String archiveKind(String filename) {
        String lower = bareName(filename);
        if (lower.endsWith(".srt") || lower.endsWith(".ssa") || lower.endsWith(".ass")) return "text";
        if (lower.endsWith(".zip")) return "zip";
        return "reject";
    }

    public static List<String> textNames(List<String> names) {
        List<String> text = new ArrayList<>();
        if (names == null) return text;
        for (String name : names) if (textSubtitle(name)) text.add(name);
        return text;
    }

    public static String pick(List<String> textNames, String episode) {
        if (textNames == null || textNames.isEmpty()) return "";
        if (textNames.size() == 1) return textNames.get(0);
        String current = episode == null ? "" : episode.trim();
        if (current.isEmpty()) return "";
        List<String> hits = new ArrayList<>();
        for (String name : textNames) if (matches(name, current)) hits.add(name);
        return hits.size() == 1 ? hits.get(0) : "";
    }

    public static boolean unsafeZipName(String name) {
        if (name == null || name.isEmpty()) return true;
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.indexOf(':') >= 0) return true;
        return normalized.contains("..");
    }

    static boolean matches(String filename, String episode) {
        if (filename.toLowerCase(Locale.ROOT).contains(episode.toLowerCase(Locale.ROOT))) return true;
        Integer fileEpisode = cnEpisode(filename);
        Integer currentEpisode = cnEpisode(episode);
        if (fileEpisode != null && fileEpisode.equals(currentEpisode)) return true;
        int[] fileSeason = seasonEpisode(filename);
        int[] currentSeason = seasonEpisode(episode);
        return fileSeason != null && currentSeason != null && fileSeason[0] == currentSeason[0] && fileSeason[1] == currentSeason[1];
    }

    private static boolean textSubtype(String subtype) {
        if (subtype == null) return false;
        String lower = subtype.toLowerCase(Locale.ROOT);
        if (lower.contains("vobsub")) return false;
        return lower.contains("srt") || lower.contains("ssa") || lower.contains("ass");
    }

    private static String bareName(String name) {
        if (name == null) return "";
        String lower = name.trim().toLowerCase(Locale.ROOT);
        int query = lower.indexOf('?');
        if (query >= 0) lower = lower.substring(0, query);
        return lower;
    }

    private static Integer cnEpisode(String text) {
        Matcher matcher = CN_EPISODE.matcher(text);
        if (!matcher.find()) return null;
        return Integer.parseInt(matcher.group(1));
    }

    private static int[] seasonEpisode(String text) {
        Matcher matcher = SEASON_EPISODE.matcher(text);
        if (!matcher.find()) return null;
        return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
    }

    public static final class Window {

        private final ArrayDeque<Long> stamps = new ArrayDeque<>();

        public synchronized boolean tryAcquire(long now) {
            while (!stamps.isEmpty() && now - stamps.peekFirst() >= 60_000L) stamps.removeFirst();
            if (stamps.size() >= 5) return false;
            stamps.addLast(now);
            return true;
        }
    }
}
