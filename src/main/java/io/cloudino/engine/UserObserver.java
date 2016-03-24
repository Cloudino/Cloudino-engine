/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.io.IOException;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public interface UserObserver {
    public void notify(DataObject msg) throws IOException;
}
