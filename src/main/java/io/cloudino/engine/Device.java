/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import io.cloudino.rules.scriptengine.RuleEngineProvider;
import io.cloudino.servlet.WebSocketUserServer;
import io.cloudino.utils.HexSender;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author javiersolis
 */
public class Device 
{
    private final Set<DeviceObserver> observers =new CopyOnWriteArraySet<DeviceObserver>();
    private DeviceMgr mgr=null;
    private String id;
    private DeviceConn con=null;
    private DataObject data=null;
    private DataObject user=null;
    private String inetAddress;
    
    private long createdTime=System.currentTimeMillis();
    private long connectedTime=System.currentTimeMillis();
    
    private Map<String,DataList<DataObject>> events=new ConcurrentHashMap();
    
    public Device(String id, DataObject data, DeviceMgr mgr) 
    {
        this.id=id;
        this.mgr=mgr;
        this.data=data;
    } 

    public String getId() {
        return id;
    }

    public long getConnectedTime() {
        return connectedTime;
    }

    public long getCreatedTime() {
        return createdTime;
    }
    
    public boolean isConnected()
    {
        return con!=null;
    }

    public DataObject getUser() {
        if(user==null)
        {
            SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
            SWBDataSource ds=engine.getDataSource("User"); 
            try
            {
                user=ds.fetchObjById(data.getString("user"));
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return user;
    }
    
    protected void setConnection(DeviceConn con)
    {
        this.con=con;
        connectedTime=System.currentTimeMillis();
        inetAddress=con.getInetAddress();
        
        try
        {
            DataList<DataObject> events=getEvents(RuleEngineProvider.ONDEVICE_CONNECTION);   
            //System.out.println("setConnection events:"+events);
            DataList<DataObject> filter=new DataList();            
            for(DataObject obj:events)
            {
                if("connected".equals(obj.getDataObject("params").getString("action")))
                {
                    filter.add(obj);
                }
            }            
            RuleEngineProvider.invokeEvent(filter, getUser());
        }catch(Exception e)
        {
            e.printStackTrace();
        }                                
    }
    
    protected void closeConnection()
    {
        if(con!=null)
        {
            DeviceConn tmp=con;
            this.con=null;
            tmp.close();
            free();
            try
            {
                DataList<DataObject> events=getEvents(RuleEngineProvider.ONDEVICE_CONNECTION);   
                //System.out.println("closeConnection events:"+events);
                DataList<DataObject> filter=new DataList();            
                for(DataObject obj:events)
                {
                    if("disconnected".equals(obj.getDataObject("params").getString("action")))
                    {
                        filter.add(obj);
                    }
                }            
                RuleEngineProvider.invokeEvent(filter, getUser());
            }catch(Exception e)
            {
                e.printStackTrace();
            }            
        }
    }
    
    protected void free()
    {
        if(this.con==null && observers.isEmpty())mgr.freeDevice(id);
    }
    
    /**
     * Receive message from de device
     * @param topic
     * @param msg 
     */
    protected void receive(String topic,String msg)
    {
        //System.out.println(id+"receive->Topic:"+topic+" msg:"+msg);
        Iterator<DeviceObserver> it=observers.iterator();
        while (it.hasNext()) {
            DeviceObserver observer = it.next();
            try
            {
                observer.notify(topic, msg);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        if(!topic.startsWith("$CDINO"))
        {            
            try
            {
                DataList<DataObject> events=getEvents(RuleEngineProvider.ONDEVICE_MESSAGE_EVENT);            
                RuleEngineProvider.invokeEvent(events, getUser(), topic, msg);
            }catch(Exception e)
            {
                e.printStackTrace();
            }    
            
            WebSocketUserServer.sendData(getUser().getId(), new DataObject().addParam("type", "onDevMsg").addParam("device", getId()).addParam("topic", topic).addParam("msg", msg));
        }
        
    }  
    
    /**
     * Receive Log data from de device
     * @param data 
     */
    protected void receiveLog(String data)
    {
        if(data.startsWith("[CCP]"))
        {
            receiveCompiler(data.substring(5));
            return;
        }
        
        //System.out.println(id+"receive->Log:"+data);
        Iterator<DeviceObserver> it=observers.iterator();
        while (it.hasNext()) {
            DeviceObserver observer = it.next();
            try
            {
                observer.notifyLog(data);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }   
    
//    /**
//     * Receive JavaScript response from the device
//     * @param data 
//     */
//    protected void receiveJSResponse(String data)
//    {
//        System.out.println(id+"receive->JSResp:"+data);
//        Iterator<DeviceObserver> it=observers.iterator();
//        while (it.hasNext()) {
//            DeviceObserver observer = it.next();
//            try
//            {
//                observer.notifyJSResponse(data);
//            }catch(Exception e)
//            {
//                e.printStackTrace();
//            }
//        }
//    }      
    
    protected void receiveCompiler(String data)
    {
        //System.out.println(id+"receive->Compiler:"+data);
        Iterator<DeviceObserver> it=observers.iterator();
        while (it.hasNext()) {
            DeviceObserver observer = it.next();
            try
            {
                observer.notifyCompiler(data);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }     
    
    /**
     * Send data raw to device
     * @param msg
     * @return 
     */
    public boolean postRaw(String msg)
    {
        //System.out.println("postRaw->"+msg);
        try
        {
            if(con!=null)
            {
                con.write(msg);
                return true;
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Send message to device
     * @param topic
     * @param msg
     * @return 
     */
    public boolean post(String topic,String msg)
    {
        //System.out.println("post->Topic:"+topic+" msg:"+msg);
        try
        {
            if(con!=null)
            {
                con.post(topic, msg);
                return true;
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }
    
//    /**
//     * Send Javascript Coommand to device
//     * @param command
//     * @return 
//     */
//    public boolean postJSCommand(String command)
//    {
//        System.out.println("post->JSCommand:"+command);
//        try
//        {
//            if(con!=null)
//            {
//                con.postJSCommand(command);
//                return true;
//            }
//        }catch(Exception e)
//        {
//            e.printStackTrace();
//        }
//        return false;
//    }    
    
    public void registerObserver(DeviceObserver obs)
    {
        if(!observers.contains(obs))
        {
            observers.add(obs);
        }
    }
    
    public void removeObserver(DeviceObserver obs)
    {
        observers.remove(obs);
        free();
    }
    
    /**
     * get Dataobject with the device information
     * @return 
     */
    public DataObject getData()
    {
        return data;
    }
    
    public boolean sendHex(InputStream hex, final Writer sout) throws IOException
    {   
        
        Writer wout=new Writer()
        {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                sout.write(cbuf, off, len);
                receiveCompiler(new String(cbuf, off, len));
            }

            @Override
            public void flush() throws IOException {
                sout.flush();
            }

            @Override
            public void close() throws IOException {
                sout.close();
            }
            
        };
        
        if(con!=null)con.setUploading(true);
        //System.out.println("sendHex...");
        boolean ret=true;
        if(isConnected())
        {
            int speed=57600;
            if(getData()!=null)
            {
                io.cloudino.compiler_.ArdCompiler cmp=io.cloudino.compiler_.ArdCompiler.getInstance();
                String type=getData().getString("type");
                //System.out.println("type:"+type);
                io.cloudino.compiler_.ArdDevice dvc=cmp.getDevices().get(type);
                //System.out.println("dvc:"+dvc);
                if(dvc!=null)speed=dvc.speed;
            }
            //System.out.println("speed:"+speed);
            
            wout.write("Cloudino remote programmer 2015 (v0.1)\n");
            HexSender obj=new HexSender();
            try
            {
                HexSender.Data[] data=obj.readHex(hex);
                wout.write("Connection Opened:\n");
                InputStream in=con.getInputStream();
                OutputStream out=con.getOutputStream();
                con.post("$CDINOUPDT", ""+speed);
                Thread.sleep(400);
                while(in.available()>0)in.read();
                if(!obj.program(data,in,out,wout))
                {
                    wout.write("--Error--");
                    ret=false;
                }
                con.close();
            }catch(Exception e)
            {
                e.printStackTrace();
                ret=false;
            }            
        }else
        {
            ret=false;
        }
        if(con!=null)con.setUploading(false);
        return ret;
    }
    
    public DataList<DataObject> getEvents(String type)
    {
        DataList<DataObject> ret=events.get(type);
        if(ret==null)
        {
            synchronized(events)
            {
                ret=events.get(type);
                if(ret==null)
                {
                    try
                    {
                        ret=RuleEngineProvider.getInstance().getOnEvents(type, getUser(), getId());
                        events.put(type, ret);
                        //System.out.println("Load Events:"+type+"-->"+events);
                    }catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                }                
            }
        }        
        return ret;
    }
    
    public void resetEvents()
    {
        events.clear();
    }

    public String getInetAddress() {
        return inetAddress;
    }
    
}
