package com.hawkeye.detection.common.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlFixer {

    /* 匹配 scheme://authority/path?query#fragment */
    private static final Pattern URI_PATTERN = Pattern.compile(
            "^(\\w+)://([^/?#]+)(/[^?#]*)?(?:\\?([^#]*))?(?:#(.*))?$"
    );

    public static String fix(String url) {
        if (url == null || url.isBlank()) return url;
        String input = url.trim().replace('\\', '/');

        Matcher m = URI_PATTERN.matcher(input);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid URL structure: " + url);
        }

        String scheme = m.group(1).toLowerCase();
        String authority = m.group(2).toLowerCase(); /* host 大小写不敏感 */
        String path = m.group(3) != null ? m.group(3) : "/";
        String query = m.group(4);
        String fragment = m.group(5);

        /* 分别编码各组件 */
        String encodedPath = encodePath(path);
        String encodedQuery = query != null ? encodeQuery(query) : null;
        String encodedFragment = fragment != null ? encodeFragment(fragment) : null;

        try {
            URI uri = new URI(scheme, authority, encodedPath, encodedQuery, encodedFragment);
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to construct URI from fixed components: " + url, e);
        }
    }

    /**
     * 编码工具
     */
    private static String encodePath(String path) {
        if (path.isEmpty() || path.equals("/")) return path;
        StringBuilder sb = new StringBuilder();
        String[] segments = path.split("/", -1);
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append('/');
            sb.append(encodePathSegment(segments[i]));
        }
        /* 保留末尾的 */
        if (path.endsWith("/") && sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private static String encodePathSegment(String segment) {
        /* 保留已编码的 %XX，其余按 UTF-8 编码非保留字符之外的内容 */
        StringBuilder sb = new StringBuilder();
        byte[] bytes = segment.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            int c = b & 0xFF;
            if (isUnreserved(c) || c == '%') {
                sb.append((char) c);
            } else {
                sb.append(String.format("%%%02X", c));
            }
        }
        return sb.toString();
    }

    private static String encodeQuery(String query) {
        if (query.isEmpty()) return query;
        StringBuilder sb = new StringBuilder();
        String[] params = query.split("&");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) sb.append('&');
            String[] kv = params[i].split("=", 2);
            sb.append(encodeQueryComponent(kv[0]));
            if (kv.length > 1) {
                sb.append('=').append(encodeQueryComponent(kv[1]));
            }
        }
        return sb.toString();
    }

    private static String encodeQueryComponent(String component) {
        /* 使用 URLEncoder，它将空格变为 +，符合 application/x-www-form-urlencoded */
        return java.net.URLEncoder.encode(component, StandardCharsets.UTF_8);
    }

    private static String encodeFragment(String fragment) {
        /* 片段与路径段处理类似 */
        return encodePathSegment(fragment);
    }

    private static boolean isUnreserved(int c) {
        /* RFC 3986 unreserved: A-Z a-z 0-9 - . _ ~ */
        return (c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '-' || c == '.' || c == '_' || c == '~';
    }
}