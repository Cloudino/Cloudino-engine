package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import io.cloudino.utils.FileUploadUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author serch
 */
public class ProfileHandler implements RouteHandler {

    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.ProfileHandler");

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
        System.out.println("profile called!");
        System.out.println("request method :" + request.getMethod());
        System.out.println("request type   :" + request.getContentType());
        if (request.getMethod().equalsIgnoreCase("POST")) {
            if (request.getContentType().startsWith("multipart/form-data")) {
                Map<String, Object> scope = savePhoto(request, user);
                scope.put("ctx", request.getContextPath());
                scope.put("user", user);
                //response.setCharacterEncoding("utf-8");
                mustache.execute(response.getWriter(), scope);
                return;
            } else {
                Map<String, Object> scope = saveData(request, user);
                scope.put("ctx", request.getContextPath());
                scope.put("user", user);
                //response.setCharacterEncoding("utf-8");
                mustache.execute(response.getWriter(), scope);
                return;
            }
        }
        Map<String, Object> scope = new HashMap<>();
        scope.put("ctx", request.getContextPath());
        scope.put("user", user);
        //response.setCharacterEncoding("utf-8");
        mustache.execute(response.getWriter(), scope);
    }

    private Map<String, Object> savePhoto(HttpServletRequest request, DataObject user) {
        Map<String, Object> scope = new HashMap<>();
        if (FileUploadUtils.saveToOutputStream(request, null, "")) { //TODO - Aún falta el método para guardar la foto en MongoDB
            scope.put("Message", "Photo Uploaded");
        } else {
            scope.put("Error", "We coudn't save your photo");
        }
        return scope;
    }

    private Map<String, Object> saveData(HttpServletRequest request, DataObject user) {
        Map<String, Object> scope = new HashMap<>();
        return scope;
    }
}
