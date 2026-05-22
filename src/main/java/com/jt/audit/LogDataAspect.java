package com.jt.audit;

import jakarta.servlet.http.HttpServletRequest;
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

        String[] paramNames = discoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
                if (args[i] instanceof HttpServletRequest servletRequest) {
                    context.setVariable("request", servletRequest);
                }
            }
        }

        HttpServletRequest request = resolveRequest(context);
        if (request == null) {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                request = attributes.getRequest();
                context.setVariable("request", request);
            }
        }

        String traceId = parseSpel(logData.traceId(), context);
        String actionType = parseSpel(logData.actionType(), context);

        if (request != null) {
            request.setAttribute("auditTraceId", traceId != null ? traceId : "");
            request.setAttribute("auditActionType", actionType != null ? actionType : "");
        }

        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        if (actionType != null) {
            MDC.put("actionType", actionType);
        }

        log.info("correlation-id:{}", MDC.get("traceId"));
        log.info("actionType:{}", MDC.get("actionType"));

        try {
            return joinPoint.proceed();
        } finally {
            MDC.remove("traceId");
            MDC.remove("actionType");
        }
    }

    private HttpServletRequest resolveRequest(StandardEvaluationContext context) {
        Object requestVar = context.lookupVariable("request");
        if (requestVar instanceof HttpServletRequest servletRequest) {
            return servletRequest;
        }
        return null;
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
