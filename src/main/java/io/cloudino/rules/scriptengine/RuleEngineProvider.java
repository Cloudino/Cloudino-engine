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
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author serch
 */
public class RuleEngineProvider {

    private static final Logger logger = Logger.getLogger("i.c.r.se.RuleEngineProvider");
    private final Map<String, SoftReference> hash = new ConcurrentHashMap<String, SoftReference>();
    private final int HARD_SIZE;
    private final LinkedList hardCache = new LinkedList();
    private final ReferenceQueue queue = new ReferenceQueue();

    public RuleEngineProvider() {
        this(100);
    }

    public RuleEngineProvider(int size) {
        this.HARD_SIZE = size;
    }

    public ScriptEngine getEngine(String id) {
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
        }
        if (null == result) {
            result = create(id);
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

    private ScriptEngine put(String key, ScriptEngine value) {
        processQueue();
        return (ScriptEngine) hash.put(key, new SoftValue(value, key, queue)).get();
    }

    private ScriptEngine remove(Object key) {
        processQueue();
        SoftReference<ScriptEngine> value = hash.remove(key);
        return (null == value) ? null : value.get();
    }

    private ScriptEngine retreiveFromSerialized(String id) {
        return null; //TODO: implementa retreive serialized bindings
    }

    private ScriptEngine create(String id) {
        ScriptEngine result;
        try {
            SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
            SWBDataSource ds = engine.getDataSource("CloudRule");
            DataObject cloudRule = ds.fetchObjById(id);
            String script = cloudRule.getString("script");
            result = NashornEngineFactory.getEngine(300, TimeUnit.MILLISECONDS);
            result.eval(script);
        } catch (InterruptedException | IOException | ScriptException exp) {
            logger.log(Level.WARNING, "can't create script with id: "+ id, exp);
            result = null;
        }
        return result;
    }

    private void serializeAndSave(String  key, ScriptEngine scriptEngine) {
        //TODO: serialize bindings and save to some storage
    }

    private static class SoftValue extends SoftReference {

        private final String key;

        private SoftValue(Object value, String key, ReferenceQueue q) {
            super(value, q);
            this.key = key;
        }
    }

}
