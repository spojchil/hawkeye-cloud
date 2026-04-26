package com.common.utils.context;

import java.util.Map;

public class RequestContext {
    private static final ThreadLocal<Map<String, String>> headers = new ThreadLocal<>();

    public static void setHeaders(Map<String, String> headerMap) {
        headers.set(headerMap);
    }

    public static String getHeader(String key) {
        Map<String, String> map = headers.get();
        return map != null ? map.get(key) : null;
    }

    public static void clear() {
        headers.remove();
    }
}
