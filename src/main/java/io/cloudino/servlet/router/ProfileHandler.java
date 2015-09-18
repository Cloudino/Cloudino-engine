package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
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
        System.out.println("request method :"+request.getMethod());
        System.out.println("request type   :"+request.getContentType());
        Map<String, Object> scope = new HashMap<>();
        scope.put("ctx", request.getContextPath());
        scope.put("user", user);
        //response.setCharacterEncoding("utf-8");
        mustache.execute(response.getWriter(), scope);
    }

}
