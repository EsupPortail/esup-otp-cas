package org.esupportail.cas.config;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.MultifactorAuthenticationFailureModeEvaluator;
import org.apereo.cas.authentication.bypass.AuthenticationMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.ChainingMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.CredentialMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.DefaultChainingMultifactorAuthenticationBypassProvider;
import org.apereo.cas.authentication.bypass.GroovyMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.HttpRequestMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.MultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.PrincipalMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.RegisteredServiceMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.bypass.RestMultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.configuration.model.support.mfa.MultifactorAuthenticationProviderBypassProperties;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.config.support.authentication.EsupOtpBypassProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * This is {@link EsupOtpMultifactorAuthenticationMultifactorProviderBypassConfiguration}.
 * 
 */
public class EsupOtpMultifactorAuthenticationMultifactorProviderBypassConfiguration {

    @Autowired
    private EsupOtpConfigurationProperties esupOtpConfigurationProperties;

    @Autowired
    private EsupOtpService esupOtpService;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("failureModeEvaluator")
    private MultifactorAuthenticationFailureModeEvaluator failureModeEvaluator;
    
    @ConditionalOnMissingBean(name = "esupOtpMultifactorBypassEvaluator")
    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorBypassEvaluator() {
    	ChainingMultifactorAuthenticationProviderBypassEvaluator bypass = new DefaultChainingMultifactorAuthenticationBypassProvider(applicationContext);
    	MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
    	
    	bypass.addMultifactorAuthenticationProviderBypassEvaluator(new EsupOtpBypassProvider(esupOtpService, esupOtpConfigurationProperties, applicationContext, failureModeEvaluator));
    	
        if (StringUtils.isNotBlank(props.getPrincipalAttributeName())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorPrincipalMultifactorAuthenticationProviderBypass());
        }
        bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorRegisteredServiceMultifactorAuthenticationProviderBypass());
        bypass.addMultifactorAuthenticationProviderBypassEvaluator(
                esupOtpRegisteredServiceMultifactorAuthenticationProviderBypassEvaluator());
        if (StringUtils.isNotBlank(props.getAuthenticationAttributeName())
            || StringUtils.isNotBlank(props.getAuthenticationHandlerName())
            || StringUtils.isNotBlank(props.getAuthenticationMethodName())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorAuthenticationMultifactorAuthenticationProviderBypass());
        }

        if (StringUtils.isNotBlank(props.getCredentialClassType())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorCredentialMultifactorAuthenticationProviderBypass());
        }
        if (StringUtils.isNotBlank(props.getHttpRequestHeaders()) || StringUtils.isNotBlank(props.getHttpRequestRemoteAddress())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorHttpRequestMultifactorAuthenticationProviderBypass());
        }
        if (props.getGroovy().getLocation() != null) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorGroovyMultifactorAuthenticationProviderBypass());
        }
        if (StringUtils.isNotBlank(props.getRest().getUrl())) {
            bypass.addMultifactorAuthenticationProviderBypassEvaluator(esupOtpMultifactorRestMultifactorAuthenticationProviderBypass());
        }
        return bypass;
    }

    @ConditionalOnMissingBean(name = "esupOtpMultifactorRestMultifactorAuthenticationProviderBypass")
    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorRestMultifactorAuthenticationProviderBypass() {
    	MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
          return new RestMultifactorAuthenticationProviderBypassEvaluator(props, esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @ConditionalOnMissingBean(name = "esupOtpMultifactorGroovyMultifactorAuthenticationProviderBypass")
    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorGroovyMultifactorAuthenticationProviderBypass() {
    	MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
          return new GroovyMultifactorAuthenticationProviderBypassEvaluator(props, esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @ConditionalOnMissingBean(name = "esupOtpMultifactorHttpRequestMultifactorAuthenticationProviderBypass")
    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorHttpRequestMultifactorAuthenticationProviderBypass() {
    	MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
          return new HttpRequestMultifactorAuthenticationProviderBypassEvaluator(props, esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @ConditionalOnMissingBean(name = "esupOtpRegisteredServiceMultifactorAuthenticationProviderBypassEvaluator")
    @Bean
    @RefreshScope
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpRegisteredServiceMultifactorAuthenticationProviderBypassEvaluator() {
          return new RegisteredServiceMultifactorAuthenticationProviderBypassEvaluator(esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "esupOtpMultifactorCredentialMultifactorAuthenticationProviderBypass")
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorCredentialMultifactorAuthenticationProviderBypass() {
    	MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
          return new CredentialMultifactorAuthenticationProviderBypassEvaluator(props, esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "esupOtpMultifactorRegisteredServiceMultifactorAuthenticationProviderBypass")
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorRegisteredServiceMultifactorAuthenticationProviderBypass() {
          return new RegisteredServiceMultifactorAuthenticationProviderBypassEvaluator(esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "esupOtpMultifactorPrincipalMultifactorAuthenticationProviderBypass")
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorPrincipalMultifactorAuthenticationProviderBypass() {
    	MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
          return new PrincipalMultifactorAuthenticationProviderBypassEvaluator(props, esupOtpConfigurationProperties.getName(), applicationContext);
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "esupOtpMultifactorAuthenticationMultifactorAuthenticationProviderBypass")
    public MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorAuthenticationMultifactorAuthenticationProviderBypass() {
        MultifactorAuthenticationProviderBypassProperties props = esupOtpConfigurationProperties.getBypass();
          return new AuthenticationMultifactorAuthenticationProviderBypassEvaluator(props, esupOtpConfigurationProperties.getName(), applicationContext);
    }

}
