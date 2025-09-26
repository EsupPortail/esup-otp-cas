package org.esupportail.cas.adaptors.esupotp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.json.*;

/**
 * This is {@link EsupOtpMethod}.
 *
 * @author Alex Bouskine
 * @since 5.0.0
 */
@Slf4j
public class EsupOtpMethod implements Serializable{
	private static final long serialVersionUID = -8908901132111037L;
    
    private String name;
    private Boolean active;
    private List<String> transports;

    /**
     * Instantiates a new Esup Otp method.
     */
    public EsupOtpMethod() {
    }

    /**
     * Instantiates a new Esup Otp method.
     *
     * @param name the method name
     * @param active is the method active
     * @param transports the transports
     */
    public EsupOtpMethod(final String name, final Boolean active, final List<String> transports) {
    	this.name = name;
    	this.active =  active;
    	this.transports = transports;
    }
    
    public EsupOtpMethod(final String name, JSONObject methodJson) {
    	this.name = name;
    	try{
    		this.active =  (Boolean)methodJson.get("active");
    		this.transports = toStringList((JSONArray)methodJson.get("transports"));
    	}catch(JSONException e){
    		log.error("Error retrieving active and/or transports on JSON for " + name + " : " + methodJson, e);
    	}
    }
    
    public static List<String> toStringList(JSONArray array) throws JSONException {
        if(array == null) {
            return null;
        }
        else {
            List<String> list = new ArrayList<String>();

            for (int i = 0; i < array.length(); i++) {
                list.add(array.get(i).toString());
            }

            return list;
        }
    }
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public List<String> getTransports() {
		return transports;
	}

	public void setTransports(List<String> transports) {
		this.transports = transports;
	}
    
    
}
