package io.cloudino.servlet;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.cloudino.servlet.router.LoginHandler;
import io.cloudino.servlet.router.RouteHandler;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.RouteData;
import org.semanticwb.datamanager.RoutesMgr;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author serch
 */
@WebFilter(urlPatterns = {"/*"})
public class CloudinoFilter implements Filter {

    private static final Logger logger = Logger.getLogger("i.c.s.CloudFilter");
    //private static final Router router = Router.getRouter();
    private final MustacheFactory mf = new DefaultMustacheFactory();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Starting Cloudino...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Object obj = ((HttpServletRequest) request).getSession().getAttribute("_USER_");
//        System.out.println("obj:"+obj);
        DataObject user = null;
        if ((null != obj) && (obj instanceof DataObject)) {
            user = (DataObject) obj;
        }
        HttpServletRequest hreq = ((HttpServletRequest) request);
//        System.out.println("*************************************");
//        System.out.println("getContextPath:"+hreq.getContextPath());
//        System.out.println("getPathInfo:"+hreq.getPathInfo());
//        System.out.println("getPathTranslated:"+hreq.getPathTranslated());
//        System.out.println("getRequestURI:"+hreq.getRequestURI());
//        System.out.println("getServletPath:"+hreq.getServletPath());
//        System.out.println("getRequestURL:"+hreq.getRequestURL());
        String path = hreq.getRequestURI().substring(hreq.getContextPath().length());
//        System.out.println("path->:"+path);
        
        RouteData data = RoutesMgr.getRouteData(getRouterPath(path));
        RouteHandler handler = null;
        if (data != null) {
            if(!(data.isSecure() && user==null))
            {            
                handler = (RouteHandler) data.getHandler();
                if (handler == null) {
                    synchronized (data) {
                        if (handler == null) {
                            handler = getHandler(data);
                        }
                    }
                }
            }else
            {
                RouteData login=RoutesMgr.getRouteData(RoutesMgr.getLoginRoute());
                if (login != null) {                             
                    handler = (RouteHandler) login.getHandler();
                    if (handler == null) {
                        synchronized (login) {
                            if (handler == null) {
                                handler = getHandler(login);
                            }
                        }
                    }
                }
            }
        }
        if (null == handler) {
            chain.doFilter(request, response);
        } else {
            handler.handle(hreq, (HttpServletResponse) response, user);
        }
    }

    @Override
    public void destroy() {

    }

    private String getRouterPath(final String path) {
        final String routeb = path.substring(1);
        String route = null;
        if (routeb.contains("/")) {
            route = routeb.substring(0, routeb.indexOf("/"));
        } else {
            route = routeb;
        }
        if (RoutesMgr.getRouteData(routeb)!=null) {
            return routeb;
        }
        final String routeJsp = routeb.substring(0, routeb.lastIndexOf("/") + 1) + "*";
        
        if (RoutesMgr.getRouteData(routeJsp)!=null) {
            return routeJsp;
        }
        
        //System.out.println("getRouterPath:"+path+"->"+route);
        return route;
    }

    private RouteHandler getHandler(RouteData data) {
        ScriptObject path = data.getScriptObject();
//        System.out.println("getHandler:" + path);
        try {
//            if ("true".equalsIgnoreCase(path.getString("isRestricted"))) {
//                securedRoutes.add(path.getString("routePath"));
//            }
            if (null != path.getString("forwardTo")) {
                final String jspRoute = path.getString("forwardTo");
                RouteHandler rh = new RouteHandler() {

                    @Override
                    public void config(Mustache mustache) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
                        request.getServletContext().getRequestDispatcher(jspRoute).forward(request, response);
                    }
                };
                //routes.put(path.getString("routePath"), rh);
                data.setHandler(rh);
                return rh;
            }
            if (null != path.getString("jspMapTo")) {
                final String jspRoute = path.getString("jspMapTo");
                RouteHandler rh = new RouteHandler() {
                    private final String mapTo = jspRoute;

                    @Override
                    public void config(Mustache mustache) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }

                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
                        String name = request.getRequestURI();
                        name = mapTo + name.substring(name.lastIndexOf("/") + 1) + ".jsp";
                        if (Files.exists(Paths.get(request.getServletContext().getRealPath(name)))) {
                            request.getServletContext().getRequestDispatcher(name).forward(request, response);
                        } else {
                            name = request.getRequestURI().substring(request.getContextPath().length()).substring(1);
                            name = name.substring(0, name.indexOf("/"));
                            
                            
                            RouteData rd = RoutesMgr.getRouteData(name);

                            if (rd != null) {
                                logger.fine("encontré handler de " + name);
                                ((RouteHandler) rd.getHandler()).handle(request, response, user);
                            } else {
                                name = request.getRequestURI();
                                name = mapTo + name.substring(name.lastIndexOf("/") + 1) + "index.jsp";
                                logger.fine("buscando si hay " + name);
                                if (Files.exists(Paths.get(request.getServletContext().getRealPath(name)))) {
                                    request.getServletContext().getRequestDispatcher(name).forward(request, response);
                                } else {
                                    response.sendError(404, request.getRequestURI() + " not found!");
                                }
                            }
                        }
                    }
                };
                //routes.put(path.getString("routePath"), rh);
                data.setHandler(rh);
                return rh;
            }
            if (null == path.getString("routeHandler")) {
                return null;
            }
            RouteHandler rh = (RouteHandler) Class.forName(path.getString("routeHandler")).newInstance();
            Mustache mustache = null;
            String template = path.getString("template");
            if (null!=template) {
                InputStreamReader reader = new InputStreamReader(LoginHandler.class.getResourceAsStream("/templates/" + template + ".mustache"));
                mustache = mf.compile(reader, template + ".mustache");
            }
            rh.config(mustache);
            //routes.put(path.getString("routePath"), rh);
            data.setHandler(rh);
            return rh;            

        } catch (Exception cnf) {
            logger.log(Level.SEVERE, "****** Can''t load class: {0}", path.getString("routeHandler"));
            Runtime.getRuntime().exit(10);
        }
        return null;
    }

}
