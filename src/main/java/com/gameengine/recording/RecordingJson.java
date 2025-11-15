package com.gameengine.recording;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON解析工具类：提供简单的JSON字段提取和解析功能
 * 用于录像文件的读取和解析
 */
public final class RecordingJson {
    // 工具类，防止实例化
    private RecordingJson() {}

    /**
     * 从JSON字符串中提取指定字段的值
     */
    public static String field(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int c = json.indexOf(':', i);
        if (c < 0) return null;
        int end = c + 1;
        // 查找字段值的结束位置（逗号或右大括号）
        int comma = json.indexOf(',', end);
        int brace = json.indexOf('}', end);
        int j = (comma < 0) ? brace : (brace < 0 ? comma : Math.min(comma, brace));
        if (j < 0) j = json.length();
        return json.substring(end, j).trim();
    }

    /**
     * 去除字符串两端的引号
     */
    public static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length()-1);
        }
        return s;
    }

    /**
     * 安全解析双精度浮点数
     */
    public static double parseDouble(String s) {
        if (s == null) return 0.0;
        try { return Double.parseDouble(stripQuotes(s)); } catch (Exception e) { return 0.0; }
    }

    /**
     * 分割顶层JSON数组（忽略嵌套数组）
     */
    public static String[] splitTopLevel(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0; int start = 0;
        for (int i = 0; i < arr.length(); i++) {
            char ch = arr.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') depth--;
            else if (ch == ',' && depth == 0) {
                out.add(arr.substring(start, i));
                start = i + 1;
            }
        }
        if (start < arr.length()) out.add(arr.substring(start));
        return out.stream().map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

    /**
     * 从指定位置开始提取JSON数组内容
     */
    public static String extractArray(String json, int startIdx) {
        int i = startIdx;
        if (i >= json.length() || json.charAt(i) != '[') return "";
        int depth = 1;
        int begin = i + 1;
        i++;
        for (; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(begin, i);
                }
            }
        }
        return "";
    }
}


