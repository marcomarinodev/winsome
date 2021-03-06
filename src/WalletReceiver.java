import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicBoolean;

public class WalletReceiver implements Runnable {

    int port;
    int byteSize;
    String hostname;
    public Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WalletReceiver(int port, int byteSize, String hostname) {
        this.port = port;
        this.byteSize = byteSize;
        this.hostname = hostname;
    }

    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    public void stop() {
        running.set(false);
        System.out.println("Interrupting the thread");
        worker.interrupt();
    }

    public void run() {
        running.set(true);
        while (running.get()) {
            try (MulticastSocket multiSocket = new MulticastSocket(port)) {
                // Get group address and check validity
                InetAddress group = InetAddress.getByName(hostname);
                if (!group.isMulticastAddress()) {
                    throw new IllegalArgumentException("Invalid multicast address: "
                            + group.getHostAddress());
                }

                multiSocket.joinGroup(group);
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);


                multiSocket.receive(packet);

                if (running.get()) {
                    System.out.println(new String(packet.getData(), packet.getOffset(), packet.getLength()) + "\n> ");
                }
            } catch (Exception e) {
                System.err.println("< Client exception: " + e.getMessage());
            }
        }
        System.out.println("< Stopped receiving wallet notifications");
    }
}
