package es.bsc.amon.mq.dispatch;

import es.bsc.amon.mq.notif.PeriodicNotificationException;
import es.bsc.amon.mq.notif.PeriodicNotifier;
import play.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

class AppMeasuresNotifier implements PeriodicNotifier {
	final String appId;
	final String deploymentId;
	final String slaId;
	final String[] terms;
	final long frequency;
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
			String topicName = new StringBuilder(TOPIC_PREFIX).append(appId).append(TOPIC_SUFFIX).toString();
			String topicKey = "topic." + appId;
			Properties p = new Properties();
			p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndi.properties"));
			p.put(topicKey, topicName);
			final Context context = new InitialContext(p);
			context.addToEnvironment(topicKey, topicName);
			final Topic topic = (Topic) context.lookup(appId);
			producer = session.createProducer(topic);
		} catch (JMSException | IOException | NamingException e) {
			throw new PeriodicNotificationException("Error instantiating App Measures Notifier: " + e.getMessage(),e);
		}
	}


	@Override
	public long getFrequency() {
		return frequency;
	}

	@Override
	public void sendNotification() throws PeriodicNotificationException {
		try {
			TextMessage response = session.createTextMessage(appId + " - " + UUID.randomUUID());
			producer.send(response);
		} catch(JMSException e) {
			throw new PeriodicNotificationException("Error sending notification: " + e.getMessage(), e);
		}
	}
	private static final String TOPIC_PREFIX = "application-monitor.monitoring.";
	private static final String TOPIC_SUFFIX = ".measurement";
}
