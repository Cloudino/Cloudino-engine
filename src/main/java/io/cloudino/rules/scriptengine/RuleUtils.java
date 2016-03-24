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
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class RuleUtils 
{        
    private static ApnsService apnsService=null;
    
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
}
