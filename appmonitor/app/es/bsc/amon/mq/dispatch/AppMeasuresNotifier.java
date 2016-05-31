package es.bsc.amon.mq.dispatch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import es.bsc.amon.controller.EventsDBMapper;
import es.bsc.amon.controller.QueriesDBMapper;
import es.bsc.amon.mq.MQManager;
import es.bsc.amon.mq.notif.PeriodicNotificationException;
import es.bsc.amon.mq.notif.PeriodicNotifier;
import play.Logger;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.*;

class AppMeasuresNotifier implements PeriodicNotifier {
	final String appId;
	final String deploymentId;
	final String slaId;
	final String[] terms;
	final Map<String,Integer> percentiles;
	final long frequency;
	String queryHead, queryTail;

	final String topicName;

	final MessageProducer producer;
	final Session session;

	public AppMeasuresNotifier(Session session, String appId, String deploymentId, String slaId, String[] terms, long frequency) throws PeriodicNotificationException {
		try {
			this.session = session;
			this.appId = appId;
			this.deploymentId = deploymentId;
			this.slaId = slaId;
			this.terms = terms;
			this.frequency = frequency;
			topicName = TOPIC_PREFIX + appId + TOPIC_SUFFIX;

			String topicKey = "topic." + appId;

			Properties p = new Properties();
			p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndi.properties"));
			p.put(topicKey, topicName);
			final Context context = new InitialContext(p);
			context.addToEnvironment(topicKey, topicName);
			final Topic topic = (Topic) context.lookup(appId);
			producer = session.createProducer(topic);

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

			Map<String,Integer> percentiles = new HashMap<>();
			// AVERAGE OF ALL TERMS: TODO: consider specifying other aggregators: sum, max, min...
			for(int i = 0 ; i < terms.length ; i++ ) {
				if(terms[i].trim().startsWith("percentile")) {
					try {
						String[] cmdParts = terms[i].split("[\\(,\\)]");
						String term = cmdParts[1].replaceAll("'","").trim();
						Integer percent = new Integer(cmdParts[2].trim());
						terms[i] = "percent_" + term + "_" + cmdParts[2];
						percentiles.put(terms[i],percent);
						sb.append( " push(data.").append(term).append(") as ").append(terms[i]);
					} catch(Exception e) {
						throw new Exception("The syntax for percentile calculation seems invalid",e);
					}
				} else {
					sb.append(" avg(data.").append(terms[i]).append(") as ").append(terms[i]);
				}
			}
			this.percentiles = Collections.unmodifiableMap(percentiles);
			queryTail = sb.toString();

			removeOn = System.currentTimeMillis() + AUTO_REMOVAL_TIME;
		} catch (Exception e) {
			Logger.error(e.getMessage(),e);
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
					// TO DO: optimize
					ObjectNode termsON = JsonNodeFactory.instance.objectNode();
					for(String t : terms) {
						if(percentiles.containsKey(t) && jn.get(t).getNodeType() == JsonNodeType.ARRAY) {
							ArrayNode arrayNode = (ArrayNode) jn.get(t);
							if(arrayNode.size() > 0) {
								List<Double> numbers = new ArrayList<>(arrayNode.size());
								for(JsonNode item : arrayNode) {
									if(item.getNodeType() == JsonNodeType.NUMBER) {
										numbers.add(item.asDouble());
									}
								}
								Collections.sort(numbers);
								int index = (int)((((double)percentiles.get(t)) * (numbers.size() - 1)) / 100);
								double val;
								if(index >= numbers.size() - 1) {
									val = numbers.get(numbers.size() - 1);
								} else {
									// hack: we calculate the linear interpolation between both values
									// to do some "significative" information when there are very few measueres
									val = numbers.get(index) * (double)percentiles.get(t) + numbers.get(index+1) * (100-(double)percentiles.get(t));
									val /= 100.0;
								}
								termsON.put(t,val);
							}
						} else {
							termsON.put(t, jn.get(t));
						}
					}

					response.set(InitiateMonitoringDispatcher.FIELD_TERMS, termsON);

					String responseStr = response.toString();
					//Logger.debug("Sending periodic notification: " + responseStr);

					TextMessage responseMessage = session.createTextMessage(responseStr);
					producer.send(responseMessage);				}
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
				", topicName='" + topicName + '\'' +
				", removeOn=" + removeOn +
				'}';
	}

	private static final String TOPIC_PREFIX = "application-monitor.monitoring.";
	private static final String TOPIC_SUFFIX = ".measurement";
}
