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
    private static int SSEP='S';        //String Separator
    private static int BSEP='B';        //Binary Command separator
    
    private static long TIMEOUT=2000;
    private ConcurrentLinkedQueue<Command> commands=new ConcurrentLinkedQueue();
    private Command cmd=null;
    private long ltime=0;
    private byte status=0;
    
    private String tmpSize="";
    private int bytecont=0;
    
    private DeviceConn deviceCon=null;
    
    public CommandBuffer() {
    }     

    public CommandBuffer(DeviceConn deviceCon) {
        this.deviceCon=deviceCon;
    }        
    
    public void write(String str) throws UnsupportedEncodingException 
    {
        byte bts[]=str.getBytes("utf8");
        for(int x=0;x<bts.length;x++)
        {
            write(bts[x]);
        }
    }
    
    public void write(int b) {
        try
        {
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
                    cmd.type=Command.TYPE_MSG;
                    status++;
                }else if(b==LSEP)
                {
                    cmd.type=Command.TYPE_LOG;
                    status++;
    //            }else if(b==BSEP)
    //            {
    //                cmd.type=Command.TYPE_BIN;
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
                    int size=Integer.parseInt(tmpSize);
                    if(size==0)status++;
                    cmd.topic=new byte[size];
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
                    if(cmd.type==Command.TYPE_LOG)// || cmd.type==2 )     //log o jscmd termina el comando
                    {
                        commands.add(cmd);
                        reset();
                    }
                }
            }else if(status==4)
            {
                if(b==SSEP)                 //String Content
                {
                    status++;
                    tmpSize="";
                }else if(b==BSEP)           //Binary Content
                {
                    status++;
                    tmpSize="";
                    cmd.type=Command.TYPE_BIN;
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
                    if(bytecont==cmd.msg.length)
                    {
                        commands.add(cmd);
                        reset();
                    }       
                    //System.out.print("topic:"+new String(cmd.topic)+" type:"+cmd.type+" size:"+tmpSize);
                }else
                {
                    reset();
                }
            }else if(status==6)                       //contenido del topico o log
            {
                cmd.msg[bytecont]=(byte)b;
                bytecont++;
                if(bytecont%500==0)
                {
                    System.out.print(" "+bytecont);
                    String txt=new String(cmd.topic)+":"+bytecont;                    
                    commands.add(new Command(Command.TYPE_LOG,txt.getBytes(),null));
                }
                if(bytecont==cmd.msg.length)
                {
                    if(cmd.type!=Command.TYPE_BIN)
                    {
                        //System.out.println(" msg:"+new String(cmd.msg));
                    }
                    else 
                    {
                        System.out.println(" end");
                        commands.add(new Command(Command.TYPE_MSG,cmd.topic, (""+bytecont).getBytes()));                        
                    }
                    commands.add(cmd);
                    reset();
                }
            }else
            {
                //System.out.println("err byte:"+b+" "+(char)b);
            }
        } catch (ArrayIndexOutOfBoundsException ae)
        {
            //System.out.println("Protocol Error..."+cmd);
            //ae.printStackTrace();
        }
    }    
    
    public void reset()
    {
        status=0;
        cmd=null;        
    }
    
    public boolean hasCommand()
    {
        long t=TIMEOUT;
        if(status>0 && cmd.type==Command.TYPE_BIN) t=t*10;                     //si es binario se espera mas tiempo
        
        if(status>0 && System.currentTimeMillis()-ltime>t)
        {
            System.out.println("TIMEOUT");
            commands.add(new Command(Command.TYPE_MSG,cmd.topic, "TIMEOUT".getBytes()));                        
            //if(cmd.type==2)commands.add(cmd);                   //si es binario se agrega parcialmente
            reset();        
        }
        return !commands.isEmpty();
    }
    
    public Command getCommand()
    {
        return commands.poll();
    }

    public DeviceConn getDeviceCon() {
        return deviceCon;
    }            
    
}