package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author serch
 */
public interface RouteHandler {
    void config(Mustache mustache);
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
}
