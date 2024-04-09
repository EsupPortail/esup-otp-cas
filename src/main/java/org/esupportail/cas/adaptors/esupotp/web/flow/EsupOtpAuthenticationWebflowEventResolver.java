package org.esupportail.cas.adaptors.esupotp.web.flow;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.web.flow.authentication.BaseMultifactorAuthenticationProviderEventResolver;
import org.apereo.cas.web.flow.resolver.impl.CasWebflowEventResolutionConfigurationContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import java.util.Set;

/**
 * This is {@link EsupOtpAuthenticationWebflowEventResolver}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
@Slf4j
public class EsupOtpAuthenticationWebflowEventResolver extends BaseMultifactorAuthenticationProviderEventResolver {

	public EsupOtpAuthenticationWebflowEventResolver(CasWebflowEventResolutionConfigurationContext webflowEventResolutionConfigurationContext) {
		super(webflowEventResolutionConfigurationContext);
	}

	@Override
	public Set<Event> resolveInternal(RequestContext requestContext) {
		return handleAuthenticationTransactionAndGrantTicketGrantingTicket(requestContext);
	}

}
