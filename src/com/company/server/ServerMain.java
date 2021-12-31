package com.company.server;

import com.company.server.Exceptions.NonExistingConfigParam;

import java.io.File;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        CustomServer customServer;

        try {
            customServer = new CustomServer(pool);
            configServer(customServer);
            customServer.setStorageService();
            customServer.initAsyncServer();
            customServer.addShutdownHook();
            pool.execute(new RewardCalculator(customServer.getRewardRate(), customServer.getStorageService()));
            customServer.runServer();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            e.printStackTrace();
        }

    }

    static void configServer(CustomServer customServer) {
        try {
            String basePath = new File("").getAbsolutePath();
            String configFullPath = basePath + "/" + "config.txt";
            customServer.config(configFullPath);
            // customServer.configTest();
        } catch (IOException e) {
            System.err.println("Server configuration Error");
        } catch (NonExistingConfigParam e) {
            System.err.println(e.getMessage());
        }
    }

}
