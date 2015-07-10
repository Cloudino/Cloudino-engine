/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

/**
 *
 * @author javiersolis
 */
public class Device 
{
    DeviceMgr mgr=null;
    private String id;

    public Device(String id, DeviceMgr mgr) 
    {
        this.id=id;
        this.mgr=mgr;
    }
    
    public void free()
    {
        mgr.freeDevice(id);
    }
    
}
