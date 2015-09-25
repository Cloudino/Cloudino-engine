/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.servlet;

import io.cloudino.server.DeviceServer;
import io.cloudino.servlet.router.Router;
import io.cloudino.utils.FileUploadUtils;
import java.io.File;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.script.ScriptObject;

@WebListener
public class CloudinoServletContextListener implements ServletContextListener {
    static Logger log=Logger.getLogger("i.c.s.CloudinoServletContextListener");

    @Override
    public void contextInitialized(ServletContextEvent sce) {   
        log.info("Starting Cloudino Portal");
        System.out.println("aplicacion web arrancada");
        DataMgr.createInstance(sce.getServletContext().getRealPath("/"));
        FileUploadUtils.init((File)sce.getServletContext().getAttribute("javax.servlet.context.tempdir"));
        log.info("Cloudino DataMgr Started");
        
        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
        log.info("Cloudino SWBScriptEngine Started");
        DeviceServer server = new DeviceServer();
        server.setPort(engine.getScriptObject().get("config").getInt("devicePort"));
        server.start();   
        log.info("Configuring Router");
        ScriptObject ros = engine.getScriptObject().get("routes");
        Router.initRouter(ros);
        log.info("Router configured");
//        SWBDataSource ds=engine.getDataSource("Device");
//        try
//        {
//            DataObject obj=ds.fetch();
//            log.info(""+obj);
//        } catch (IOException ioe) {log.throwing("CloudinoServletContextListener","contextInitialized", ioe);}
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("aplicacion web parada");
    }
}
