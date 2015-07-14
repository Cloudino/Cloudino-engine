/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.servlet;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.semanticwb.datamanager.DataMgr;

@WebListener
public class CloudinoServletContextListener implements ServletContextListener {
    static Logger log=Logger.getLogger(CloudinoServletContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {   
        log.info("Starting Cloudino Portal");
        System.out.println("aplicacion web arrancada");
        DataMgr.createInstance(sce.getServletContext().getRealPath("/"));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("aplicacion web parada");
    }
}
