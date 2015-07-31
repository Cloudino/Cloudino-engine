package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import com.sun.istack.internal.logging.Logger;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author serch
 */
public class PanelHandler implements RouteHandler {
    private Mustache mustache;
    private static final Logger logger = Logger.getLogger(PanelHandler.class);
    
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        DataObject user = (DataObject)request.getSession().getAttribute("_USER_");
        Map<String, Object> scope = new HashMap<>();
            scope.put("ctx", request.getContextPath());
            scope.put("fullname", user.getString("fullname"));
            response.setCharacterEncoding("utf-8");
            mustache.execute(response.getWriter(), scope);
    }
    
    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }
}
