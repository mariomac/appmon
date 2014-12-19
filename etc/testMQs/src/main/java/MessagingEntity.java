import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;
import org.apache.qpid.amqp_1_0.jms.impl.DestinationImpl;
import org.apache.qpid.amqp_1_0.jms.impl.QueueImpl;
import org.apache.qpid.amqp_1_0.jms.jndi.PropertiesFileInitialContextFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.*;
import javax.naming.NamingException;

/**
 * Created by mmacias on 18/12/14.
 */
public class MessagingEntity {
	public static final String MQ_ADDRESS = "amqp://0.0.0.0:5672/testmqs";
	private static Set<MessagingEntity> allMEs = new HashSet<MessagingEntity>();

	private boolean stop = true;

	private String name;

	public static MessagingEntity create(String name) {
		MessagingEntity me = new MessagingEntity(name);
		allMEs.add(me);
		return me;
	}

	private MessagingEntity(String name) {
		this.name = name;
	}

	Context context;
	Connection conn;
	Session session;
	Session producerSession;
	Queue recvQueue;

	public void start() {
		if (!stop)
			throw new RuntimeException("This subscription manager has been used. Please instantiate another.");
		stop = false;

		try {

			// example taken from http://svn.apache.org/repos/asf/qpid/branches/0.30/qpid/java/amqp-1-0-client-jms/example/src/main/java/org/apache/qpid/amqp_1_0/jms/example/hello.properties
			Properties p = new Properties();

			p.load(new StringReader(
						"java.naming.factory.initial = org.apache.qpid.amqp_1_0.jms.jndi.PropertiesFileInitialContextFactory\n" +
						//"connectionfactory.localhost = amqp://guest:guest@localhost?brokerlist='tcp://localhost:5672'\n" +
						"connectionfactory.localhost = amqp://guest:guest@localhost:5672?clientid=test-client&remote-host=default\n" +
					"queue.queue = "+name));
			context = new InitialContext(p);

			ConnectionFactory cf = (ConnectionFactory) context.lookup("localhost");
			conn = cf.createConnection();

			producerSession = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			recvQueue = (Queue) context.lookup("queue");

			Session consumerSession = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageConsumer mc = consumerSession.createConsumer(recvQueue);

			mc.setMessageListener(new MessageListener() {
				                      @Override
				                      public void onMessage(Message message) {
					                      try {
						                      TextMessage tm = (TextMessage) message;
						                      if(tm.getJMSReplyTo() != null) {
							                      //System.out.println("tm.getJMSReplyTo().toString() = " + tm.getJMSReplyTo().toString());
							                      if("producer".equals("name")) sendMessage((Queue)tm.getJMSReplyTo(),tm.getText());
						                      }
						                      System.out.println("tm.getText() = " + tm.getText());
					                      }catch(JMSException e) {
						                      e.printStackTrace();
					                      }
				                      }
			                      });


			conn.start();
			stop = false;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void sendMessage(Queue to, String msg) {
		try {
			MessageProducer prod = producerSession.createProducer(to);
			TextMessage tm = producerSession.createTextMessage(msg);
			tm.setJMSReplyTo(to);
			prod.send(tm);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void sendMessage(String to, String msg) {
			if (stop) throw new RuntimeException("You must start first the MessagingEntity instance");
		try {
			Properties p = new Properties();
			p.load(new StringReader(
					"java.naming.factory.initial = org.apache.qpid.amqp_1_0.jms.jndi.PropertiesFileInitialContextFactory\n" +
							"connectionfactory.localhost = amqp://localhost:5672\n" +
							"queue.toQueue = "+to));
			Context sendContext = new InitialContext(p);

			Queue sendQueue = (Queue) sendContext.lookup("toQueue");

			sendMessage(sendQueue,msg);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void stop() {
//        if(!used) throw new RuntimeException("this subscription manager cannot be stopped because has not been started");
//        if(stop) throw new RuntimeException("This subscription manager has already been stopped");
		stop = true;
		try {
			conn.close();
			context.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void stopAll() {
		for (MessagingEntity me : allMEs) {
			me.stop();
		}
		allMEs.clear();
	}
}
