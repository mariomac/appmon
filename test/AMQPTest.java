import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;
import org.apache.qpid.proton.messenger.Tracker;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by mmacias on 16/12/14.
 */

@Ignore
public class AMQPTest {
	@Test
	public void testProton() throws Exception {

		String ADDRESS = "amqp://localhost/amqp-1.0-test";

		Messenger msn = Messenger.Factory.create();
		System.out.println("starting messenger");
		msn.start();
		System.out.println("Subscribing to " + ADDRESS);

		msn.subscribe(ADDRESS);

		System.out.println("Creating sender");
		Messenger sendMsn = Messenger.Factory.create();
		sendMsn.start();
		Message msg = Message.Factory.create();
		msg.setAddress(ADDRESS);
		msg.setSubject("Que te zurzan");
		msg.setBody(new AmqpValue("Hola tio"));
		//msg.set
		System.out.println("Sending message...");
		sendMsn.put(msg);
		sendMsn.send();

		boolean done = false;
		int v = 0;
		msn.setBlocking(true);
		msn.setTimeout(1000);
		while(!done) {
			msn.recv();
			System.out.println("("+v+") msn.incoming() = " + msn.incoming());
			v++;
			while(msn.incoming() > 0) {
				Message m = msn.get();
				System.out.println("New message:\n" + m);
				System.out.println("m.getBody() = " + m.getBody());
				//done = true;
			}

		}

		msn.stop();
		sendMsn.stop();


/*
		Messenger mng = Messenger.Factory.create();
		mng.start();
		Message msg = Message.Factory.create();
		msg.setAddress(ADDRESS);
		msg.setSubject("hello");
		msg.setContentType("application/octet-stream");
		msg.setBody(new AmqpValue("Passatron"));
		mng.put(msg);
		mng.send();

		mng.subscribe(ADDRESS);
		mng.recv();
		Message msg2 = mng.get();
		assertEquals(msg.getSubject(), msg2.getSubject());
		assertEquals(msg.getContentType(), msg2.getContentType());
		assertEquals(msg.getBody().toString(), msg2.getBody().toString());
		mng.stop();
		*/
	}
}
