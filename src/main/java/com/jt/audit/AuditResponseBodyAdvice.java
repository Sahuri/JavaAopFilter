package com.jt.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class AuditResponseBodyAdvice implements ResponseBodyAdvice<Object> {
    private static final Logger log = LoggerFactory.getLogger(AuditResponseBodyAdvice.class);

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Hanya aktifkan jika method controller tersebut memiliki anotasi @LogData
        return returnType.hasMethodAnnotation(LogData.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // DI SINI DATA NYATA SUDAH KELUAR DARI CompletableFuture!
        try {

            // 1. Ambil HttpServletRequest asli untuk menarik data dari Aspect sebelumnya
            if (request instanceof ServletServerHttpRequest) {
                HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();

                // Ambil data yang dikirim oleh Aspect lewat Request Attributes
                String traceId = (String) servletRequest.getAttribute("auditTraceId");
                String actionType = (String) servletRequest.getAttribute("auditActionType");

                // Set ke MDC thread penampung baru ini agar log tercetak rapi
                MDC.put("traceId", traceId != null ? traceId : "N/A");
                MDC.put("actionType", actionType != null ? actionType : "N/A");
            }

            // 💡 Kunci Perbaikan: Cek dan cast ke ServletServerHttpResponse
            if (response instanceof ServletServerHttpResponse) {
                ServletServerHttpResponse servletResponse = (ServletServerHttpResponse) response;

                // Ambil HttpServletResponse asli bawaan Tomcat/Undertow
                int statusCode = servletResponse.getServletResponse().getStatus();

                MDC.put("status", String.valueOf(statusCode));
                log.info("[Advice] HTTP Status Terdeteksi (via Servlet): {}", statusCode);
            }

            if (body != null) {
                MDC.put("body", body.toString());
            }

            // 1. Cetak log audit final Anda di sini
            log.info("--- LOG AUDIT ASYNC ---");
            log.info("actionType: {}", MDC.get("actionType"));
            log.info("traceId   : {}", MDC.get("traceId"));
            log.info("url       : {}", request.getURI().getPath());
            log.info("status    : {}", MDC.get("status"));
            log.info("body      : {}", MDC.get("body"));
            log.info("-----------------------");

        } finally {
            // 2. DI SINI TEMPAT TERBAIKNYA!
            // Bersihkan MDC tepat setelah log selesai dicetak agar tidak terjadi memory leak pada Thread Pool
            MDC.clear();
        }

        return body; // Kembalikan body asli agar tidak mengubah response ke client
    }
}