/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
import com.notnoop.apns.*;

public class MyPush {

    public static void main(String[] args) {
        String myCertPath = "/Users/javiersolis/Desktop/iphone_certs/apn_development.p12";
        String myPassword = "cloudino";
        String myToken = "25de509de97a9efafaee322eefdb6f051d84580d67830849e350e72ff230b66c";

        ApnsService service = APNS.newService()
                .withCert(myCertPath, myPassword)
                .withSandboxDestination()
                .build();
        String myPayload = APNS.newPayload()
                //.alertBody(args[0])
                .alertBody("Hola Mundo")
                .badge(1)
                .sound("default")
                .build();
        service.push(myToken, myPayload);
    }

}
