package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
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
public class LoginHandler implements RouteHandler {

    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.LoginHandler");
    
    private void login(HttpServletRequest request, HttpServletResponse response, DataObject user)throws IOException
    {
        user.put("isSigned", "true");
        user.put("signedAt", java.time.ZonedDateTime.now().toString());
        if (!user.containsKey("registro")) {
            user.put("registeredAt", java.time.ZonedDateTime.now().toString());
        }
        request.getSession().setAttribute("_USER_", user);
        //System.out.println("getRequestURI:"+request.getRequestURI());
        //System.out.println("getRequestURL:"+request.getRequestURL());
        //System.out.println("getQueryString:"+request.getQueryString());

        String query=request.getQueryString();
        String path=request.getRequestURI()+((query!=null)?"?"+query:"");
        if(path.startsWith("/login"))
        {
            path="/panel/";
        }
        response.sendRedirect(request.getContextPath() + path);        
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject pUser) throws IOException, ServletException {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
        SWBDataSource ds = engine.getDataSource("User");
        
        if (request.getMethod().equals("POST")) {
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String remember = request.getParameter("remember");
            if (email != null && password != null) {
                DataObject r = new DataObject();
                DataObject data = new DataObject();
                r.put("data", data);
                data.put("email", email);
                data.put("password", password);
                data.put("active", "true");
                DataObject ret = ds.fetch(r);
                //engine.close();

                DataList rdata = ret.getDataObject("response").getDataList("data");
                if (!rdata.isEmpty()) {
                    DataObject user = (DataObject) rdata.get(0);
                    
                    if(remember!=null)
                    {
                        Cookie c=new Cookie("cdino",TokenGenerator.nextTokenByUserId(user.getNumId()));
                        c.setMaxAge(60*60*24*365);
                        response.addCookie(c);
                    }                        
                    
                    login(request, response, user);
                    return;
                }
            } 
            Map<String, Object> scope = new HashMap<>();
            scope.put("ctx", request.getContextPath());
            scope.put("error", "Authentication failed");
            response.setCharacterEncoding("utf-8");
            mustache.execute(response.getWriter(), scope);
        } else {
            if(null!=request.getParameter("logout")){
                Cookie cooks[]=request.getCookies();
                if(cooks!=null)
                {
                    for(int i=0;i<cooks.length;i++)
                    {
                        if(cooks[i].getName().equals("cdino"))
                        {
                            cooks[i].setMaxAge(0);
                            cooks[i].setPath("/panel");
                            response.addCookie(cooks[i]);
                        }
                    }                
                }
                request.getSession().invalidate();
                response.sendRedirect(request.getContextPath() + "/");                
                return;                
            } else {
                Cookie cooks[]=request.getCookies();
                if(cooks!=null)
                {
                    for(int i=0;i<cooks.length;i++)
                    {
                        if(cooks[i].getName().equals("cdino"))
                        {
                            String uid=TokenGenerator.getUserIdFromToken(cooks[i].getValue());
                            DataObject user=ds.getObjectByNumId(uid);
                            login(request, response, user);
                            return;
                        }
                    }
                }
                Map<String, Object> scope = new HashMap<>();
                scope.put("ctx", request.getContextPath());
                response.setCharacterEncoding("utf-8");
                response.setStatus(403);
                mustache.execute(response.getWriter(), scope);
            }
        }
    }

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }

}
