package com.company.server;

import com.company.server.Utils.NIOHelper;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class WriterThread implements Runnable {

    private String message;
    private SocketChannel client;
    private SelectionKey key;
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
            NIOHelper.writeResponse(message, client);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!message.equals("< Goodbye!")) {
            try {
                key.channel().register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        } else {
            try {
                key.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        selector.wakeup();
    }
}
