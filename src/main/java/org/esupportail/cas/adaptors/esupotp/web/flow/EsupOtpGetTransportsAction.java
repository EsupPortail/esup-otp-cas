package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apereo.cas.web.flow.actions.AbstractMultifactorAuthenticationAction;
import org.apereo.cas.web.support.WebUtils;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMethod;
import org.esupportail.cas.adaptors.esupotp.EsupOtpMultifactorAuthenticationProvider;
import org.esupportail.cas.adaptors.esupotp.EsupOtpService;
import org.esupportail.cas.adaptors.esupotp.EsupOtpUser;
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
        boolean pushAsked = false;
        boolean webauthnAsked = false;

        requestContext.getFlowScope().put("uid", uid);
        requestContext.getFlowScope().put("userHash", userHash);
        requestContext.getFlowScope().put("divNoCodeDisplay", false);

        JSONObject userInfos = esupOtpService.getUserInfos(uid);
        List<EsupOtpMethod> listMethods = new ArrayList<EsupOtpMethod>();
        EsupOtpUser user = new EsupOtpUser(uid, userHash);

        try {
            JSONObject methods = (JSONObject) ((JSONObject) userInfos.get("user")).get("methods");
            JSONObject transports = (JSONObject) ((JSONObject) userInfos.get("user")).get("transports");

            Boolean defaultWaitingFor = false;
            boolean displayInputCode = false;
            for (String method : methods.keySet()) {
                if ("waitingFor".equals(method)) {
                    defaultWaitingFor = (Boolean) methods.get(method);
                } else if ("codeRequired".equals(method)) {
                	Boolean codeRequired = (Boolean) methods.get(method);
                	log.debug("codeRequired : {}", codeRequired);
                } else {
                	EsupOtpMethod m = new EsupOtpMethod(method, (JSONObject) methods.get(method));
                    listMethods.add(m);
                    if (m.getActive()) {
                        if ("push".equals(m.getName())) {
                            pushAsked = true;
                        } else if ("webauthn".equals(m.getName())) {
                            webauthnAsked = true;
                        } else if ("esupnfc".equals(m.getName())) {
                            // nothing
                        } else {
                            displayInputCode = true;
                        }
                    }
                }
            }

            // will give order to the page to display only WaitingFor block if needed
            requestContext.getFlowScope().put("divNoCodeDisplay", defaultWaitingFor);
            requestContext.getFlowScope().put("pushAsked", pushAsked);
            requestContext.getFlowScope().put("webauthnAsked", webauthnAsked);
            requestContext.getFlowScope().put("displayInputCode", displayInputCode);
            
            user = new EsupOtpUser(uid, userHash, listMethods, transports);
        } catch (JSONException e) {
        	log.error("JSONException ...", e);
        }
        List<Map<String, String>> listTransports = esupOtpService.getTransports(listMethods);

        requestContext.getFlowScope().put("apiUrl", esupOtpConfigurationProperties.getUrlApi());
        requestContext.getFlowScope().put("listTransports", listTransports);
        requestContext.getFlowScope().put("user", user);

        return new EventFactorySupport().event(this, "authWithCode");
    }
}
