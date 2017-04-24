/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author javiersolis
 */
public class CommandBuffer
{
    private static int SEP='|';         //Separator
    private static int MSEP='M';        //Message separator
    private static int LSEP='L';        //Log separator
//    private static int JSEP='J';        //JS Command separator
    private static int SSEP='S';        //String Separator
    
    private static long TIMEOUT=2000;
    private ConcurrentLinkedQueue<Command> commands=new ConcurrentLinkedQueue();
    private Command cmd=null;
    private long ltime=0;
    private byte status=0;
    
    private String tmpSize="";
    private int bytecont=0;
    
    public void write(String str) throws UnsupportedEncodingException 
    {
        byte bts[]=str.getBytes("utf8");
        for(int x=0;x<bts.length;x++)
        {
            write(bts[x]);
        }
    }
    
    public void write(int b) {
        //System.out.print((char)b);
        ltime=System.currentTimeMillis();
        
        if(status==0)                       //inicio de trama
        {
            if(b==SEP)              
            {
                cmd=new Command();
                status++;
            }
        }else if(status==1)                       //tipo de mensaje
        {
            tmpSize="";
            if(b==MSEP)
            {
                cmd.type=0;
                status++;
            }else if(b==LSEP)
            {
                cmd.type=1;
                status++;
//            }else if(b==JSEP)
//            {
//                cmd.type=2;
//                status++;
            }else
            {
                reset();
            }
        }else if(status==2)                       //tamaño del topico 
        {
            if(b>='0' && b<='9')
            {
                tmpSize+=(char)b;
            }else if(b==SEP)
            {
                status++;
                cmd.topic=new byte[Integer.parseInt(tmpSize)];
                bytecont=0;
            }else
            {
                reset();
            }
        }else if(status==3)                       //contenido del topico o log
        {
            cmd.topic[bytecont]=(byte)b;
            bytecont++;
            if(bytecont==cmd.topic.length)
            {
                status++;
                if(cmd.type==1)// || cmd.type==2 )     //log o jscmd termina el comando
                {
                    commands.add(cmd);
                    reset();
                }
            }
        }else if(status==4)
        {
            if(b==SSEP)
            {
                status++;
                tmpSize="";
            }else
            {
                reset();
            }
        }else if(status==5)                       //tamaño del mensage 
        {
            if(b>='0' && b<='9')
            {
                tmpSize+=(char)b;
            }else if(b==SEP)
            {
                status++;
                cmd.msg=new byte[Integer.parseInt(tmpSize)];
                bytecont=0;
            }else
            {
                reset();
            }
        }else if(status==6)                       //contenido del topico o log
        {
            cmd.msg[bytecont]=(byte)b;
            bytecont++;
            if(bytecont==cmd.msg.length)
            {
                commands.add(cmd);
                reset();
            }
        }
    }    
    
    public void reset()
    {
        status=0;
        cmd=null;
    }
    
    public boolean hasCommand()
    {
        if(status>0 && System.currentTimeMillis()-ltime>TIMEOUT)
        {
            reset();
        }
        return !commands.isEmpty();
    }
    
    public Command getCommand()
    {
        return commands.poll();
    }    
    
}