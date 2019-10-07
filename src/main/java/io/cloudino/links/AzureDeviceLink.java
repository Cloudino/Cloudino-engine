/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.links;

import com.microsoft.azure.sdk.iot.device.*;
import io.cloudino.engine.Device;
import io.cloudino.engine.DeviceMgr;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class AzureDeviceLink extends DeviceLink {

    private String connection_str = null;
    private  DeviceClient client=null;
    

    public AzureDeviceLink(DataObject data, DeviceLinkMgr mgr) {
        super(data, mgr);
        connection_str = data.getDataObject("data").getString("connection_str");
    }
    
    private static class EventCallback implements IotHubEventCallback {
      public void execute(IotHubStatusCode status, Object context) {
        System.out.println("IoT Hub responded to message with status: " + status.name());

        if (context != null) {
          synchronized (context) {
            context.notify();
          }
        }
      }
    }        
    
    @Override
    public boolean post(String topic, String message)
    {
        if(data.getBoolean("active"))
        {
            if(client==null)
            {
                try
                {
                    client = new DeviceClient(connection_str, IotHubClientProtocol.MQTT_WS);
                    client.open();    

                    Object context = new Object();
                    client.setMessageCallback((msg, callbackContext) -> {
                        Device dev=DeviceMgr.getInstance().getDevice(getDevice());
                        if(dev!=null)
                        {
                            String content=new String(msg.getBytes());
                            dev.post("", content);
                            System.out.println("body:"+content);
                        }                        
                        //System.out.println("getBytes:"+new String(msg.getBytes()));
                        //System.out.println("getMessageType:"+msg.getMessageType());
                        //System.out.println("getIotHubConnectionString:"+msg.getIotHubConnectionString());
                        MessageProperty props[]=msg.getProperties();
                        for(int x=0;x<props.length;x++)
                        {
                            MessageProperty prop=props[x];
                            dev.post(prop.getName(), prop.getValue());
                            System.out.println("prop:"+prop.getName()+"->"+prop.getValue());  
                        }
                        return IotHubMessageResult.COMPLETE; 
                    },context);                
                }catch(Exception e)
                {
                    client=null;
                }
            }
            return ipost(topic, message);
        }else
        {
            if(client!=null)
            {
                try
                {
                    client.closeNow();
                }catch(IOException e)
                {
                    e.printStackTrace();
                }
                client=null;
            }
        }
        return false;
    }    
    
    protected Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
    
/*    
    protected static Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
      final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
      final String[] pairs = url.getQuery().split("&");
      for (String pair : pairs) {
        final int idx = pair.indexOf("=");
        final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
        if (!query_pairs.containsKey(key)) {
          query_pairs.put(key, new LinkedList<String>());
        }
        final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
        query_pairs.get(key).add(value);
      }
      return query_pairs;
    }    
*/

    @Override
    protected boolean ipost(String topic, String message) {
        System.out.println(topic+":"+message);        
        if(client!=null)
        {
            Message msg=null;
            if(topic.length()==0 || topic.startsWith("?"))
            {
                msg = new Message(message);
                if(topic.length()>0)
                {
                    topic=topic.substring(1);   
                    try
                    {
                        Map<String,String> params=splitQuery(topic);
                        Iterator<String> it=params.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            msg.setProperty(key,params.get(key));
                        }
                    }catch(UnsupportedEncodingException e)
                    {
                        e.printStackTrace();
                    }                    
                }                
            }else
            {
                msg = new Message("{}");
                msg.setProperty(topic, message);
            }
            msg.setMessageId(java.util.UUID.randomUUID().toString()); 

            Object lockobj = new Object();
            EventCallback callback = new EventCallback();
            
            try {
                client.sendEventAsync(msg, callback, lockobj);
                synchronized (lockobj) {
                  lockobj.wait();
                }
            }catch(InterruptedException ie)
            {
                System.out.println("Finished.");
            }        
            return true;
        }else
        {
            return false;
        }
    }
}
