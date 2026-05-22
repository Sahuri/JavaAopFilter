package com.jt.audit;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class AuditAutoConfiguration {

    @Bean
    public FilterRegistrationBean<HttpWrapperFilter> auditFilterRegistration(RequestMappingHandlerMapping handlerMapping) {
        FilterRegistrationBean<HttpWrapperFilter> registrationBean = new FilterRegistrationBean<>();

        // Oper handlerMapping ke dalam constructor filter
        registrationBean.setFilter(new HttpWrapperFilter(handlerMapping));
        registrationBean.addUrlPatterns("/*"); // Tetap pantau semua URL, tapi filternya pintar memilih
        registrationBean.setOrder(1);

        return registrationBean;
    }
}