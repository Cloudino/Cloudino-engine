/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.links;

import io.cloudino.engine.Device;
import java.util.ArrayList;
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
public class DeviceLinkMgr 
{
    private ConcurrentHashMap<String, ArrayList<String>> deviceLinksByDevice=new ConcurrentHashMap();
    private ConcurrentHashMap<String, DeviceLink> deviceLinks=new ConcurrentHashMap();
    private static DeviceLinkMgr instance=null;

    private DeviceLinkMgr() {
    }
    
    public static DeviceLinkMgr getInstance()
    {
        if(instance==null)
        {
            synchronized(DeviceLinkMgr.class)
            {
                if(instance==null)
                {
                    instance=new DeviceLinkMgr();
                }
            }
        }
        return instance;
    }    
    
    public void clearCache()
    {
        deviceLinksByDevice.clear();
        deviceLinks.clear();
    }
    
    public void clearCache(String id)
    {
        deviceLinks.remove(id);
    }    
    
    public void clearCacheByDevice(String dev_id)
    {
        ArrayList<String> arr=getDeviceLinksByDevice(dev_id);
        Iterator<String> it=arr.iterator();
        while (it.hasNext()) {
            String id = it.next();
            clearCache(id);
        }
        deviceLinksByDevice.remove(dev_id);
    }
    
    /**
     * Regresa ID del Dispositivo
     * @param id
     * @return 
     */
    public ArrayList<String> getDeviceLinksByDevice(String dev_id)
    {
        ArrayList ids=deviceLinksByDevice.get(dev_id);
        if(ids==null)
        {
            synchronized(deviceLinksByDevice)
            {
                if(ids==null)
                {
                    try
                    {
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        SWBDataSource ds=engine.getDataSource("DeviceLinks");   
                        DataObject query=new DataObject();
                        DataObject data=new DataObject();
                        query.put("data", data);
                        data.put("device", dev_id);
                        DataObject ret=ds.fetch(query);
                        ids=new ArrayList();
                        if(ret!=null)
                        {
                            DataList list=ret.getDataObject("response").getDataList("data");
                            if(list!=null)
                            {
                                for(int x=0;x<list.size();x++)
                                {
                                    ids.add(list.getDataObject(x).getNumId());
                                }
                            }
                        }      
                        deviceLinksByDevice.put(dev_id, ids);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ids;       
    }       
    
    

    /**
     * Regresa un Objeto DataStream, si esta presente regresa objeto en cache
     * @param id
     * @return 
     */    
    public DeviceLink getDeviceLink(String id) {
        if(id==null)return null;
        DeviceLink dstm=deviceLinks.get(id);
        if(dstm==null)
        {
            synchronized(deviceLinks)
            {
                if(dstm==null)
                {
                    try
                    {
                        //TODO: Validar si aqui habra seguridad
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        SWBDataSource ds=engine.getDataSource("DeviceLinks");   
                        DataObject obj=ds.fetchObjById(ds.getBaseUri()+id);
                        if(obj!=null)
                        {
                            if("OCB".equals(obj.getString("type")))
                            {
                                dstm=new OCBLink(obj,this);                                
                            }else if("AzureDevice".equals(obj.getString("type")))
                            {
                                dstm=new AzureDeviceLink(obj,this);                                
                            }else
                            {
                                dstm=new DeviceLink(obj,this);
                            }
                            deviceLinks.put(id, dstm);
                        }
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return dstm;
    }
    
    public boolean onMessage(Device dev, String topic, String msg)
    {
        Iterator<String> it=getDeviceLinksByDevice(dev.getId()).iterator();
        boolean ret=it.hasNext();
        while (it.hasNext()) {
            String id = it.next();
            DeviceLink link=getDeviceLink(id);
            if(!link.post(topic,msg))ret=false;
        }
        return ret;
    }    
    
}
