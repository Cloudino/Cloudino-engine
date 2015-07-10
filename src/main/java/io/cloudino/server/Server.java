/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.server;

import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author javiersolis
 */
public class Server extends Thread
{
    private int port = 9595;
    private boolean running = false;
    private ServerSocket sserv = null;
    
    /** Creates a new instance of ChatServer */
    public Server()
    {
        System.out.println("Server Started...");
    }
    
    public int getPort()
    {
        return port;
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    public void run()
    {
        try
        {
            sserv = new ServerSocket(port);
            
            while (running)
            {
                //System.out.println("running");
                Socket sock = sserv.accept();
                try
                {
                    Connection conn = new Connection(sock, this);
                    conn.start();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                System.out.println("accept:"+sock);
                //this.sleep(1000);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void start()
    {
        running = true;
        super.start();
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
        Server server = new Server();
        server.setPort(port);
        server.start();
    }

}
