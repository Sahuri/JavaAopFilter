package com.jt.audit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HttpContextHolder {
    private static final ThreadLocal<HttpServletRequest> REQUEST_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<HttpServletResponse> RESPONSE_HOLDER = new ThreadLocal<>();

    public static void setRequest(HttpServletRequest request) { REQUEST_HOLDER.set(request); }
    public static HttpServletRequest getRequest() { return REQUEST_HOLDER.get(); }

    public static void setResponse(HttpServletResponse response) { RESPONSE_HOLDER.set(response); }
    public static HttpServletResponse getResponse() { return RESPONSE_HOLDER.get(); }

    public static void clear() {
        REQUEST_HOLDER.remove();
        RESPONSE_HOLDER.remove();
    }
}
