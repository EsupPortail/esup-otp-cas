package org.esupportail.cas.adaptors.esupotp;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apereo.cas.authentication.credential.AbstractCredential;

/**
 * This is {@link EsupOtpCredential}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
public class EsupOtpCredential extends AbstractCredential {
	
    private static final long serialVersionUID = -7570600701132111037L;

    private String token;

    public EsupOtpCredential() {
    }

    @Override
    public String getId() {
        return this.token;
    }


    public String getToken() {
        return this.token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return token;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(token)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EsupOtpCredential other = (EsupOtpCredential) obj;
        return new EqualsBuilder()
            .append(token, other.token)
            .isEquals();
    }

}
