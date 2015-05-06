package es.bsc.amon.mq;

import javax.jms.Queue;

/**
 * Created by mmacias on 6/5/15.
 */
public interface QueueDispatcher {
	Queue getQueue();

	/**
	 *
	 * @param msgText
	 * @return
	 */
	void onMessage(String msgText);
}
