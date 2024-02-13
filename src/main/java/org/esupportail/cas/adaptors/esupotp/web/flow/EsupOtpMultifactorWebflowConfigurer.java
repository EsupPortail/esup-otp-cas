package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.val;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.configurer.AbstractCasMultifactorWebflowConfigurer;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;
import org.apereo.cas.util.CollectionUtils;
import org.esupportail.cas.adaptors.esupotp.EsupOtpCredential;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.action.SetAction;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.ViewState;
import org.springframework.webflow.engine.builder.BinderConfiguration;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;


/**
 * This is {@link EsupOtpMultifactorWebflowConfigurer}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
public class EsupOtpMultifactorWebflowConfigurer extends AbstractCasMultifactorWebflowConfigurer {

    public static final String MFA_ESUPOTP_EVENT_ID = "mfa-esupotp";
    
    static final String STATE_ID_TRANSPORT_FORM = "getTransportsForm";
    

    public EsupOtpMultifactorWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                                           final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                           final FlowDefinitionRegistry flowDefinitionRegistry,
                                                           final ConfigurableApplicationContext applicationContext,
                                                           final CasConfigurationProperties casProperties,
                                                           final List<CasMultifactorWebflowCustomizer> mfaFlowCustomizers) {
        super(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties, Optional.of(flowDefinitionRegistry), mfaFlowCustomizers);;
    }
    
    @Override
    protected void doInitialize() {

    	multifactorAuthenticationFlowDefinitionRegistries.forEach(registry -> {
    		
            Flow flow = getFlow(registry, MFA_ESUPOTP_EVENT_ID);
            
            createFlowVariable(flow, CasWebflowConstants.VAR_ID_CREDENTIAL, EsupOtpCredential.class);
            
            flow.getStartActionList().add(createEvaluateAction(CasWebflowConstants.ACTION_ID_INITIAL_FLOW_SETUP));

            ActionState initLoginFormState = createActionState(flow, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM,
                createEvaluateAction(CasWebflowConstants.ACTION_ID_INIT_LOGIN_ACTION));
            createTransitionForState(initLoginFormState, CasWebflowConstants.TRANSITION_ID_SUCCESS, STATE_ID_TRANSPORT_FORM);
            
            setStartState(flow, initLoginFormState);
            createEndState(flow, CasWebflowConstants.STATE_ID_SUCCESS);

            ActionState transportForm = createActionState(flow, STATE_ID_TRANSPORT_FORM,
                createEvaluateAction("esupotpGetTransportsAction"));
            createTransitionForState(transportForm, "authWithoutCode", "noCodeWaitingView");
            createTransitionForState(transportForm, "authWithCode", "submitCodeFormView");
            
            ViewState noCodeWaitingViewState = createViewState(flow, "noCodeWaitingView", "casEsupOtpNoCodeView");
            createTransitionForState(noCodeWaitingViewState, CasWebflowConstants.TRANSITION_ID_SUBMIT, "submitTransportEsupOtp");

            SetAction setPrincipalAction = createSetAction("viewScope.principal", "conversationScope.authentication.principal");
            List<String> propertiesToBind = CollectionUtils.wrapList("token", "transport", "method", "uid", "userHash");
            BinderConfiguration binder = createStateBinderConfiguration(propertiesToBind);
            ViewState viewLoginFormState = createViewState(flow, "submitCodeFormView",
            		"esupOtpCodeView", binder);
            createStateModelBinding(viewLoginFormState, CasWebflowConstants.VAR_ID_CREDENTIAL, EsupOtpCredential.class);

            viewLoginFormState.getEntryActionList().add(setPrincipalAction);
            createTransitionForState(viewLoginFormState, CasWebflowConstants.TRANSITION_ID_SUBMIT,
                CasWebflowConstants.STATE_ID_REAL_SUBMIT, Map.of("bind", Boolean.TRUE, "validate", Boolean.FALSE));

            createTransitionForState(viewLoginFormState, "submitCallTransport", "submitTransportEsupOtp");
            
            ActionState submitTransportEsupOtpState = createActionState(flow, "submitTransportEsupOtp",
                    createEvaluateAction("esupotpTransportService.sendCode(credential)"));

            createTransitionForState(submitTransportEsupOtpState, CasWebflowConstants.TRANSITION_ID_SUCCESS, "successView");
            
            createTransitionForState(submitTransportEsupOtpState, CasWebflowConstants.TRANSITION_ID_ERROR, "errorView");
            
            createViewState(flow, "successView", "fragments/esupOtpSuccessView");
            createViewState(flow, "errorView", "fragments/esupOtpErrorView");

            ActionState realSubmitState = createActionState(flow, CasWebflowConstants.STATE_ID_REAL_SUBMIT,
                createEvaluateAction("esupotpAuthenticationWebflowAction"));
            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_SUCCESS, CasWebflowConstants.STATE_ID_SUCCESS);
            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_ERROR, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM);
        });        
    	registerMultifactorProviderAuthenticationWebflow(getLoginFlow(), MFA_ESUPOTP_EVENT_ID);
    }
}
