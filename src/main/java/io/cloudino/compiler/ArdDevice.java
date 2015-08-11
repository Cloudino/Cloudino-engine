/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.compiler;

import java.util.Properties;

/**
 *
 * @author javiersolis
 */
public class ArdDevice {
 
        public String key;
        public String name;
        public String sname;
        public int speed;
        public String mcu;
        public String variant;
        public String cpu;
        public String core;
        public String board;

        public ArdDevice(String key, Properties props) {
            this.key=key;
            name=props.getProperty(getRoot()+".name");
            if(!isRoot())sname=props.getProperty(key);
            speed=findInt(".upload.speed",props);
            mcu=find(".build.mcu",props);
            variant=find(".build.variant",props);
            cpu=find(".build.f_cpu",props);
            core=find(".build.core",props);
            board=find(".build.board",props);
        }
        
        private int findInt(String val, Properties props)
        {
            String s=find(val,props);
            if(s!=null)
            {
                return Integer.parseInt(s);
            }
            return 0;
        }
        
        private String find(String val, Properties props)
        {
            String s=props.getProperty(key+val);
            if(s==null && !isRoot())s=props.getProperty(getRoot()+val);
            return s;
        }
        
        @Override
        public String toString() 
        {
            return name+(sname!=null?" -> ("+sname+")":"");
        }
        
        public String detail() 
        {
            return key+" name:"+name+(sname!=null?" -> ("+sname+")":"")+" speed:"+speed+" mcu:"+mcu;
        }
        
        public String getRoot()
        {
            int i=key.indexOf('.');
            if(i>-1)return key.substring(0,i);
            return key;
        }
        
        public boolean isRoot()
        {
            return key.indexOf('.')==-1;
        }   
}
