/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

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

    public Device(String id, DeviceMgr mgr) 
    {
        this.id=id;
        this.mgr=mgr;
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
    
}
