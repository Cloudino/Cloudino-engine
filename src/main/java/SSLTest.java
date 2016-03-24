/* HttpsHello.java
 - Copyright (c) 2014, HerongYang.com, All Rights Reserved.
 */
import java.io.*;
import java.security.*;
import javax.net.ssl.*;
public class SSLTest {
   public static void main(String[] args) {
      String ksName = "/programming/proys/cloudino/server/MyDSKeyStore.jks";
      char ksPass[] = "changeit".toCharArray();
      char ctPass[] = "changeit".toCharArray();
      try {
         KeyStore ks = KeyStore.getInstance("JKS");
         ks.load(new FileInputStream(ksName), ksPass);
         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
         kmf.init(ks, ctPass);
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(kmf.getKeyManagers(), null, null);
         SSLServerSocketFactory ssf = sc.getServerSocketFactory();
         SSLServerSocket s = (SSLServerSocket) ssf.createServerSocket(9494);
         System.out.println("Server started:");
         printServerSocketInfo(s);
         // Listening to the port
         SSLSocket c = (SSLSocket) s.accept();
         printSocketInfo(c);
         c.setTcpNoDelay(true);
         
         OutputStream w = c.getOutputStream();
         InputStream r = c.getInputStream();
         //while(r.available()<0);
         System.out.print((char)r.read());
         while(r.available()>0)
         {
             System.out.print((char)r.read());
         }
         w.write("OK\n".getBytes());
         w.flush();
         w.close();
         r.close();
         c.close();
         System.out.println("\nend");
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   private static void printSocketInfo(SSLSocket s) {
      System.out.println("Socket class: "+s.getClass());
      System.out.println("   Remote address = "
         +s.getInetAddress().toString());
      System.out.println("   Remote port = "+s.getPort());
      System.out.println("   Local socket address = "
         +s.getLocalSocketAddress().toString());
      System.out.println("   Local address = "
         +s.getLocalAddress().toString());
      System.out.println("   Local port = "+s.getLocalPort());
      System.out.println("   Need client authentication = "
         +s.getNeedClientAuth());
      SSLSession ss = s.getSession();
      System.out.println("   Cipher suite = "+ss.getCipherSuite());
      System.out.println("   Protocol = "+ss.getProtocol());
   }
   private static void printServerSocketInfo(SSLServerSocket s) {
      System.out.println("Server socket class: "+s.getClass());
      System.out.println("   Socket address = "
         +s.getInetAddress().toString());
      System.out.println("   Socket port = "
         +s.getLocalPort());
      System.out.println("   Need client authentication = "
         +s.getNeedClientAuth());
      System.out.println("   Want client authentication = "
         +s.getWantClientAuth());
      System.out.println("   Use client mode = "
         +s.getUseClientMode());
   } 
}