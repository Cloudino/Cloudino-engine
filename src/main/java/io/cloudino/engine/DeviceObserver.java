/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.io.IOException;

/**
 *
 * @author javiersolis
 */
public interface DeviceObserver {
    public void notify(String topic, String msg) throws IOException;
    public void notifyLog(String data) throws IOException;
    //public void notifyJSResponse(String data) throws IOException;
    public void notifyCompiler(String data) throws IOException;
    
}
