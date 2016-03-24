/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import io.cloudino.server.DeviceSSLServer;
import io.cloudino.server.handlers.PacketChannel;
import io.cloudino.server.handlers.PacketChannelListener;
import io.cloudino.server.io.ProtocolDecoder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author javiersolis
 */
public class DeviceSSLConn extends DeviceConn implements PacketChannelListener
{
    private SocketChannel sock = null;
    private DeviceSSLServer server = null;
    
    
    /**
     * Create an instance of Device Conenction
     * @param sock
     * @param server
     * @throws IOException 
     */
    public DeviceSSLConn(SocketChannel sc, DeviceSSLServer server) throws IOException
    {
        System.out.println("Connection open:"+sc.socket().getInetAddress());
        this.sock = sc;
        this.server = server;
        
        this.outputStream=sock.socket().getOutputStream();
        this.inputStream = sock.socket().getInputStream();        
        
        DeviceSSLConn _this=this;
        
        try {
            sc.socket().setReceiveBufferSize(2 * 1024);
            sc.socket().setSendBufferSize(2 * 1024);
            
            PacketChannel pc = new PacketChannel(sc, server.getChannelFactory(), server.getSelectorThread(),
                new ProtocolDecoder() {

                    @Override
                    public ByteBuffer decode(ByteBuffer socketBuffer) throws IOException 
                    {
                        while (socketBuffer.hasRemaining()) 
                        {
                              // Copies into the temporary buffer
                              byte b = socketBuffer.get();
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
                                    device.setConnection(_this);
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
                        
                        return null;
                    }
                },
                this);
            pc.resumeReading();
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
    
    public boolean isClosed()
    {
        return !sock.isConnected() || !sock.isOpen();
    }
    
    public void close()
    {
        try
        {
            if(sock!=null && sock.isOpen())
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
    public void packetArrived(PacketChannel pc, ByteBuffer pckt) {
        System.out.println("packetArrived");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void packetSent(PacketChannel pc, ByteBuffer pckt) {
        System.out.println("packetSent");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void socketException(PacketChannel pc, Exception ex) {
        System.out.println("socketException:"+pc);
        server.removeDeviceConn(this);
        ex.printStackTrace();
    }

    @Override
    public void socketDisconnected(PacketChannel pc) {
        System.out.println("Remove Connection:"+pc);
        server.removeDeviceConn(this);
    }

    @Override
    public String getInetAddress() {
        return sock.socket().getInetAddress().toString();     
    }
}

