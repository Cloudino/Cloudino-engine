package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataMgr;

/**
 *
 * @author serch
 */
public class PhotoHandler implements RouteHandler {
    private static byte[] defaultImage;

    @Override
    public void config(Mustache mustache) {
        try (
            FileInputStream fis = new FileInputStream(DataMgr.getApplicationPath()+"/static/cloudino.jpg");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ) {
            byte[] tmp = new byte[8192];
            int c=-1;
            while( (c = fis.read(tmp)) >= 0 ){
                bos.write(tmp, 0, c);
            }
            bos.flush();
            defaultImage = bos.toByteArray();
        } catch (IOException ioe) {ioe.printStackTrace();}
        
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("image/jpg");
        response.getOutputStream().write(defaultImage);
    }
    
}
