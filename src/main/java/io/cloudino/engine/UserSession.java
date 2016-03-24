/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class UserSession 
{
    DataObject user;
    private final Set<UserObserver> observers =new CopyOnWriteArraySet<UserObserver>();
    private long createdTime=System.currentTimeMillis();
    private long connectedTime=System.currentTimeMillis();

    public UserSession(DataObject user) {
        this.user=user;
    }

    public DataObject getUser() {
        return user;
    }
    
    public void registerObserver(UserObserver obs)
    {
        if(!observers.contains(obs))
        {
            observers.add(obs);
        }
    }
    
    public void removeObserver(UserObserver obs)
    {
        observers.remove(obs);
    }
    
}
