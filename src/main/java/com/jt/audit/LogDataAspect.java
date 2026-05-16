package com.jt.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;

@Component
@Aspect
@Slf4j
public class LogDataAspect {
    private static final Logger log = LoggerFactory.getLogger(LogDataAspect.class);
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(logData)")
    public Object intercept(ProceedingJoinPoint joinPoint, LogData logData) throws Throwable {

        // 1. Ambil traceId menggunakan SpEL
        Object traceId = getArgsValue(joinPoint, logData.traceId());
        if (traceId != null) {
            MDC.put("traceId", traceId.toString());
        }

        // 2. Simpan actionType ke MDC agar bisa dibaca oleh Filter nanti
        MDC.put("actionType", logData.actionType());
        log.info("correlation-id:{}", MDC.get("traceId"));

        try {
            // 3. Jalankan method Controller asli
            return joinPoint.proceed();
        } finally {
        }
    }

    private Object getArgsValue(ProceedingJoinPoint joinPoint, String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                Object[] args = joinPoint.getArgs();
                String[] paramNames = nameDiscoverer.getParameterNames(method);

                if (paramNames != null && args != null) {
                    EvaluationContext context = new StandardEvaluationContext();
                    for (int i = 0; i < paramNames.length; i++) {
                        context.setVariable(paramNames[i], args[i]);
                    }
                    Expression expression = parser.parseExpression(parameter);
                    return expression.getValue(context);
                }
            } catch (Exception e) {
                log.error("[Library SpEL] Gagal memproses ekspresi SpEL: " + parameter, e);
            }
        }
        return null;
    }
}
