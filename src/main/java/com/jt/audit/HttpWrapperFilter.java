package com.jt.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

public class HttpWrapperFilter implements Filter {

    private final RequestMappingHandlerMapping handlerMapping;

    public HttpWrapperFilter(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        if (!checkIfMethodHasLogData(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);

        try {
            MDC.put("url", requestWrapper.getRequestURI());
            HttpContextHolder.setRequest(requestWrapper);
            HttpContextHolder.setResponse(httpResponse);
            chain.doFilter(requestWrapper, httpResponse);
        } finally {
            HttpContextHolder.clear();
        }
    }

    private boolean checkIfMethodHasLogData(HttpServletRequest request) {
        try {
            if (handlerMapping != null) {
                HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
                if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod handlerMethod) {
                    return handlerMethod.hasMethodAnnotation(LogData.class);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
