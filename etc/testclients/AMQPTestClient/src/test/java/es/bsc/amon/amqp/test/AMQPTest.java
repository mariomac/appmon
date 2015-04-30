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

		ConnectionFactory connectionFactory
				= (ConnectionFactory) context.lookup("asceticpaas");
		Connection connection = connectionFactory.createConnection();
		connection.start();

		Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

		Queue queue = (Queue) context.lookup("appmon");

		MessageProducer messageProducer = session.createProducer(queue);

		TextMessage message = session.createTextMessage("Hello world!");
		messageProducer.send(message);
		session.commit();

		connection.close();
		context.close();
    }
}
