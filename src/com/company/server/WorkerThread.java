package com.company.server;

import java.net.Socket;

public class WorkerThread implements Runnable {

    private Socket socket;

    WorkerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("Connected: " + socket);
    }
}
