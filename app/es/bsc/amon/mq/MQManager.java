package es.bsc.amon.mq;

import amqp.TestServiceImpl;
import easyrpc.client.ClientFactory;
import easyrpc.client.protocol.amqp.AmqpClient;
import easyrpc.client.serialization.jsonrpc.JSONCaller;
import easyrpc.server.RpcServer;
import easyrpc.server.protocol.amqp.AmqpService;
import easyrpc.server.serialization.jsonrpc.JSONCallee;

import javax.naming.Context;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Created by mmacias on 17/12/14.
 */
public class MQManager {
	public static final MQManager instance = new MQManager();

	RpcServer server;

	private static Logger log = Logger.getLogger(MQManager.class.getName());

	public void init(String mqUrl, String qName) throws URISyntaxException {
		log.info("Initializing MQ manager at " + mqUrl + " / " + qName);
		URI brokerUri = new URI(mqUrl); //new URI("amqp://guest:guest@localhost:5672?clientid=test-client&remote-host=default");
		server = new RpcServer(
				new AmqpService(brokerUri, qName),
				new JSONCallee());

		server.addEndpoint(new TestServiceImpl());

		System.setProperty(Context.PROVIDER_URL,"file:/");
		Thread th = new Thread(server::start);
		th.start();
		log.info("MQ manager correctly initiated");
	}

}
