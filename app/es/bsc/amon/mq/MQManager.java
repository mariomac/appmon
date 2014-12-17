package es.bsc.amon.mq;

import org.apache.qpid.proton.TimeoutException;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.impl.SessionImpl;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by mmacias on 17/12/14.
 */
public class MQManager implements Runnable {
	public static final MQManager instance = new MQManager();

	private static Logger log = Logger.getLogger(MQManager.class.getName());


	private String mqUrl;
	private Messenger msn;
	private boolean stop = true;

	private MQManager() {

	}

	public void init(String mqUrl) {
		log.info("Starting " + mqUrl);
		this.mqUrl = mqUrl;
		msn = Messenger.Factory.create();
		try {
			msn.start();
			msn.subscribe(mqUrl);
			msn.setBlocking(true);
			msn.setTimeout(-1);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		new Thread(this).start();
	}

	public void stop() {
		stop = true;
	}

	@Override
	public void run() {
		stop = false;
		while(!stop) {
			try {
				log.info("Receiving info...");
				msn.recv();
				while(msn.incoming() > 0) {
					Message m = msn.get();
					print(m);
				}
			} catch(TimeoutException ex) {
				log.warning(ex.getMessage() + " ... calling recv() again...");
			}

		}
		msn.stop();
		log.info("Stopping MQManager...");
	}

	private void print(Message msg) {
		StringBuilder b = new StringBuilder("message: ");
		b.append("Address: ").append(msg.getAddress()).append("\n");
		b.append("Subject: ").append(msg.getSubject()).append("\n");

			b.append("Props:     ").append(msg.getProperties()).append("\n");
			b.append("App Props: ").append(msg.getApplicationProperties()).append("\n");
			b.append("Msg Anno:  ").append(msg.getMessageAnnotations()).append("\n");
			b.append("Del Anno:  ").append(msg.getDeliveryAnnotations()).append("\n");

		ApplicationProperties p = msg.getApplicationProperties();
		String s = (p == null) ? "null" : String.valueOf(p.getValue());
		b.append("Headers: ").append(s).append("\n");

		b.append(((AmqpValue)msg.getBody()).getValue()).append("\n");
		b.append("END").append("\n");
		System.out.println(b.toString());
	}
}
