
import java.util.Timer;
import java.util.TimerTask;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class TestTimer {
    public static void main(String[] args) throws Exception
    {
        setTimeout();
        Thread.sleep(10000);       
        System.gc();
    }
    
    static void setTimeout() throws Exception
    {
        Timer time=new Timer("Rule");
        time.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Hola");
            }
        }, 5000);   
    }
}
