/*
 * Copyright 2004 WIT-Software, Lda. 
 * - web: http://www.wit-software.com 
 * - email: info@wit-software.com
 *
 * All rights reserved. Relased under terms of the 
 * Creative Commons' Attribution-NonCommercial-ShareAlike license.
 */

import io.cloudino.server.handlers.Acceptor;
import io.cloudino.server.handlers.AcceptorListener;
import io.cloudino.server.handlers.ChannelFactory;
import io.cloudino.server.handlers.PacketChannel;
import io.cloudino.server.handlers.PacketChannelListener;
import io.cloudino.server.handlers.SimpleProtocolDecoder;
import io.cloudino.server.io.ProtocolDecoder;
import io.cloudino.server.io.SelectorThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import io.cloudino.server.ssl.SSLChannelFactory;

/**
 * A simple server for demonstrating the IO Multiplexing framework in action.
 * After accepting a connection, it will read packets as defined by the
 * SimpleProtocolDecoder class and echo them back.
 *
 * This server can accept and manage large numbers of incoming connections. For
 * added fun remove the System.out statements and try it with several thousand
 * (>10.000) clients. You might have to increase the maximum number of sockets
 * allowed by the operating system.
 *
 * @author Nuno Santos
 */
public class TestServer implements AcceptorListener, PacketChannelListener {

    private final SelectorThread st;
    private static ChannelFactory channelFactory;

    private static final String KEYSTORE = "keystore.ks";
    private static final String KEYSTORE_PASSWORD = "password";

    private static int connections = 0;

    /**
     * Starts the server.
     *
     * @param listenPort The port where to listen for incoming connections.
     *
     * @throws Exception
     */
    public TestServer(int listenPort) throws Exception {
        st = new SelectorThread();
        Acceptor acceptor = new Acceptor(listenPort, st, this);
        acceptor.openServerSocket();
        System.out.println("Listening on port: " + listenPort + ". SSL? " + (channelFactory instanceof SSLChannelFactory));
    }

    public static void main(String[] args) throws Exception {
        channelFactory = new SSLChannelFactory(false, "/programming/proys/cloudino/server/MyDSKeyStore.jks", "changeit");
        try {
            new TestServer(9494);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //////////////////////////////////////////
    // Implementation of the callbacks from the 
    // Acceptor and PacketChannel classes
    //////////////////////////////////////////
    /**
     * A new client connected. Creates a PacketChannel to handle it.
     */
    public void socketConnected(Acceptor acceptor, SocketChannel sc) {
        System.out.println("[" + acceptor + "] Socket connected: "
                + sc.socket().getInetAddress());
        try {
            // We should reduce the size of the TCP buffers or else we will
            // easily run out of memory when accepting several thousands of
            // connctions
            sc.socket().setReceiveBufferSize(2 * 1024);
            sc.socket().setSendBufferSize(2 * 1024);
            // The contructor enables reading automatically.
            PacketChannel pc = new PacketChannel(sc, channelFactory, st,
                new ProtocolDecoder() {

                    @Override
                    public ByteBuffer decode(ByteBuffer socketBuffer) throws IOException 
                    {
                        while (socketBuffer.hasRemaining()) 
                        {
                              // Copies into the temporary buffer
                              byte b = socketBuffer.get();
                              System.out.println(b);
                        }
                        return null;
                    }
                },
                this);
            pc.resumeReading();
            connections++;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sc.close();
            } catch (IOException e1) {
                e1.printStackTrace();
                // Ignore
            }
        }
    }

    public void socketError(Acceptor acceptor, Exception ex) {
        System.out.println("[" + acceptor + "] Error: " + ex.getMessage());
    }

    public void packetArrived(PacketChannel pc, ByteBuffer pckt) {
        StringBuffer sb = new StringBuffer("[" + pc.toString() + "] Packet received. Size: " + pckt.remaining());
//		int limit = Math.min(pckt.remaining(), 20);
//		for (int i = 0; i < limit; i++) {
//			sb.append((char)pckt.get(i));
//		}
        System.out.println(sb.toString());
        pc.sendPacket(pckt);
    }

    public void socketException(PacketChannel pc, Exception ex) {
        connections--;
        System.out.println("[" + pc.toString() + "] Error: ");
        ex.printStackTrace();
        System.out.println(" Active connections: " + connections);
    }

    public void socketDisconnected(PacketChannel pc) {
        connections--;
        System.out.println("[" + pc.toString() + "] Disconnected. Active connections: " + connections);
    }

    /**
     * The answer to a request was sent. Prepare to read the next request.
     */
    public void packetSent(PacketChannel pc, ByteBuffer pckt) {
        try {
            pc.resumeReading();
        } catch (Exception e) {
            socketException(pc, e);
        }
    }
}
