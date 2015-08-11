/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author javiersolis
 */
public class ArdLibraryMgr extends ArrayList<ArdLibrary>
{
    private String arduino_path;
    
    public ArdLibraryMgr(String arduino_path) 
    {
        this.arduino_path=arduino_path;
    }
    
    ArdLibraryMgr addLocalLibrary(String name)
    {
        ArdLibrary l=new ArdLibrary(name, arduino_path);
        if(l.base.exists() && !this.contains(l))
        {
            add(l);
        }
        return this;
    }    
    
    ArdLibraryMgr addExternalLibrary(String library_path)
    {
        ArdLibrary l=new ArdLibrary(library_path);
        if(l.base.exists() && !this.contains(l))
        {
            add(l);
        }
        return this;
    }    
    
    ArdLibraryMgr addInclude(String include)
    {
        if(include.endsWith(".h"))
        {
            ArdLibrary l=new ArdLibrary(include.substring(0,include.length()-2), arduino_path);
            if(l.base.exists() && !this.contains(l))
            {
                add(l);
            }
        }
        return this;
    } 
    
    
    void addLibsFromIno(String ino)
    {
        Pattern include1 = Pattern.compile("\\#include ?<(.*?)\\>");
        Pattern include2 = Pattern.compile("\\#include ?\"(.*?)\\\"");
        Matcher m = include1.matcher(ino);
        while (m.find()) {
            String s = m.group(1);
            addInclude(s);
        }
        m = include2.matcher(ino);
        while (m.find()) {
            String s = m.group(1);
            addInclude(s);
        }        
    }
    
    public ArrayList<File> getSrcList()
    {
        ArrayList<File> arr=new ArrayList();
        Iterator<ArdLibrary> it=iterator();
        while (it.hasNext()) {
            ArdLibrary library = it.next();
            arr.add(library.src);
        }
        return arr;
    }
}
