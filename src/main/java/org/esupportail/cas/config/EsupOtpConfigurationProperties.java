package org.esupportail.cas.config;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.configuration.model.support.mfa.BaseMultifactorAuthenticationProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EsupOtpConfigurationProperties extends BaseMultifactorAuthenticationProviderProperties implements InitializingBean {
	@Serial
	private static final long serialVersionUID = 1L;

	private final Logger log = LoggerFactory.getLogger(getClass());

	String urlApi;

	String usersSecret;

	String apiPassword;

	String otpManagerUrl;

	Boolean byPassIfNoEsupOtpMethodIsActive = true;

	public EsupOtpConfigurationProperties() {
	}

	@Override
	public void afterPropertiesSet() {
		if (StringUtils.isBlank(getName())) {
			throw new IllegalStateException("The [esupotp.name] property must be set for each instance in esupotp.properties.");
		}
		setId(getName());
		if (urlApi == null) throw new IllegalStateException("The [esupotp.urlApi] property must be set for each instance in esupotp.properties.");
		if (usersSecret == null) throw new IllegalStateException("The [esupotp.usersSecret] property must be set for each instance in esupotp.properties.");
		if (apiPassword == null) throw new IllegalStateException("The [esupotp.apiPassword] property must be set for each instance in esupotp.properties.");
		if (otpManagerUrl == null) throw new IllegalStateException("The [esupotp.otpManagerUrl] property must be set for each instance in esupotp.properties.");
		log.info("id : {}", getId());
		log.info("name : {}", getName());
		log.info("rank : {}", this.getRank());
		log.info("urlApi : {}", urlApi);
		log.info("usersSecret : {}", usersSecret);
		log.info("apiPassword : {}", apiPassword);
		log.info("byPassIfNoEsupOtpMethodIsActive : {}", byPassIfNoEsupOtpMethodIsActive);
		log.info("otpManagerUrl : {}", otpManagerUrl);
		log.info("failureMode : {}", getFailureMode());
	}
}
