/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import org.semanticwb.datamanager.DataList;
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
    
    /**
     * Regresa true si el dispositivo esta conectado (online)
     * @param id
     * @return 
     */
    public boolean isDeviceConnected(String id)
    {
        Device dev=devices.get(id);
        if(dev!=null && dev.isConnected())
        {
            return true;
        }
        return false;
    }
    
    /**
     * Regresa dispositivo si esta conectado el dispositivo o un usuario por websockets
     * @param id
     * @return 
     */
    public Device getDeviceIfPresent(String id)
    {
        return devices.get(id);
    }    
    
    /**
     * Regresa un Objeto Device, si esta presente regresa objeto en cache, de lo contrario crea un objeto temporal
     * @param id
     * @return 
     */
    public Device getDevice(String id)
    {
        if(id==null)return null;
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
                        DataObject obj=ds.fetchObjById(ds.getBaseUri()+id);
                        //engine.close();
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
    
    /**
     * Regresa ID del Dispositivo
     * @param id
     * @return 
     */
    public String getDeviceIdByAuthToken(String token)
    {
        //System.out.println("token:"+token);
        String id=null;
        try
        {
            SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
            SWBDataSource ds=engine.getDataSource("Device");   
            DataObject query=new DataObject();
            DataObject data=new DataObject();
            query.put("data", data);
            data.put("authToken", token);
            DataObject ret=ds.fetch(query);
            //System.out.println("query:"+query);
            //System.out.println("ret:"+ret);
            //engine.close();
            if(ret!=null)
            {
                DataList list=ret.getDataObject("response").getDataList("data");
                if(list!=null && list.size()>0)
                {
                    id=list.getDataObject(0).getNumId();
                }
            }
            //System.out.println("id:"+id);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return id;
    }      
    
    /**
     * Regresa un Objeto Device, si esta presente regresa objeto en cache, de lo contrario crea un objeto temporal
     * @param id
     * @return 
     */
    public Device getDeviceByAuthToken(String token)
    {
        String id=getDeviceIdByAuthToken(token);
        if(id!=null)return getDevice(id);
        return null;
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
