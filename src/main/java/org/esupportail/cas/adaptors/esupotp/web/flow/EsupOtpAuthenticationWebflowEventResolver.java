package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.Set;

import org.apereo.cas.web.flow.authentication.BaseMultifactorAuthenticationProviderEventResolver;
import org.apereo.cas.web.flow.resolver.impl.CasWebflowEventResolutionConfigurationContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Runs the CAS authentication transaction for the ESUP-OTP credential and resolves the resulting webflow event.
 *
 * @author Alex Bouskine
 */
@Slf4j
public class EsupOtpAuthenticationWebflowEventResolver extends BaseMultifactorAuthenticationProviderEventResolver {

	public EsupOtpAuthenticationWebflowEventResolver(CasWebflowEventResolutionConfigurationContext webflowEventResolutionConfigurationContext) {
		super(webflowEventResolutionConfigurationContext);
	}

	@Override
	public Set<Event> resolveInternal(RequestContext requestContext) throws Throwable {
		return handleAuthenticationTransactionAndGrantTicketGrantingTicket(requestContext);
	}

}
