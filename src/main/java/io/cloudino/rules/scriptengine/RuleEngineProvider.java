package io.cloudino.rules.scriptengine;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author serch
 */
public class RuleEngineProvider 
{

    private static final Logger logger = Logger.getLogger("i.c.r.se.RuleEngineProvider");
    private final Map<String, SoftReference> hash = new ConcurrentHashMap<>();
    private final int HARD_SIZE;
    private final LinkedList hardCache = new LinkedList();
    private final ReferenceQueue queue = new ReferenceQueue();
    private static RuleEngineProvider instance=null;
    
    public static String ONDEVICE_MESSAGE_EVENT="cdino_ondevice_message";
    public static String ONCHANGE_CONTEXT="cdino_onchange_context";
    public static String ONDEVICE_CONNECTION="cdino_ondevice_connection";

    private RuleEngineProvider() {
        this(100);
    }
    
    private RuleEngineProvider(int size) {
        this.HARD_SIZE = size;
    }
    
    public static RuleEngineProvider getInstance()
    {
        if(instance==null)
        {
            synchronized(RuleEngineProvider.class)
            {
                if(instance==null)
                {
                    instance=new RuleEngineProvider();
                }
            }
        }
        return instance;
    }

    public ScriptEngine getEngine(String id, DataObject user) {
        //System.out.println("getEngine:"+id);
        
        ScriptEngine result = null;
        SoftReference<ScriptEngine> soft_ref = (SoftReference) hash.get(id);
        if (soft_ref != null) {
            result = soft_ref.get();
            if (result == null) {
                hash.remove(id);
            } else {
                synchronized (hardCache) {
                    hardCache.addFirst(result);
                    if (hardCache.size() > HARD_SIZE && !hardCache.isEmpty()) {
                        hardCache.removeLast();
                    }
                }
            }
        }
        //if not in cache recover-create engine
        if (null == result) {
            result = retreiveFromSerialized(id);
            if(result!=null)result.put("_cdino_user", user);
        }
        if (null == result) {
            result = create(id);
            if(result!=null)result.put("_cdino_user", user);
        }
        if (null != result) {            
            put(id, result);
        }
        return result;
    }

    private synchronized void processQueue() {
        SoftValue sv;
        while ((sv = (SoftValue) queue.poll()) != null) {
            hash.remove(sv.key);
            serializeAndSave(sv.key, (ScriptEngine) sv.get());
        }
    }

    private SoftValue put(String key, ScriptEngine value) {
        processQueue();
        return (SoftValue) hash.put(key, new SoftValue(value, key, queue));
    }
    
    public ScriptEngine removeEngine(String id)
    {
        //System.out.println("removeEngine:"+id);
        return remove(id);
    }

    private ScriptEngine remove(Object key) {
        processQueue();
        SoftReference<ScriptEngine> value = hash.remove(key);
        return (null == value) ? null : value.get();
    }

    private ScriptEngine retreiveFromSerialized(String id) {
        ScriptEngine result = null;
        try {
            String state = getStringFromCloudRule(id, "state");
            //System.out.println("CloudRule state:"+state);
            if (null != state) {
                result = NashornEngineFactory.getEngine(300, TimeUnit.MILLISECONDS);
                result.eval(state);
            }
        } catch (InterruptedException | IOException | ScriptException exp) {
            logger.log(Level.WARNING, "can't create script with id: " + id, exp);
            result = null;
        }
        return result;
    }

    private ScriptEngine create(String id) {
        ScriptEngine result = null;
        try {
            String script = getStringFromCloudRule(id, "script");
            //System.out.println("CloudRule script:"+script);
            if (null != script) {
                result = NashornEngineFactory.getEngine(300, TimeUnit.MILLISECONDS);
                result.eval(script);
            }
        } catch (InterruptedException | IOException | ScriptException exp) {
            logger.log(Level.WARNING, "can't create script with id: " + id, exp);
            result = null;
        }
        return result;
    }

    private String getStringFromCloudRule(String id, String field) throws IOException {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
        SWBDataSource ds = engine.getDataSource("CloudRule");
        DataObject cloudRule = ds.fetchObjById(id);        
        //System.out.println("getStringFromCloudRule: "+field+" "+cloudRule.getString(field));
        return cloudRule!=null?cloudRule.getString(field):null;
    }

    private void serializeAndSave(String id, ScriptEngine scriptEngine) {
        try { 
            //System.out.println("Serializing....");
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
            SWBDataSource ds = engine.getDataSource("CloudRule");
            DataObject cloudRule = ds.fetchObjById(id);
            cloudRule.put("state", NashornEngineFactory.serialize(scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)));
            ds.updateObj(cloudRule);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static class SoftValue extends SoftReference {

        private final String key;

        private SoftValue(Object value, String key, ReferenceQueue q) {
            super(value, q);
            this.key = key;
        }
    }
    
    /**
     * Invoca todos los eventos que cuplan con el criterio establecido
     * @param events
     * @param functParams
     * @throws IOException 
     */
    public static void invokeEvent(DataList<DataObject> events, DataObject user, Object... functParams) throws IOException
    {
        RuleEngineProvider rep = RuleEngineProvider.getInstance();
        for(DataObject obj : events)
        {                
            ScriptEngine engine = rep.getEngine(obj.getString("cloudRule"),user);
            if(engine!=null)
            {
                ScriptObjectMirror evtents=(ScriptObjectMirror)engine.get("_cdino_events");        
                ScriptObjectMirror event=(ScriptObjectMirror)evtents.getSlot(obj.getInt("arrayIndex"));
                //System.out.println("event:"+event);
                event.callMember("funct", functParams);
                //out.println();        
            }
        }        
    }
    
    /**
     * Regresa CloudRuleEvent que cumplan con el criterio definido
     * @param type
     * @param user
     * @param params
     * @return
     * @throws IOException 
     */
    public DataList getOnEvents(String type, DataObject user, String devid) throws IOException
    {
        String context=user.getString("userContext");
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", user);
        SWBDataSource ds = engine.getDataSource("CloudRuleEvent");
        DataObject obj=new DataObject();
        DataObject data=obj.addSubObject("data").addParam("type", type).addParam("user", user.getId());
        if(context!=null)data.addParam("context", context);
        data.addParam("params.device", devid);
        //System.out.println("getOnEvents:"+obj);
        DataObject ret=ds.fetch(obj);
        return ret.getDataObject("response").getDataList("data");
    }  

}
