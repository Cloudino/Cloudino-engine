/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.datastreams;

import io.cloudino.engine.Device;
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
public class DataStreamMgr 
{
    private ConcurrentHashMap<String, String> datastreamsByTopic=new ConcurrentHashMap();
    private ConcurrentHashMap<String, DataStream> datastreams=new ConcurrentHashMap();
    private static DataStreamMgr instance=null;

    private DataStreamMgr() {
    }
    
    public static DataStreamMgr getInstance()
    {
        if(instance==null)
        {
            synchronized(DataStreamMgr.class)
            {
                if(instance==null)
                {
                    instance=new DataStreamMgr();
                }
            }
        }
        return instance;
    }    
    
    public void clearCache()
    {
        datastreamsByTopic.clear();
        datastreams.clear();
    }
    
    public void clearCache(String id)
    {
        datastreams.remove(id);
    }    
    
    public void clearCache(String user, String topic)
    {
        DataStream obj=getDataStreamByTopic(user, topic);
        if(obj!=null)
        {
            datastreamsByTopic.remove(user+"_"+topic);
            datastreams.remove(obj.getID());
        }
    }
    
    /**
     * Regresa ID del Dispositivo
     * @param id
     * @return 
     */
    public DataStream getDataStreamByTopic(String user, String topic)
    {
        String id=datastreamsByTopic.get(user+"_"+topic);
        if(id==null)
        {
            synchronized(datastreamsByTopic)
            {
                if(id==null)
                {
                    try
                    {
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        SWBDataSource ds=engine.getDataSource("DataStream");   
                        DataObject query=new DataObject();
                        DataObject data=new DataObject();
                        query.put("data", data);
                        data.put("user", user);
                        data.put("topic", topic);
                        DataObject ret=ds.fetch(query);
                        if(ret!=null)
                        {
                            DataList list=ret.getDataObject("response").getDataList("data");
                            if(list!=null && list.size()>0)
                            {
                                id=list.getDataObject(0).getNumId();
                            }
                        }      
                        if(id!=null)
                        {
                            datastreamsByTopic.put(user+"_"+topic, id);
                        }else
                        {
                            datastreamsByTopic.put(user+"_"+topic, "");
                        }
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(id==null || id.length()==0)return null;
        return getDataStream(id);        
    }       
    
    

    /**
     * Regresa un Objeto DataStream, si esta presente regresa objeto en cache
     * @param id
     * @return 
     */    
    public DataStream getDataStream(String id) {
        if(id==null)return null;
        DataStream dstm=datastreams.get(id);
        if(dstm==null)
        {
            synchronized(datastreams)
            {
                if(dstm==null)
                {
                    try
                    {
                        //TODO: Validar si aqui habra seguridad
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        SWBDataSource ds=engine.getDataSource("DataStream");   
                        DataObject obj=ds.fetchObjById(ds.getBaseUri()+id);
                        if(obj!=null)
                        {
                            dstm=new DataStream(obj,this);
                            datastreams.put(id, dstm);
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
        boolean ret=false;
        DataStream stm=getDataStreamByTopic(dev.getUser().getId(), topic);
        if(stm!=null && stm.isActive())
        {
            ret=stm.stream(dev,msg);
        }
        return ret;
    }
    
    
}
