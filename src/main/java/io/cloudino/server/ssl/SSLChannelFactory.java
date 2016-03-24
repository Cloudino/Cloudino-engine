/*
 * Copyright 2004 WIT-Software, Lda. 
 * - web: http://www.wit-software.com 
 * - email: info@wit-software.com
 *
 * All rights reserved. Relased under terms of the 
 * Creative Commons' Attribution-NonCommercial-ShareAlike license.
 */
package io.cloudino.server.ssl;

import io.cloudino.server.handlers.Channel;
import io.cloudino.server.handlers.ChannelFactory;
import io.cloudino.server.handlers.ChannelListener;
import io.cloudino.server.io.SelectorThread;

import java.io.FileInputStream;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author Nuno Santos
 */
public class SSLChannelFactory implements ChannelFactory {

    private final boolean clientMode;
    private final SSLContext sslContext;
    private final static Logger log = Logger.getLogger("handlers");

    public static SSLContext createSSLContext(
            boolean clientMode,
            String keystore,
            String password)
            throws Exception {
        // Create/initialize the SSLContext with key material
        char[] passphrase = password.toCharArray();
        // First initialize the key and trust material.
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystore), passphrase);
        SSLContext sslContext = SSLContext.getInstance("TLS");

        if (clientMode) {
            // TrustManager's decide whether to allow connections.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            sslContext.init(null, tmf.getTrustManagers(), null);

        } else {
            // KeyManager's decide which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, passphrase);
            sslContext.init(kmf.getKeyManagers(), null, null);
        }
        return sslContext;
    }

    public SSLChannelFactory(boolean clientMode, String keystore, String password)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("Initializing SSL context.");
        if (clientMode) {
            sb.append(" Client mode. TrustStore: " + keystore);
        } else {
            sb.append(" Server mode. KeyStore: " + keystore);
        }
        log.info(sb.toString());

        this.clientMode = clientMode;
        sslContext = createSSLContext(clientMode, keystore, password);
    }

    public Channel createChannel(SocketChannel sc, SelectorThread st, ChannelListener l)
            throws Exception {
        log.info("Creating SecureChannel. Client mode: " + clientMode);
        SSLEngine engine = sslContext.createSSLEngine();
        engine.setUseClientMode(clientMode);

        return new SSLChannel(st, sc, l, engine);
    }
}
