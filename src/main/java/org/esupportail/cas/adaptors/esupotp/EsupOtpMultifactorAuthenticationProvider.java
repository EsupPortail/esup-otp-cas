package org.esupportail.cas.adaptors.esupotp;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.authentication.AbstractMultifactorAuthenticationProvider;
import org.apereo.cas.services.RegisteredService;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;

/**
 * Represents the ESUP-OTP MFA provider in CAS and supplies the default provider identifier when none is configured.
 *
 * @author Alex Bouskine
 */
public class EsupOtpMultifactorAuthenticationProvider extends AbstractMultifactorAuthenticationProvider {

    private static final long serialVersionUID = 4789727148634156909L;
        
    @Override
    public String getId() {
        return StringUtils.defaultIfBlank(super.getId(), EsupOtpConfigurationProperties.DEFAULT_IDENTIFIER);
    }

    @Override
    public boolean isAvailable(final RegisteredService service) {
        return true;
    }

    @Override
    public String getFriendlyName() {
        return "Esup OTP MFA";
    }
}
