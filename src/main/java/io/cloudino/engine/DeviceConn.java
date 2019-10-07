/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
            this.buffer=new CommandBuffer(this);
        }  

        public void post(String topic, String msg)
        {
            try
            {
                write(""+(char)SEP+(char)MSEP+topic.getBytes().length+(char)SEP+topic+(char)SSEP+msg.getBytes().length+(char)SEP+msg);
            }catch(IOException e)
            {
                //System.out.println("Clossing Connection...");
                close();
            }
        } 

    //    public void postJSCommand(String command)
    //    {
    //        write(""+(char)SEP+(char)JSEP+command.getBytes().length+(char)SEP+command);
    //    }       

        public void write(String str) throws IOException
        {
            write(str.getBytes("utf8"));
        }

        public void write(byte data[]) throws IOException
        {
            //System.out.println("write_data:"+new String(data));
            try
            {
                if(!isClosed())
                {
                    synchronized(outputStream)
                    {
                        outputStream.write(data);
                        outputStream.flush();
                    }
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
            byte arr[]={b};
            write(arr);
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

        public Device getDevice() {
            return device;
        }

    }

