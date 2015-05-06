package es.bsc.amon.amqp.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


public class AMQPTest
    extends TestCase
{
	Context context;
	ConnectionFactory connectionFActory;
	Session session;
	MessageProducer messageProducer;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
     * Rigourous Test :-)
     */
    public void testCall() throws NamingException, JMSException {
		context = new InitialContext();

		TopicConnectionFactory connectionFactory
				= (TopicConnectionFactory) context.lookup("asceticpaas");


		TopicConnection connection = connectionFactory.createTopicConnection();

		TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		Queue sendQueue = (Queue) context.lookup("appmon");
		Topic topic = (Topic) context.lookup("topic");


		TopicSubscriber clientTopic = session.createSubscriber(topic);
		connection.start();

		MessageProducer messageProducer = session.createProducer(sendQueue);

		TextMessage message = session.createTextMessage("perracozz");
		messageProducer.send(message);

		message = session.createTextMessage("zorronzz");
		messageProducer.send(message);
		message = session.createTextMessage("byezz");
		messageProducer.send(message);

		System.out.println("Message sent");
		String s = "";

		while(!"Hello bye".equals(s)) {
			System.out.println("Waiting for a new message...");
			TextMessage tm = (TextMessage) clientTopic.receive();
			s = tm.getText();
			System.out.println("received message: " + s);
		}


		connection.close();
		context.close();
	}
}
