
import io.cloudino.rules.scriptengine.RuleEngineProvider;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.semanticwb.datamanager.DataMgr;
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
public class JavaScriptTest {

    public static void main(String[] args) throws ScriptException {
        DataMgr.createInstance(null);
        RuleEngineProvider rep = new RuleEngineProvider(10);
        ScriptEngine engine = rep.getEngine("_suri:Cloudino:CloudRule:1");
        engine = rep.getEngine("_suri:Cloudino:CloudRule:2");
        engine = rep.getEngine("_suri:Cloudino:CloudRule:3");
        engine = rep.getEngine("_suri:Cloudino:CloudRule:4");
        engine = rep.getEngine("_suri:Cloudino:CloudRule:5");
        engine = rep.getEngine("_suri:Cloudino:CloudRule:6");
        ScriptObjectMirror bind = ((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE));
        ScriptEngine[] arr1 = useLotsOfMemory();
        System.gc();
        ((ScriptObjectMirror) ((ScriptObjectMirror) bind.get("events")).getSlot(0)).callMember("funct", "hola");
        arr1 = useLotsOfMemory();
        arr1[500].eval("print('memorias....')");
        arr1=null;
        System.gc();
        engine = rep.getEngine("_suri:Cloudino:CloudRule:1");
        bind = ((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE));
        ((ScriptObjectMirror) ((ScriptObjectMirror) bind.get("events")).getSlot(0)).callMember("funct", "hola");
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
            } else if (om.toString().indexOf("global") > -1) {
                Iterator<Map.Entry<String, Object>> it = om.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = it.next();
                    ret.append("var " + entry.getKey() + "=" + serialize(entry.getValue()) + ";\n");
                }
            } else {
                ret.append("{");
                Iterator<Map.Entry<String, Object>> it = om.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Object> entry = it.next();
                    ret.append(entry.getKey() + ":" + serialize(entry.getValue()));
                    if (it.hasNext()) {
                        ret.append(",");
                    }
                }
                ret.append("}");
            }
        } else if (obj instanceof String) {
            ret.append("\"" + obj + "\"");
        } else {
            ret.append(obj);
        }
        return ret.toString();
    }

    public static void main3(String[] args) throws Exception {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        //engine.eval("function p(x){print(x);};var c=function(x){print(x);};var i=function(){x++;};var x=10;var cap=function(z){return function(msg){print(msg+\" \"+z);}}(x);");
        String script = ""
                + "var events=[];\n"
                + "var msg;\n"
                + "{ \n"
                + "	var _cntx=\"contx1\"; \n"
                + "	events.push({cntx:_cntx,type:\"cdino_ondevice_message\",funct:function(msg){print(msg);},params:{topic:\"topic\"}});\n"
                + "}";
        engine.eval(script);
        ScriptObjectMirror bind = ((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE));
        //bind.callMember("i");
        //bind.callMember("cap","hola");

        String txt = serialize(bind);
        System.out.println(txt);

        //System.out.println(xml);
//        long time=System.currentTimeMillis();
//        for(int x=0;x<1000;x++)
//        {
//            engine = factory.getEngineByName("JavaScript");
//            engine.eval(txt);
//            bind=((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE));
//            bind.callMember("i");
////            bind=(ScriptObjectMirror)engine.createBindings();
////            bind.eval(txt);
////            bind.callMember("i");
//            txt=serialize(bind);
//        }
//        System.out.println("Time:"+(System.currentTimeMillis()-time));
        engine = factory.getEngineByName("JavaScript");
        engine.eval(txt);

        bind = ((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE));

        ((ScriptObjectMirror) ((ScriptObjectMirror) bind.get("events")).getSlot(0)).callMember("funct", "hola");

        //bind.callMember("i");
        //bind.callMember("cap","hola");
        //System.out.println(engine.getFactory().getParameter("THREADING"));
        txt = serialize(bind);
        System.out.println(txt);
    }

    public static void main2(String[] args) throws Exception {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        System.out.println(((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());
        System.out.println("x:" + engine.get("x"));
        System.out.println("var x:" + engine.eval("function p(x){print(x);};x=10;"));
        System.out.println("x:" + engine.get("x"));
        System.out.println(((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());

        ScriptObjectMirror bind = ((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE));
        bind.callMember("p", "Hola");

        engine = factory.getEngineByName("JavaScript");
        //engine.setBindings(engine.createBindings(),ScriptContext.ENGINE_SCOPE);

        System.out.println(((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());
        System.out.println("x:" + engine.get("x"));
        System.out.println("var x:" + engine.eval("function p(x){print(x);};x=10;"));
        System.out.println("x:" + engine.get("x"));
        System.out.println(((ScriptObjectMirror) engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());

    }

    private static ScriptEngine[] useLotsOfMemory() {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engines[] = new ScriptEngine[10000];
        for (int i = 0; i < 10000; i++) {
            engines[i] = factory.getEngineByName("nashorn");
        }
        return engines;
    }
}
