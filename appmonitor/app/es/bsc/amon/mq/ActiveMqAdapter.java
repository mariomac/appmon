package es.bsc.amon.mq;

import es.bsc.amon.Global;
import es.bsc.amon.mq.dispatch.InitiateMonitoringDispatcher;
import org.apache.activemq.ActiveMQConnectionFactory;
import play.Logger;

import javax.jms.*;
import javax.jms.Queue;
import java.io.IOException;
import java.util.*;

// TO DO: this code is duplicated from demiurge. Put it into a library
@Deprecated

public class ActiveMqAdapter {

	private final ActiveMQConnectionFactory connectionFactory;

	public ActiveMqAdapter() throws IOException {
		Properties p = new Properties();
		p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndi.properties"));
		connectionFactory = new ActiveMQConnectionFactory(
				p.getProperty("java.naming.security.principal"),
				p.getProperty("java.naming.security.credentials"),
				p.getProperty("connectionfactory.asceticpaas"));
	}

	/**
	 * Publishes a message in the queue with the topic and the message specified
	 *
	 * @param topic   the topic
	 * @param message the message
	 */
	public void publishMessage(String topic, String message) {
		Connection connection = null;
		Session session = null;
		try {
			// Create a Connection
			connection = connectionFactory.createConnection();
			connection.start();

			// Create a Session
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Create the destination
			Destination destination = session.createTopic(topic);

			// Create a MessageProducer from the Session to the Topic
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

			producer.send(session.createTextMessage(message));

			session.close();
			connection.close();
		} catch (Exception e) {
			Logger.warn("[VMM] Could not send topic " + topic + " to the message queue");
		} finally {
			try {
				session.close();
				connection.close();
			} catch (Exception e) {
				Logger.warn("Can't close connection: " + e.getMessage());
			}
		}
	}

	private Map<String, Connection> openConnections = new HashMap<>();
	private Map<String, Session> openSessions = new HashMap<>();

	public void listenToQueue(String queueName, MessageListener listener) throws JMSException {
		Logger.debug("Listening for messages to queue: " + queueName);
		QueueConnection connection = null;
		QueueSession session = null;
		connection = connectionFactory.createQueueConnection();
		session = connection.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		Queue q = session.createQueue(queueName);
		MessageConsumer consumer = session.createConsumer(q);
		consumer.setMessageListener(listener);
		connection.start();
		openConnections.put(queueName, connection);
		openSessions.put(queueName, session);
	}

	public void closeQueue(String queueName) {
		try {
			Logger.debug("Closing queue " + queueName);
			Connection connection = openConnections.remove(queueName);
			Session session = openSessions.remove(queueName);
			connection.stop();
			connection.close();
			session.close();
		} catch (Exception e) {
			Logger.warn("Can't close connection: " + e.getMessage());
		}
	}

	public void closeAllQueues() {
		Set<String> cn = new HashSet<>(openConnections.keySet());
		for (String queue : cn) {
			closeQueue(queue);
		}
	}

}
