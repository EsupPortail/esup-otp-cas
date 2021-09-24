package org.esupportail.cas.authentication;

import org.jasig.cas.authentication.UsernamePasswordCredential;
import org.jasig.cas.authentication.HandlerResult;
import org.jasig.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.SimplePrincipal;

import org.json.*;

import javax.security.auth.login.FailedLoginException;
import org.jasig.cas.authentication.PreventedException;
import java.security.GeneralSecurityException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import java.security.MessageDigest;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.Calendar;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BypassEsupOtpApiAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public static String urlApi;

	private static String apiPassword;
	
	public static String usersSecret;


	@Override
	protected final HandlerResult authenticateUsernamePasswordInternal(final UsernamePasswordCredential credential)throws GeneralSecurityException, PreventedException{
		try{
			JSONObject methods = (JSONObject) ((JSONObject) getUserInfos(credential.getUsername()).get("user")).get("methods");
			for (Object method : methods.keySet()) {
				if(methods.get(method.toString()) instanceof JSONObject){
					if((boolean) (((JSONObject)methods.get(method.toString())).get("active")))throw new FailedLoginException();
				}
			}
			return createHandlerResult(credential, this.principalFactory.createPrincipal(credential.getUsername()), null);
		}catch(IOException e){
			logger.info("HTTP Request error", e);
			throw new PreventedException("HTTP Request error", e);
		}
	}

	private JSONObject getUserInfos(String uid) throws IOException, NoSuchAlgorithmException {
		String url = urlApi + "/users/" + uid + "/" + getUserHash(uid);
		URL obj = new URL(url);
		int responseCode;
		HttpURLConnection con = null;
		con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		responseCode = con.getResponseCode();
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return new JSONObject(response.toString());
	}
	
    public String getUserHash(String uid) throws NoSuchAlgorithmException, UnsupportedEncodingException{
    	MessageDigest md5Md = MessageDigest.getInstance("MD5");
		String md5 = (new HexBinaryAdapter()).marshal(md5Md.digest(usersSecret.getBytes()));
		md5 = md5.toLowerCase();
    	String salt = md5+getSalt(uid);
    	MessageDigest sha256Md = MessageDigest.getInstance("SHA-256");
		String userHash = (new HexBinaryAdapter()).marshal(sha256Md.digest(salt.getBytes()));
		userHash = userHash.toLowerCase();
    	return userHash; 
    }
    
    public String getSalt(String uid){
    	Calendar calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
    	int day = calendar.get(Calendar.DAY_OF_MONTH);
    	int hour = calendar.get(Calendar.HOUR_OF_DAY);
    	String salt = uid+day+hour;
    	return salt; 
    }
    
    public void setUrlApi(String urlApi) {
    	this.urlApi = urlApi; 
 	}

 	public void setApiPassword(String apiPassword) {
    	this.apiPassword = apiPassword;
    }	
 	
 	public String getUsersSecret() {
    	return usersSecret; 
    }

    public void setUsersSecret(String usersSecret) {
     	this.usersSecret = usersSecret; 
 	}
}
