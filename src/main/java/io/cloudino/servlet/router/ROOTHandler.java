package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import com.sun.istack.internal.logging.Logger;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author serch
 */
public class ROOTHandler implements RouteHandler {
    private Mustache mustache;
    private static final Logger logger = Logger.getLogger(ROOTHandler.class);
    

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, Object> scope = new HashMap<>();
        scope.put("ctx", request.getContextPath());
        response.setCharacterEncoding("utf-8");
        mustache.execute(response.getWriter(), scope);
    }
    
}
