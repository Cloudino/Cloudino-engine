package io.cloudino.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.semanticwb.datamanager.DataMgr;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.SWBDataSource;
import org.semanticwb.datamanager.SWBScriptEngine;

/**
 *
 * @author serch
 */
public class MailSender {

    private static final ExecutorService proccessor = Executors.newSingleThreadExecutor();
    private static final SWBScriptEngine engine = DataMgr.getUserScriptEngine("/cloudino.js", null);
    private static final Logger logger = Logger.getLogger("i.c.u.MailSender");
    private static final SWBDataSource ds = engine.getDataSource("User");

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                proccessor.shutdown();
                proccessor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException ie) {
                logger.log(Level.OFF, "Exiting...", ie);
            }
        }));
    }

    /**
     * Sends an confirmation email in the background using the server configured in cloudino.js
     * @param userAddr Address to send the mail
     * @param subject email subject
     * @param content email content
     * @param userID id to update on mail sent
     */
    public static void send(final Address userAddr, final String subject, final String content, final String userID) {
        try {
            Session session = MailSender.getSession();
            Message msg = new MimeMessage(session);
            Address[] addrs = new Address[]{MailSender.getFromAdd()};
            msg.addFrom(addrs);
            msg.setRecipient(Message.RecipientType.TO, userAddr);
            msg.setSubject(subject);
            msg.setDataHandler(new DataHandler(content, "text/html"));
            proccessor.submit(() -> {
                try {
                    Transport t = session.getTransport(engine.getScriptObject().get("config").getString("smtptransport"));
                    t.connect(engine.getScriptObject().get("config").getString("smtpHost"),
                            engine.getScriptObject().get("config").getInt("smtpPort"),
                            engine.getScriptObject().get("config").getString("smtpUser"),
                            engine.getScriptObject().get("config").getString("smtpPassword"));
                    t.sendMessage(msg, (new Address[]{userAddr}));
                    t.close();
                    DataObject dao = ds.fetchObjByNumId(userID);
                    dao.put("emailSent", "true");
                    ds.updateObj(dao);
                } catch (IOException | MessagingException uex) {
                    logger.log(Level.SEVERE, "Email Sending error ", uex);
                }
            });
        } catch (IOException | MessagingException uex) {
            logger.log(Level.SEVERE, "Email Sender is not configured ", uex);
        }
    }

    private static Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", engine.getScriptObject().get("config").getString("smtpHost"));
        props.put("mail.smtp.port", engine.getScriptObject().get("config").getInt("smtpPort"));
        if (null != engine.getScriptObject().get("config").getString("ssltrust")) {
            props.put("mail.smtps.ssl.trust", engine.getScriptObject().get("config").getString("ssltrust"));
        }
        if (null != engine.getScriptObject().get("config").getString("starttls")) {
            props.put("mail.smtp.starttls.enable", engine.getScriptObject().get("config").getString("starttls"));
        }
        return Session.getInstance(props);
    }

    private static Address getFromAdd() throws UnsupportedEncodingException {
        return new InternetAddress(engine.getScriptObject().get("config").getString("fromEmail"),
                engine.getScriptObject().get("config").getString("fromName"));
    }

}
