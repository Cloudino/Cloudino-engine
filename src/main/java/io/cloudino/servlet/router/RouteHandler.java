package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author serch
 */
public interface RouteHandler {
    void config(Mustache mustache);
    void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException;
}
