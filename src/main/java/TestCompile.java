
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class TestCompile {
    public static void main(String[] args) throws IOException, InterruptedException 
    {
        String txt="bash /programming/proys/cloudino/server/Cloudino-web/target/Cloudino-web-1.0-SNAPSHOT/WEB-INF/compile.sh /Applications/Arduino.app/Contents/Java arduino:avr:uno /Users/javiersolis/Documents/Arduino/build /Users/javiersolis/Documents/Arduino /Applications/Arduino.app/Contents/Java/examples/01.Basics/Blink/Blink.ino";
        Process p=Runtime.getRuntime().exec(txt);
        InputStream err=p.getInputStream();
        int x=p.waitFor();
        if(err.available()>0)
        {
            byte r[]=new byte[err.available()];
            err.read(r);
            System.out.println(new String(r));
        }
    }
}
