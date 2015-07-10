/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.server;

import io.cloudino.engine.Device;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author javiersolis
 */
public class Connection extends Thread
{
    private Socket sock = null;
    private Server server = null;
    private boolean running = false;
    private Device device=null;
    
    
    /** Creates a new instance of SConn */
    public Connection(Socket sock, Server server)
    {
        System.out.println("Connection open");
        this.sock = sock;
        this.server = server;
    }  
    
    public byte[] readMessage(InputStream in)throws IOException
    {
        ArrayList<Byte> ret=new ArrayList();
        int b=-1;
        do
        {
            if(in.available()>0)
            {
                b=in.read();
                ret.add((byte)b);
                System.out.println("c:"+(char)b);
            }
        }while(b!=0);
        byte arr[]=new byte[ret.size()];
        for(int x=0;x<ret.size();x++)
        {
            arr[x]=ret.get(x);
        }
        return arr;
    }
    
    public void run()
    {
        try
        {
            InputStream in = sock.getInputStream();
            while (running)
            {
                byte[] aux=readMessage(in);
                if (aux!=null)
                {
                    accion(new String(aux));
                } else
                {
                    running = false;                    
                    break;
                }
            }            
        } catch (Exception e)
        {
            e.printStackTrace();
        }     
        
        running=false;
        if(device!=null)
        {
            device.free();
        }
        
        //System.out.println("out:"+name);
    }
    
    public void accion(String acc)
    {
        System.out.println("acc:"+acc);
/*        
        if(firstline && acc.startsWith("GET"))
        {
            firstline=false;
            websocket=true;
            //System.out.println("Wensocket:true");
            handshake=true;
        }
        firstline=false;
        
        if(websocket)
        {
            //System.out.println("ws:"+acc);
            if(acc.startsWith("Sec-WebSocket-Key:"))
            {
                key=acc.substring(18).trim();
                //System.out.println("ws key:"+key);
            }else if(acc.startsWith("Origin:"))
            {
                origin=acc.substring(7).trim();
                //System.out.println("ws origin:"+origin);
            }else if(acc.isEmpty())
            {
                handshake=false;
                if(key!=null)
                {
                    String response="";
                    
                    response+="HTTP/1.1 101 Web Socket Protocol Handshake\r\n";
                    response+="Upgrade: WebSocket\r\n";
                    response+="Connection: Upgrade\r\n";
                    response+="Sec-WebSocket-Accept: "+getAcceptKey() +"\r\n";
                    response+="Server: SWB4Domotic Gateway\r\n";
                    //response+="Server: Kaazing Gateway\n";
                    response+="Date: "+(new Date().toGMTString()) +"\r\n";
                    response+="Access-Control-Allow-Origin: "+origin+"\r\n";
                    response+="Access-Control-Allow-Credentials: true\r\n";
                    response+="Access-Control-Allow-Headers: content-type\r\n";
                    response+="Access-Control-Allow-Headers: authorization\r\n";
                    response+="Access-Control-Allow-Headers: x-websocket-extensions\r\n";
                    response+="Access-Control-Allow-Headers: x-websocket-version\r\n";
                    response+="Access-Control-Allow-Headers: x-websocket-protocol\r\n";
                    response+="(Challenge Response):00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00\r\n";
                    //response+="Sec-WebSocket-Protocol: chat\n";
                    response+="\r\n";
                    //System.out.println("ws response:"+response);
                    sendMessage(response);                    
                }
            }
        }else
        {
            try
            {
                //System.out.println("sk:"+acc);

                StringTokenizer st = new StringTokenizer(acc, " ");
                if(st.hasMoreTokens())
                {
                    String head = st.nextToken();            

                    if(head.equals("ini"))
                    {
                        String serial = st.nextToken();
                        findDomGateway(serial, server.getModel());
                    }else if(head.equals("rep"))
                    {
                        String grp = st.nextToken();            
                        String id = st.nextToken();            
                        String cmd = st.nextToken();            

                        if(cmd.equals(""+DomNodeDevice.CMD_REPORT))
                        {
                            String pin = st.nextToken();            
                            String val1 = st.nextToken();                             
                            String val2 = st.nextToken(); 
                            int val=Integer.parseInt(val1)*256+Integer.parseInt(val2);

                            DomNodeDevice dev=findDomDevice(id, grp, pin, server.getModel());
                            dev.setStatus(val,false);
                        }
                    }                
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }            
        }   
*/        
    }    
    
    public void start()
    {
        running = true;
        super.start();
    }    
    
}
