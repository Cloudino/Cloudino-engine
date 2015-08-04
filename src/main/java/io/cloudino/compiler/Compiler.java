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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author javiersolis
 */
public class Compiler 
{
    private String apath=null;
    private Properties props=new Properties();
    private HashMap<String,Device> devices;
    private TreeSet<Device> odevices;
    
    private static String gcc="/hardware/tools/avr/bin/avr-gcc";
    private static String gpp="/hardware/tools/avr/bin/avr-g++";
    private static String ar="/hardware/tools/avr/bin/avr-ar"; 
    private static String ocpy="/hardware/tools/avr/bin/avr-objcopy";    
    
    private static Compiler instance=null;
    
    public static Compiler getInstance()
    {
        if(instance==null)
        {
            synchronized(Compiler.class)
            {
                if(instance==null)
                {
                    try
                    {
                        SWBScriptEngine engine=DataMgr.getUserScriptEngine("/cloudino.js",null);
                        instance=new Compiler(engine.getScriptObject().get("config").getString("arduinoPath"));
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }
    

    public Compiler(String arduino_path) throws IOException
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
        compile(path, device, build, new LibraryMgr(apath));
    }    
    
    public void compile(String path, String device, String build, String libs[]) throws IOException, InterruptedException
    {
        LibraryMgr libmgr=new LibraryMgr(apath);
        for(int x=0;x<libs.length;x++)
        {
            libmgr.addLocalLibrary(libs[x]);
        }
        compile(path, device, build, libmgr);
    }

    public void compile(String path, String device, String build, LibraryMgr libs) throws IOException, InterruptedException
    {
        File fino=new File(path);
        compileCode(readFile(fino), path, device, build, libs);
    }
    
    public void compileCode(String code, String path, String device, String build) throws IOException, InterruptedException
    {
        compileCode(code, path, device, build, new LibraryMgr(apath));
    }    
    
    public void compileCode(String code, String path, String device, String build, LibraryMgr libs) throws IOException, InterruptedException
    {
        Device d=devices.get(device);
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

    public HashMap<String, Device> getDevices() {
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
            Compiler com=new Compiler("/Applications/Arduino.app/Contents/Java");
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
        odevices=new TreeSet(new Comparator<Device>(){
            @Override
            public int compare(Device o1, Device o2) {
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
                Device d=new Device(key.substring(0,key.length()-patern.length()),props);
                devices.put(d.key, d);
            }
        }
        
        System.out.println("Encontrados;");
        devices.forEach(new BiConsumer<String, Device>(){
            @Override
            public void accept(String t, Device u) {
                System.out.println(u);
            }
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
    
    private static FilenameFilter CFILTER=new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if(name.endsWith(".c"))return true;
                    return false;
                }
            };
    
    private static FilenameFilter CPPFILTER=new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if(name.endsWith(".cpp"))return true;
                    return false;
                }
            };   
    
    private static FileFilter DIRFILTER=new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };
    
    private static FilenameFilter OFILTER=new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if(name.endsWith(".o"))return true;
                    return false;
                }
            };   
    
    private static FilenameFilter OAFILTER=new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if(name.endsWith(".o")||name.endsWith(".a"))return true;
                    return false;
                }
            };     
    
    private void compileFile(File file, Device d, File build, ArrayList<File> baseLibs, ArrayList<File> extLibs) throws IOException, InterruptedException
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
    
    private void compileLibrary(File lib, Device d, File build, ArrayList<File> baseLibs, ArrayList<File> extLibs) throws IOException, InterruptedException
    {
        if(lib.isDirectory())
        {
            //************ Compile ******************
            File files[]=lib.listFiles(CFILTER);
            for(int x=0;x<files.length;x++)
            {
                compileFile(files[x], d, build, baseLibs, extLibs);
            }
            files=lib.listFiles(CPPFILTER);
            for(int x=0;x<files.length;x++)
            {
                compileFile(files[x], d, build, baseLibs, extLibs);
            }      
            files=lib.listFiles(DIRFILTER);
            for(int x=0;x<files.length;x++)
            {
                File subDir=files[x];
                compileLibrary(subDir, d, new File(build,subDir.getName()), baseLibs, extLibs);
            }          
        }        
    }    

    private void archiveLibrary(File lib, Device d, File build, File lib_build) throws IOException, InterruptedException
    {
        String exec;
        File files[];
        if(lib_build.isDirectory())
        {
            //************ Archive ******************
            files=lib_build.listFiles(OFILTER);
            for(int x=0;x<files.length;x++)
            {
                File file=files[x];
                exec=apath+ar+" rcs "+build.getPath()+"/"+((lib.getName().endsWith("src"))?lib.getParentFile().getName():lib.getName())+".a "+file.getPath();
                //exec=apath+ar+" rcs "+build.getPath()+"/core.a "+file.getPath();
                exec(exec);                
            }
            
            files=lib_build.listFiles(DIRFILTER);
            for(int x=0;x<files.length;x++)
            {
                File subDir=files[x];
                archiveLibrary(lib, d, build,subDir);
            }            
        }        
    }
    
    private void getCompiledLibraryFiles(File lib_build, ArrayList<File> ret)
    {
        File[] files=lib_build.listFiles(OFILTER);
        for(int x=0;x<files.length;x++)
        {
            File file=files[x];
            ret.add(file);
        }
        
        files=lib_build.listFiles(DIRFILTER);
        for(int x=0;x<files.length;x++)
        {
            File subDir=files[x];
            getCompiledLibraryFiles(subDir,ret);
        }    
    }
    
    private void makeElf(String fname, Device d, File build, ArrayList<File> baseLibs, ArrayList<File> extLibs) throws IOException, InterruptedException
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
