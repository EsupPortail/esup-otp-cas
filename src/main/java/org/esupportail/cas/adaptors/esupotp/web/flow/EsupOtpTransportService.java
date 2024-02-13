package org.esupportail.cas.adaptors.esupotp.web.flow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import org.esupportail.cas.adaptors.esupotp.EsupOtpCredential;
import org.esupportail.cas.config.EsupOtpConfigurationProperties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
@RefreshScope
@Component("esupotpTransportService")
public class EsupOtpTransportService {
	
    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    EsupOtpConfigurationProperties esupOtpConfigurationProperties;
    
	public String sendCode(EsupOtpCredential credential) {
        try {
                JSONObject response = sendCodeRequest(credential.getUid(), credential.getUserHash(), credential.getTransport(), credential.getMethod());
                if(response.getString("code").equals("Ok"))return "success";
        } catch (NoSuchAlgorithmException|IOException e) {
        	logger.error("sendCode failed", e);
        } 
        return "error";
	}

	JSONObject sendCodeRequest(String uid, String userHash, String transport, String method) throws IOException, NoSuchAlgorithmException {
		String url = esupOtpConfigurationProperties.getUrlApi() + "/users/" + uid + "/methods/" + method + "/transports/"+ transport + "/" + userHash;
		URL obj = new URL(url);
		HttpURLConnection con = null;
		con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
        logger.info("mfa-esupotp request send to [{}]", (String) url);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return new JSONObject(response.toString());
	}
}
