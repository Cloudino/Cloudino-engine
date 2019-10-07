/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.servlet;

import io.cloudino.engine.Command;
import io.cloudino.engine.CommandBuffer;
import io.cloudino.engine.Device;
import io.cloudino.engine.DeviceConn;
import io.cloudino.engine.DeviceMgr;
import io.cloudino.engine.DeviceObserver;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
@ServerEndpoint(value = "/websocket/cdino")
public class WebSocketServer implements DeviceObserver
{
    private static final String GUEST_PREFIX = "Guest";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<WebSocketServer> connections =new CopyOnWriteArraySet<WebSocketServer>();
    private static final CommandBuffer buffer=new CommandBuffer();    
    
    private Device device=null;

    private Session session;

    public WebSocketServer() {
    }


    @OnOpen
    public void start(Session session) {
        this.session = session;
        List<String> ID=session.getRequestParameterMap().get("ID");
        String id=(ID!=null && ID.size()>0)?ID.get(0):null;
        if(id!=null)
        {
            device=DeviceMgr.getInstance().getDevice(id);
            if(device!=null)device.registerObserver("WebSocketServer",this);
        }
        //System.out.println("WebSockets Connection:"+id+" device:"+device);
        connections.add(this);
    }


    @OnClose
    public void end() {
        connections.remove(this);
        if(device!=null)
        {
            device.removeObserver("WebSocketServer");
        }
        //System.out.println("end:"+device);
    }

    @OnMessage
    public void incoming(String message) {
        //System.out.println("incoming:"+message);
        //"|M"+topic.length+"|"+topic+"S"+message.length+"|"+message;
        if(device!=null)
        {
            device.postRaw(message);
            WebSocketUserServer.sendRawData(device.getUser().getId(),device.getId(),message);
            
            try
            {
                buffer.write(message);
                while(buffer.hasCommand())
                {
                    Command cmd=buffer.getCommand();
                    if(cmd.type==Command.TYPE_MSG)
                    {
                        device.setDeviceData(new String(cmd.topic,"utf8"), new String(cmd.msg,"utf8"));
                    }
                }
            }catch(Exception e)
            {
                e.printStackTrace();
            }                
            
        }
        
    }

    @OnError
    public void onError(Throwable t) throws Throwable {
        t.printStackTrace();
    }

    @Override
    public void notify(String topic, String msg) throws IOException
    {
        session.getBasicRemote().sendText("msg:"+topic+"\t"+msg);
    }
    
    @Override
    public void notifyFromRule(String topic, String msg) throws IOException
    {
        session.getBasicRemote().sendText("rmsg:"+topic+"\t"+msg);
    }    

    @Override
    public void notifyLog(String data) throws IOException {
        session.getBasicRemote().sendText("log:"+data);
    }

    @Override
    public void notifyCompiler(String data) throws IOException {
        session.getBasicRemote().sendText("cmp:"+data);
    }

//    @Override
//    public void notifyJSResponse(String data) throws IOException {
//        session.getBasicRemote().sendText("jsr:"+data);
//    }
    
}
