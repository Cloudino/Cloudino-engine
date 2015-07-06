/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.compiler;

import java.io.File;

/**
 *
 * @author javiersolis
 */
public class Library {
    public String include;
    public String name;
    public File base;
    public File src;
    
    public Library(String name, String arduino_path)
    {
        this(name, name+".h", arduino_path);
    }    
    
    public Library(String name, String include, String arduino_path)
    {
        this.name=name;
        this.include=include;
        this.base=new File(arduino_path+"/libraries/"+name);
        this.src=new File(arduino_path+"/libraries/"+name+"/src");
    }
    
    public Library(String library_path)
    {
        this.base=new File(library_path);
        this.src=this.base;
        if(base.getName().equals("src"))
        {
            this.base=this.src.getParentFile();
        }else
        {
            File s=new File(base,"src");
            if(s.exists())this.src=s;
        }
        this.name=base.getName();
        this.include=base.getName()+".h";
    }

    @Override
    public boolean equals(Object obj) 
    {
        if(!(obj instanceof Library))return false;
        return base.getPath().equals(((Library)obj).base.getPath());
        
    }

    @Override
    public int hashCode() {
        return base.getPath().hashCode();
    }
}
