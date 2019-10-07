/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.links;

import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class DeviceLink 
{
    protected DataObject data=null;
    protected DeviceLinkMgr mgr=null;
    
    public DeviceLink(DataObject data, DeviceLinkMgr mgr) {
        this.data=data;
        this.mgr=mgr;
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
    
    public String getDevice()
    {
        return data.getString("device");
    }            

    public boolean post(String topic, String msg)
    {
        if(data.getBoolean("active"))
        {
            return ipost(topic, msg);
        }
        return false;
    }
    
    protected boolean ipost(String topic, String msg)
    {
        return false;
    }

}
