
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Created by mmacias on 18/12/14.
 */
public class Main {

    public static final void main(String[] args) {
        int sessionId = new Random(System.currentTimeMillis()).nextInt(100);
        System.out.println("sessionId = " + sessionId);

        MessagingEntity pub = MessagingEntity.create("publisher");
        pub.start();

        MessagingEntity c1 = MessagingEntity.create("client1");
        c1.start();
        MessagingEntity c2 = MessagingEntity.create("client2");
        c2.start();

        System.out.println("Enviando mensajes....");

        c1.sendMessage("publisher", "Hola tio " + sessionId);

        c2.sendMessage("publisher", "Que tal? " + sessionId);

        System.out.println("Durmiendo...");
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MessagingEntity.stopAll();
        System.out.println("Exited");

    }
}
