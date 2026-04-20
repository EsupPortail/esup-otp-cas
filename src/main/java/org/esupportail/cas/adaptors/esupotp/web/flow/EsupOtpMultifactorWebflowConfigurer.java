package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.flow.configurer.AbstractCasMultifactorWebflowConfigurer;
import org.apereo.cas.web.flow.configurer.CasMultifactorWebflowCustomizer;
import org.esupportail.cas.adaptors.esupotp.EsupOtpCredential;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.execution.Action;
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

    static final String STATE_ID_GET_INFOS = "getTransportsForm";
    static final String STATE_ID_ASK_CODE_VIEW = "submitCodeFormView";

    private final String authenticationWebflowActionBeanName;
    private final String getTransportsActionBeanName;

    public EsupOtpMultifactorWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                                           final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                           final FlowDefinitionRegistry flowDefinitionRegistry,
                                                           final ConfigurableApplicationContext applicationContext,
                                                           final CasConfigurationProperties casProperties,
                                                           final List<CasMultifactorWebflowCustomizer> mfaFlowCustomizers) {
        this(flowBuilderServices, loginFlowDefinitionRegistry, flowDefinitionRegistry, applicationContext, casProperties,
            mfaFlowCustomizers, "esupotpAuthenticationWebflowAction", "esupotpGetTransportsAction");
    }

    public EsupOtpMultifactorWebflowConfigurer(final FlowBuilderServices flowBuilderServices,
                                                           final FlowDefinitionRegistry loginFlowDefinitionRegistry,
                                                           final FlowDefinitionRegistry flowDefinitionRegistry,
                                                           final ConfigurableApplicationContext applicationContext,
                                                           final CasConfigurationProperties casProperties,
                                                           final List<CasMultifactorWebflowCustomizer> mfaFlowCustomizers,
                                                           final String authenticationWebflowActionBeanName,
                                                           final String getTransportsActionBeanName) {
        super(flowBuilderServices, loginFlowDefinitionRegistry, applicationContext, casProperties, Optional.of(flowDefinitionRegistry), mfaFlowCustomizers);
        this.authenticationWebflowActionBeanName = authenticationWebflowActionBeanName;
        this.getTransportsActionBeanName = getTransportsActionBeanName;
    }
    
    @Override
    protected void doInitialize() {

	multifactorAuthenticationFlowDefinitionRegistries.forEach(registry -> {
            final String[] flowDefinitionIds = registry.getFlowDefinitionIds();
            if (flowDefinitionIds.length != 1) {
                throw new IllegalStateException("Expected exactly one ESUP-OTP flow definition but found " + flowDefinitionIds.length);
            }
            final String flowId = flowDefinitionIds[0];
            final Flow flow = getFlow(registry, flowId);

            // to store errors & token
            createFlowVariable(flow, CasWebflowConstants.VAR_ID_CREDENTIAL/*"credential"*/, EsupOtpCredential.class);
            
            // call CAS "initialFlowSetupAction" to init various things
            final var startActionList = Objects.requireNonNull(flow.getStartActionList(),
                () -> "Flow start action list is not available for ESUP-OTP flow '" + flowId + "'.");
            startActionList.add(createEvaluateAction(CasWebflowConstants.ACTION_ID_INITIAL_FLOW_SETUP));

            // state #1: start with CAS "initializeLoginForm"
            ActionState initLoginFormState = createActionState(flow, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM,
                createEvaluateAction(CasWebflowConstants.ACTION_ID_INIT_LOGIN_ACTION)); // action triggers "success" event
            createTransitionForState(initLoginFormState, CasWebflowConstants.TRANSITION_ID_SUCCESS, STATE_ID_GET_INFOS);
            setStartState(flow, initLoginFormState);
            createEndState(flow, CasWebflowConstants.STATE_ID_SUCCESS);

            // state #2: get various vars in flow scope
            ActionState transportForm = createActionState(flow, STATE_ID_GET_INFOS,
                resolveActionBean(getTransportsActionBeanName)); // mettre dans le flow les params de la vue "esupOtpCodeView" en cas d'evt "authWithCode"
            createTransitionForState(transportForm, CasWebflowConstants.TRANSITION_ID_SUCCESS, STATE_ID_ASK_CODE_VIEW);

            // state #3: prompt OTP (using JS to trigger methods)
            ViewState viewLoginFormState = createViewState(flow, STATE_ID_ASK_CODE_VIEW, "esupOtpCodeView");
            // give access to flow variable "credential" in esupOtpCodeView.html (to store errors & token)
            createStateModelBinding(viewLoginFormState, CasWebflowConstants.VAR_ID_CREDENTIAL/*"credential"*/, EsupOtpCredential.class);
            createTransitionForState(viewLoginFormState, CasWebflowConstants.TRANSITION_ID_SUBMIT,
                CasWebflowConstants.STATE_ID_REAL_SUBMIT, Map.of("bind", Boolean.TRUE, "validate", Boolean.TRUE));

            // state #4: verify OTP
            ActionState realSubmitState = createActionState(flow, CasWebflowConstants.STATE_ID_REAL_SUBMIT,
                resolveActionBean(authenticationWebflowActionBeanName));

            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_SUCCESS, CasWebflowConstants.STATE_ID_SUCCESS);
            createTransitionForState(realSubmitState, CasWebflowConstants.TRANSITION_ID_ERROR, CasWebflowConstants.STATE_ID_INIT_LOGIN_FORM); // go back to state #1

            registerMultifactorProviderAuthenticationWebflow(getLoginFlow(), flowId);

        });
    }

    private Action resolveActionBean(final String beanName) {
        return getApplicationContext().getBean(beanName, Action.class);
    }
}
