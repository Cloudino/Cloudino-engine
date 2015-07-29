/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.servlet;

import io.cloudino.engine.Device;
import io.cloudino.engine.DeviceMgr;
import io.cloudino.engine.Observer;
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

/**
 *
 * @author javiersolis
 */
@ServerEndpoint(value = "/websocket/cdino")
public class WebSocketServer implements Observer
{
    private static final String GUEST_PREFIX = "Guest";
    private static final AtomicInteger connectionIds = new AtomicInteger(0);
    private static final Set<WebSocketServer> connections =new CopyOnWriteArraySet<WebSocketServer>();
    //private static final LinkedList<Message> messages=new LinkedList();    
    
    private Device device=null;

    private final String nickname;
    
    private Session session;

    public WebSocketServer() {
        nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
    }


    @OnOpen
    public void start(Session session) {
        this.session = session;
        List<String> ID=session.getRequestParameterMap().get("ID");
        String id=(ID!=null && ID.size()>0)?ID.get(0):null;
        if(id!=null)
        {
            device=DeviceMgr.getInstance().getDevice(id);
            if(device!=null)device.registerObserver(this);
        }
        System.out.println(id);
        connections.add(this);
        String message = String.format("* %s %s", nickname, "has joined.");
        System.out.println("start:"+message);
        //broadcast(message);
    }


    @OnClose
    public void end() {
        connections.remove(this);
        if(device!=null)
        {
            device.removeObserver(this);
        }
        String message = String.format("* %s %s", nickname, "has disconnected.");
        System.out.println("end:"+message);
        //broadcast(message);
    }


    @OnMessage
    public void incoming(String message) {
        // Never trust the client
        String filteredMessage = String.format("%s: %s", nickname, message.toString());
        //broadcast(filteredMessage);
        System.out.println("incoming:"+message);
        device.postRaw(message);

/*        
        StringTokenizer st=new StringTokenizer(message," ");
        if (st.hasMoreTokens())
        {
             String suri=st.nextToken();
             String sstat=st.nextToken();

            String uri=SemanticObject.shortToFullURI(suri);
            GenericObject obj=SemanticObject.createSemanticObject(uri).createGenericInstance();
            if(obj instanceof DomNodeDevice)
            {        
                DomNodeDevice dev = (DomNodeDevice)obj;
                dev.setStatus(Integer.parseInt(sstat));
            }else if(obj instanceof DomGroup)
            {
                DomGroup grp=(DomGroup)obj;
                grp.setStatus(Integer.parseInt(sstat));
            }else if(obj instanceof DomContext)
            {
                DomContext cnt=(DomContext)obj;
                cnt.setActive(Boolean.parseBoolean(sstat));
            }
        }         
*/        
    }

    @OnError
    public void onError(Throwable t) throws Throwable {
        t.printStackTrace();
    }

/*
    public static void broadcast(String msg) {
        for (WebSocketServer client : connections) {
            try {
                synchronized (client) {
                    client.session.getBasicRemote().sendText(msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
                //log.debug("Chat Error: Failed to send message to client", e);
                connections.remove(client);
                try {
                    client.session.close();
                } catch (IOException e1) {
                    // Ignore
                }
                String message = String.format("* %s %s", client.nickname, "has been disconnected.");
                broadcast(message);
            }
        }
        
        messages.add(new Message(System.currentTimeMillis(), msg));
    }
    
    public static List<String> getLastMessages()
    {
        long time=System.currentTimeMillis()-4000;
        ArrayList arr=new ArrayList();
        Iterator<Message> it=messages.listIterator();
        while (it.hasNext())
        {
            Message message = it.next();
            if(message.getTime()>time)
            {
                arr.add(message.getMessage());
            }else
            {
                it.remove();
            }
        }
        return arr;
    }    
*/    

    @Override
    public void notify(String topic, String msg) throws IOException
    {
        session.getBasicRemote().sendText("msg:"+topic+"\t"+msg);
    }

    @Override
    public void notifyLog(String data) throws IOException {
        session.getBasicRemote().sendText("log:"+data);
    }
    
}

/*
class Message
{
    private long time;
    private String message=null;

    public Message(long time, String message)
    {
        this.time = time;
        this.message=message;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }
    
    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

}
*/
