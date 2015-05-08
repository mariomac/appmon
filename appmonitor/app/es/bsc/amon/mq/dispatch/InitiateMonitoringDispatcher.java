package es.bsc.amon.mq.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bsc.amon.mq.CommandDispatcher;
import play.Logger;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.*;

public class InitiateMonitoringDispatcher implements CommandDispatcher {


	private static final String FIELD_APP_ID = "ApplicationId";
	private static final String FIELD_DEPLOYMENT_ID = "DeploymentId";
	private static final String FIELD_TERMS = "Terms";
	private static final String FIELD_FREQUENCY = "Frequency";
	private static final String FIELD_SLA_ID = "SLAId";

	private static final long DEFAULT_FREQUENCY = 5*60*1000;


	private Session session;

	public InitiateMonitoringDispatcher(Session session) {
		this.session = session;
	}

	private static String getString(ObjectNode n, String field) {
		JsonNode jn = n.get(field);
		return jn == null ? null : jn.textValue();
	}


	@Override
	public void onCommand(ObjectNode msgBody) {
		try {
			String appId = getString(msgBody,FIELD_APP_ID);
			String deploymentId = getString(msgBody, FIELD_DEPLOYMENT_ID);
			String slaId = getString(msgBody, FIELD_SLA_ID);

			if(appId == null && deploymentId == null && slaId == null) {
				Exception ife = new IllegalArgumentException("appId == null && deploymentId == null && slaId == null");
				throw ife;
			}

			JsonNode termsJson = msgBody.get(FIELD_TERMS);
			List<String> terms = new ArrayList<String>();

			if(termsJson != null && termsJson.isTextual()) {
				terms.add(termsJson.textValue());
			} else if(termsJson != null && termsJson.isArray()) {
				for(JsonNode jn :(termsJson)) {
					if(jn.isTextual()) {
						terms.add(jn.textValue());
					}
				}
			}

			if(termsJson == null || terms.size() == 0) {
				throw new IllegalArgumentException("There are no valid SLA terms: " + termsJson);
			}

			JsonNode freqJson = msgBody.get(FIELD_FREQUENCY);
			long frequency = freqJson == null ? DEFAULT_FREQUENCY : freqJson.asLong(DEFAULT_FREQUENCY);

			AppMeasuresNotifier amn = new AppMeasuresNotifier(appId,deploymentId, slaId, terms.toArray(new String[terms.size()]),frequency);


			// TODO: METER TODA LA MIERDACA A CONTINUACIÓN EN LA CLASE APPMEASUERESNOTIFIER





		} catch(IllegalArgumentException e ) {
			Logger.debug("Bad command format: " + e.getMessage());
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
	}
}
