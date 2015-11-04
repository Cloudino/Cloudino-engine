package io.cloudino.rules.scriptengine;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 *
 * @author serch
 */
public class NashornEngineFactory {
    
    private static final Logger logger = Logger.getLogger("i.c.r.se.NashornEngineFactory");
    
    //ScriptEngines to create al launch;
    final static private int size = 50; 
    final static private Executor service = Executors.newWorkStealingPool();
    //give the internal array some extra room
    final static private BlockingQueue<ScriptEngine> cola = new ArrayBlockingQueue<>((int)(size*110/100)); 
    final static ScriptEngineManager factory = new ScriptEngineManager();
    final static Runnable task = ()-> {
            try {
                cola.offer(factory.getEngineByName("nashorn"), 1, TimeUnit.MINUTES);
            } catch (InterruptedException ie) {
                logger.log(Level.WARNING, "Got interrupted while wating to add an engine", ie);
            }
        };
    static {
        for(int i=0; i<size; i++)
            service.execute(task);
    }

    /**
     * Obtain a pre-created Nashorn ScriptEngine
     * @param timeout time to wait for an engine
     * @param unit units of timeout
     * @return a Nashorn ScriptEngine or null if the time elapses before creating a new one
     * @throws InterruptedException 
     */
    public static ScriptEngine getEngine(final long timeout, final TimeUnit unit) throws InterruptedException {
        service.execute(task);
        return cola.poll(timeout, unit);
    }
    
    public static String serialize(Object obj) {
        StringBuilder ret = new StringBuilder();
        if (obj instanceof ScriptObjectMirror) {
            ScriptObjectMirror om = (ScriptObjectMirror) obj;
//            System.out.println(om+" isArray "+om.isArray());
//            System.out.println(om+" isEmpty "+om.isEmpty());
//            System.out.println(om+" isExtensible "+om.isExtensible());
//            System.out.println(om+" isFrozen "+om.isFrozen());
//            System.out.println(om+" isFunction "+om.isFunction());
//            System.out.println(om+" isSealed "+om.isSealed());
//            System.out.println(om+" isStrictFunction "+om.isStrictFunction());            
//            System.out.println(om+" getOwnKeys "+Arrays.asList(om.getOwnKeys(true)));  

            if (om.isFunction()) {
                ret.append(om.toString());
            } else if (om.isArray()) {
                ret.append("[");
                //ret.append("isArray:"+om.toString());
                for (int x = 0; x < om.size(); x++) {
                    Object o = om.getSlot(x);
                    ret.append(serialize(o));
                    if (x + 1 < om.size()) {
                        ret.append(",");
                    }
                }
                ret.append("]");
            } else if (om.toString().contains("global")) {
                Iterator<Map.Entry<String, Object>> it = om.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = it.next();
                    ret.append("var ")
                            .append(entry.getKey())
                            .append("=")
                            .append(serialize(entry.getValue()))
                            .append(";\n");
                }
            } else {
                ret.append("{");
                Iterator<Map.Entry<String, Object>> it = om.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = it.next();
                    ret.append(entry.getKey())
                            .append(":")
                            .append(serialize(entry.getValue()));
                    if (it.hasNext()) {
                        ret.append(",");
                    }
                }
                ret.append("}");
            }
        } else if (obj instanceof String) {
            ret.append("\"")
                    .append(obj)
                    .append("\"");
        } else {
            ret.append(obj);
        }
        return ret.toString();
    }
    
}
