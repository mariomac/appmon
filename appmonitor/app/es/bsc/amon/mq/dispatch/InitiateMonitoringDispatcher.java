package es.bsc.amon.mq.dispatch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import es.bsc.amon.mq.CommandDispatcher;
import play.Logger;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class InitiateMonitoringDispatcher implements CommandDispatcher {
	private static final String TOPIC_PREFIX = "application-monitor.monitoring.";
	private static final String TOPIC_SUFFIX = ".measurement";

	private static final String FIELD_APP_ID = "ApplicationId";

	private Context context;
	private Session session;

	public InitiateMonitoringDispatcher(Context context, Session session) {
		this.context = context;
		this.session = session;
	}

	@Override
	public void onCommand(ObjectNode msgBody) {
		Logger.debug("InitiateMonitoringDispatcheasdfadsfadsafr.onCommand = " + msgBody.toString());
		String appId = msgBody.get(FIELD_APP_ID).textValue();
		final String topicName = new StringBuilder(TOPIC_PREFIX).append(appId).append(TOPIC_SUFFIX).toString();
		String topicKey = "topic." + appId;
		try {
			Properties p = new Properties();
			p.load(InitiateMonitoringDispatcher.class.getResourceAsStream("/jndi.properties"));
			p.put(topicKey,topicName);
			final Context context = new InitialContext(p);
			context.addToEnvironment(topicKey, topicName);
			final Topic topic = (Topic) context.lookup(appId);
			final MessageProducer producer = session.createProducer(topic);

			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						TextMessage response = session.createTextMessage(topicName + " - " + UUID.randomUUID());
						producer.send(response);
					} catch (JMSException e) {
						Logger.error(e.getMessage(),e);
					}
				}
			},0,4000);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e.getMessage(), e);
		}
	}
}
