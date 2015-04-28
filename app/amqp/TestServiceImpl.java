package amqp;

/**
 * Created by mmacias on 28/4/15.
 */
public class TestServiceImpl implements TestService {
    @Override
    public String getSomething(String word, int times) {
        String w = "";
        for(int i = 0 ; i < times ; i++) {
            w += word + " ";
        }
        return w;
    }
}
