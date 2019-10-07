/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class Utils {

    private static final int BUFFERSIZE = 8192;
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    static
    {
        df.setTimeZone(TimeZone.getTimeZone("UTC"));    
    }

    /**
     * Copies an input stream into an output stream using the buffer size
     * defined by {@code SWBUtils.bufferSize} in the reading/writing operations.
     * <p>
     * Copia un flujo de entrada en uno de salida utilizando el tama&ntilde;o de
     * buffer definido por {@code SWBUtils.bufferSize} en las operaciones de
     * lectura/escritura.</p>
     *
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @throws IOException if either the input or the output stream is
     * {@code null}.
     * <p>
     * Si el flujo de entrada o el de salida es {@code null}.</p>
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        copyStream(in, out, BUFFERSIZE);
    }

    /**
     * Copies an input stream into an output stream using the specified buffer
     * size in the reading/writing operations.
     * <p>
     * Copia un flujo de entrada en uno de salida utilizando el tama&ntilde;o de
     * buffer especificado en las operaciones de lectura/escritura.</p>
     *
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @param bufferSize the number of bytes read/writen at the same time in
     * each I/O operation
     * @throws IOException if either the input or the output stream is
     * {@code null}.
     * <p>
     * Si el flujo de entrada o el de salida es {@code null}.</p>
     */
    public static void copyStream(InputStream in, OutputStream out, int bufferSize) throws IOException 
    {
        if (in == null) {
            throw new IOException("Input Stream null");
        }
        if (out == null) {
            throw new IOException("Ouput Stream null");
        }
        byte[] bfile = new byte[bufferSize];
        int x;
        while ((x = in.read(bfile, 0, bufferSize)) > -1) {
            out.write(bfile, 0, x);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * takes a text inputstream and load it into a String in memory
     * @param is inputstream with the text data
     * @param charset charset of the textdata
     * @return a string with the contents of the inpustream
     * @throws IOException if an error occurs reading the file
     */
    public static String textInputStreamToString(final InputStream is, final String charset) throws IOException {
        InputStreamReader in=null;
        if(charset!=null)
        {
            in=new InputStreamReader(is, charset);
        }else
        {
            in=new InputStreamReader(is);
        }
        try (BufferedReader br = new BufferedReader(in)) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
        
    public static DataObject readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        String jsonText = textInputStreamToString(is, "UTF-8");
        return (DataObject) DataObject.parseJSON(jsonText);
    }
    
    public static String sendData(String url, String data, String method, String content_type, String auth_token) throws IOException 
    {    
        //System.out.println("url:"+url);
        //System.out.println("data:"+data);
        //System.out.println("method:"+method);
        //System.out.println("content_type:"+content_type);
        //System.out.println("auth_token:"+auth_token);
        String ret=null;
        URL _url = new URL(url);
        HttpURLConnection con = (HttpURLConnection)_url.openConnection();
        con.setRequestMethod(method);
        if(data!=null)con.setRequestProperty("Content-length", String.valueOf(data.length())); 
        if(content_type!=null)con.setRequestProperty("Content-Type",content_type);
        if(auth_token!=null && auth_token.length()>0)con.setRequestProperty("X-Auth-Token",auth_token);    
        con.setDoInput(true); 
        if(data!=null)
        {
            con.setDoOutput(true); 
            DataOutputStream output = new DataOutputStream(con.getOutputStream());  
            output.writeBytes(data);
            output.close();
        }
        //System.out.println("getResponseCode:"+con.getResponseCode());
        //System.out.println("getContentEncoding:"+con.getContentEncoding());
        if(con.getResponseCode()>=200 && con.getResponseCode()<300)
        {
            ret=textInputStreamToString(con.getInputStream(),con.getContentEncoding());
            //System.out.println("getContent:"+ret);            
        }else
        {
            //System.out.println("getResponseMessage:"+con.getResponseMessage());
            throw new IOException(con.getResponseMessage());
        }
        //Utils.copyStream((InputStream)con.getContent(), response.getOutputStream());  
        return ret;
    }
    
    public static String nullValidate(String txt, String defValue)
    {
        if(txt==null)return defValue;
        return txt;
    }
    
    public static String toISODate(Date date)
    {
        return df.format(date);
    }

}
