/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import io.cloudino.utils.HexSender;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class Device 
{
    private static final Set<Observer> observers =new CopyOnWriteArraySet<Observer>();
    private DeviceMgr mgr=null;
    private String id;
    private DeviceConn con=null;
    private DataObject data=null;
    
    private long createdTime=System.currentTimeMillis();
    private long connectedTime=System.currentTimeMillis();

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
    
    protected void setConnection(DeviceConn con)
    {
        this.con=con;
        connectedTime=System.currentTimeMillis();
    }
    
    protected void closeConnection()
    {
        if(con!=null)
        {
            DeviceConn tmp=con;
            this.con=null;
            tmp.close();
            free();
        }
    }
    
    protected void free()
    {
        if(this.con==null && observers.isEmpty())mgr.freeDevice(id);
    }
    
    public void pushNotification(String topic, String msg)
    {
        //TODO:configurar esto
        String myCertPath = DataMgr.getApplicationPath()+"/WEB-INF/classes/apn.p12";
        String myPassword = "cloudino";
        //TODO:registrar Token
        String myToken = "25de509de97a9efafaee322eefdb6f051d84580d67830849e350e72ff230b66c";
        if(!"softjei@gmail.com".equals(data.getString("email")))
        {
            return;
        }

        ApnsService service = APNS.newService()
                .withCert(myCertPath, myPassword)
                .withSandboxDestination()
                .build();
        String myPayload = APNS.newPayload()
                //.alertBody(args[0])
                .alertAction(topic)
                .alertTitle("Cloudino")
                .alertBody(topic+":"+msg)
                .badge(1)
                .sound("default")
                .build();
        service.push(myToken, myPayload);        
    }
    
    protected void receive(String topic,String msg)
    {
        System.out.println("receive->Topic:"+topic+" msg:"+msg);
        Iterator<Observer> it=observers.iterator();
        while (it.hasNext()) {
            Observer observer = it.next();
            try
            {
                observer.notify(topic, msg);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        //TODO: Validar cuales llevan PN
        pushNotification(topic, msg);
        
    }  
    
    protected void receiveLog(String data)
    {
        System.out.println("receive->Log:"+data);
        Iterator<Observer> it=observers.iterator();
        while (it.hasNext()) {
            Observer observer = it.next();
            try
            {
                observer.notifyLog(data);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }    
    
    protected void receiveCompiler(String data)
    {
        System.out.println("receive->Log:"+data);
        Iterator<Observer> it=observers.iterator();
        while (it.hasNext()) {
            Observer observer = it.next();
            try
            {
                observer.notifyCompiler(data);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }     
    
    public boolean postRaw(String msg)
    {
        System.out.println("postRaw->"+msg);
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
    
    public boolean post(String topic,String msg)
    {
        System.out.println("post->Topic:"+topic+" msg:"+msg);
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
    
    public void registerObserver(Observer obs)
    {
        if(!observers.contains(obs))
        {
            observers.add(obs);
        }
    }
    
    public void removeObserver(Observer obs)
    {
        observers.remove(obs);
        free();
    }
    
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
        System.out.println("sendHex...");
        boolean ret=true;
        if(isConnected())
        {
            int speed=57600;
            if(getData()!=null)
            {
                io.cloudino.compiler_.ArdCompiler cmp=io.cloudino.compiler_.ArdCompiler.getInstance();
                String type=getData().getString("type");
                System.out.println("type:"+type);
                io.cloudino.compiler_.ArdDevice dvc=cmp.getDevices().get(type);
                System.out.println("dvc:"+dvc);
                if(dvc!=null)speed=dvc.speed;
            }
            System.out.println("speed:"+speed);
            
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
}
