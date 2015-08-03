/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

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
                    try
                    {
                        //TODO: Validar si aqui habra seguridad
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        SWBDataSource ds=engine.getDataSource("Device");   
                        DataObject obj=ds.fetchObjById("_suri:Cloudino:Device:"+id);
                        engine.close();
                        if(obj==null)obj=new DataObject();
                        dev=new Device(id,obj,this);
                        devices.put(id, dev);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
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
