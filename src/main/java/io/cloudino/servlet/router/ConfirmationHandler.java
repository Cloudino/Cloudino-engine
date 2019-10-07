package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;
import org.semanticwb.datamanager.utils.TokenGenerator;

/**
 *
 * @author serch
 */
public class ConfirmationHandler implements RouteHandler {
    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.ConfirmationHandler");
    private final SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
    private final SWBDataSource ds = engine.getDataSource("User");
    
    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
        String email = request.getParameter("mail");
        String token = request.getServletPath();
        token = token.substring(token.lastIndexOf("/")+1);
        String id = TokenGenerator.getUserIdFromToken(token);
        DataObject dao = ds.fetchObjByNumId(id);
        logger.log(Level.FINE, "id:  {0}", id);
        logger.log(Level.FINE, "dao: {0}", dao);
        Map<String, Object> scope = new HashMap<>();
        scope.put("ctx", request.getContextPath());
        if (email!=null && dao!=null && email.equals(dao.getString("email"))){
            dao.put("confirmed", "true");
            dao.put("active", "true");
            dao = ds.updateObj(dao);
            logger.log(Level.FINE, "updated: {0}", dao);
            scope.put("confirmed", "true");
        } else {
            scope.put("error", "Invalid request");
        }
            response.setCharacterEncoding("utf-8");
            mustache.execute(response.getWriter(), scope);
        
    }
}
