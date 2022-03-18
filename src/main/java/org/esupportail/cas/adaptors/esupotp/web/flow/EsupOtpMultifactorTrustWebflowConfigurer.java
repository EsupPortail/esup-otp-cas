package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.trusted.web.flow.AbstractMultifactorTrustedDeviceWebflowConfigurer;
import org.apereo.cas.trusted.web.flow.MultifactorAuthenticationTrustBean;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;
import org.apereo.cas.web.support.WebUtils;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;

import lombok.val;


public class EsupOtpMultifactorTrustWebflowConfigurer extends AbstractMultifactorTrustedDeviceWebflowConfigurer {
	
	private Boolean isDeviceRegistrationRequired = false;

    private long deviceRegistrationExpirationInDays;

    private final FlowDefinitionRegistry flowDefinitionRegistry;

    public EsupOtpMultifactorTrustWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                                      final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                      final EsupOtpConfigurationProperties esupOtpConfigurationProperties,
                                                      final FlowDefinitionRegistry flowDefinitionRegistry,
                                                      final ConfigurableApplicationContext applicationContext,
                                                      final CasConfigurationProperties casProperties,
                                                      final List<CasMultifactorWebflowCustomizer> mfaFlowCustomizers) {
        super(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties, Optional.of(flowDefinitionRegistry), mfaFlowCustomizers);
        this.flowDefinitionRegistry = flowDefinitionRegistry;
        this.isDeviceRegistrationRequired = esupOtpConfigurationProperties.getIsDeviceRegistrationRequired();
        this.deviceRegistrationExpirationInDays = esupOtpConfigurationProperties.getDeviceRegistrationExpirationInDays();
    }

    @Override
    protected void doInitialize() {      
        if(!isDeviceRegistrationRequired) {
            // Hack : override register trusted device flow AbstractMultifactorTrustedDeviceWebflowConfigurer.registerMultifactorTrustedAuthentication
            // -> with this, we auto register device (without form) if isDeviceRegistrationRequired=false
            val flowId = Arrays.stream(flowDefinitionRegistry.getFlowDefinitionIds()).findFirst().get();
            val flow = (Flow) flowDefinitionRegistry.getFlowDefinition(flowId);
            val registerAction = createActionState(flow, CasWebflowConstants.STATE_ID_PREPARE_REGISTER_TRUSTED_DEVICE, "mfaSetTrustAction");
            createStateDefaultTransition(registerAction, CasWebflowConstants.STATE_ID_SUCCESS);
            flow.getStartActionList().add(requestContext -> {
	        	val deviceBean = WebUtils.getMultifactorAuthenticationTrustRecord(requestContext, MultifactorAuthenticationTrustBean.class);
	            val deviceRecord = deviceBean.get();
	            deviceRecord.setDeviceName("auto-device-registration");
                if(deviceRegistrationExpirationInDays>-1) {
                    deviceRecord.setExpiration(deviceRegistrationExpirationInDays);
                    deviceRecord.setTimeUnit(ChronoUnit.DAYS);
                }
	            return null;
	        });
        } 
        registerMultifactorTrustedAuthentication();
    }

}
