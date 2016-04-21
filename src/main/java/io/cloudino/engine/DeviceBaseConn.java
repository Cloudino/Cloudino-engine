/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import io.cloudino.server.DeviceServer;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author javiersolis
 */
public class DeviceBaseConn extends DeviceConn
{
    private Socket sock = null;
    private DeviceServer server = null;
    private boolean running = true;
    
    /**
     * Create an instance of Device Conenction
     * @param sock
     * @param server
     * @throws IOException 
     */
    public DeviceBaseConn(Socket sock, DeviceServer server) throws IOException
    {
        System.out.println("Connection open");
        this.sock = sock;
        this.server = server;
        this.outputStream=sock.getOutputStream();
        this.inputStream = sock.getInputStream();
    }  
    
    public boolean loop()
    {
        //System.out.println("loop");
        boolean ret=false;
        try
        {
            if(!uploading)
            {
                if(System.currentTimeMillis()-time>10000)
                {
                    time=System.currentTimeMillis();
                    //System.out.println("Ping");
                    write((byte)'_');
                }
                while(inputStream.available()>0)
                {
                    //System.out.println(inputStream.available());
                    time=System.currentTimeMillis();
                    int b=inputStream.read();
                    if(b==-1)
                    {
                        close();
                        break;
                    }
                    buffer.write(b);
                }
                if(buffer.hasCommand())
                {
                    Command cmd=buffer.getCommand();
                    if(cmd.type==0)
                    {
                        String topic=new String(cmd.topic,"utf8");
                        String msg=new String(cmd.msg,"utf8");
                        //System.out.println("Topic:"+topic+":"+msg);
                        if(topic.equals("$ID"))
                        {
                            device=DeviceMgr.getInstance().getDeviceByAuthToken(msg);
                            device.setConnection(this);
                        }else
                        {
                            if(device!=null)
                            {
                                device.receive(topic, msg);
                            }
                        }      
                    }else if(cmd.type==1)               //LOG
                    {
                        device.receiveLog(new String(cmd.topic,"utf8"));
//                    }else if(cmd.type==2)               //LOG
//                    {
//                        device.receiveJSResponse(new String(cmd.topic,"utf8"));
                    }
                }
            }
        } catch (Exception e)
        {
            System.out.println("Clossing Connection...,"+e);
            close();            
        }     
        return ret;
    }    
    
    public boolean isClosed()
    {
        return !running || !sock.isConnected() || sock.isClosed();
    }
    
    public void close()
    {
        running=false;
        try
        {
            if(sock!=null && !sock.isClosed())
            {
                sock.close();
            }
            if(device!=null)
            {
                Device tmp=device;
                device=null;
                tmp.closeConnection();
            }            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public String getInetAddress() {
        return sock.getInetAddress().toString();     
    }

}
