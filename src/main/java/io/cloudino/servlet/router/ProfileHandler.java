package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import io.cloudino.utils.FileUploadUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.filestore.SWBFileSource;

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
        if (request.getMethod().equalsIgnoreCase("POST")) {
            if (null!=request.getContentType() && request.getContentType().startsWith("multipart/form-data")) {
                Map<String, Object> scope = savePhoto(request, user);
                scope.put("ctx", request.getContextPath());
                scope.put("user", user);
                response.setCharacterEncoding("utf-8");
                mustache.execute(response.getWriter(), scope);
                PrintWriter out = response.getWriter();
                out.print("{");
                if (scope.containsKey("Error")) {
                    out.print("error\":\""+scope.get("Error")+"\"");
                }
                out.print("}");
                return;
            } else {
                Map<String, Object> scope = saveData(request, user);
                scope.put("ctx", request.getContextPath());
                scope.put("user", user);
                response.setCharacterEncoding("utf-8");
                mustache.execute(response.getWriter(), scope);
                return;
            }
        }
        Map<String, Object> scope = saveData(request, user);
        scope.put("ctx", request.getContextPath());
        scope.put("user", user);
        response.setCharacterEncoding("utf-8");
        mustache.execute(response.getWriter(), scope);
    }

    private Map<String, Object> savePhoto(HttpServletRequest request, DataObject user) {
        
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", user);
        SWBFileSource fs = engine.getFileSource("UserPhoto");

        Map<String, Object> scope = new HashMap<>();
        if (FileUploadUtils.saveToSWBFileSource(request, fs, "inputPhoto", user.getNumId()+":photo")) { 
            scope.put("Message", "Photo Uploaded");
        } else {
            scope.put("Error", "We coudn't save your photo, it has to be below 100Kb");
        }
        return scope;
    }

    private Map<String, Object> saveData(HttpServletRequest request, DataObject user) throws IOException {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", user);
        SWBDataSource ds = engine.getDataSource("User");
        String fullname = request.getParameter("fullname");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        if (null!=fullname && email!=null){
            user.put("fullname", fullname);
            user.put("email", email);
            if(password!=null && password.trim().length()>0)user.put("password",password);
            ds.updateObj(user);
        }
        Map<String, Object> scope = new HashMap<>();
        return scope;
    }
}
