package io.cloudino.compiler_.utils;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by serch on 7/27/15.
 */
public class FileUtils {

    private static Logger logger = Logger.getLogger("i.c.c.u.FileUtils");

    public static byte[] doGzipFile(final File file){
        try (InputStream is = new FileInputStream(file);
             ByteArrayOutputStream os = new ByteArrayOutputStream();
             GZIPOutputStream gos = new GZIPOutputStream(os)){
            byte[] buff = new byte[8192];
            int br = 0;
            while((br=is.read(buff))>-1){
                gos.write(buff, 0, br);
            }
            gos.flush();
            gos.finish();
            return os.toByteArray();
        } catch (IOException ioe){
            logger.warning("Error: "+ioe.getLocalizedMessage());
            return null;
        }

    }

    public static boolean doUnGzipFile(final File file, final byte[]data){
        try (InputStream is = new ByteArrayInputStream(data);
        InputStream gis = new GZIPInputStream(is);
        OutputStream os = new FileOutputStream(file)){
            byte[] buff = new byte[8192];
            int br = 0;
            while((br=gis.read(buff))>-1){
                os.write(buff, 0, br);
            }
            os.flush();
            return true;
        } catch (IOException ioe){
            logger.warning("Error: "+ioe.getLocalizedMessage());
            return false;
        }
    }

    
}
