package io.cloudino.rules.scriptengine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 *
 * @author serch
 */
public class NashornEngineFactory {
    
    private static final Logger logger = Logger.getLogger("i.c.r.se.NashornEngineFactory");
    
    //ScriptEngines to create al launch;
    final static private int size = 50; 
    final static private Executor service = Executors.newWorkStealingPool();
    //give the internal array some extra room
    final static private BlockingQueue<ScriptEngine> cola = new ArrayBlockingQueue<>((int)(size*110/100)); 
    final static ScriptEngineManager factory = new ScriptEngineManager();
    final static Runnable task = ()-> {
            try {
                cola.offer(factory.getEngineByName("nashorn"), 1, TimeUnit.MINUTES);
            } catch (InterruptedException ie) {
                logger.log(Level.WARNING, "Got interrupted while wating to add an engine", ie);
            }
        };
    static {
        System.out.println("cola:"+cola.remainingCapacity()+" creating scripts.... ");
        for(int i=0; i<size; i++)
            service.execute(task);
    }

    /**
     * Obtain a pre-created Nashorn ScriptEngine
     * @param timeout time to wait for an engine
     * @param unit units of timeout
     * @return a Nashorn ScriptEngine or null if the time elapses before creating a new one
     * @throws InterruptedException 
     */
    public static ScriptEngine getEngine(final long timeout, final TimeUnit unit) throws InterruptedException {
        service.execute(task);
        return cola.poll(timeout, unit);
    }
    
}
