
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

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
    
    public static String serialize(Object obj)
    {
        StringBuilder ret=new StringBuilder();
        if(obj instanceof ScriptObjectMirror)
        {
            ScriptObjectMirror om=(ScriptObjectMirror)obj; 
//            System.out.println(om+" isArray "+om.isArray());
//            System.out.println(om+" isEmpty "+om.isEmpty());
//            System.out.println(om+" isExtensible "+om.isExtensible());
//            System.out.println(om+" isFrozen "+om.isFrozen());
//            System.out.println(om+" isFunction "+om.isFunction());
//            System.out.println(om+" isSealed "+om.isSealed());
//            System.out.println(om+" isStrictFunction "+om.isStrictFunction());            
//            System.out.println(om+" getOwnKeys "+Arrays.asList(om.getOwnKeys(true)));            
            if(om.isFunction())
            {
                ret.append(om.toString());
            }else
            {
                Iterator<Map.Entry<String,Object>> it=om.entrySet().iterator();
                while (it.hasNext()) {
                   Map.Entry<String, Object> entry = it.next();
                   ret.append("var "+entry.getKey()+"="+serialize(entry.getValue())+";\n");
               }
            }
        }else
        {
            ret.append(obj.toString());
        }
        return ret.toString();
    }
    
    public static void main(String[] args) throws Exception
    {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        engine.eval("function p(x){print(x);};var c=function(x){print(x);};var i=function(){x++;};var x=10;var cap=function(z){return function(msg){print(msg+\" \"+z);}}(x);");
        ScriptObjectMirror bind=((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE));
        bind.callMember("i");
        bind.callMember("cap","hola");
        
        String txt=serialize(bind);
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

        bind=((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE));
        bind.callMember("i");
        //bind.callMember("cap","hola");
        
        System.out.println(engine.getFactory().getParameter("THREADING"));
        
        txt=serialize(bind);
        System.out.println(txt);        
    }

    public static void main2(String[] args) throws Exception {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        System.out.println(((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());
        System.out.println("x:"+engine.get("x"));
        System.out.println("var x:"+engine.eval("function p(x){print(x);};x=10;"));
        System.out.println("x:"+engine.get("x"));
        System.out.println(((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());
        
        ScriptObjectMirror bind=((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE));
        bind.callMember("p", "Hola");
        
        engine = factory.getEngineByName("JavaScript");
        //engine.setBindings(engine.createBindings(),ScriptContext.ENGINE_SCOPE);
        
        System.out.println(((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());
        System.out.println("x:"+engine.get("x"));
        System.out.println("var x:"+engine.eval("function p(x){print(x);};x=10;"));
        System.out.println("x:"+engine.get("x"));
        System.out.println(((ScriptObjectMirror)engine.getBindings(ScriptContext.ENGINE_SCOPE)).size());
        

    }
}
