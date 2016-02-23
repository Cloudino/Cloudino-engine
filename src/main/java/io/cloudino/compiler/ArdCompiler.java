/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.compiler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author javiersolis
 */
public class ArdCompiler 
{
    private String apath=null;
    private String alib=null;
    private final Properties props=new Properties();
    private HashMap<String,ArdDevice> devices;
    private TreeSet<ArdDevice> odevices;
   
    private static ArdCompiler instance=null;
    
    public static ArdCompiler getInstance()
    {
        if(instance==null)
        {
            synchronized(ArdCompiler.class)
            {
                if(instance==null)
                {
                    try
                    {
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        ScriptObject config=engine.getScriptObject().get("config");
                        instance=new ArdCompiler(config.getString("arduinoPath"),config.getString("arduinoLib"));
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }
    

    public ArdCompiler(String arduino_path, String arduino_lib) throws IOException
    {
        this.apath=arduino_path;
        this.alib=arduino_lib;
        init();
    }

    public String compile(String path, String device, String build, String userPath) throws IOException, InterruptedException
    {
        device=getVariant(device);
        //bash /programming/proys/cloudino/server/Cloudino-web/target/Cloudino-web-1.0-SNAPSHOT/WEB-INF/compile.sh /Applications/Arduino.app/Contents/Java arduino:avr:uno /Users/javiersolis/Documents/Arduino/build /Users/javiersolis/Documents/Arduino /Applications/Arduino.app/Contents/Java/examples/01.Basics/Blink/Blink.ino

        StringBuilder ret=new StringBuilder();
        //ArdDevice dev=getDevices().get(device);
        //System.out.println("dev:"+dev.board+"->"+dev.core+"->"+dev.cpu+"->"+dev.key+"->"+dev.mcu+"->"+dev.name+"->"+dev.sname+"->"+dev.variant);
        String txt="bash "+DataMgr.getApplicationPath()+"/WEB-INF/compile.sh "+apath+" "+alib+" arduino:avr:"+device+" "+build+" "+userPath+" "+path;
        Process p=Runtime.getRuntime().exec(txt);
        InputStream in=p.getInputStream();
        InputStream err=p.getErrorStream();
        int x=p.waitFor();
        if(err.available()>0)
        {
            byte r[]=new byte[err.available()];
            err.read(r);
            ret.append(new String(r,"utf8"));
        }  
        if(in.available()>0)
        {
            byte r[]=new byte[in.available()];
            in.read(r);
            ret.append(new String(r,"utf8"));
        }     
        return ret.toString();
    }

    public HashMap<String, ArdDevice> getDevices() {
        return devices;
    }
    
    public Iterator listDevices()
    {
        return odevices.iterator();
    }
    
    public String getVariant(String device)
    {
        //pro.menu.cpu.8MHzatmega328
        String r[]=device.split("\\.");
        String ret=r[0];
        if(r.length>3 && r[1].equals("menu"))
        {
            ret+=":"+r[2]+"="+r[3];
        }
        //System.out.println("ret:"+ret);
        return ret;
    }
    
    public static void main(String[] args) 
    {
        try
        {
            DataMgr.createInstance("/programming/proys/cloudino/server/Cloudino-web/target/Cloudino-web-1.0-SNAPSHOT");
            ArdCompiler com=new ArdCompiler("/Applications/Arduino.app/Contents/Java","/Applications/Arduino.app/Contents/Java");
            //String ret=com.compile("/Applications/Arduino.app/Contents/Java/examples/01.Basics/Blink/Blink.ino","uno","/Users/javiersolis/Documents/Arduino/build","/Users/javiersolis/Documents/Arduino");
            String ret=com.compile("/Applications/Arduino.app/Contents/Java/examples/01.Basics/Blink/Blink.ino","pro.menu.cpu.8MHzatmega328","/Users/javiersolis/Documents/Arduino/build","/Users/javiersolis/Documents/Arduino");
            System.out.println("ret:"+ret);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }    
    
    /******************************** private *******************************************/
    
    private void init() throws IOException
    {
        System.out.println("Cloudino compiler v0.1");
        devices=new HashMap();
        odevices=new TreeSet(new Comparator<ArdDevice>(){
            @Override
            public int compare(ArdDevice o1, ArdDevice o2) {
                return o1.toString().compareTo(o2.toString());
            }
        });
        
        props.load(new InputStreamReader(new FileInputStream(apath+"/hardware/arduino/avr/boards.txt"),"UTF8"));
        Iterator it=props.keySet().iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            String patern=".build.mcu";
            if(key.endsWith(patern))
            {
                ArdDevice d=new ArdDevice(key.substring(0,key.length()-patern.length()),props);
                devices.put(d.key, d);
            }
        }
        
        System.out.println("Encontrados;");
        devices.forEach((String t, ArdDevice u) -> {
            System.out.println(u);
        });
        odevices.addAll(devices.values());
    }
    
    private String readFile(File fino)throws IOException
    {
        StringBuilder ret=new StringBuilder();
        FileInputStream in=new FileInputStream(fino);
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
            ret.append(new String(buffer,0,len));
            len = in.read(buffer);
        }
        return ret.toString();
    }
    
    private static FilenameFilter CFILTER=(File dir, String name) -> {
        return name.endsWith(".c");
    };
    
    private static FilenameFilter CPPFILTER=(File dir, String name) -> {
        return name.endsWith(".cpp");
    };   
    
    private static FileFilter DIRFILTER=(File pathname) -> pathname.isDirectory();
    
    private static final FilenameFilter OFILTER=(File dir, String name) -> {
        return name.endsWith(".o");
    };   
    
    private static final FilenameFilter OAFILTER=(File dir, String name) -> {
        return name.endsWith(".o")||name.endsWith(".a");
    };
   
   
}
