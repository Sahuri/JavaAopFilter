package com.jt.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

public class HttpWrapperFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(HttpWrapperFilter.class);

    // HandlerMapping disuntikkan untuk mendeteksi anotasi @LogData pada method Controller tujuan
    private final RequestMappingHandlerMapping handlerMapping;

    public HttpWrapperFilter(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Metode inisialisasi opsional
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 1. Validasi: Cek apakah method Controller tujuan dipasangi @LogData
            boolean hasLogDataAnnotation = checkIfMethodHasLogData(httpRequest);

            // 2. Jika TIDAK ADAn anotasi @LogData, bypass filter ini sepenuhnya (hemat memori)
            if (!hasLogDataAnnotation) {
                chain.doFilter(request, response);
                return;
            }

            // 3. Bungkus request & response agar data stream-nya bisa di-cache/dibaca ulang
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

            // 4. Simpan ke ThreadLocal Context Holder agar bisa diakses komponen lain jika dibutuhkan
            HttpContextHolder.setRequest(requestWrapper);
            HttpContextHolder.setResponse(responseWrapper);

            try {
                // Set metadata awal URL ke MDC sebelum masuk ke Controller
                MDC.put("url", requestWrapper.getRequestURI());

                // 5. Lanjutkan eksekusi ke Filter selanjutnya -> Aspect -> Controller
                chain.doFilter(requestWrapper, responseWrapper);

                // Catat HTTP Status setelah proses selesai/mulai dikembalikan


            } finally {
                // 6. WAJIB untuk Caching Wrapper: Alirkan kembali data dari cache ke response stream asli client
                // Jika baris ini hilang, browser client akan menerima halaman blank/kosong.
                responseWrapper.copyBodyToResponse();

                // 7. Bersihkan ThreadLocal Context Holder demi mencegah memory leak
                HttpContextHolder.clear();

                // CATATAN UNTUK MDC:
                // Kita TIDAK melakukan MDC.clear() di sini karena jika method bertipe CompletableFuture,
                // MDC ini masih dibutuhkan oleh AuditResponseBodyAdvice untuk mencetak log final saat thread async selesai.
                // Pembersihan MDC yang aman dipindahkan ke dalam Filter penutup atau Interceptor Async bawaan Spring jika diperlukan.
            }
        } else {
            // Loloskan jika request bukan merupakan tipe HTTP (misal: WebSocket)
            chain.doFilter(request, response);
        }
    }

    /**
     * Helper untuk memeriksa apakah endpoint URL yang dituju mengarah ke method Controller
     * yang memiliki anotasi @LogData.
     */
    private boolean checkIfMethodHasLogData(HttpServletRequest request) {
        try {
            if (handlerMapping != null) {
                HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
                if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod) {
                    HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
                    return handlerMethod.hasMethodAnnotation(LogData.class);
                }
            }
        } catch (Exception e) {
            // Kembalikan false jika terjadi error pencarian (misal request menuju URL statis /error atau /favicon.ico)
            return false;
        }
        return false;
    }

    @Override
    public void destroy() {
        // Metode pembersihan filter opsional
    }
}