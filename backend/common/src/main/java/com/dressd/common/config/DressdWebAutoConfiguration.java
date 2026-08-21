package com.dressd.common.config;

import com.dressd.common.auth.CurrentOwnerArgumentResolver;
import com.dressd.common.auth.OwnerAuthProperties;
import com.dressd.common.auth.OwnerIdentityFilter;
import com.dressd.common.auth.OwnerResolver;
import com.dressd.common.web.GlobalExceptionHandler;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Wires the cross-service web concerns — owner authentication, the shared error
 * contract and CORS — into every Dressd service.
 *
 * <p>Registered as a Spring Boot auto-configuration rather than something the
 * services component-scan, so adding {@code common} to a module's dependencies is
 * all it takes to inherit the behaviour.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({OwnerAuthProperties.class, DressdCorsProperties.class})
public class DressdWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler dressdGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public OwnerResolver dressdOwnerResolver(OwnerAuthProperties properties) {
        return OwnerResolver.from(properties);
    }

    @Bean
    public FilterRegistrationBean<OwnerIdentityFilter> dressdOwnerIdentityFilter(
            OwnerResolver resolver, OwnerAuthProperties properties, ObjectMapper objectMapper) {
        FilterRegistrationBean<OwnerIdentityFilter> registration = new FilterRegistrationBean<>(
                new OwnerIdentityFilter(resolver, properties, objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100);
        return registration;
    }

    @Bean
    public WebMvcConfigurer dressdWebMvcConfigurer(DressdCorsProperties cors) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentOwnerArgumentResolver());
            }

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(cors.getAllowedOrigins())
                        .allowedHeaders("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
            }
        };
    }
}
