package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
import java.time.ZonedDateTime;
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
import sun.security.krb5.JavaxSecurityAuthKerberosAccess;

/**
 *
 * @author serch
 */
public class RegisterHandler implements RouteHandler {

    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.RegisterHandler");

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
        SWBDataSource ds = engine.getDataSource("User");
        
        if (request.getMethod().equals("POST")) {
            String fullname = request.getParameter("fullname");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String password2 = request.getParameter("password2");
            if (email != null && password != null) {
                if (password.equals(password2)) {
                    DataObject obj = new DataObject();
                    obj.put("fullname", fullname);
                    obj.put("email", email);
                    obj.put("password", password);
                    obj.put("registeredAt", ZonedDateTime.now().toString());
                    ds.addObj(obj);
                    //engine.close();
                    response.sendRedirect(request.getContextPath() + "/panel");
                }
            }
        } else {
            Map<String, Object> scope = new HashMap<>();
            scope.put("ctx", request.getContextPath());
            response.setCharacterEncoding("utf-8");
            mustache.execute(response.getWriter(), scope);
        }
    }

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }
}
