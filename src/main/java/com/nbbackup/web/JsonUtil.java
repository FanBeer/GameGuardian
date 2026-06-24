package com.nbbackup.web;

import java.util.*;
import java.text.SimpleDateFormat;

public class JsonUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String toJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, obj);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object obj) {
        if (obj == null) {
            sb.append("null");
        } else if (obj instanceof String) {
            writeString(sb, (String) obj);
        } else if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj.toString());
        } else if (obj instanceof Date) {
            writeString(sb, DATE_FORMAT.format((Date) obj));
        } else if (obj instanceof Map) {
            writeMap(sb, (Map<?, ?>) obj);
        } else if (obj instanceof List) {
            writeList(sb, (List<?>) obj);
        } else if (obj.getClass().isArray()) {
            writeArray(sb, obj);
        } else {
            writeBean(sb, obj);
        }
    }

    private static void writeString(StringBuilder sb, String str) {
        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(',');
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            writeValue(sb, entry.getValue());
            first = false;
        }
        sb.append('}');
    }

    private static void writeList(StringBuilder sb, List<?> list) {
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, list.get(i));
        }
        sb.append(']');
    }

    private static void writeArray(StringBuilder sb, Object array) {
        sb.append('[');
        int length = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(',');
            writeValue(sb, java.lang.reflect.Array.get(array, i));
        }
        sb.append(']');
    }

    private static void writeBean(StringBuilder sb, Object obj) {
        sb.append('{');
        boolean first = true;
        try {
            for (java.lang.reflect.Field field : obj.getClass().getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                String name = field.getName();
                Object value = field.get(obj);
                if (value == null) continue;
                if (!first) sb.append(',');
                writeString(sb, name);
                sb.append(':');
                writeValue(sb, value);
                first = false;
            }
            for (java.lang.reflect.Method method : obj.getClass().getMethods()) {
                String methodName = method.getName();
                if (method.getParameterCount() > 0) continue;
                if (methodName.startsWith("get") && methodName.length() > 3
                        && !methodName.equals("getClass")) {
                    String propName = Character.toLowerCase(methodName.charAt(3))
                            + methodName.substring(4);
                    try {
                        obj.getClass().getDeclaredField(propName);
                        continue;
                    } catch (NoSuchFieldException e) {}
                    Object value = method.invoke(obj);
                    if (value == null) continue;
                    if (!first) sb.append(',');
                    writeString(sb, propName);
                    sb.append(':');
                    writeValue(sb, value);
                    first = false;
                }
            }
        } catch (Exception e) {}
        sb.append('}');
    }

    public static Map<String, Object> parseMap(String json) {
        json = json.trim();
        JsonParser parser = new JsonParser(json);
        Object result = parser.parse();
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return new LinkedHashMap<>();
    }

    public static List<Object> parseList(String json) {
        json = json.trim();
        JsonParser parser = new JsonParser(json);
        Object result = parser.parse();
        if (result instanceof List) {
            return (List<Object>) result;
        }
        return new ArrayList<>();
    }

    public static class JsonParser {
        private String src;
        private int pos;

        JsonParser(String src) {
            this.src = src;
            this.pos = 0;
        }

        Object parse() {
            skipWhitespace();
            return parseValue();
        }

        void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') {
                pos += 4; // skip "null"
                return null;
            }
            return parseNumber();
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // skip {
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ':') pos++;
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    break;
                }
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // skip [
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                    continue;
                }
                if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    break;
                }
            }
            return list;
        }

        String parseString() {
            StringBuilder sb = new StringBuilder();
            pos++; // skip opening "
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') break;
                if (c == '\\' && pos < src.length()) {
                    char next = src.charAt(pos++);
                    switch (next) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 <= src.length()) {
                                String hex = src.substring(pos, pos + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default: sb.append(next);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        Boolean parseBoolean() {
            if (pos + 4 <= src.length() && "true".equals(src.substring(pos, pos + 4))) {
                pos += 4;
                return true;
            }
            if (pos + 5 <= src.length() && "false".equals(src.substring(pos, pos + 5))) {
                pos += 5;
                return false;
            }
            return false;
        }

        Object parseNumber() {
            int start = pos;
            boolean isFloat = false;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c) || c == '-' || c == '+' || c == '.'
                        || c == 'e' || c == 'E') {
                    if (c == '.' || c == 'e' || c == 'E') isFloat = true;
                    pos++;
                } else break;
            }
            String numStr = src.substring(start, pos);
            try {
                if (isFloat) return Double.parseDouble(numStr);
                long l = Long.parseLong(numStr);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                return l;
            } catch (NumberFormatException e) { return 0; }
        }
    }

    public static Map<String, Object> success(Object data) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", 200);
        map.put("message", "操作成功");
        map.put("data", data);
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    public static Map<String, Object> fail(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", 500);
        map.put("message", message);
        map.put("data", null);
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    public static Map<String, Object> fail(int code, String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("data", null);
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }
}
