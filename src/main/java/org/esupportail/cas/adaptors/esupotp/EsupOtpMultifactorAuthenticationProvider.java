package org.esupportail.cas.adaptors.esupotp;

import org.apereo.cas.authentication.AbstractMultifactorAuthenticationProvider;
import org.apereo.cas.services.RegisteredService;

/**
 * The authentication provider
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
public class EsupOtpMultifactorAuthenticationProvider extends AbstractMultifactorAuthenticationProvider {

    private static final long serialVersionUID = 4789727148634156909L;
        
    @Override
    public String getId() {
        return super.getId();
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
