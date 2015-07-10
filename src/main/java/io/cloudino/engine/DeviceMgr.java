/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author javiersolis
 */
public class DeviceMgr 
{
    ConcurrentHashMap<String, Device> devices=new ConcurrentHashMap();
    
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
    
}
