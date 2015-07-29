/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author javiersolis
 */
public class DeviceMgr 
{
    private ConcurrentHashMap<String, Device> devices=new ConcurrentHashMap();
    private static DeviceMgr instance=null;

    private DeviceMgr() {
    }
    
    public static DeviceMgr getInstance()
    {
        if(instance==null)
        {
            synchronized(DeviceMgr.class)
            {
                if(instance==null)
                {
                    instance=new DeviceMgr();
                }
            }
        }
        return instance;
    }
    
    public Device getDevice(String id)
    {
        Device dev=devices.get(id);
        if(dev==null)
        {
            synchronized(devices)
            {
                if(dev==null)
                {
                    dev=new Device(id,this);
                    devices.put(id, dev);
                }
            }
        }
        return dev;
    }
    
    public void freeDevice(String id)
    {
        devices.remove(id);
    }
    
    public Iterator<Device>listDevices()
    {
        return devices.values().iterator();
    }
    
}
