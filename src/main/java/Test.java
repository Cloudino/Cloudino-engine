
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class Test {
    public static void main(String[] args) {
        String ino="/* \n" +
" Controlling a servo position using a potentiometer (variable resistor) \n" +
" by Michal Rinott <http://people.interaction-ivrea.it/m.rinott> \n" +
"\n" +
" modified on 8 Nov 2013\n" +
" by Scott Fitzgerald\n" +
" http://arduino.cc/en/Tutorial/Knob\n" +
"*/\n" +
"\n" +
"#include <Servo.h>\n" +
"#include <arduino.h>\n" +
"#include \"Jei1.h\"\n" +
"#include \"Jei2.h\"\n" +                
"\n" +
"Servo myservo;  // create servo object to control a servo\n" +
"\n" +
"int potpin = 0;  // analog pin used to connect the potentiometer\n" +
"int val;    // variable to read the value from the analog pin\n" +
"\n" +
"void setup()\n" +
"{\n" +
"  myservo.attach(9);  // attaches the servo on pin 9 to the servo object\n" +
"}\n" +
"\n" +
"void loop() \n" +
"{ \n" +
"  val = analogRead(potpin);            // reads the value of the potentiometer (value between 0 and 1023) \n" +
"  val = map(val, 0, 1023, 0, 180);     // scale it to use it with the servo (value between 0 and 180) \n" +
"  myservo.write(val);                  // sets the servo position according to the scaled value \n" +
"  delay(15);                           // waits for the servo to get there \n" +
"} ";
        
        
        //Pattern MY_PATTERN = Pattern.compile("\\#include <(.*?)\\>");
        Pattern MY_PATTERN = Pattern.compile("\\#include \"(.*?)\\\"");
        Matcher m = MY_PATTERN.matcher(ino);
        while (m.find()) {
            String s = m.group(1);
            System.out.println("s:"+s);
        }
    }
}
