package es.bsc.amon.mq.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bsc.amon.controller.EventsDBMapper;
import es.bsc.amon.controller.QueriesDBMapper;
import es.bsc.amon.mq.ActiveMqAdapter;
import es.bsc.amon.mq.MQManager;
import es.bsc.amon.mq.notif.PeriodicNotificationException;
import es.bsc.amon.mq.notif.PeriodicNotifier;
import play.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

class AppMeasuresNotifier implements PeriodicNotifier {
	final String appId;
	final String deploymentId;
	final String slaId;
	final String[] terms;
	final long frequency;
	String queryHead, queryTail;

	final ActiveMqAdapter messageQueue;
	final String topicName;

	public AppMeasuresNotifier(String appId, String deploymentId, String slaId, String[] terms, long frequency) throws PeriodicNotificationException {
		try {
			this.appId = appId;
			this.deploymentId = deploymentId;
			this.slaId = slaId;
			this.terms = terms;
			this.frequency = frequency;
			topicName = TOPIC_PREFIX + appId + TOPIC_SUFFIX;
			messageQueue = new ActiveMqAdapter();

			StringBuilder sb = new StringBuilder("FROM ").append(EventsDBMapper.COLL_NAME).append(" MATCH ");
			if(appId != null) {
				sb.append(EventsDBMapper.APPID).append(" = '").append(appId).append("' ");
				if(deploymentId != null) {
					sb.append("AND ");
				}
			}
			if(deploymentId != null) {
				sb.append(EventsDBMapper.DEPLOYMENT_ID).append(" = '").append(deploymentId).append("' ");
			}
			queryHead = sb.toString();

			sb = new StringBuilder(" GROUP BY NOTHING"); //.append(EventsDBMapper.TIMESTAMP).append(" - ").append(EventsDBMapper.TIMESTAMP).append(" % ").append(getFrequency());

			// AVERAGE OF ALL TERMS: TODO: consider specifying other aggregators: sum, max, min...
			for(String t : terms) {
				sb.append(" avg(data.").append(t).append(") as ").append(t);
			}
			queryTail = sb.toString();

			removeOn = System.currentTimeMillis() + AUTO_REMOVAL_TIME;
		} catch (Exception e) {
			throw new PeriodicNotificationException("Error instantiating App Measures Notifier: " + e.getMessage(),e);
		}
	}


	@Override
	public long getFrequency() {
		return frequency;
	}

	// TODO --> substitute AUTO-REMOVAL (initally 24h) BY
	//				1 - Subscribte to App Manager events (on undeployment)
	//				2 - Remove after X minutes/without new metrics
	private static final long AUTO_REMOVAL_TIME = 24 * 60 * 60 * 1000;
	private long removeOn;
	@Override
	public void sendNotification() throws PeriodicNotificationException {
		long now = System.currentTimeMillis();
		if(now >= removeOn) {
			MQManager.INSTANCE.removeNotifier(this);
			Logger.debug("Asking for AUTO-REMOVAL for notifier: " + toString());
			return;
		}
		try {
			StringBuilder sb = new StringBuilder(queryHead)
					.append(" AND ").append(EventsDBMapper.TIMESTAMP).append(" > ").append(now - frequency)
					.append(" AND ").append(EventsDBMapper.TIMESTAMP).append(" <= ").append(now).append(queryTail);;
							//EventsDBMapper
			String query = sb.toString();
//			Logger.debug("Sending query to aggregation framework: " + query);
			ArrayNode an = QueriesDBMapper.INSTANCE.aggregate(query);
			if(an == null || an.size() == 0) {
//				Logger.debug("Response is null or 0");
			} if(an != null && an.size() > 0) {
				for(JsonNode jn : an) {
					ObjectNode response = JsonNodeFactory.instance.objectNode();
					if(appId != null) {
						response.put(InitiateMonitoringDispatcher.FIELD_APP_ID, appId);
					}
					if(deploymentId != null) {
						response.put(InitiateMonitoringDispatcher.FIELD_DEPLOYMENT_ID, deploymentId);
					}
					if(slaId != null) {
						response.put(InitiateMonitoringDispatcher.FIELD_SLA_ID, slaId);
					}
					response.put("Timestamp", System.currentTimeMillis());
					ObjectNode termsON = JsonNodeFactory.instance.objectNode();
					for(String t : terms) {
						termsON.set(t, jn.get(t));
					}
					response.set(InitiateMonitoringDispatcher.FIELD_TERMS, termsON);

					String responseStr = response.toString();
//					Logger.debug("Sending periodic notification: " + responseStr);

					messageQueue.publishMessage(topicName, responseStr);
				}
				}
		} catch(Exception e) {
			throw new PeriodicNotificationException("Error sending notification: " + e.getMessage(), e);
		}
	}

	@Override
	public String toString() {
		return "AppMeasuresNotifier{" +
				"appId='" + appId + '\'' +
				", deploymentId='" + deploymentId + '\'' +
				", slaId='" + slaId + '\'' +
				", terms=" + Arrays.toString(terms) +
				", frequency=" + frequency +
				", queryHead='" + queryHead + '\'' +
				", queryTail='" + queryTail + '\'' +
				", messageQueue=" + messageQueue +
				", topicName='" + topicName + '\'' +
				", removeOn=" + removeOn +
				'}';
	}

	private static final String TOPIC_PREFIX = "application-monitor.monitoring.";
	private static final String TOPIC_SUFFIX = ".measurement";
}
