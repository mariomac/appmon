package es.bsc.amon.mq;

import play.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by mmacias on 17/12/14.
 */
public class MQManager {
	public static final MQManager instance = new MQManager();

	Context context;
	Connection connection;
	ConnectionFactory connectionFactory;
	Session session;
	Queue queue;
	MessageConsumer messageConsumer;
	MessageProducer messageProducer;


	MessageDispatcher messageDispatcherInstance;

	public void init() {

		try {
			Logger.info("Initiating Message Queue Manager...");

			context = new InitialContext();

			connectionFactory
					= (ConnectionFactory) context.lookup("asceticpaas");
			connection = connectionFactory.createConnection();
			connection.start();

			session = connection.createSession(true, Session.SESSION_TRANSACTED);
			queue = (Queue) context.lookup("appmon");

			messageConsumer = session.createConsumer(queue);

			messageDispatcherInstance = new MessageDispatcher();
			new Thread(messageDispatcherInstance).start();
			Logger.info("Message Queue Manager Sucessfully created...");
		} catch(JMSException|NamingException e) {
			Logger.error("Error initializing MQ Manager: " + e.getMessage() + " Continuing startup without MQ services...");
		}
	}

	public void stop() {
		if(messageDispatcherInstance != null) messageDispatcherInstance.running = false;
		try {
			if(messageConsumer != null) messageConsumer.close();
			if(session != null) session.close();
			if(connection != null) connection.close();
			if(context != null) context.close();
		} catch(Exception e) {
			Logger.error(e.getMessage());
		}
	}

	private class MessageDispatcher implements Runnable {
		boolean running;
		@Override
		public void run() {
			running = true;
			while(running) {
				try {
					TextMessage message = (TextMessage)messageConsumer.receive();
					Logger.debug("received message: " + message.getText());
					session.commit();
				} catch(JMSException e) {
					if(running) {
						Logger.error("Error dispatching messages: " + e.getMessage());
					} else {
						Logger.debug("While closing MessageDispatcher: " + e.getMessage());
					}
				}
			}
			Logger.info("MessageDispatcher successfully finished...");
		}
	}

}
