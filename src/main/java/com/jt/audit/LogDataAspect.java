package com.jt.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Component
@Aspect
public class LogDataAspect {
    private static final Logger log = LoggerFactory.getLogger(LogDataAspect.class);
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(logData)")
    public Object intercept(ProceedingJoinPoint joinPoint, LogData logData) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();

        // 1. DAFTARKAN PARAMETER METHOD (Termasuk jika ada HttpServletRequest di argumen)
        String[] paramNames = discoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);

                // Backup plan: jika parameternya bertipe HttpServletRequest tapi namanya bukan 'request'
                if (args[i] instanceof HttpServletRequest) {
                    context.setVariable("request", args[i]);
                }
            }
        }

        // 2. BACKUP GLOBAL CONTEXT (Jika method TIDAK punya parameter HttpServletRequest)
        HttpServletRequest request = null;
        if (context.lookupVariable("request") == null) {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                context.setVariable("request", attributes.getRequest());
                request = attributes.getRequest();
            }
        }

        // 3. Ambil traceId menggunakan SpEL
        String traceId = parseSpel(logData.traceId(), context);
        String actionType = parseSpel(logData.actionType(), context);

        // 4. Simpan ke Request Attribute jika objek request berhasil ditemukan
        if (request != null) {
            request.setAttribute("auditTraceId", traceId != null ? traceId : "");
            request.setAttribute("auditActionType", actionType != null ? actionType : "");
        }

        // 4. Simpan actionType ke MDC agar bisa dibaca oleh Filter nanti
        if (traceId != null) MDC.put("traceId", traceId);
        if (actionType != null) MDC.put("actionType", actionType);


        log.info("correlation-id:{}", MDC.get("traceId"));
        log.info("actionType:{}", MDC.get("actionType"));

        try {
            // 3. Jalankan method Controller asli
            return joinPoint.proceed();
        } finally {
        }
    }

    private String parseSpel(String expressionStr, EvaluationContext context) {
        if (expressionStr != null && expressionStr.startsWith("#")) {
            Expression expression = parser.parseExpression(expressionStr);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : "";
        }
        return expressionStr;
    }

}
