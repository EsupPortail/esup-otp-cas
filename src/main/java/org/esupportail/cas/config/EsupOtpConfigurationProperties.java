package org.esupportail.cas.config;

import java.util.ArrayList;
import java.util.List;

import org.apereo.cas.configuration.model.support.mfa.BaseMultifactorAuthenticationProviderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@PropertySource(ignoreResourceNotFound = true, value={"classpath:esupotp.properties", "file:/var/cas/config/esupotp.properties", "file:/opt/cas/config/esupotp.properties", "file:/etc/cas/config/esupotp.properties", "file:${cas.standalone.configurationDirectory}/esupotp.properties"})
@ConfigurationProperties(prefix = "esupotp", ignoreUnknownFields = false)
public class EsupOtpConfigurationProperties extends BaseMultifactorAuthenticationProviderProperties implements InitializingBean {

	private static final long serialVersionUID = 1L;

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	String urlApi = "CAS";
	
	String usersSecret = "CAS";
	
	String apiPassword = "CAS";
	
	Boolean byPassIfNoEsupOtpMethodIsActive = true;
	
	List<Long> byPassServicesIfNoEsupOtpMethodIsActive = new ArrayList<Long>();
	
	Boolean trustedDeviceEnabled = true;
	
	Boolean isDeviceRegistrationRequired = false;

	long deviceRegistrationExpirationInDays = -1;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		log.info("rank : {}", this.getRank());
		log.info("urlApi : {}", urlApi); 
		log.info("usersSecret : {}", usersSecret); 
		log.info("apiPassword : {}", apiPassword);
		log.info("byPassIfNoEsupOtpMethodIsActive : {}", byPassIfNoEsupOtpMethodIsActive);
		log.info("byPassServicesIfNoEsupOtpMethodIsActive : {}", byPassServicesIfNoEsupOtpMethodIsActive);
		log.info("trustedDeviceEnabled : {}", trustedDeviceEnabled);
		log.info("isDeviceRegistrationRequired : {}", isDeviceRegistrationRequired);
		log.info("deviceRegistrationExpirationInDays : {}", deviceRegistrationExpirationInDays);
	}
	
}
