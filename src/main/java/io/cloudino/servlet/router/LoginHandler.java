package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataList;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author serch
 */
public class LoginHandler implements RouteHandler {

    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.LoginHandler");

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
        SWBDataSource ds = engine.getDataSource("User");
        
        if (request.getMethod().equals("POST")) {
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            if (email != null && password != null) {
                DataObject r = new DataObject();
                DataObject data = new DataObject();
                r.put("data", data);
                data.put("email", email);
                data.put("password", password);
                DataObject ret = ds.fetch(r);
                //engine.close();

                DataList rdata = ret.getDataObject("response").getDataList("data");
                if (!rdata.isEmpty()) {
                    DataObject user = (DataObject) rdata.get(0);
                    user.put("isSigned", "true");
                    user.put("signedAt", java.time.Instant.now().toString());
                    request.getSession().setAttribute("_USER_", user);
                    response.sendRedirect(request.getContextPath() + "/panel/");
                }
            }
        } else {
            if(null!=request.getParameter("logout")){
                request.getSession().invalidate();
                response.sendRedirect(request.getContextPath() + "/");
            } else {
                Map<String, Object> scope = new HashMap<>();
                scope.put("ctx", request.getContextPath());
                response.setCharacterEncoding("utf-8");
                mustache.execute(response.getWriter(), scope);
            }
        }
    }

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }

}
