/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.engine;

import io.cloudino.server.DeviceServer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author javiersolis
 */
public class DeviceBaseConn extends DeviceConn {

    static Logger log = Logger.getLogger(DeviceBaseConn.class.getName());

    private Socket sock = null;
    private DeviceServer server = null;
    private boolean running = true;

    /**
     * Create an instance of Device Conenction
     *
     * @param sock
     * @param server
     * @throws IOException
     */
    public DeviceBaseConn(Socket sock, DeviceServer server) throws IOException {
        log.log(Level.OFF, "Connection open");
        this.sock = sock;
        this.server = server;
        this.outputStream = sock.getOutputStream();
        this.inputStream = sock.getInputStream();
    }

    public boolean loop() {
        //System.out.println("loop");
        boolean ret = false;
        try {
            if (!uploading) {
                if (device != null && System.currentTimeMillis() - time > 30000) {
                    time = System.currentTimeMillis();
                    //System.out.println("Ping");
                    write((byte) '_');
                }
                if(inputStream.available()>0)ret = true;
                while (inputStream.available() > 0) {
                    //System.out.println(inputStream.available());
                    //time=System.currentTimeMillis();
                    int b = inputStream.read();
                    if (b == -1) {
                        close();
                        break;
                    }
                    buffer.write(b);
                }
            }
        } catch (IOException e) {
            log.log(Level.OFF, "Closing Connection...");
            e.printStackTrace();
            close();
        }
        return ret;
    }
    
    public boolean hasCommands()
    {
        return buffer.hasCommand();
    }
    
    public List<Command> getCommands()
    {
        ArrayList<Command> arr=new ArrayList();
        while(buffer.hasCommand())
        {
            arr.add(buffer.getCommand());
        }
        return  arr;
    }
    
    public boolean processCommands(List<Command> cmds) throws UnsupportedEncodingException
    {
        boolean ret = true;
        Iterator<Command> it=cmds.iterator();
        while (it.hasNext()) {
            Command cmd = it.next();        
            try
            {
                if (cmd.type == Command.TYPE_MSG) {
                    if (cmd.topic != null) {
                        String topic = new String(cmd.topic, "utf8");
                        String msg = "";
                        if (cmd.msg != null) {
                            msg = new String(cmd.msg, "utf8");
                        }
                        //System.out.println("Topic:"+topic+":"+msg);
                        if (topic.equals("$ID")) {
                            device = DeviceMgr.getInstance().getDeviceByAuthToken(msg);
                            if (device != null) {
                                device.setConnection(this);
                                device.synchDeviceData();
                            } else {
                                log.log(Level.OFF, "Not AuthToken found:" + msg);
                                close();
                            }
                        } else {
                            if (device != null) {
                                device.receive(topic, msg);
                            }
                        }
                    }
                } else if (cmd.type == Command.TYPE_LOG) //LOG
                {
                    String msg = "";
                    if (cmd.topic != null) {
                        msg = new String(cmd.topic, "utf8");
                    }
                    device.receiveLog(msg);
    //                    }else if(cmd.type==2)               //LOG
    //                    {
    //                        device.receiveJSResponse(new String(cmd.topic,"utf8"));
                } else if (cmd.type == Command.TYPE_BIN) {
                    if (cmd.topic != null) {
                        String topic = new String(cmd.topic, "utf8");
                        device.setDeviceData(topic, cmd.msg);
                    }
                }
            }catch(Exception e)
            {
                ret=false;
                e.printStackTrace();
            }
        }
        return ret;
    }

    public boolean isClosed() {
        return !running || !sock.isConnected() || sock.isClosed();
    }

    public void close() {
        running = false;
        try {
            if (sock != null && !sock.isClosed()) {
                sock.close();
            }
            if (device != null) {
                Device tmp = device;
                device = null;
                tmp.closeConnection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getInetAddress() {
        return sock.getInetAddress().toString();
    }

}
