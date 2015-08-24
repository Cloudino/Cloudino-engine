/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.compiler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author javiersolis
 */
public class ArdCompiler 
{
    private String apath=null;
    private final Properties props=new Properties();
    private HashMap<String,ArdDevice> devices;
    private TreeSet<ArdDevice> odevices;
    
    private static String gcc="/hardware/tools/avr/bin/avr-gcc";
    private static String gpp="/hardware/tools/avr/bin/avr-g++";
    private static String ar="/hardware/tools/avr/bin/avr-ar"; 
    private static String ocpy="/hardware/tools/avr/bin/avr-objcopy";    
    
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
                        instance=new ArdCompiler(engine.getScriptObject().get("config").getString("arduinoPath"));
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }
    

    public ArdCompiler(String arduino_path) throws IOException
    {
        this.apath=arduino_path;
        init();
    }
    
    public void compile(String path, String device) throws IOException, InterruptedException
    {
        File fino=new File(path);
        String fname=fino.getName().split("\\.")[0];
        String build=fino.getParent()+"/build"; 
        compile(path, device, build);
    }
    
    public void compile(String path, String device, String build) throws IOException, InterruptedException
    {
        compile(path, device, build, new ArdLibraryMgr(apath));
    }    
    
    public void compile(String path, String device, String build, String libs[]) throws IOException, InterruptedException
    {
        ArdLibraryMgr libmgr=new ArdLibraryMgr(apath);
        for (String lib : libs) {
            libmgr.addLocalLibrary(lib);
        }
        compile(path, device, build, libmgr);
    }

    public void compile(String path, String device, String build, ArdLibraryMgr libs) throws IOException, InterruptedException
    {
        File fino=new File(path);
        compileCode(readFile(fino), path, device, build, libs);
    }
    
    public void compileCode(String code, String path, String device, String build) throws IOException, InterruptedException
    {
        compileCode(code, path, device, build, new ArdLibraryMgr(apath));
    }    
    
    public void compileCode(String code, String path, String device, String build, ArdLibraryMgr libs) throws IOException, InterruptedException
    {
        ArdDevice d=devices.get(device);
        File fino=new File(path);
        String fname=fino.getName().split("\\.")[0];
        File fbuild=new File(build);
        fbuild.mkdirs();
        
        String ino_txt=convertIno2Cpp(code, fino, build+"/"+fname+".cpp");
        
        /************ define libraries ************************/
        ArrayList<File> baseLibs=new ArrayList();
        baseLibs.add(new File(apath+"/hardware/arduino/avr/cores/"+d.core));
        baseLibs.add(new File(apath+"/hardware/arduino/avr/variants/"+d.variant));
        
        libs.addLibsFromIno(ino_txt);
        ArrayList extLibs=libs.getSrcList();
               
        /************ compile archivo.ino ************************/
        compileFile(new File(build+"/"+fname+".cpp"), d, fbuild, baseLibs, extLibs);
        
        /************ compile library ************************/
        {
            Iterator<File> it=extLibs.iterator();
            while (it.hasNext()) {
                File lib=it.next();
                compileLibrary(lib, d, new File(fbuild,lib.getName().equals("src")?lib.getParentFile().getName():lib.getName()), baseLibs, extLibs);
            } 
            it=baseLibs.iterator();
            while (it.hasNext()) {
                File lib=it.next();
                compileLibrary(lib, d, new File(fbuild,lib.getName().equals("src")?lib.getParentFile().getName():lib.getName()), baseLibs, new ArrayList());
            }
        }
        
        /************ archive library ************************/
        {
            Iterator<File> it=extLibs.iterator();
            while (it.hasNext()) {
                File lib=it.next();
                archiveLibrary(lib, d, fbuild, new File(fbuild,lib.getName().equals("src")?lib.getParentFile().getName():lib.getName()));
            } 
            it=baseLibs.iterator();
            while (it.hasNext()) {
                File lib=it.next();
                archiveLibrary(lib, d, fbuild, new File(fbuild,lib.getName().equals("src")?lib.getParentFile().getName():lib.getName()));
            }  
        }

        makeElf(fname, d, fbuild, baseLibs, extLibs);
        String exec;
        //exec=apath+gcc+" -w -Os -Wl,--gc-sections -mmcu="+d.mcu+" -o "+build+"/"+fname+".cpp.elf "+build+"/"+fname+".cpp.o "+build+"/arduino.a -L"+build+" -lm";
        //exec(exec);
        exec=apath+ocpy+" -O ihex -j .eeprom --set-section-flags=.eeprom=alloc,load --no-change-warnings --change-section-lma .eeprom=0 "+build+"/"+fname+".cpp.elf "+build+"/"+fname+".cpp.eep";
        exec(exec);
        exec=apath+ocpy+" -O ihex -R .eeprom "+build+"/"+fname+".cpp.elf "+build+"/"+fname+".cpp.hex";
        exec(exec);

    }

    public HashMap<String, ArdDevice> getDevices() {
        return devices;
    }
    
    public Iterator listDevices()
    {
        return odevices.iterator();
    }
    
    public static void main(String[] args) 
    {
        try
        {
            ArdCompiler com=new ArdCompiler("/Applications/Arduino.app/Contents/Java");
            //com.compile("/Applications/Arduino1.6.4.app/Contents/Java/examples/01.Basics/Blink/Blink.ino","pro.menu.cpu.8MHzatmega328","/Users/javiersolis/Documents/Arduino/build");
            //com.compile("/Applications/Arduino1.6.4.app/Contents/Java/libraries/Servo/examples/Knob/Knob.ino","uno","/Users/javiersolis/Documents/Arduino/build");
            com.compile("/opt/cloudino/bor/src/Blink.ino","uno");
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
    
    private String convertIno2Cpp(String code, File fino,String file) throws IOException
    {
        StringBuilder ret=new StringBuilder();
        FileOutputStream out=new FileOutputStream(file);
        out.write((
            "#include \"Arduino.h\"\n" +
            "void setup();\n" +
            "void loop();\n" +
            "#line 1 \""+fino.getName()+"\"\n"
            ).getBytes());  
        out.write(code.getBytes("utf8"));
        ret.append(code);
        return ret.toString();        
    }    
    
    private String convertIno2Cpp(File fino, String file) throws IOException
    {        
        return convertIno2Cpp(readFile(fino), fino, file);
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
    
    private int exec(String exec)throws IOException, InterruptedException
    {
        System.out.println(exec);
        Process p=Runtime.getRuntime().exec(exec);
        InputStream err=p.getErrorStream();
        int x=p.waitFor();
        if(err.available()>0)
        {
            byte r[]=new byte[err.available()];
            err.read(r);
            throw new RuntimeException(new String(r));
        }
        return x;
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
    
    private void compileFile(File file, ArdDevice d, File build, ArrayList<File> baseLibs, ArrayList<File> extLibs) throws IOException, InterruptedException
    {
        if(!build.exists())build.mkdirs();
        
        String exec;
        if(file.getName().endsWith(".c"))
        {
            exec=apath+gcc+" -c -g -Os -w -ffunction-sections -fdata-sections -MMD -mmcu="+d.mcu+" -DF_CPU="+d.cpu+" -DARDUINO=10604 -DARDUINO_"+d.board+" -DARDUINO_ARCH_AVR";
        }else
        {
            exec=apath+gpp+" -c -g -Os -w -fno-exceptions -ffunction-sections -fdata-sections -fno-threadsafe-statics -MMD -mmcu="+d.mcu+" -DF_CPU="+d.cpu+" -DARDUINO=10604 -DARDUINO_"+d.board+" -DARDUINO_ARCH_AVR";
        }
        Iterator<File> it=baseLibs.iterator();
        while (it.hasNext()) {
            exec+=" -I"+it.next().getPath();
        }
        it=extLibs.iterator();
        while (it.hasNext()) {
            exec+=" -I"+it.next().getPath();
        }            
        exec+=" "+file.getPath()+" -o "+build.getPath()+"/"+file.getName()+".o";
        exec(exec);
    }
    
    private void compileLibrary(File lib, ArdDevice d, File build, ArrayList<File> baseLibs, ArrayList<File> extLibs) throws IOException, InterruptedException
    {
        if(lib.isDirectory())
        {
            //************ Compile ******************
            File files[]=lib.listFiles(CFILTER);
            for (File file : files) {
                compileFile(file, d, build, baseLibs, extLibs);
            }
            files=lib.listFiles(CPPFILTER);
            for (File file : files) {
                compileFile(file, d, build, baseLibs, extLibs);
            }      
            files=lib.listFiles(DIRFILTER);
            for (File subDir : files) {
                compileLibrary(subDir, d, new File(build,subDir.getName()), baseLibs, extLibs);
            }          
        }        
    }    

    private void archiveLibrary(File lib, ArdDevice d, File build, File lib_build) throws IOException, InterruptedException
    {
        String exec;
        File files[];
        if(lib_build.isDirectory())
        {
            //************ Archive ******************
            files=lib_build.listFiles(OFILTER);
            for (File file : files) {
                exec=apath+ar+" rcs "+build.getPath()+"/"+((lib.getName().endsWith("src"))?lib.getParentFile().getName():lib.getName())+".a "+file.getPath();
                //exec=apath+ar+" rcs "+build.getPath()+"/core.a "+file.getPath();
                exec(exec);                
            }
            
            files=lib_build.listFiles(DIRFILTER);
            for (File subDir : files) {
                archiveLibrary(lib, d, build,subDir);
            }            
        }        
    }
    
    private void getCompiledLibraryFiles(File lib_build, ArrayList<File> ret)
    {
        File[] files=lib_build.listFiles(OFILTER);
        if(files!=null)
        {
            ret.addAll(Arrays.asList(files));
        }
        
        files=lib_build.listFiles(DIRFILTER);
        if(files!=null)
        {
            for (File subDir : files) {
                getCompiledLibraryFiles(subDir,ret);
            }    
        }
    }
    
    private void makeElf(String fname, ArdDevice d, File build, ArrayList<File> baseLibs, ArrayList<File> extLibs) throws IOException, InterruptedException
    {
        String exec=apath+gcc+" -w -Os -Wl,--gc-sections -mmcu="+d.mcu+" -o "+build+"/"+fname+".cpp.elf "+build+"/"+fname+".cpp.o";

        //TODO:revisar porque no se puede utilizar una archive en ligar de archivo por archivo para externas
        Iterator<File> it=extLibs.iterator();
        while (it.hasNext()) {
            File tmp=it.next();
            File lib=new File(build,(tmp.getName().equals("src")?tmp.getParentFile().getName():tmp.getName()));
            ArrayList<File> f=new ArrayList();
            getCompiledLibraryFiles(lib, f);
            
            Iterator<File> it2=f.iterator();
            while (it2.hasNext()) {
                File file=it2.next();
                exec+=" "+file.getPath();
            }
        }
//        Iterator<File> it=extLibs.iterator();
//        while (it.hasNext()) {
//            File tmp=it.next();
//            File file=new File(build,(tmp.getName().equals("src")?tmp.getParentFile().getName():tmp.getName())+".a");
//            if(file.exists())exec+=" "+file.getPath();
//        }
        it=baseLibs.iterator();
        while (it.hasNext()) {
            File tmp=it.next();
            File file=new File(build,(tmp.getName().equals("src")?tmp.getParentFile().getName():tmp.getName())+".a");
            if(file.exists())exec+=" "+file.getPath();
        }

        exec+=" -L"+build+" -lm";
        exec(exec);        
    }
   
}
