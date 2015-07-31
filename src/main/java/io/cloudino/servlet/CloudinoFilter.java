package io.cloudino.servlet;

import io.cloudino.servlet.router.RouteHandler;
import io.cloudino.servlet.router.Router;
import java.io.IOException;
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

/**
 *
 * @author serch
 */
@WebFilter(urlPatterns = {"/*"})
public class CloudinoFilter implements Filter {
    private static final Logger logger = Logger.getLogger("i.c.s.CloudFilter");
    //private static final Router router = Router.getRouter();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Starting Cloudino...");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Object obj = ((HttpServletRequest)request).getSession().getAttribute("_USER_");System.out.println("obj:"+obj);
        DataObject dobj = null;
        if ((null!=obj) && (obj instanceof DataObject)) {
            dobj = (DataObject)obj;
        }
        HttpServletRequest hreq = ((HttpServletRequest)request);
        System.out.println("*************************************");
        System.out.println("getContextPath:"+hreq.getContextPath());
        System.out.println("getPathInfo:"+hreq.getPathInfo());
        System.out.println("getPathTranslated:"+hreq.getPathTranslated());
        System.out.println("getRequestURI:"+hreq.getRequestURI());
        System.out.println("getServletPath:"+hreq.getServletPath());
        System.out.println("getRequestURL:"+hreq.getRequestURL());
        RouteHandler rh = Router.getHandler(hreq.getServletPath(), dobj);System.out.println("rh:"+rh);
        if (null==rh) { 
            chain.doFilter(request, response);
        } else {
            rh.handle(hreq, (HttpServletResponse)response);
        }
        System.out.println("");
    }

    @Override
    public void destroy() {
        
    }
    
}
