package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import java.io.IOException;
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
public class ValidatorHandler implements RouteHandler {

    private static final Logger logger = Logger.getLogger("i.c.s.r.ValidatorHandler");
    SWBDataSource ds = null;

    @Override
    public void config(Mustache mustache) {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
        ds = engine.getDataSource("User");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, DataObject user) throws IOException, ServletException {
        String cmd = request.getServletPath();
        cmd = cmd.substring(cmd.lastIndexOf("/") + 1);
        switch (cmd) {
            case "email": {
                String email = request.getParameter("email");
                if (email != null) {
                    DataObject data = new DataObject();
                    data.put("email", email);
                    DataObject query = new DataObject();
                    query.put("data", data);
                    query = ds.fetch(query);
                    DataList lista = query.getDataObject("response").getDataList("data");
                    if (lista.size() == 0) {
                        response.getWriter().println("ok");
                    } else {
                        response.sendError(409, "email already in use");
                    }
                }
                break;
            }
            default: {
                response.sendError(405, "No Validator available for that message");
            }
        }
    }
}
