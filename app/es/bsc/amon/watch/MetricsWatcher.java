package es.bsc.amon.watch;

/**
 * Created by mmacias on 17/12/14.
 */
public class MetricsWatcher implements Runnable {
	public static final MetricsWatcher instance = new MetricsWatcher();

	private boolean stop = false;

	void stop() {
		stop = true;
	}

	@Override
	public void run() {
		stop = true;
		while(!stop) {
		}
	}
}
