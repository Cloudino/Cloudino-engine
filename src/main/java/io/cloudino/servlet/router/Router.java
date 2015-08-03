package io.cloudino.servlet.router;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author serch
 */
public class Router {
    private static final Logger logger = Logger.getLogger("i.c.s.r.Router");
    private static Router instance = null;
    private final Map<String, RouteHandler> routes  = new ConcurrentHashMap<>();
    private final Set<String> securedRoutes = new TreeSet<>();
    private final String loginRoute;
    private final MustacheFactory mf = new DefaultMustacheFactory();
    
    private Router(ScriptObject config){
        loginRoute = (null==config.getString("loginFallback")?"login":config.getString("loginFallback"));
        ScriptObject routesFile = config.get("routeList");
        if (routesFile.isArray()){
            routesFile.values().forEach(this::process);
        }
    }
    
    public static synchronized RouteHandler getHandler(final String path, final DataObject user){
        return instance.getRouterHandler(path, user);
    }
    
    public static void initRouter(ScriptObject config){
        instance = new Router(config);
    }
    private void process(ScriptObject path){
        try {
            if ("true".equalsIgnoreCase(path.getString("isRestricted"))){
                securedRoutes.add(path.getString("routePath"));
            }
            if (null!=path.getString("forwardTo")) {
                final String jspRoute = path.getString("forwardTo");
                RouteHandler rh = new RouteHandler() {

                    @Override
                    public void config(Mustache mustache) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                        System.out.println("about to dispatch");
                        request.getServletContext().getRequestDispatcher(jspRoute).include(request, response);
                        System.out.println("Return from dispatcher");
                    }
                };
                routes.put(path.getString("routePath"), rh);
                return;
            }
            if (null==path.getString("routeHandler")) return;
            RouteHandler rh = (RouteHandler)Class.forName(path.getString("routeHandler")).newInstance();
            String template = path.getString("template");
            InputStreamReader reader = new InputStreamReader(LoginHandler.class.getResourceAsStream("/templates/"+template+".mustache"));
            Mustache mustache = mf.compile(reader, template+".mustache");
            rh.config(mustache);
            routes.put(path.getString("routePath"), rh);
        } catch (ReflectiveOperationException cnf) {
            logger.severe("****** Can't load class: "+path.getString("routeHandler"));
            Runtime.getRuntime().exit(10);
        }
    }
    private RouteHandler getRouterHandler(final String path, final DataObject user){
        String routeb = path.substring(1);
        String route = null;
        if (routeb.contains("/")){
            route=routeb.substring(0, routeb.indexOf("/"));
        } else route=routeb;
        logger.fine("processing route: "+route); 
        if(securedRoutes.contains(route) && 
                ((null==user) || 
                !"true".equalsIgnoreCase(user.getString("isSigned")))){ System.out.println("is Secure, but no User, going for loginRoute");
            return routes.get(loginRoute);
        }
        if (routes.containsKey(routeb))
            return routes.get(routeb);
        return routes.get(route);
    } 
}