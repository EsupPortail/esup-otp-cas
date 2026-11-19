package org.esupportail.cas.adaptors.esupotp.web.flow;

import lombok.RequiredArgsConstructor;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.web.flow.actions.BaseCasWebflowAction;
import org.apereo.cas.web.flow.resolver.CasWebflowEventResolver;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * Delegates OTP submission to the ESUP-OTP event resolver and returns the resulting webflow event.
 *
 * @author Alex Bouskine
 */
public class EsupOtpAuthenticationWebflowAction extends BaseCasWebflowAction {
	CasWebflowEventResolver esupotpAuthenticationWebflowEventResolver;

    @Override
    protected Event doExecuteInternal(final RequestContext requestContext) throws Exception {
        return FunctionUtils.doUnchecked(() -> this.esupotpAuthenticationWebflowEventResolver.resolveSingle(requestContext));
    }

    public void setEsupotpAuthenticationWebflowEventResolver(CasWebflowEventResolver esupotpAuthenticationWebflowEventResolver) {
        this.esupotpAuthenticationWebflowEventResolver = esupotpAuthenticationWebflowEventResolver;
    }
}