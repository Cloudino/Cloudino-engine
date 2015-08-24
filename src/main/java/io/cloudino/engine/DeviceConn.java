/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import io.cloudino.server.DeviceServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author javiersolis
 */
public class DeviceConn
{
    public static int SEP='|';
    public static int MSEP='M';
    public static int LSEP='L';
    public static int SSEP='S';
    
    private Socket sock = null;
    private DeviceServer server = null;
    private boolean running = true;
    private Device device=null;
    private OutputStream outputStream=null;
    private InputStream inputStream=null;
    private long time=System.currentTimeMillis();
    private CommandBuffer buffer=null;
    
    private boolean uploading=false;

    public void setUploading(boolean uploading) {
        this.uploading = uploading;
    }

    public boolean isUploading() {
        return uploading;
    }
    
    /** Creates a new instance of SConn */
    public DeviceConn(Socket sock, DeviceServer server) throws IOException
    {
        System.out.println("Connection open");
        this.sock = sock;
        this.server = server;
        this.outputStream=sock.getOutputStream();
        this.inputStream = sock.getInputStream();
        this.buffer=new CommandBuffer(){
        };
    }  
    
    public boolean loop()
    {
        boolean ret=false;
        try
        {
            if(!uploading)
            {
                if(System.currentTimeMillis()-time>10000)
                {
                    time=System.currentTimeMillis();
                    System.out.println("Ping");
                    write((byte)'_');
                }
                while(inputStream.available()>0)
                {
                    time=System.currentTimeMillis();
                    int b=inputStream.read();
                    if(b==-1)
                    {
                        close();
                        break;
                    }
                    buffer.write(b);
                }
                if(buffer.hasCommand())
                {
                    Command cmd=buffer.getCommand();
                    if(cmd.type==0)
                    {
                        String topic=new String(cmd.topic,"utf8");
                        String msg=new String(cmd.msg,"utf8");
                        System.out.println("Topic:"+topic+":"+msg);
                        if(topic.equals("$ID"))
                        {
                            device=DeviceMgr.getInstance().getDeviceByAuthToken(msg);
                            device.setConnection(this);
                        }else
                        {
                            if(device!=null)
                            {
                                device.receive(topic, msg);
                            }
                        }      
                    }else if(cmd.type==1)               //LOG
                    {
                        device.receiveLog(new String(cmd.topic,"utf8"));
                    }
                }
            }
        } catch (Exception e)
        {
            System.out.println("Clossing Connection...");
            close();            
        }     
        return ret;
    }    
    
    public void post(String topic, String msg)
    {
        write(""+(char)SEP+(char)MSEP+topic.getBytes().length+(char)SEP+topic+(char)SSEP+msg.getBytes().length+(char)SEP+msg);
    }     
    
    public void write(String str)
    {
        System.out.println("Write String:"+str);
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
            System.out.println("Clossing Connection...");
            close();
        }
            
    }
    
    public void write(byte data[]) throws IOException
    {
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
            System.out.println("Clossing Connection...");
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
            System.out.println("Clossing Connection...");
            close();
        }
    }      
    
    public boolean isClosed()
    {
        return !running || !sock.isConnected() || sock.isClosed();
    }
    
    public void close()
    {
        running=false;
        try
        {
            if(sock!=null && !sock.isClosed())
            {
                sock.close();
            }
            if(device!=null)
            {
                Device tmp=device;
                device=null;
                tmp.closeConnection();
            }            
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    protected InputStream getInputStream() {
        return inputStream;
    }

    protected OutputStream getOutputStream() {
        return outputStream;
    }
}

class Command
{
    byte type=0;                //0=message, 1=log
    byte topic[];
    byte msg[];
}

class CommandBuffer
{
    private static int SEP='|';
    private static int MSEP='M';
    private static int LSEP='L';
    private static int SSEP='S';    
    
    private static long TIMEOUT=2000;
    private ConcurrentLinkedQueue<Command> commands=new ConcurrentLinkedQueue();
    private Command cmd=null;
    private long ltime=0;
    private byte status=0;
    
    private String tmpSize="";
    private int bytecont=0;
    
    public void write(int b) {
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
                if(cmd.type==1)
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