import java.net.Inet4Address;
import java.net.UnknownHostException;

class InetTests {

    public static void main(String args[]) throws UnknownHostException {
        System.out.println(Inet4Address.getLocalHost().getHostAddress());
    }
}