import amqp.TestService;
import easyrpc.client.ClientFactory;
import easyrpc.client.protocol.amqp.AmqpClient;
import easyrpc.client.serialization.jsonrpc.JSONCaller;

import org.junit.Ignore;
import org.junit.Test;

import javax.naming.Context;
import java.net.URI;
import java.util.logging.Logger;

/**
 * Created by mmacias on 16/12/14.
 */

public class AMQPTest {
	@Test
	public void testQueue() throws Exception {

		System.setProperty(Context.PROVIDER_URL,"file:/");
		ClientFactory cf = new ClientFactory(
				new AmqpClient(URI.create("amqp://localhost:5672"),"AppMonitor"),
				new JSONCaller());

		TestService ts = cf.instantiate(TestService.class);
		System.out.println(ts.getSomething("hola", 6));

	}
}
