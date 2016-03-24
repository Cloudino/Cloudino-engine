/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.server;

import io.cloudino.engine.DeviceConn;
import io.cloudino.engine.DeviceSSLConn;
import io.cloudino.server.handlers.Acceptor;
import io.cloudino.server.handlers.AcceptorListener;
import io.cloudino.server.handlers.ChannelFactory;
import io.cloudino.server.io.SelectorThread;
import io.cloudino.server.ssl.SSLChannelFactory;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Logger;

/**
 *
 * @author javiersolis
 */
public class DeviceSSLServer implements AcceptorListener
{
    static Logger log=Logger.getLogger("i.c.e.DeviceSSLServer");
    
    private int port = 9595;
    
    private ConcurrentLinkedDeque<DeviceConn> conns=new ConcurrentLinkedDeque();
    private ChannelFactory channelFactory=null;
    private SelectorThread selectorThread=null;
    
    /** Creates a new instance of ChatServer */
    public DeviceSSLServer() 
    {
        log.info("SSLServer Started...");
    }
    
    public int getPort()
    {
        return port;
    }
    
    public void setPort(int port)
    {
        if(port>0)this.port = port;
    }
   
    public void start()
    {
        try
        {
            System.out.println("SSLServer Started on port:"+port);
            channelFactory = new SSLChannelFactory(false, "/programming/proys/cloudino/server/MyDSKeyStore.jks", "changeit");
            selectorThread = new SelectorThread();
            Acceptor acceptor = new Acceptor(port, selectorThread, this);
            acceptor.openServerSocket();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        int port=9595;
        if(args.length>0)
        {
            try
            {
                port=Integer.parseInt(args[0]);
            }catch(NumberFormatException e){}
        }
        DeviceSSLServer server = new DeviceSSLServer();
        server.setPort(port);
        server.start();
    }

    public ChannelFactory getChannelFactory() {
        return channelFactory;
    }

    public SelectorThread getSelectorThread() {
        return selectorThread;
    }        
        
    
    @Override
    public void socketConnected(Acceptor acceptor, SocketChannel sc) {
        try
        {
            conns.add(new DeviceSSLConn(sc, this));
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void removeDeviceConn(DeviceConn conn)
    {
        conns.remove(conn);
    }    

    @Override
    public void socketError(Acceptor acceptor, Exception ex) {
        System.out.println("socketError:"+acceptor);
        ex.printStackTrace();
    }

}
