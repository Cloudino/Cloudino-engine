package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.filestore.SWBFileObject;
import org.semanticwb.datamanager.filestore.SWBFileSource;

/**
 *
 * @author serch
 */
public class PhotoHandler implements RouteHandler {
    private static byte[] defaultImage;
    private final SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
    private final SWBFileSource fs = engine.getFileSource("UserPhotos");

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
    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
        System.out.println("photo:"+user);
        if(null!=user){
            System.out.println("buscar: "+user.getNumId()+":photo");
            SWBFileObject fo = fs.getFile(user.getNumId()+":photo");System.out.println("fo:"+fo);
            if (null!=fo){
                response.setContentType(fo.getContentType());
                response.getOutputStream().write(fo.getContentAsByteArray());
            } else {
                response.setContentType("image/jpg");
                response.getOutputStream().write(defaultImage);
            }
        } else {
            response.setContentType("image/jpg");
            response.getOutputStream().write(defaultImage);
        }
    }
    
}
