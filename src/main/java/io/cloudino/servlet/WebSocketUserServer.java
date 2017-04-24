/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.servlet;

import io.cloudino.engine.Command;
import io.cloudino.engine.CommandBuffer;
import io.cloudino.engine.Device;
import io.cloudino.engine.DeviceMgr;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author javiersolis
 */
@ServerEndpoint(value = "/websocket/user")
public class WebSocketUserServer
{
    private static final ConcurrentHashMap<String,ArrayList<WebSocketUserServer>> connections =new ConcurrentHashMap();
    
    private DataObject user=null;

    private Session session;

    public WebSocketUserServer() {
    }

    @OnOpen
    public void start(Session session) {
        this.session = session;
        //System.out.println("WebSockets User Connection");
    }


    @OnClose
    public void end() {
        if(user!=null)
        {
            ArrayList<WebSocketUserServer> arr=connections.get(user.getId());
            if(arr!=null)connections.get(user.getId()).remove(this);
        }
        //System.out.println("end user:"+user);
    }

    @OnMessage
    public void incoming(String message) 
    {
        int status=1;
        //System.out.println(message);
        DataObject msg=(DataObject)DataObject.parseJSON(message);
        String type=msg.getString("type");
        String tid=msg.getString("tid");
        if(user==null && !type.equals("login"))
        {
            end();
            return;
        }
        switch(type)
        {
            case "login":   
                {
                    SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
                    SWBDataSource ds = engine.getDataSource("User");

                    String email = msg.getString("email");
                    String password = msg.getString("password");

                    if (email != null && password != null) 
                    {
                        try
                        {
                            DataObject r = new DataObject();
                            DataObject data = r.addSubObject("data");
                            data.put("email", email);
                            data.put("password", password);
                            data.put("active", "true");
                            DataObject ret = ds.fetch(r);

                            DataList rdata = ret.getDataObject("response").getDataList("data");
                            if (!rdata.isEmpty()) {
                                user = (DataObject) rdata.get(0);
                                user.put("isSigned", "true");
                                user.put("signedAt", java.time.ZonedDateTime.now().toString());
                                if (!user.containsKey("registro")) {
                                    user.put("registeredAt", java.time.ZonedDateTime.now().toString());
                                }
                                status=0;
                                //addConnection
                                ArrayList<WebSocketUserServer> arr=connections.get(user.getId());
                                if(arr==null)
                                {
                                    arr=new ArrayList();
                                    connections.put(user.getId(), arr);
                                }
                                arr.add(this);
                            }
                        }catch(IOException e)
                        {
                            e.printStackTrace();
                        }
                    }       
                }
                break;
            case "post2Device":
                {
                    SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
                    SWBDataSource ds = engine.getDataSource("Device");
                    
                    String dev = msg.getString("device");
                    String topic = msg.getString("topic");
                    String mssg = msg.getString("msg");
                    
                    Device device=DeviceMgr.getInstance().getDeviceIfPresent(dev);
                    if(device!=null && device.getUser().getId().equals(user.getId()))
                    {
                        if(device.post(topic, mssg))
                        {
                            device.notifyFromRule(topic, mssg);
                            status=0;
                        }
                    }    
                }
                break;
            default: //post                
                break;                    
        }
        DataObject resp=new DataObject();
        DataObject response=resp.addSubObject("response").addParam("status", status);        
        if(tid!=null)
        {
            response.addParam("tid", tid);
        }
        sendData(resp);        
    }
    
    public boolean sendData(DataObject msg)
    {
        try
        {
            //DataObject tx=new DataObject();
            //tx.putAll(msg);
            //tx.addParam("tid", UUID.randomUUID());
            //session.getBasicRemote().sendText(tx.toString());
            session.getBasicRemote().sendText(msg.toString());
            //TODO:recive confirmation...
            return true;
        }catch(IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    public static boolean sendData(String userid, DataObject msg)
    {
        ArrayList<WebSocketUserServer> arr=connections.get(userid);
        if(arr!=null)
        {
            Iterator<WebSocketUserServer>it=arr.iterator();
            while (it.hasNext()) {
                WebSocketUserServer webSocketUserServer = it.next();
                webSocketUserServer.sendData(msg);
            }
        }
        return false;
    }
    
    public static void sendRawData(String userid, String devid, String data)
    {
        ArrayList<WebSocketUserServer> arr=connections.get(userid);
        if(arr!=null)
        {
            try
            {
                CommandBuffer buf=new CommandBuffer();
                buf.write(data);
                if(buf.hasCommand())
                {
                    Command cmd=buf.getCommand();
                    if(cmd.type==0)
                    {
                        String topic=new String(cmd.topic,"utf8");
                        String msg=new String(cmd.msg,"utf8");
                        //System.out.println("Topic:"+topic+":"+msg);
                        if(!topic.equals("$ID"))
                        {                
                            sendData(userid, new DataObject().addParam("type", "onDevMsg").addParam("device", devid).addParam("topic", topic).addParam("msg", msg));
                        }
                    }
                }  
            }catch(UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }
    }
    
            

}
