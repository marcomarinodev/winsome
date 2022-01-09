import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class WriterThread implements Runnable {

    // key attachment
    private String message;
    // client socket channel
    private SocketChannel client;
    // selection key
    private SelectionKey key;
    // selector
    private Selector selector;

    public WriterThread(SelectionKey key, String message, SocketChannel client, Selector selector) {
        this.message = message;
        this.client = client;
        this.key = key;
        this.selector = selector;
    }

    @Override
    public void run() {
        System.out.println("Writer thread");
        System.out.println("==> " + message);
        try {
            // send response string
            NIOHelper.writeResponse(message, client);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!message.equals("< Goodbye!")) {
            try {
                // telling the selector to re-listen the key
                // because it can request again
                key.channel().register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        } else {
            try {
                // client is disconnecting, so we're going to close the communication socket
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        selector.wakeup();
    }
}
