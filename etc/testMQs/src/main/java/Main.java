import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import javax.jms.QueueConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by mmacias on 18/12/14.
 */
public class Main {
    public static final void main(String[] args) {
        BrokerService brokerService = null;
        try {

            brokerService = new BrokerService();
            brokerService.addConnector("amqp://0.0.0.0:5762");
            brokerService.setPersistent(false);
                    //BrokerFactory.createBroker("broker:(amqp://0.0.0.0:5762)?persistent=false");
            brokerService.start();

            SubscriptionManager sm = new SubscriptionManager();
            sm.start();

            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String line = "send lkjaslkjafsdlkj";
            int mn = 1;
//            do {
//                System.out.print("Command: ");
//                line = br.readLine();
//                if(line.startsWith("send ")) {
                    Messenger msn = Messenger.Factory.create(); //"Msn"+mn);
                    msn.start();
                    Message ms = Message.Factory.create();
                    ms.setAddress(SubscriptionManager.MQ_ADDRESS);

            System.out.print("sending... ");
                    ms.setBody(new AmqpValue(line.substring(5)));
                    mn++;
                    msn.put(ms);
            msn.setTimeout(3000);
                    msn.send();
            System.out.println("sent!");
                    msn.stop();
//                }
//            } while (!"exit".equalsIgnoreCase(line));
            sm.stop();
            brokerService.stop();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
