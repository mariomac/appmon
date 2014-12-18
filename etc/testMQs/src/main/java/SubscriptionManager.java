import org.apache.qpid.proton.TimeoutException;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.messenger.Messenger;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by mmacias on 18/12/14.
 */
public class SubscriptionManager {
    public static final String MQ_ADDRESS = "amqp://localhost:5762/testmqs";

    private Messenger msn;
    private boolean used = false;
    private boolean stop = false;

    private Logger log = Logger.getLogger(SubscriptionManager.class.getName());

    public void start() {
        if(used) throw new RuntimeException("This subscription manager has been used. Please instantiate another.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                msn = Messenger.Factory.create();//"publisher");
                try {
                    msn.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                msn.subscribe(MQ_ADDRESS);
                used = true;
                while(!stop) {
                    try {
                        msn.recv();
                        while (msn.incoming() > 0) {
                            Message m = msn.get();
                            log.info("received message from " + m.getReplyTo() + ": " + m.getBody());
                        }
                    } catch(TimeoutException toe) {
                        log.warning("Request timeout. Trying again...");
                    }
                }

                log.info("Stopping subscription manager...");
                msn.stop();
            }
        }).start();
    }

    public void stop() {
        if(!used) throw new RuntimeException("this subscription manager cannot be stopped because has not been started");
        if(stop) throw new RuntimeException("This subscription manager has already been stopped");
        stop = true;
    }
}
