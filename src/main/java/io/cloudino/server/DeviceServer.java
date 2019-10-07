/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.server;

import io.cloudino.engine.Command;
import io.cloudino.engine.DeviceBaseConn;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author javiersolis
 */
public class DeviceServer extends Thread {

    static Logger log = Logger.getLogger(DeviceServer.class.getName());

    private int port = 9494;
    private boolean running = false;

    private ConcurrentLinkedDeque<DeviceBaseConn> conns = new ConcurrentLinkedDeque();
    private Thread processor = null;
    ExecutorService executor = Executors.newFixedThreadPool(16);

    /**
     * Creates a new instance of ChatServer
     */
    public DeviceServer() {
        log.info("Server Started...");
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void run() {
        try {
            ServerSocket sserv = new ServerSocket(port);
            while (running) {
                //System.out.println("running");
                Socket sock = sserv.accept();
                sock.setTcpNoDelay(true);
                try {
                    DeviceBaseConn conn = new DeviceBaseConn(sock, this);
                    conns.add(conn);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log.log(Level.OFF, "accept:" + sock);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        running = true;
        processor = new Thread() {
            @Override
            public void run() {
                try {
                    while (running) {
                        boolean wait = true;
                        Iterator<DeviceBaseConn> it = conns.iterator();
                        while (it.hasNext()) {
                            DeviceBaseConn connection = it.next();
                            if (!connection.isClosed()) {
                                try {
                                    connection.loop();
                                    if (connection.hasCommands()) 
                                    {
                                        wait = false;
                                        List<Command> cmds=connection.getCommands();
                                        executor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    connection.processCommands(cmds);
                                                } catch (Exception e) {
                                                    log.log(Level.WARNING, "Error Processing Message", e);
                                                }
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                connection.close();
                                it.remove();
                                System.out.println("Remove Connection...");
                            }
                        }
                        if (wait) {
                            Thread.sleep(20);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        processor.start();

        super.start();

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int port = 9494;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
            }
        }
        DeviceServer server = new DeviceServer();
        server.setPort(port);
        server.start();
    }

}
