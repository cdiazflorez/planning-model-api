package com.mercadolibre.planning.model.api.config;

import com.mercadolibre.planning.model.api.web.filter.EndpointMetricInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private EndpointMetricInterceptor endpointMetricInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(endpointMetricInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/ping");
    }
}
