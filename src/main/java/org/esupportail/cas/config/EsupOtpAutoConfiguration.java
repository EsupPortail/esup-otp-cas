package org.esupportail.cas.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/**
 * Auto-configuration entry point for ESUP-OTP multi-instances support.
 *
 * <p>The {@link EsupOtpMultiInstancesConfigurationRegistrar} is declared as a {@code static @Bean}
 * so that Spring Boot can instantiate it very early in the context lifecycle — before any
 * {@code @Configuration} classes or regular beans are processed — as required by the
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor} contract.
 *
 * <p>Declaring it via {@code @Component} or as a non-static bean would risk premature
 * initialization of other beans and unpredictable ordering issues.
 *
 * <p>{@link EsupOtpInstancesConfigurationProperties} is registered here so that Spring Boot
 * validates and exposes the {@code esupotp[*]} configuration entries as a typed bean,
 * enabling IDE auto-completion and standard Spring Boot property validation.
 */
@Slf4j
@AutoConfiguration
@PropertySource(ignoreResourceNotFound = true, value = {
    "file:/var/cas/config/esupotp.properties",
    "file:/opt/cas/config/esupotp.properties",
    "file:/etc/cas/config/esupotp.properties",
    "classpath:esupotp.properties"
})
@EnableConfigurationProperties(EsupOtpInstancesConfigurationProperties.class)
public class EsupOtpAutoConfiguration {

    /**
     * Registers the ESUP-OTP multi-instances bean definition post-processor.
     *
     * <p>Must be {@code static} so that Spring can create it without instantiating the
     * enclosing {@code @Configuration} class first.
     */
    @Bean
    public static EsupOtpMultiInstancesConfigurationRegistrar esupOtpMultiInstancesConfigurationRegistrar() {
        log.info("EsupOtpMultiInstancesConfigurationRegistrar");
        return new EsupOtpMultiInstancesConfigurationRegistrar();
    }
}
