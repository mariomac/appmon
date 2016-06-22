package es.bsc.amon.mq.dispatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import es.bsc.amon.controller.EventsDBMapper;
import es.bsc.amon.model.Event;
import play.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.*;

public class AppEstimationsReader {

	boolean running = true;
	Thread theThread;

	public void stop() {
		running = false;
	}
	public AppEstimationsReader() {
		try {
			theThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Logger.info("Initializing Estimations Reader thread");
						Properties p = new Properties();
						p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndi.properties"));
						p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndiEstimations.properties"));
						final Context context = new InitialContext(p);
						TopicConnectionFactory connectionFactory
								= (TopicConnectionFactory) context.lookup("asceticpaas");
						Topic topic = (Topic) context.lookup("prediction");
						TopicConnection connection = connectionFactory.createTopicConnection();
						connection.start();
						TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
						TopicSubscriber clientTopic = session.createSubscriber(topic);
						while (running) {
							try {
								TextMessage tm = (TextMessage) clientTopic.receive(5000);
//						{"lakjsf":null,"provider":"00000","applicationid":"maximTestApp","deploymentid":"938","vms":["5699"],"unit":"WATTHOUR","generattiontimestamp":"8 Apr 2016 08:57:56 GMT","referredtimestamp":"8 Apr 2016 08:57:56 GMT","value":48.36}
								if (tm != null) {
									System.out.println("received message: " + tm.getText());

									ObjectNode estimation = (ObjectNode) new ObjectMapper().readTree(tm.getText());
									ObjectNode asEvent = JsonNodeFactory.instance.objectNode();
									asEvent.set(EventsDBMapper.APPID, estimation.get("applicationid"));
									asEvent.set(EventsDBMapper.DEPLOYMENT_ID, estimation.get("deploymentid"));
									ObjectNode data = JsonNodeFactory.instance.objectNode();
									data.set("energyEstimation", estimation.get("value"));
									asEvent.set(EventsDBMapper.DATA, data);

									submitEstimation(asEvent);
								}
								//EventsDBMapper.INSTANCE.storeEvent(asEvent);
							} catch (Exception e) {
								Thread.sleep(3000);
								Logger.warn(e.getMessage(), e);
								if (running) {
									try {
										connectionFactory
												= (TopicConnectionFactory) context.lookup("asceticpaas");
										topic = (Topic) context.lookup("prediction");
										connection = connectionFactory.createTopicConnection();
										connection.start();
										session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
										clientTopic = session.createSubscriber(topic);
									} catch (Exception ex) {
										Logger.error("Error reconnecting from estimations reader", ex);
									}
								}
							}
						}
					} catch (Exception e) {
						Logger.error("Error initializing EM estimations reader: " + e.getMessage());
					}
				}
			});

			theThread.start();

			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					running = false;
				}
			}));

		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
		}
	}

	private void submitEstimation(ObjectNode event) throws JMSException, IOException, NamingException {
		String appId = event.get(EventsDBMapper.APPID).asText();
		String deploymentId = event.get(EventsDBMapper.DEPLOYMENT_ID).asText();

		String topicName = TOPIC_PREFIX + appId + "." + deploymentId  + TOPIC_SUFFIX;
		String topicKey = "topic." + appId + deploymentId;

		Properties p = new Properties();
		p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndi.properties"));
		p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndiEstimations.properties"));
		p.put(topicKey,topicName);

		final Context context = new InitialContext(p);
		TopicConnectionFactory connectionFactory
				= (TopicConnectionFactory) context.lookup("asceticpaas");
		TopicConnection connection = connectionFactory.createTopicConnection();
		connection.start();
		TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		Logger.debug("Submitting estimation to topic: " + topicName);
		Logger.debug("Submitting next estimation: " + event.toString());
		TextMessage estimationMessage = session.createTextMessage(event.toString());
		final Topic topic = (Topic) context.lookup(appId+deploymentId);
		session.createProducer(topic).send(estimationMessage);
	}

	final static String getTopicKey(String appId, String deploymentId) {
		return "topic." + appId + "." + deploymentId;
	}

	final static String getTopicName(String appId, String deploymentId) {
		return TOPIC_PREFIX + appId + "." + deploymentId + TOPIC_SUFFIX;

	}

	private static final String TOPIC_PREFIX = "application-monitor.monitoring.";
	private static final String TOPIC_SUFFIX = ".estimation";
}
