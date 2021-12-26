package com.company.server.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NIOHelper {
    public static String readRequest(SelectionKey key, ByteBuffer byteBuffer) {
        String ret = "";
        try {
            SocketChannel client = (SocketChannel) key.channel();
            byteBuffer.clear();
            client.read(byteBuffer);
            byteBuffer.flip();
            int receivedLength = byteBuffer.getInt();
            byte[] receivedBytes = new byte[receivedLength];
            byteBuffer.get(receivedBytes);
            ret = new String(receivedBytes);
        } catch (IOException e) {
            System.out.println("ERROR");
            return ret;
        }
        return ret;
    }

    public static void writeResponse(String message, SocketChannel client) throws IOException {
        ByteBuffer newBuffer = ByteBuffer.allocate(32 * 1024);
        byte[] mex = message.getBytes();
        newBuffer.clear();
        newBuffer.putInt(mex.length);
        newBuffer.put(mex);
        newBuffer.flip();
        client.write(newBuffer);
    }

    public static String removeLastChar(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }
}
