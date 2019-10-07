/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.links;

import io.cloudino.utils.Utils;
import java.io.IOException;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class OCBLink extends DeviceLink {

    private long auth_upd = 0;
    private String token = null;

    public OCBLink(DataObject data, DeviceLinkMgr mgr) {
        super(data, mgr);
    }

    boolean sendContent(String url, String content_type, String content) {
        boolean exist = true;
        {
            String ret = null;
            try 
            {
                ret = Utils.sendData(data.getDataObject("data").getString("serverURL") + url, content, "PUT", content_type, token);
                return true;
            } catch (IOException e) 
            {
                if (e.getMessage().equals("Not Found")) 
                {
                    //System.out.println("Creating Entinty...");
                    try 
                    {
                        String createURI="/v2/entities";
                        String create=data.getDataObject("data").getString("entityDef");
                        if(create==null || create.length()==0)
                        {
                          create="{\"id\": \""+data.getDataObject("data").getString("entityId")+"\"}";
                        }                                                
                        Utils.sendData(data.getDataObject("data").getString("serverURL") + createURI, create, "POST", "application/json", token);
                        //retry ypdate property
                        ret = Utils.sendData(data.getDataObject("data").getString("serverURL") + url, content, "PUT", content_type, token);
                        return true;
                    } catch (IOException e2) 
                    {
                        e2.printStackTrace();
                    }
                }else
                {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    protected boolean ipost(String topic, String msg) {
        //System.out.println("ipos:" + topic + "->" + msg);
        //if(user.length()==0)
        String user = data.getDataObject("data").getString("user");
        String passwd = data.getDataObject("data").getString("password");

        if (user.length() > 0 && (auth_upd == 0 || ((System.currentTimeMillis() - auth_upd) > 59 * 60 * 1000))) {
            String cont = "{\"username\": \"" + user + "\", \"password\":\"" + passwd + "\"}";
            try {
                token = Utils.sendData(data.getDataObject("data").getString("authUrl"), cont, "POST", null, null);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            auth_upd = System.currentTimeMillis();
        }
        if (topic == "$CONTENT") {
            return sendContent("/v2/entities/" + data.getDataObject("data").getString("entityId") + "/attrs", "application/json", msg);
        } else {
            if((msg.startsWith("{") && msg.endsWith("}")) || (msg.startsWith("[") && msg.endsWith("]")))
            {
                return sendContent("/v2/entities/" + data.getDataObject("data").getString("entityId") + "/attrs/" + topic + "/value", "application/json", msg);                
            }else
            {
                String cnt = "\"" + msg + "\"";
                return sendContent("/v2/entities/" + data.getDataObject("data").getString("entityId") + "/attrs/" + topic + "/value", "text/plain", cnt);
            }
        }
    }
}
