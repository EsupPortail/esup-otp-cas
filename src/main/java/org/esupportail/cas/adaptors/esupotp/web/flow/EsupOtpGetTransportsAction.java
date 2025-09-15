package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apereo.cas.web.flow.actions.AbstractMultifactorAuthenticationAction;
import org.apereo.cas.web.support.WebUtils;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMultifactorAuthenticationProvider;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.action.EventFactorySupport;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

import lombok.extern.slf4j.Slf4j;

/**
 * This is {@link EsupOtpGetTransportsAction}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
@Slf4j
public class EsupOtpGetTransportsAction extends AbstractMultifactorAuthenticationAction<EsupOtpMultifactorAuthenticationProvider> {

    EsupOtpConfigurationProperties esupOtpConfigurationProperties;

    EsupOtpService esupOtpService;

    public EsupOtpGetTransportsAction(ApplicationContext applicationContext,
			EsupOtpConfigurationProperties esupOtpConfigurationProperties, EsupOtpService esupOtpService) {
		super();
		this.esupOtpConfigurationProperties = esupOtpConfigurationProperties;
		this.esupOtpService = esupOtpService;
	}

	@Override
    protected Event doExecuteInternal(final RequestContext requestContext) throws Exception {
        final RequestContext context = RequestContextHolder.getRequestContext();
        final String uid = WebUtils.getAuthentication(context).getPrincipal().getId();
        String userHash = esupOtpService.getUserHash(uid);

        requestContext.getFlowScope().put("uid", uid);
        requestContext.getFlowScope().put("userHash", userHash);
        requestContext.getFlowScope().put("apiUrl", esupOtpConfigurationProperties.getUrlApi());
        requestContext.getFlowScope().put("otpManagerUrl", esupOtpConfigurationProperties.getOtpManagerUrl());

        return success();
    }
}
