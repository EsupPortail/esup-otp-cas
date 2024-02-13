package org.esupportail.cas.config;

import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.audit.AuditableExecution;
import org.apereo.cas.authentication.AuthenticationEventExecutionPlan;
import org.apereo.cas.authentication.AuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.MultifactorAuthenticationContextValidator;
import org.apereo.cas.authentication.MultifactorAuthenticationProviderSelector;
import org.apereo.cas.config.MultifactorAuthnTrustConfiguration;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.apereo.cas.web.cookie.CasCookieBuilder;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;
import org.apereo.cas.web.flow.SingleSignOnParticipationStrategy;
import org.apereo.cas.web.flow.authentication.RankedMultifactorAuthenticationProviderSelector;
import org.apereo.cas.web.flow.configurer.AbstractCasWebflowConfigurer;
import org.apereo.cas.web.flow.resolver.CasDelegatingWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;
import org.apereo.cas.web.flow.resolver.impl.CasWebflowEventResolutionConfigurationContext;
import org.apereo.cas.web.flow.util.MultifactorAuthenticationWebflowUtils;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpAuthenticationWebflowAction;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpAuthenticationWebflowEventResolver;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpGetTransportsAction;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpMultifactorTrustWebflowConfigurer;
import org.esupportail.cas.adaptors.esupotp.web.flow.EsupOtpMultifactorWebflowConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.webflow.config.FlowDefinitionRegistryBuilder;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.FlowBuilder;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration("esupotpConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class EsupOtpConfiguration {
	
    private static final int WEBFLOW_CONFIGURER_ORDER = 100;
    		
	@Autowired
	private CasConfigurationProperties casProperties;

	@Autowired
	private ConfigurableApplicationContext applicationContext;


	@Autowired
	@Qualifier("loginFlowRegistry")
	private FlowDefinitionRegistry loginFlowDefinitionRegistry;

    @Autowired
    private ObjectProvider<FlowBuilderServices> flowBuilderServices;

    @Autowired
    @Qualifier("flowBuilder")
    private ObjectProvider<FlowBuilder> flowBuilder;
    
    @Autowired
    EsupOtpConfigurationProperties esupOtpConfigurationProperties;
            
	@RefreshScope
	@Bean
	public FlowDefinitionRegistry esupotpFlowRegistry() {
		final FlowDefinitionRegistryBuilder builder = new FlowDefinitionRegistryBuilder(this.applicationContext, flowBuilderServices.getObject());
		builder.addFlowBuilder(flowBuilder.getObject(), EsupOtpMultifactorWebflowConfigurer.MFA_ESUPOTP_EVENT_ID);
		return builder.build();
	}

	@Bean
	public Action esupotpAuthenticationWebflowAction(final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
		final EsupOtpAuthenticationWebflowAction esupOtpAuthenticationWebflowAction = new EsupOtpAuthenticationWebflowAction();
        esupOtpAuthenticationWebflowAction.setEsupotpAuthenticationWebflowEventResolver(esupotpAuthenticationWebflowEventResolver(casWebflowConfigurationContext));
		return esupOtpAuthenticationWebflowAction;
	}

	@Bean
	@RefreshScope public Action esupotpGetTransportsAction(EsupOtpService esupOtpService) {
		final EsupOtpGetTransportsAction a = new EsupOtpGetTransportsAction(applicationContext, esupOtpConfigurationProperties, esupOtpService);
		return a;
	}

    @ConditionalOnMissingBean(name = "esupotpAuthenticationWebflowEventResolver")
	@Bean
	public EsupOtpAuthenticationWebflowEventResolver esupotpAuthenticationWebflowEventResolver(final CasWebflowEventResolutionConfigurationContext casWebflowConfigurationContext) {
		return new EsupOtpAuthenticationWebflowEventResolver(casWebflowConfigurationContext);
	}
    
    @ConditionalOnMissingBean(name = "esupotpMultifactorWebflowConfigurer")
    @Bean
    @DependsOn("defaultWebflowConfigurer")
    public CasWebflowConfigurer esupotpMultifactorWebflowConfigurer() {
        final AbstractCasWebflowConfigurer cfg = new EsupOtpMultifactorWebflowConfigurer(flowBuilderServices.getObject(), loginFlowDefinitionRegistry,
                esupotpFlowRegistry(), applicationContext, casProperties, MultifactorAuthenticationWebflowUtils.getMultifactorAuthenticationWebflowCustomizers(applicationContext));
        cfg.setOrder(WEBFLOW_CONFIGURER_ORDER);
        return cfg;
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "esupotpCasWebflowExecutionPlanConfigurer")
    public CasWebflowExecutionPlanConfigurer mfaSimpleCasWebflowExecutionPlanConfigurer() {
	return plan -> plan.registerWebflowConfigurer(esupotpMultifactorWebflowConfigurer());
    }

    /**                                                                                                                                                                                                            
     * multifactor trust configuration.                                                                                                                                                                 
     */
    @ConditionalOnClass(value = MultifactorAuthnTrustConfiguration.class)
    @ConditionalOnProperty(prefix = "esupotp", name = "trustedDeviceEnabled", havingValue = "true", matchIfMissing = true)
    @Configuration("esupOtpMultifactorTrustConfiguration")
    public class EsupOtpMultifactorTrustConfiguration {

        @ConditionalOnMissingBean(name = "esupotpMultifactorTrustWebflowConfigurer")
        @Bean
        @DependsOn({"defaultWebflowConfigurer", "esupotpMultifactorWebflowConfigurer"})
        public CasWebflowConfigurer esupotpMultifactorTrustWebflowConfigurer() {
        	log.debug("esupotp.trustedDeviceEnabled true, esupotpMultifactorTrustWebflowConfigurer ok");
        	final AbstractCasWebflowConfigurer w =  new EsupOtpMultifactorTrustWebflowConfigurer(flowBuilderServices.getObject(), loginFlowDefinitionRegistry,
                esupotpFlowRegistry(),
                applicationContext, casProperties, MultifactorAuthenticationWebflowUtils.getMultifactorAuthenticationWebflowCustomizers(applicationContext));
        	w.setOrder(WEBFLOW_CONFIGURER_ORDER + 1);
            return w;
        }

        @ConditionalOnMissingBean(name = "esupOtpMultifactorTrustWebflowExecutionPlanConfigurer")
        @Bean
        public CasWebflowExecutionPlanConfigurer casSimpleMultifactorTrustWebflowExecutionPlanConfigurer() {
            return plan -> plan.registerWebflowConfigurer(esupotpMultifactorTrustWebflowConfigurer());
        }
    }


}