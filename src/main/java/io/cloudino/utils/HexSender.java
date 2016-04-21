package io.cloudino.utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class HexSender 
{
    int MAX_TIMEOUT=5000;
    
    /**
     * Hex Data Class
     */
    public class Data{
        public int addr;
        public byte bin[];        
        Data(int addr, byte bin[])
        {
            this.addr=addr;
            this.bin=bin;
        }
    }

    String toByte2Hex(byte b)
    {
        String r=Integer.toString(Byte.toUnsignedInt(b),16);
        if(r.length()==1)return "0"+r;
        else return r;
    }

    String toBin2Hex(byte[] bin)
    {
        return toBin2Hex(bin,0,bin.length);
    }    
    
    String toBin2Hex(byte[] bin,int offset,int len)
    {
        if(bin==null)return null;
        String ret="";
        for(int x=offset;x<(offset+len);x++)
        {
            ret+=toByte2Hex(bin[x]);
        }
        return ret;
    }
    
    byte[] toHex2Bin(String hex)
    {
        if(hex==null)return null;
        byte[] data=new byte[hex.length()/2];
        for(int x=0;x<hex.length();x+=2)
        {
            data[x/2]=(byte)Integer.parseInt(hex.substring(x, x+2), 16);
        }
        return data;
    }
    
    Data parseLine(String line)
    {
        if(line==null)return null;
        int size=Integer.parseInt(line.substring(1, 3),16);
        int address=Integer.parseInt(line.substring(3, 7),16);
        int type=Integer.parseInt(line.substring(7, 9),16);
        int next_index = (9 + size * 2);
        byte data[] = toHex2Bin(line.substring(9,next_index));
        int checksum = Integer.parseInt(line.substring(next_index), 16);
        
        int chk=size + (address >> 8) + (address & 0xFF) + type;
        for(int x=0;x<data.length;x++)chk+=Byte.toUnsignedInt(data[x]);
        chk=(~(chk & 0xFF) + 1) & 0xFF;
        
        if(chk!=checksum)new RuntimeException("Error in Data..");
        
        return new Data(address,data);
    }
    
    public Data[] readHex(InputStream in) throws IOException
    {
        ArrayList<Data> arr=new ArrayList<Data>();
        BufferedReader reader=new BufferedReader(new InputStreamReader(in));
        
        Data line=parseLine(reader.readLine());
        Data act=line;
        if(line!=null)
        {
            arr.add(line);
            while((line=parseLine(reader.readLine()))!=null)
            {
                if(act.addr+act.bin.length==line.addr)
                {
                    byte[] combined = new byte[act.bin.length + line.bin.length];
                    System.arraycopy(act.bin,0,combined,0,act.bin.length);
                    System.arraycopy(line.bin,0,combined,act.bin.length,line.bin.length);
                    act.bin=combined;
                }else
                {
                    arr.add(line);
                    act=line;
                }
            }
        }
        return (Data[])arr.toArray(new Data[0]);
    }
    
    public void send(OutputStream out, byte data[]) throws IOException
    {
        send(out,data,0,data.length);
    }    
    
    /**
     * 
     * @param out
     * @param data
     * @param offset
     * @param size
     * @throws IOException 
     */
    public void send(OutputStream out, byte data[],int offset, int size) throws IOException
    {
        if(data.length<offset+size)size=data.length-offset;
        //System.out.println("->"+toBin2Hex(data,offset,size));
        out.write(data, offset, size);
        out.flush();
    }
    
    public boolean wait_for(InputStream in, byte b,int timeout) throws IOException, InterruptedException
    {
        long time=System.currentTimeMillis();
        while(System.currentTimeMillis()-time<timeout)
        {
            if(in.available()>0)
            {
                int r=in.read();
                //System.out.println("wf:"+r+"="+b);
                if(r==-1 || r==0 || r==Byte.toUnsignedInt(b))return true;
            }
            Thread.sleep(1L);
        }
        return false;
    }
    
    public byte[] return_data(InputStream in, int timeout,int n) throws IOException
    {
        long time=System.currentTimeMillis();
        while(System.currentTimeMillis()-time<timeout)
        {
            if(in.available()>=n)
            {
                byte ret[]=new byte[n];
                in.read(ret);
                //System.out.println("read:"+new String(ret));
                return ret;
            }
        }
        return null;
    }
    
    public boolean acknowledge(InputStream in, Writer sout) throws IOException, InterruptedException
    {
        if(
                wait_for(in,(byte)0x14, MAX_TIMEOUT) && 
                wait_for(in,(byte)0x10, MAX_TIMEOUT))    // #STK_INSYNC, STK_OK
        {
            sout.write("success\n"); 
            return true;
        }
        else
        {
            sout.write("failed\n"); 
            return false;
        }
    }    
    
    public boolean program(Data[] chunks, InputStream in, OutputStream out, Writer sout) throws IOException, InterruptedException
    {
        sout.write("Connection to Arduino bootloader:");
        send(out,new byte[]{0x30,0x20});    // #STK_GET_SYNCH, SYNC_CRC_EOP
        if(!acknowledge(in,sout))return false;
        sout.write("Enter in programming mode:");
        send(out,new byte[]{0x50,0x20});    // #STK_ENTER_PROGMODE, SYNC_CRC_EOP
        if(!acknowledge(in,sout))return false;
        sout.write("Read device signature:");
        send(out,new byte[]{0x75,0x20});    // #STK_READ_SIGN, SYNC_CRC_EOP
        if(wait_for(in,(byte)0x14, MAX_TIMEOUT))   //#STK_INSYNC
        {
            byte[] received=return_data(in,MAX_TIMEOUT, 3);
            sout.write(toBin2Hex(received)+"\n");
            if(!wait_for(in,(byte)0x10, MAX_TIMEOUT))    //#STK_INSYNC
            {
                return false;
            }
        }else
        {
            return false;
        }
        for(int x=0;x<chunks.length;x++)
        {
            Data chunk=chunks[x];
            int total=chunk.bin.length;
            //sout.writeln("Chunk Size:"+chunk.addr+" "+chunk.bin.length);
            if(total>0)
            {
                int current_page=chunk.addr;
                int index=0;
                while(total>0)
                {
                    sout.write("Loading addr:"+" "+current_page+":");
                    send(out,new byte[]{0x55,(byte)(current_page&0xFF),(byte)(current_page>>8),0x20}); //#STK_LOAD_ADDRESS, address, SYNC_CRC_EOP
                    if(!acknowledge(in,sout))return false;
                    sout.write("Prgmming:");
                    if(total<0x80)
                    {
                        send(out,new byte[]{0x64,0x00,(byte)total,0x20}); //#STK_PROGRAM_PAGE, page size, flash memory
                        send(out,chunk.bin,index,total);                  //data
                        send(out,new byte[]{0x20});                      //SYNC_CRC_EOP                        
                    }else
                    {
                        send(out,new byte[]{0x64,0x00,(byte)0x80,0x46}); //#STK_PROGRAM_PAGE, page size, flash memory
                        send(out,chunk.bin,index,0x80);                  //data
                        send(out,new byte[]{0x20});                      //SYNC_CRC_EOP
                    }
                    if(!acknowledge(in,sout))return false;
                    current_page+= 0x40;
                    total -= 0x80;
                    index += 0x80;
                }
            }
            
        }
        sout.write("Leave programming mode:");
        send(out,new byte[]{0x51,0x20});            //#STK_LEAVE_PROGMODE, SYNC_CRC_EOP
        if(!acknowledge(in,sout))return false;
        return true;
    }
    
    public static String getParam(String param, String args[], String def)
    {
        for(int x=0;x<args.length;x++)
        {
            if(args[x].startsWith(param))
                return args[x].substring(param.length());
        }
        return def;
    }
    
    public static void main(String[] args) 
    {
        System.out.println("Cloudino remote programmer 2015 (v0.1)");
        HexSender obj=new HexSender();
        try
        {
            File f=new File("");
            String path=f.getAbsolutePath()+"/";
            
            if(args.length>0)
            {
                String fname=args[0];
                if(fname.charAt(0)!='/')fname=path+fname;

                Data[] data=obj.readHex(new FileInputStream(fname));
                
                String addr=getParam("-a", args, "192.168.4.1");
                int port=Integer.parseInt(getParam("-p", args, "9494"));
                Integer speed=Integer.parseInt(getParam("-s", args, "57600"));
                                
                Socket socket=new Socket(addr, port);
                InputStream in=socket.getInputStream();
                OutputStream out=socket.getOutputStream();
                System.out.println("Connection Opened:");
                out.write((byte)0);             //init
                out.write(ByteBuffer.allocate(4).putInt(speed).array()); 
                out.flush();
                Thread.sleep(400);
                if(!obj.program(data,in,out,new PrintWriter(System.out)))System.out.println("--Error--");
                socket.close();
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
