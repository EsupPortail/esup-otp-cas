package org.esupportail.cas.adaptors.esupotp;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import javax.security.auth.login.FailedLoginException;

import org.apereo.cas.authentication.AuthenticationHandlerExecutionResult;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.PreventedException;
import org.apereo.cas.authentication.handler.support.AbstractPreAndPostProcessingAuthenticationHandler;
import org.apereo.cas.authentication.principal.PrincipalFactory;
import org.apereo.cas.authentication.principal.Service;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.web.support.WebUtils;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONObject;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * An authentication handler that uses the token provided to authenticator
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
@Slf4j
public class EsupOtpAuthenticationHandler extends AbstractPreAndPostProcessingAuthenticationHandler {

    EsupOtpConfigurationProperties esupOtpConfigurationProperties;
    
    EsupOtpService esupOtpService;
    
	/**
	 * Instantiates a new Esup otp authentication handler.
	 */
    public EsupOtpAuthenticationHandler(final String name, final ServicesManager servicesManager, final PrincipalFactory principalFactory, final EsupOtpConfigurationProperties esupOtpConfigurationProperties, EsupOtpService esupOtpService) {
    	super(name, servicesManager, principalFactory, esupOtpConfigurationProperties.getRank());
    	this.esupOtpConfigurationProperties = esupOtpConfigurationProperties;
    	this.esupOtpService = esupOtpService;
    }

	@Override
	protected AuthenticationHandlerExecutionResult doAuthentication(final Credential credential, Service service)
			throws GeneralSecurityException, PreventedException {
		final EsupOtpCredential esupotpCredential = (EsupOtpCredential) credential;
		final String otp = esupotpCredential.getToken();
		final RequestContext context = RequestContextHolder.getRequestContext();
		final String uid = WebUtils.getAuthentication(context).getPrincipal().getId();

		try {
			JSONObject response = esupOtpService.verifyOtp(uid, otp);
			if(response.getString("code").equals("Ok")){
				return createHandlerResult(esupotpCredential, this.principalFactory.createPrincipal(uid), new ArrayList<>(0));
			}
		} catch (IOException e) {
			 log.error("doAuthentication failed", e);
		}
		throw new FailedLoginException("Failed to authenticate code " + otp);
	}

	@Override
	public boolean supports(final Credential credential) {
		return EsupOtpCredential.class.isAssignableFrom(credential.getClass());
	}

}
