/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author javiersolis
 */
public abstract class DeviceConn
{
    private static int SEP='|';         //Separator
    private static int MSEP='M';        //Message separator
    private static int LSEP='L';        //Log separator
//    private static int JSEP='J';        //JS Command separator
    private static int SSEP='S';        //String Separator    
    
    protected Device device=null;
    protected long time=System.currentTimeMillis();
    protected CommandBuffer buffer=null;    
    protected boolean uploading=false;
    
    protected OutputStream outputStream=null;
    protected InputStream inputStream=null;    

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    public boolean isUploading() {
        return uploading;
    }
    
    /**
     * Create an instance of Device Conenction
     * @param sock
     * @param server
     * @throws IOException 
     */
    public DeviceConn() throws IOException
    {
        this.buffer=new CommandBuffer();
    }  
    
    public void post(String topic, String msg)
    {
        write(""+(char)SEP+(char)MSEP+topic.getBytes().length+(char)SEP+topic+(char)SSEP+msg.getBytes().length+(char)SEP+msg);
    } 
    
//    public void postJSCommand(String command)
//    {
//        write(""+(char)SEP+(char)JSEP+command.getBytes().length+(char)SEP+command);
//    }       
    
    public void write(String str)
    {
        //System.out.println("write_str:"+str);
        try
        {
            if(!isClosed())
            {
                outputStream.write(str.getBytes("utf8"));
                outputStream.flush();
            }
            else close();
        }catch(IOException e)
        {
            //System.out.println("Clossing Connection...");
            close();
        }
    }
    
    public void write(byte data[]) throws IOException
    {
        //System.out.println("write_data:"+new String(data));
        try
        {
            if(!isClosed())
            {
                outputStream.write(data);
                outputStream.flush();
            }
            else close();
        }catch(IOException e)
        {
            //System.out.println("Clossing Connection...");
            close();
        }
    }   
    
    public void write(byte b) throws IOException
    {
        try
        {
            if(!isClosed())outputStream.write(b);
            else close();
        }catch(IOException e)
        {
            //System.out.println("Clossing Connection...");
            close();
        }
    }  
    
    public abstract boolean isClosed();
    
    public abstract void close();
    
    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
    
    public abstract String getInetAddress();
    
}

class Command
{
    byte type=0;                //0=message, 1=log, 2=jscmd
    byte topic[];
    byte msg[];
}

class CommandBuffer
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