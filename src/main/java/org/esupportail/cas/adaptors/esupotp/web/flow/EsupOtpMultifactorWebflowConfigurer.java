package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.configurer.AbstractCasMultifactorWebflowConfigurer;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;
import org.esupportail.cas.adaptors.esupotp.EsupOtpCredential;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.ActionState;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.ViewState;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;


/**
 * This is {@link EsupOtpMultifactorWebflowConfigurer}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
public class EsupOtpMultifactorWebflowConfigurer extends AbstractCasMultifactorWebflowConfigurer {

    public static final String MFA_ESUPOTP_EVENT_ID = "mfa-esupotp";
    
    static final String STATE_ID_GET_INFOS = "getTransportsForm";
    static final String STATE_ID_ASK_CODE_VIEW = "submitCodeFormView";

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
            
            // to store errors & token
            createFlowVariable(flow, CasWebflowConstants.VAR_ID_CREDENTIAL/*"credential"*/, EsupOtpCredential.class);
            
            // call CAS "initialFlowSetupAction" to init various things
            flow.getStartActionList().add(createEvaluateAction(CasWebflowConstants.ACTION_ID_INITIAL_FLOW_SETUP));

            // state #1: start with CAS "initializeLoginForm"
            ActionState initLoginFormState = createActionState(flow, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM,
                createEvaluateAction(CasWebflowConstants.ACTION_ID_INIT_LOGIN_ACTION)); // action triggers "success" event
            createTransitionForState(initLoginFormState, CasWebflowConstants.TRANSITION_ID_SUCCESS, STATE_ID_GET_INFOS);
            setStartState(flow, initLoginFormState);
            createEndState(flow, CasWebflowConstants.STATE_ID_SUCCESS);

            // state #2: get various vars in flow scope
            ActionState transportForm = createActionState(flow, STATE_ID_GET_INFOS,
                createEvaluateAction("esupotpGetTransportsAction")); // mettre dans le flow les params de la vue "esupOtpCodeView" en cas d'evt "authWithCode"
            createTransitionForState(transportForm, CasWebflowConstants.TRANSITION_ID_SUCCESS, STATE_ID_ASK_CODE_VIEW);

            // state #3: prompt OTP (using JS to trigger methods)
            ViewState viewLoginFormState = createViewState(flow, STATE_ID_ASK_CODE_VIEW, "esupOtpCodeView");
            // give access to flow variable "credential" in esupOtpCodeView.html (to store errors & token)
            createStateModelBinding(viewLoginFormState, CasWebflowConstants.VAR_ID_CREDENTIAL/*"credential"*/, EsupOtpCredential.class);
            createTransitionForState(viewLoginFormState, CasWebflowConstants.TRANSITION_ID_SUBMIT,
                CasWebflowConstants.STATE_ID_REAL_SUBMIT, Map.of("bind", Boolean.TRUE, "validate", Boolean.TRUE));

            // state #4: verify OTP
            ActionState realSubmitState = createActionState(flow, CasWebflowConstants.STATE_ID_REAL_SUBMIT,
                createEvaluateAction("esupotpAuthenticationWebflowAction"));

            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_SUCCESS, CasWebflowConstants.STATE_ID_SUCCESS);
            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_ERROR, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM); // go back to state #1

        });        
    	registerMultifactorProviderAuthenticationWebflow(getLoginFlow(), MFA_ESUPOTP_EVENT_ID);
    }
}
