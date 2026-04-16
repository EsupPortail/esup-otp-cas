package org.esupportail.cas.config.support.authentication;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.*;
import org.apereo.cas.authentication.bypass.MultifactorAuthenticationProviderBypassEvaluator;
import org.apereo.cas.authentication.metadata.AuthenticationContextAttributeMetaDataPopulator;
import org.apereo.cas.authentication.principal.DefaultPrincipalFactory;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.esupportail.cas.adaptors.esupotp.EsupOtpAuthenticationHandler;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMultifactorAuthenticationProvider;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * This is {@link EsupOtpAuthenticationEventExecutionPlanConfiguration}.
 *
 * @author Francis Le Coq
 * @since 5.2.2
 */
public class EsupOtpAuthenticationEventExecutionPlanConfiguration {

    @Autowired
    private CasConfigurationProperties casProperties;
    
    @Autowired
    private EsupOtpConfigurationProperties esupOtpConfigurationProperties;
    
    @Autowired
    @Qualifier("esupOtpMultifactorBypassEvaluator")
    private MultifactorAuthenticationProviderBypassEvaluator esupOtpMultifactorBypassEvaluator;

    @Autowired
    @Qualifier("failureModeEvaluator")
    private MultifactorAuthenticationFailureModeEvaluator failureModeEvaluator;

    @ConditionalOnMissingBean(name = "esupotpAuthenticationHandler")
    @Bean
    @RefreshScope
    public AuthenticationHandler esupotpAuthenticationHandler() {
        return new EsupOtpAuthenticationHandler(
	        	StringUtils.defaultIfBlank(esupOtpConfigurationProperties.getName(), esupOtpConfigurationProperties.getId()),
        	esupotpPrincipalFactory(),
        	esupOtpConfigurationProperties,
        	esupOtpService()
        );
    }

	@Bean
	public EsupOtpService esupOtpService() {
		return new EsupOtpService(esupOtpConfigurationProperties);
	}
	
	@Bean
	public PrincipalFactory esupotpPrincipalFactory() {
		return new DefaultPrincipalFactory();
	}
	
	@Bean
    @RefreshScope(proxyMode = ScopedProxyMode.DEFAULT)
    @ConditionalOnMissingBean(name = "esupotpAuthenticationProvider")
	public MultifactorAuthenticationProvider esupotpAuthenticationProvider() {
        final EsupOtpMultifactorAuthenticationProvider p = new EsupOtpMultifactorAuthenticationProvider();
        p.setBypassEvaluator(esupOtpMultifactorBypassEvaluator);
        p.setFailureMode(esupOtpConfigurationProperties.getFailureMode());
        p.setFailureModeEvaluator(failureModeEvaluator);
        p.setOrder(esupOtpConfigurationProperties.getRank());
	    p.setId(esupOtpConfigurationProperties.getName());
		return p;
	}
	
	@Bean
	@RefreshScope
	public AuthenticationMetaDataPopulator esupotpAuthenticationMetaDataPopulator() {
            return new AuthenticationContextAttributeMetaDataPopulator(
                casProperties.getAuthn().getMfa().getCore().getAuthenticationContextAttribute(),
                esupotpAuthenticationHandler(),
                esupOtpConfigurationProperties.getName()
            );
        }
	
    @ConditionalOnMissingBean(name = "esupotpAuthenticationEventExecutionPlanConfigurer")
    @Bean
    public AuthenticationEventExecutionPlanConfigurer esupotpAuthenticationEventExecutionPlanConfigurer() {
        return plan -> {
            plan.registerAuthenticationHandler(esupotpAuthenticationHandler());
            plan.registerAuthenticationMetadataPopulator(esupotpAuthenticationMetaDataPopulator());
        };
    }
}
