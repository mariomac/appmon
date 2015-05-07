package es.bsc.amon.mq;

import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bsc.amon.mq.dispatch.InitiateMonitoringDispatcher;
import play.Logger;
import play.libs.Json;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Map;
import java.util.TreeMap;

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

	Map<String,CommandDispatcher> commandDispatchers = new TreeMap<String,CommandDispatcher>();

	public void init() {

		try {
			Logger.info("Initiating Message Queue Manager...");

			context = new InitialContext();

			connectionFactory
					= (ConnectionFactory) context.lookup("asceticpaas");
			connection = connectionFactory.createConnection();
			connection.start();

			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			queue = (Queue) context.lookup("appmon");

			messageConsumer = session.createConsumer(queue);

			messageDispatcherInstance = new MessageDispatcher();
			new Thread(messageDispatcherInstance).start();
			Logger.info("Message Queue Manager Sucessfully created...");

			commandDispatchers.put("initiateMonitoring", new InitiateMonitoringDispatcher(context, session));

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
					Logger.trace("received message: " + message.getText());

					ObjectNode on = (ObjectNode)Json.parse(message.getText());
					String command =  on.get(CommandDispatcher.FIELD_COMMAND).textValue();
					commandDispatchers.get(command).onCommand(on);
				} catch(Exception e) {
					if(running) {
						Logger.error("Error dispatching messages: " + e.getMessage(), e);
					} else {
						Logger.debug("While closing MessageDispatcher: " + e.getMessage(), e);
					}
				}
			}
			Logger.info("MessageDispatcher successfully finished...");
		}
	}

}
