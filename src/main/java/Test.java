
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class Test {
    public static void main(String[] args) throws Exception
    {
        DataMgr.createInstance(null);
        
        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
        SWBDataSource ds=engine.getDataSource("Device");        
        ds.addObj((DataObject)DataObject.parseJSON("{\"hola\":\"mundo\"}"));
        engine.close();
    }
}
