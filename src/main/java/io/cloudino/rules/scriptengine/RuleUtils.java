/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.rules.scriptengine;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import io.cloudino.engine.Device;
import io.cloudino.engine.DeviceMgr;
import io.cloudino.servlet.WebSocketUserServer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author javiersolis
 */
public class RuleUtils 
{        
    private static ApnsService apnsService=null;
    
    private static final ExecutorService proccessor = Executors.newSingleThreadExecutor();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                proccessor.shutdown();
                proccessor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }));
    }
    
    
    static
    {
        //PUSH Notifications Service
        //TODO:configurar esto
        try
        {
            String myCertPath = DataMgr.getApplicationPath()+"/WEB-INF/classes/apn.p12";
            String myPassword = "cloudino";
            apnsService = APNS.newService()
                    .withCert(myCertPath, myPassword)
                    .withSandboxDestination()
                    .build();        
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }    
    
    public static boolean sendMessage(String dev, String topic, String msg)
    {
        boolean ret=false;
        Device device=DeviceMgr.getInstance().getDeviceIfPresent(dev);
        if(device!=null)
        {
            ret=device.post(topic, msg);            
            WebSocketUserServer.sendData(device.getUser().getId(), new DataObject().addParam("type", "onDevMsg").addParam("device", device.getId()).addParam("topic", topic).addParam("msg", msg));
            //Notify WebSockets Rule Message "rmsg"
            device.notifyFromRule(topic, msg);
        }        
        return ret;
    }
    
    public static boolean pushNotification(DataObject _cdino_user, String title, String msg)
    {
        boolean ret=false;
        //System.out.println("_cdino_user:"+_cdino_user+" msg:"+msg);

        if(apnsService!=null)
        {
            //TODO:configurar esto
            String myToken = "25de509de97a9efafaee322eefdb6f051d84580d67830849e350e72ff230b66c";
            if("_suri:Cloudino:User:55e0d655e4b0cb620e1910e5".equals(_cdino_user.getId()))
            {
                String myPayload = APNS.newPayload()
                        //.alertBody(args[0])
                        .alertAction("cloudino")
                        .alertTitle(title)
                        .alertBody(msg)
                        .badge(1)
                        .sound("default")
                        .build();

                apnsService.push(myToken, myPayload);        
            }
        }
        
        return ret;
    }   
    
    public static boolean emailNotification(DataObject _cdino_user, String subject, String msg)
    {
        try
        {
            //System.out.println("user:"+_cdino_user.getString("email")+","+subject+","+msg);
            DataMgr.getUserScriptEngine("/cloudino.js", null).getUtils().sendMail(_cdino_user.getString("email"), subject, msg);
            return true;
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }       
    
    public static boolean smsNotification(DataObject _cdino_user, String number, String msg)
    {
        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js", null);
        
        ScriptObject config = engine.getScriptObject().get("config");
        if (config != null) {
            ScriptObject sms = config.get("sms");
            if (sms != null) {
                String baseUrl = sms.getString("baseUrl");
                String toParam = sms.getString("toParam");
                String textParam = sms.getString("textParam");
                
                proccessor.submit(() -> 
                {
                    try
                    {
                        StringBuilder u=new StringBuilder(baseUrl);
                        if(u.indexOf("?")>-1)u.append("&");
                        else u.append("?");
                        u.append(toParam).append("=").append(number);
                        u.append("&").append(textParam).append("=").append(URLEncoder.encode(msg,"utf8"));     

                        URL url=new URL(u.toString());
                        Object obj=url.getContent();
                        //System.out.println("sms ret:"+obj.getClass()+" "+obj);
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }                    
                });
                return true;
            }
        }
        return false;
    }     
}
