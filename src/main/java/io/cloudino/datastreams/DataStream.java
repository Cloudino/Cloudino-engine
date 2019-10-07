/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.datastreams;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import io.cloudino.engine.Device;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.datastore.DataStoreMongo;
import org.semanticwb.datamanager.datastore.SWBDataStore;

/**
 *
 * @author javiersolis
 */
public class DataStream {
    public static int MAXCACHE=1440;
    
    DataObject data=null;
    DataStreamMgr mgr=null;
    Calendar lastDate=null;
    
    private static SWBDataStore dataStore=null;    
    private static SWBDataSource dataSourceData=null;
    private static SWBDataSource dataSourceDef=null;
    
    private ConcurrentHashMap<String,LinkedList<DataObject>> cache=new ConcurrentHashMap();
    
    public DataStream(DataObject data, DataStreamMgr mgr) {
        this.data=data;
        this.mgr=mgr;
        
        if(dataStore==null)
        {
            SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
            this.dataSourceData=engine.getDataSource("DataStreamData");   
            this.dataSourceDef=engine.getDataSource("DataStream");   
            String dataStore=dataSourceData.getDataSourceScript().getString("dataStore");
            this.dataStore=engine.getDataStore(dataStore);
        }
        
        //load cache
        try
        {
            //System.out.println("data:"+data);
            
            DataObject query=new DataObject();    
            query.addSubList("sortBy").add("-timestamp");
            query.addParam("endRow", 100);
            DataObject qdata=query.addSubObject("data");
            qdata.addParam("dataStream", data.getNumId());
            DataList rdata=dataSourceData.fetch(query).getDataObject("response").getDataList("data");
            if(rdata==null)rdata=new DataList();
            
//            //System.out.println("rdata:"+rdata);
//            DataObject samples=data.getDataObject("samples");
//            if(samples!=null)
//            {
//                DataObject devices=samples.getDataObject("devices");
//                String devs[]=devices.keySet().toArray(new String[0]);
//            }

            Iterator<DataObject> it=rdata.iterator();
            while (it.hasNext()) 
            {
                DataObject rec = it.next();
                DataObject devices=rec.getDataObject("devices");
                String devs[]=devices.keySet().toArray(new String[0]);

                for(int d=0;d<devs.length;d++)
                {
                    LinkedList list=cache.get(devs[d]);
                    if(list==null)
                    {
                        list=new LinkedList();
                        cache.put(devs[d], list);
                    }                    
                    
                    for(int m=59;m>=0;m--)
                    {
                        for(int s=59;s>=0;s--)
                        {
                            DataObject minute=devices.getDataObject(devs[d]).getDataObject(""+m);
                            if(minute!=null)
                            {
                                Object second=minute.get(""+s);                                
                                if(validateObject(second))
                                {
                                    Date timestamp=(Date)rec.get("timestamp");
                                    
                                    Calendar cal=Calendar.getInstance();
                                    cal.setTime(timestamp);
                                    cal.set(Calendar.MINUTE, m);
                                    cal.set(Calendar.SECOND, s);                                    
                                    
                                    DataObject obj=new DataObject();
                                    obj.addParam("data", second);
                                    obj.addParam("timestamp", cal.getTime());

                                    list.add(obj);
                                    if(list.size()>MAXCACHE)list.removeLast();                                    
                                }
                            }
                        }
                    }            
                }
            }       
                        
        }catch(Exception e)
        {
            e.printStackTrace();
        }

        
    }

    public DataObject getData() {
        return data;
    }        
    
    public String getID()
    {
        return data.getNumId();
    }
    
    public String getUser()
    {
        return data.getString("user");
    }
    
    public String getTopic()
    {
        return data.getString("topic");
    }          
    
    public boolean isActive()
    {
        return data.getBoolean("active");
    }

    public Map<String, LinkedList<DataObject>> getCache() {
        return cache;
    }
    
    public List<DataObject> getCache(String devid) {
        return cache.get(devid);
    }    
    
    public boolean validateObject(Object data)
    {
        if(data==null)return false;
        if(data instanceof DataObject)
        {
            return validateDataObject((DataObject)data);            
        }
        return true;
    }
    
    public boolean validateDataObject(DataObject data)
    {
        if(data==null)return false;
        DataList list=getData().getDataList("fields");
        for(int x=0;x<list.size();x++)
        {
            DataObject field=list.getDataObject(x);
            String prop=field.getString("name");
            Object value=data.get("prop");
            if(value!=null)
            {
                if(!validateValue(value, field))return false;
            }
        }
        return true;
    }    
    
    public boolean validateValue(Object value, String property)
    {
        if(value==null)return false;
        DataObject field=getData().getDataList("fields").findDataObject("name", property);        
        return validateValue(value, field);
    }
    
    public boolean validateValue(Object value, DataObject field)
    {
        if(value==null)return false;
        String type=field.getString("type");
        if(type.equals("int") || type.equals("double"))
        {
            double val=0;
            if(value instanceof Double) val=(Double)value;
            else if(value instanceof Integer) val=(double)(Integer)value;
            else if(value instanceof String)
            {
                try
                {
                    val=Double.parseDouble((String)value);
                }catch(NumberFormatException e)
                {
                    return false;
                }
            }
            
            Object minValue=field.get("minValue");
            Object maxValue=field.get("maxValue");        
            
            if(minValue!=null)
            {
                
                if(minValue instanceof Integer)
                {
                    if(val<(Integer)minValue)return false;             
                }
                if(minValue instanceof Double)
                {
                    if(val<(Double)minValue)return false;           
                }
            }
            if(maxValue!=null)
            {
                if(maxValue instanceof Integer)
                {
                    if(val>(Integer)maxValue)return false;             
                }
                if(maxValue instanceof Double)
                {
                    if(val>(Double)maxValue)return false;           
                }
            }            
        }
        return true;
    }    
    
    public boolean stream(Device dev, String msg)
    {
        return stream(new Date(), dev, msg, true);
    }
    
    public boolean stream(Date timestamp, Device dev, String msg, boolean addCache)
    {
        boolean ret=false;        
        Calendar d=Calendar.getInstance();
        d.setTime(timestamp);
        d.set(Calendar.MILLISECOND, 0);
        int m=d.get(Calendar.MINUTE);
        int s=d.get(Calendar.SECOND);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        
        Object msgobj=DataObject.parseJSON(msg);
        if(!validateObject(msgobj))return false;
        
        //add cache
        if(addCache)
        {
            DataObject obj=new DataObject();
            obj.addParam("data", msgobj);
            obj.addParam("timestamp", timestamp);
            LinkedList list=cache.get(dev.getId());
            if(list==null)
            {
                list=new LinkedList();
                cache.put(dev.getId(), list);
            }
            list.addFirst(obj);
            if(list.size()>MAXCACHE)list.removeLast();
        }        
        
        try
        {
            DataObject obj=new DataObject();
            if(lastDate==null)
            {
                DataObject data=obj.addSubObject("data");
                data.addParam("dataStream", getID());
                data.addParam("timestamp", d.getTime());
                DataObject res=dataSourceData.fetch(obj);
                if(res!=null)
                {
                    DataList list=res.getDataObject("response").getDataList("data");
                    if(list!=null && list.size()>0)
                    {
                        lastDate=d;
                    }
                }      
                
            }
            if(!d.equals(lastDate))
            {
                DataObject data=obj.addSubObject("data");
                data.addParam("dataStream", getID());
                data.addParam("timestamp", d.getTime());
                data.addSubObject("samples").addParam("total", 1).addSubObject("devices").addParam(dev.getId(),1);                
                data.addSubObject("devices").addSubObject(dev.getId()).addSubObject(""+m).addParam(""+s, msgobj);            
                dataSourceData.add(obj);
            }else
            {
                if(dataStore instanceof DataStoreMongo)
                {
                    DataStoreMongo mongo=(DataStoreMongo)dataStore;
                    MongoClient mongoClient=mongo.getMongoClient();
                    String modelid=dataSourceData.getModelId();
                    String scls=dataSourceData.getClassName();
                    DB db = mongoClient.getDB(modelid);
                    DBCollection coll = db.getCollection(scls);
                    
                    DataObject query=new DataObject();
                    query.addParam("dataStream", getID());
                    query.addParam("timestamp", d.getTime());
                    
                    DataObject data=new DataObject();
                    data.addSubObject("$set").addParam("devices."+dev.getId()+"."+m+"."+s, msgobj);  
                    data.addSubObject("$inc").addParam("samples.total", 1).addParam("samples.devices."+dev.getId(), 1);
                    
                    coll.update(mongo.toBasicDBObject(query), mongo.toBasicDBObject(data));                    
                }
            }            
            {
                DataStoreMongo mongo=(DataStoreMongo)dataStore;
                MongoClient mongoClient=mongo.getMongoClient();
                String modelid=dataSourceDef.getModelId();
                String scls=dataSourceDef.getClassName();
                DB db = mongoClient.getDB(modelid);
                DBCollection coll = db.getCollection(scls);

                DataObject query=new DataObject();
                query.addParam("_id", data.getId());

                DataObject data=new DataObject();
                data.addSubObject("$inc").addParam("samples.total", 1).addParam("samples.devices."+dev.getId(), 1);

                coll.update(mongo.toBasicDBObject(query), mongo.toBasicDBObject(data)); 
            }
            ret=true;
        }catch(IOException e)
        {
            e.printStackTrace();
        }      
        lastDate=d;
        return ret;
    }
    
    /**
     * return StreamData from any device
     * @param Date from 
     * @param Date to
     * @param int type, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND
     * @return 
     */    
    public DataObject getStreamData(Date from, Date to, int type)
    {
        return (DataObject)getSData(from, to, type, null, null);
    }
    
    public DataList getStreamData(Date from, Date to, int type, String devid)
    {
        return (DataList)getSData(from, to, type, devid, null);
    }    
    
    /**
     * return StreamData from specific device
     * @param Date from 
     * @param Date to
     * @param int type, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND
     * @param String devid
     * @return 
     */
    public DataList getStreamData(Date from, Date to, int type, String devid, String property)
    {
        return (DataList)getSData(from, to, type, devid, property);
    }
    
    
    /**
     * return StreamData from specific device
     * @param Date from 
     * @param Date to
     * @param int type, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND
     * @param String devid
     * @return 
     */
    private Object getSData(Date from, Date to, int type, String devid, String property)
    {
        Date ifrom=from;
        //getStart Hour
        if(from!=null)
        {
            Calendar cal=Calendar.getInstance();
            cal.setTime(from);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);   
            ifrom=cal.getTime();
        }
        
        DataObject ret=new DataObject();
        //load cache
        try
        {
            //System.out.println("data:"+data);
            
            DataObject query=new DataObject();    
            query.addSubList("sortBy").add("-timestamp");
            //query.addParam("endRow", 100);
            DataObject qdata=query.addSubObject("data");
            qdata.addParam("dataStream", data.getNumId());
            if(from!=null || to!=null)
            {
                DataObject ts=qdata.addSubObject("timestamp");
                if(ifrom!=null)ts.addParam("$gte", ifrom);
                if(to!=null)ts.addParam("$lt", to);            
            }
            DataList rdata=dataSourceData.fetch(query).getDataObject("response").getDataList("data");
            if(rdata==null)rdata=new DataList();            

            Iterator<DataObject> it=rdata.iterator();
            while (it.hasNext()) 
            {
                DataObject rec = it.next();
                DataObject devices=rec.getDataObject("devices");
                String devs[]=devices.keySet().toArray(new String[0]);

                for(int d=0;d<devs.length;d++)
                {          
                    if(devid!=null && !devid.equals(devs[d]))continue;
                    
                    DataList list=ret.getDataList(devs[d]);
                    if(list==null)
                    {
                        list=new DataList();
                        ret.put(devs[d], list);
                    }                     
                                        
                    for(int m=59;m>=0;m--)
                    {
                        for(int s=59;s>=0;s--)
                        {
                            DataObject minute=devices.getDataObject(devs[d]).getDataObject(""+m);
                            if(minute!=null)
                            {
                                DataObject second=minute.getDataObject(""+s);
                                if(second!=null)
                                {
                                    Date timestamp=(Date)rec.get("timestamp");
                                    
                                    Calendar cal=Calendar.getInstance();
                                    cal.setTime(timestamp);
                                    cal.set(Calendar.MINUTE, m);
                                    cal.set(Calendar.SECOND, s);   
                                    
                                    if((from==null || cal.getTime().getTime()>=from.getTime()) && (to==null || cal.getTime().getTime()<to.getTime()))
                                    {
                                        Object val=second;
                                        if(property!=null)
                                        {
                                            val=second.get(property);
                                            if(!validateValue(val, property))val=null;
                                        }else
                                        {
                                            if(!validateObject(val))val=null;
                                        }
                                        if(val!=null)
                                        {
                                            DataObject obj=new DataObject();
                                            obj.addParam("data", val);
                                            obj.addParam("timestamp", cal.getTime());
                                            list.add(obj);                                        
                                        }
                                    }
                                }
                            }
                        }
                    }            
                }
            }       
                        
        }catch(Exception e)
        {
            e.printStackTrace();
        }        
        if(devid!=null)return ret.get(devid);
        return ret;
    }
}
