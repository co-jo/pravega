import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;

class InetTests {

    public static void main(String args[]) throws UnknownHostException {
        System.out.println(Inet4Address.getLocalHost().getHostAddress());
        InetSocketAddress inetAddr = new InetSocketAddress("localhost", 3181);
        System.out.println(InetAddress.getByName("2c1b9d5bfb2c").getHostName());
        System.out.println(InetAddress.getByName("2c1b9d5bfb2c").isUnresolved());
    }
}