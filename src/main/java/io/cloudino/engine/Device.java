/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import io.cloudino.datastreams.DataStreamMgr;
import io.cloudino.links.DeviceLinkMgr;
import io.cloudino.rules.scriptengine.RuleEngineProvider;
import io.cloudino.servlet.WebSocketUserServer;
import io.cloudino.utils.HexSender;
import io.cloudino.utils.Utils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.DataObjectIterator;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author javiersolis
 */
public class Device 
{
    private final ConcurrentHashMap<String,DeviceObserver> observers =new ConcurrentHashMap();
    private DeviceMgr mgr=null;
    private String id;
    private DeviceConn con=null;
    private DataObject data=null;
    private DataObject devData=null;
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
     * Receive message from rule
     * @param topic
     * @param msg 
     */
    public void notifyFromRule(String topic,String msg)
    {
        //System.out.println(id+"receive->Topic:"+topic+" msg:"+msg);
        Iterator<DeviceObserver> it=observers.values().iterator();
        while (it.hasNext()) {
            DeviceObserver observer = it.next();
            try
            {
                observer.notifyFromRule(topic, msg);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Receive message from de device
     * @param topic
     * @param msg 
     */
    public void receive(String topic,String msg)
    {
        //System.out.println(id+"receive->Topic:"+topic+" msg:"+msg);
        //Filter special tags
        if(msg!=null)
        {
            if(msg.startsWith("{"))
            {
                msg.replace("$ISODATE", Utils.toISODate(new Date()));
            }else
            {
                if(msg.equals("$ISODATE"))
                {
                    msg=Utils.toISODate(new Date());
                }
            }
        }
        
        Iterator<DeviceObserver> it=observers.values().iterator();
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
        
        //Notify DataStreams
        DataStreamMgr.getInstance().onMessage(this, topic, msg);
        
        //Notify DeviceLinks
        DeviceLinkMgr.getInstance().onMessage(this, topic, msg);
        
        setDeviceData(topic, msg);        
    }  
    
    /**
     * Receive Log data from de device
     * @param data 
     */
    public void receiveLog(String data)
    {
        if(data.startsWith("[CCP]"))
        {
            receiveCompiler(data.substring(5));
            return;
        }
        
        //System.out.println(id+"receive->Log:"+data);
        Iterator<DeviceObserver> it=observers.values().iterator();
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
    
    public void receiveCompiler(String data)
    {
        //System.out.println(id+"receive->Compiler:"+data);
        Iterator<DeviceObserver> it=observers.values().iterator();
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
        //"|M"+topic.length+"|"+topic+"S"+message.length+"|"+message;
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
            setDeviceData(topic, msg);
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
    
    public void registerObserver(String key, DeviceObserver obs)
    {
        if(!observers.contains(obs))
        {
            observers.put(key, obs);
        }
    }
    
    public DeviceObserver getObserver(String key)
    {
        return observers.get(key);
    }    
    
    public void removeObserver(String key)
    {
        observers.remove(key);
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
    
    private void setDeviceData(DataObject devData)
    {    
        this.devData=devData;
    }
    
    /**
     * Get DeviceData stored values  
     * @return DataObject
     */
    public DataObject getDeviceData()
    {
        if(devData==null)
        {
            SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",getUser());
            SWBDataSource ds=engine.getDataSource("DeviceData"); 
            try
            {
                DataObject query=new DataObject();
                query.addSubObject("data").addParam("device", getId());
                DataObjectIterator it=ds.find(query);
                if(it.hasNext())devData=it.next();
                else{
                    devData=new DataObject();
                    devData.addParam("device", getId());
                    devData.addSubObject("data");
                }
            }catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return devData;
    }    
    
    /**
     * Get Temporal device data
     * @param topic
     * @return 
     */
    public Object getDeviceData(String topic)
    {
        return _getDeviceData(getDeviceData().getDataObject("data"), topic);
    }
    
    private Object _getDeviceData(DataObject data, String topic)
    {
        if(data==null)return null;
        int i=topic.indexOf('.');
        if(i>-1)
        {
            DataObject ret=data.getDataObject(topic.substring(0,i));
            return _getDeviceData(ret, topic.substring(i+1));
        }else return data.get(topic);
    }    
    
    /**
     * Post DeviceData do Device
     */
    public void synchDeviceData()
    {
        DataList dataModel=getData().getDataList("dataModel");  
        if(dataModel!=null)
        {
            for(int x=0;x<dataModel.size();x++)
            {
                String topic=dataModel.getDataObject(x).getString("topic");
                Object val=getDeviceData(topic);
                if(val!=null)
                {
                    post(topic, val.toString());
                }
            }
        }
    }
    
    /**
     * Set temporal device data
     * @param topic
     * @param val 
     * @return boolean
     */
    public boolean setDeviceData(String topic, Object val)
    {   
        boolean add=true;
        try
        {
            DataList dataModel=getData().getDataList("dataModel");
            if(dataModel!=null)
            {
                DataObject field=dataModel.findDataObject("topic", topic);
                if(field!=null)
                {
                    String type=field.getString("type");
                    String minValue=field.getString("minValue");
                    String maxValue=field.getString("maxValue");
                    if("string".equals(type))
                    {           
                        String v=null;
                        if(val instanceof byte[])
                            v=new String((byte[])val);
                        else v=val.toString();
                        if(add)_setDeviceData(topic, v, false);
                    }else if("boolean".equals(type))
                    {           
                        boolean v=Boolean.parseBoolean(val.toString());
                        if(add)_setDeviceData(topic, v, false);
                    }else if("object".equals(type))
                    {
                        String v=null;
                        if(val instanceof byte[])
                            v=new String((byte[])val);
                        else v=val.toString();
                        Object obj=DataObject.parseJSON(v);               
                        if(add)_setDeviceData(topic, obj, false);
                    }else if("binary".equals(type))
                    {
                        byte v[]=null;
                        if(val instanceof byte[])
                        {
                            v=(byte[])val;
                        }else if(val instanceof String)
                        {
                            v=((String)val).getBytes("utf8");
                        }
                        if(add)_setDeviceData(topic, (byte[])val, true);       
                    }else if("int".equals(type))
                    {
                        int v=Integer.parseInt(val.toString());
                        if(minValue!=null && v<Integer.parseInt(minValue))
                        {
                            add=false;
                        }
                        if(maxValue!=null && v>Integer.parseInt(maxValue))
                        {
                            add=false;
                        }
                        if(add)_setDeviceData(topic, v, false);
                    }else if("double".equals(type))
                    {                    
                        double v=Double.parseDouble(val.toString());
                        if(minValue!=null && v<Double.parseDouble(minValue))
                        {
                            add=false;
                        }
                        if(maxValue!=null && v>Double.parseDouble(maxValue))
                        {
                            add=false;
                        }
                        if(add)_setDeviceData(topic, v, false);
                    }
                }else
                {
                    add=false;
                }
            }else
            {
                add=false;
            }
        }catch(Exception e)
        {
            e.printStackTrace();
            add=false;
        }
        return add;
    }    
    
    /**
     * Set temporal device data
     * @param topic
     * @param val
     */
    protected void _setDeviceData(String topic, Object val, boolean fileStore)
    {
        //System.out.println("setDeviceData:"+topic+" fileStore:"+fileStore);        
        DataObject dd=getDeviceData();
        dd.getDataObject("data").put(topic, val);
        dd.addParam("timestamp", new Date());
        
        //store value to DB
        try
        {
            SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",getUser());
            SWBDataSource ds=engine.getDataSource("DeviceData"); 
            if(dd.getId()==null)
            {
                dd=ds.addObj(dd).getDataObject("response").getDataObject("data");
                setDeviceData(dd);
            }else
            {
                ds.updateObj(dd);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        
        
        if(fileStore && val instanceof byte[])
        {
            byte data[]=(byte[])val;
            try
            {
                String appPath = DataMgr.getApplicationPath();
                String path=appPath+"/work/cloudino/devices/"+getId()+"/";
                (new File(path)).mkdirs();

                FileOutputStream fout=new FileOutputStream(new File(path,topic.replace(' ', '_')));
                fout.write(data);
                fout.close();

                fout=new FileOutputStream(new File(path,topic.replace(' ', '_')+"_"+Utils.toISODate(new Date())));
                fout.write(data);
                fout.close();
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        
    }
    
}
