package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import io.cloudino.utils.Utils;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.utils.TokenGenerator;

/**
 *
 * @author serch
 */
public class PasswordRecoveryHandler implements RouteHandler {

    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.PasswordRecoveryHandler");
    private final SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
    private final SWBDataSource ds = engine.getDataSource("User");

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
        if (request.getMethod().equals("POST")) {
            String k=request.getParameter("k");            
            if(k!=null)
            {
                String userid=TokenGenerator.getUserIdFromToken(k);
                DataObject obj=ds.getObjectByNumId(userid);
                if(obj!=null && k.equals(obj.getString("recoveryToken")))
                {                
                    obj.put("fullname", request.getParameter("fullname"));
                    obj.put("password", request.getParameter("password"));
                    obj.put("confirmed", "true");
                    obj.put("active", "true");
                    obj.put("recoveryToken",null);
                    ds.updateObj(obj);
                    Map<String, Object> register = new HashMap<>();
                    Map<String, Object> scope = new HashMap<>();
                    scope.put("ctx", request.getContextPath());
                    register.put("changed", scope);
                    response.setCharacterEncoding("utf-8");
                    mustache.execute(response.getWriter(), register);
                }else
                {
                    Map<String, Object> register = new HashMap<>();
                    Map<String, Object> scope = new HashMap<>();
                    scope.put("error", "Error recovering password");
                    scope.put("ctx", request.getContextPath());
                    register.put("recovery", scope);
                    response.setCharacterEncoding("utf-8");
                    mustache.execute(response.getWriter(), register);                    
                }                
            }else
            {
                String email = request.getParameter("email");
                if (email != null) {
                    DataObject data = new DataObject();
                    data.put("email", email);
                    DataObject query = new DataObject();
                    query.put("data", data);
                    query = ds.fetch(query);
                    DataList lista = query.getDataObject("response").getDataList("data");
                    if (lista.size() == 1) 
                    {
                        DataObject obj=lista.getDataObject(0);
                        String token=TokenGenerator.nextTokenByUserId(obj.getNumId());

                        String content = MessageFormat.format(Utils.textInputStreamToString(
                            LoginHandler.class.getResourceAsStream("/templates/passwordRecoveryMail.template"),"utf-8"),
                            obj.getString("fullname"), 
                            token
                        ); 

                        engine.getUtils().sendMail(email, obj.getString("fullname"), "Cloudino Password Recovery Email", content, "text/html", x->{
                            try
                            {
                                obj.put("recoveryToken", token);                    
                                ds.updateObj(obj);    
                            }catch(IOException e)
                            {
                                logger.log(Level.WARNING,"Error updating User Object",e);
                            }
                        });

                        logger.log(Level.FINE, "Password Recovery send mail to: {0}", email);
                        Map<String, Object> register = new HashMap<>();
                        Map<String, Object> scope = new HashMap<>();
                        scope.put("ctx", request.getContextPath());
                        register.put("confirm", scope);
                        response.setCharacterEncoding("utf-8");
                        mustache.execute(response.getWriter(), register);
                    } else {
                        Map<String, Object> register = new HashMap<>();
                        Map<String, Object> scope = new HashMap<>();
                        scope.put("error", "Email is not registered");
                        scope.put("ctx", request.getContextPath());
                        register.put("recovery", scope);
                        response.setCharacterEncoding("utf-8");
                        mustache.execute(response.getWriter(), register);
                    }
                }
            }
        } else {
            String k=request.getParameter("k");
            if(k!=null)
            {
                String userid=TokenGenerator.getUserIdFromToken(k);
                DataObject obj=ds.getObjectByNumId(userid);
                if(obj!=null && k.equals(obj.getString("recoveryToken")))
                {                
                    Map<String, Object> register = new HashMap<>();
                    Map<String, Object> scope = new HashMap<>();
                    scope.put("fullname", obj.getString("fullname"));
                    scope.put("email", obj.getString("email"));
                    scope.put("k", k);
                    scope.put("ctx", request.getContextPath());
                    register.put("restore", scope);
                    response.setCharacterEncoding("utf-8");
                    mustache.execute(response.getWriter(), register);                
                }else
                {
                    Map<String, Object> register = new HashMap<>();
                    Map<String, Object> scope = new HashMap<>();
                    scope.put("error", "Error recovering password");
                    scope.put("ctx", request.getContextPath());
                    register.put("recovery", scope);
                    response.setCharacterEncoding("utf-8");
                    mustache.execute(response.getWriter(), register);                    
                }
            }else
            {
                Map<String, Object> register = new HashMap<>();
                Map<String, Object> scope = new HashMap<>();
                scope.put("ctx", request.getContextPath());
                register.put("recovery", scope);
                response.setCharacterEncoding("utf-8");
                mustache.execute(response.getWriter(), register);
            }
        }
    }

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }
}
