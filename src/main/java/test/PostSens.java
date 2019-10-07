package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.semanticwb.datamanager.DataObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class PostSens {
    
    public static String httpGet(String url) throws IOException
    {
        StringBuffer ret=new StringBuffer();
        URL url1=new URL(url);
        URLConnection con=url1.openConnection();
        
        // open the stream and put it into BufferedReader
        BufferedReader br = new BufferedReader(
                           new InputStreamReader(con.getInputStream()));

        String inputLine;
        while ((inputLine = br.readLine()) != null) {
                ret.append(inputLine);
        }
        br.close();        
        
        return ret.toString();            
    }
    
    public static String post(String server, String token, String topic, String data) throws IOException
    {
        StringBuffer ret=new StringBuffer();
        URL url=new URL(server+"/api/post2Srv/"+token+"?"+topic+"="+URLEncoder.encode(data));
        URLConnection con=url.openConnection();
        
        // open the stream and put it into BufferedReader
        BufferedReader br = new BufferedReader(
                           new InputStreamReader(con.getInputStream()));

        String inputLine;
        while ((inputLine = br.readLine()) != null) {
                ret.append(inputLine);
        }
        br.close();        
        
        return ret.toString();
    }
    
    public static DataObject getData(String url) throws IOException
    {
        String sdata=httpGet(url);
        DataObject data=((DataObject)DataObject.parseJSON(sdata)).getDataObject("main");
        System.out.println("data:"+data);        
        
        //DataObject data=(DataObject)DataObject.parseJSON("{\"temp\":28, \"temp_min\":22, \"humidity\":53, \"pressure\":827, \"temp_max\":22}");
        
        double temp=data.getDouble("temp");
        double humidity=data.getDouble("humidity");
        double pressure=data.getDouble("pressure")-180;
        double altitude=(((Math.log((pressure*100)/101325.0)*287.053)*((temp+459.67)*(5.0/9.0)))/-9.8)+60;
        //double altitude=(Math.log((pressure*100)/101325.0)*287.053);
        double precipitation=0.0;
        
        // { "temperature" : 22, "humidity" : 60, "pressure" : 829, "altitude" : 1652, "precipitation" : 0.00 }
        DataObject msg=new DataObject();
        msg.addParam("temperature", (int)temp);
        msg.addParam("humidity", (int)humidity);
        msg.addParam("pressure", (int)pressure);
        msg.addParam("altitude", (int)altitude);
        msg.addParam("precipitation", precipitation);
        
        return msg;        
    }
    
    public static void process()
    {
        try
        {
            DataObject msg=getData("http://api.openweathermap.org/data/2.5/weather?id=3992986&units=metric&appid=c6cb38c5e77e1ab0f948396530f2d0c5");
            System.out.println(msg);

            String ret=post("http://cloudino.io","az3mlw6zx3vq1oya0hqln0c0xk2pf7c8ro2ujmt","sens",msg.toString());
            System.out.println(ret);
        }catch(IOException e)
        {

        }
        
    }
    
    public static void main(String[] args) {
        System.out.println("Start Service...");
        final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                process();
                //System.out.println("Hola");
            }
        }, 0, 1, TimeUnit.MINUTES);
    }    
    
    
}
