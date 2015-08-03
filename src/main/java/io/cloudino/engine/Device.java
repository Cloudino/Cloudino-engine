/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import io.cloudino.utils.HexSender;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class Device 
{
    private static final Set<Observer> observers =new CopyOnWriteArraySet<Observer>();
    DeviceMgr mgr=null;
    private String id;
    DeviceConn con=null;
    DataObject data=null;

    public Device(String id, DataObject data, DeviceMgr mgr) 
    {
        this.id=id;
        this.mgr=mgr;
        this.data=data;
    } 

    public String getId() {
        return id;
    }
    
    public boolean isConnected()
    {
        return con!=null;
    }
    
    protected void setConnection(DeviceConn con)
    {
        this.con=con;
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
        observers.add(obs);
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
    
    public boolean sendHex(InputStream hex, PrintWriter sout)
    {
        boolean ret=false;
        if(isConnected())
        {
            con.post("$CDINOUPDT", id);
            
            sout.println("Cloudino remote programmer 2015 (v0.1)");
            HexSender obj=new HexSender();
            try
            {
                HexSender.Data[] data=obj.readHex(hex);
                Integer speed=Integer.parseInt("57600");
                sout.println("Connection Opened:");
                InputStream in=con.getInputStream();
                OutputStream out=con.getOutputStream();
                out.write((byte)0);             //init
                out.write(ByteBuffer.allocate(4).putInt(speed).array()); 
                out.flush();
                Thread.sleep(400);
                if(!obj.program(data,in,out))System.out.println("--Error--");
                con.close();
            }catch(Exception e)
            {
                e.printStackTrace();
            }            
            
            
            
            
        }
        return ret;
    }
}
