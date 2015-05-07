package es.bsc.amon.amqp.test;

import junit.framework.TestCase;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;


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
    public void testCall() throws Exception {

		String appName = "appJarl"+System.currentTimeMillis()%1000;
		System.out.println("Initiating " + appName);
		Properties p = new Properties();
		p.load(getClass().getResourceAsStream("/jndi.properties"));
		p.put("topic.topic", "application-monitor.monitoring." + appName + ".measurement");
		context = new InitialContext(p);

		TopicConnectionFactory connectionFactory
				= (TopicConnectionFactory) context.lookup("asceticpaas");


		TopicConnection connection = connectionFactory.createTopicConnection();

		TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);


		Queue sendQueue = (Queue) context.lookup("appmon");
		Topic topic = (Topic) context.lookup("topic");


		TopicSubscriber clientTopic = session.createSubscriber(topic);
		connection.start();

		MessageProducer messageProducer = session.createProducer(sendQueue);

		TextMessage message = session.createTextMessage("{\n" +
				"\t\"Command\" : \"initiateMonitoring\",\n" +
				"\t\"SLAId\" : \"alskdfj\",\n" +
				"\t\"ApplicationId\" : \""+ appName + "\",\n" +
				"\t\"DeploymentId\" : \"lasdkjf\",\n" +
				"\t\"VMId\" : \"asdfljsladkfj\",\n" +
				"\t\"Terms\" : [\"list\", \"of\", \"terms\", \"to\", \"monitor\" ],\n" +
				"\t\"Frequency\" : 12039\n" +
				"}");
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
