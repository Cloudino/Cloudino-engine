package io.cloudino.servlet.router;

import com.github.mustachejava.Mustache;
import io.cloudino.utils.MailSender;
import io.cloudino.utils.TokenGenerator;
import io.cloudino.utils.Utils;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.internet.InternetAddress;
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
public class RegisterHandler implements RouteHandler {

    private Mustache mustache;
    private static final Logger logger = Logger.getLogger("i.c.s.r.RegisterHandler");

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
        SWBDataSource ds = engine.getDataSource("User");

        if (request.getMethod().equals("POST")) {
            String fullname = request.getParameter("fullname");
            String email = request.getParameter("email");
            String password = request.getParameter("password");
            String password2 = request.getParameter("password2");
            if (email != null) {
                DataObject data = new DataObject();
                data.put("email", email);
                DataObject query = new DataObject();
                query.put("data", data);
                query = ds.fetch(query);
                DataList lista = query.getDataObject("response").getDataList("data");
                if (lista.size() == 0) {
                    if (password != null) {
                        if (password.equals(password2)) {
                            DataObject obj = new DataObject();
                            obj.put("fullname", fullname);
                            obj.put("email", email);
                            obj.put("password", password);
                            obj.put("registeredAt", ZonedDateTime.now().toString());
                            obj.put("confirmed", "false");
                            obj.put("emailSent", "false");
                            Address userAddr = new InternetAddress(email, fullname, "utf-8");
                            DataObject dao = ds.addObj(obj); 
                            dao = dao.getDataObject("response").getDataObject("data"); System.out.println("dao:"+dao);
                            String content = MessageFormat.format(Utils.textInputStreamToString(
                                    LoginHandler.class.getResourceAsStream("/templates/confirmationMail.template"),"utf-8"),
                                    fullname, TokenGenerator.nextTokenByUserId(dao.getNumId()), email); 
                            MailSender.send(userAddr, "Cloudino confirmation email", content, dao.getNumId());
                            logger.log(Level.FINE, "Register and send mail to: {0}", email);
                            Map<String, Object> register = new HashMap<>();
                            Map<String, Object> scope = new HashMap<>();
                            scope.put("ctx", request.getContextPath());
                            register.put("confirm", scope);
                            response.setCharacterEncoding("utf-8");
                            mustache.execute(response.getWriter(), register);
                        }
                    }
                } else {
                    Map<String, Object> register = new HashMap<>();
                    Map<String, Object> scope = new HashMap<>();
                    scope.put("error", "Email was already registered");
                    scope.put("ctx", request.getContextPath());
                    register.put("register", scope);
                    response.setCharacterEncoding("utf-8");
                    mustache.execute(response.getWriter(), register);
                }
            }
        } else {
            Map<String, Object> register = new HashMap<>();
            Map<String, Object> scope = new HashMap<>();
            scope.put("ctx", request.getContextPath());
            register.put("register", scope);
            response.setCharacterEncoding("utf-8");
            mustache.execute(response.getWriter(), register);
        }
    }

    @Override
    public void config(Mustache mustache) {
        this.mustache = mustache;
    }
}
