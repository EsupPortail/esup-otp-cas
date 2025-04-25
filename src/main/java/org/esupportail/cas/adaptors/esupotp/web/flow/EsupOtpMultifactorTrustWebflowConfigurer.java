package org.esupportail.cas.adaptors.esupotp.web.flow;

import lombok.val;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.trusted.util.MultifactorAuthenticationTrustUtils;
import org.apereo.cas.trusted.web.flow.AbstractMultifactorTrustedDeviceWebflowConfigurer;
import org.apereo.cas.trusted.web.flow.MultifactorAuthenticationTrustBean;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;
import org.apereo.cas.web.support.CookieUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class EsupOtpMultifactorTrustWebflowConfigurer extends AbstractMultifactorTrustedDeviceWebflowConfigurer {

    private final long deviceRegistrationExpirationInSeconds;
    private final FlowDefinitionRegistry flowDefinitionRegistry;

    public EsupOtpMultifactorTrustWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                                      final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                      final FlowDefinitionRegistry flowDefinitionRegistry,
                                                      final ConfigurableApplicationContext applicationContext,
                                                      final CasConfigurationProperties casProperties,
                                                      final List<CasMultifactorWebflowCustomizer> mfaFlowCustomizers) {
        super(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties, Optional.of(flowDefinitionRegistry), mfaFlowCustomizers);
        this.flowDefinitionRegistry = flowDefinitionRegistry;
        this.deviceRegistrationExpirationInSeconds = CookieUtils.getCookieMaxAge(casProperties.getAuthn().getMfa().getTrusted().getDeviceFingerprint().getCookie().getMaxAge());
    }

    @Override
    protected void doInitialize() {
        registerMultifactorTrustedAuthentication();
        // Hack : CAS don't set the deviceRegistrationExpiration in the MultifactorAuthenticationTrustBean
        // so we set it here if cas.authn.mfa.trusted.device-fingerprint.cookie.max-age is set (>-1)
        if(deviceRegistrationExpirationInSeconds > -1) {
            val flowId = Arrays.stream(flowDefinitionRegistry.getFlowDefinitionIds()).findFirst().get();
            val flow = (Flow) flowDefinitionRegistry.getFlowDefinition(flowId);
            flow.getStartActionList().add(requestContext -> {
                val deviceBean = MultifactorAuthenticationTrustUtils.getMultifactorAuthenticationTrustRecord(requestContext, MultifactorAuthenticationTrustBean.class);
                val deviceRecord = deviceBean.get();
                deviceRecord.setExpiration(deviceRegistrationExpirationInSeconds);
                deviceRecord.setTimeUnit(ChronoUnit.SECONDS);
                return null;
            });
        }
    }

}
