
import java.io.IOException;
import java.util.Date;
import static org.rrd4j.ConsolFun.AVERAGE;
import static org.rrd4j.ConsolFun.MAX;
import static org.rrd4j.ConsolFun.TOTAL;
import static org.rrd4j.DsType.GAUGE;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;
import org.rrd4j.demo.Demo;
import java.util.Random;
import static org.rrd4j.ConsolFun.AVERAGE;
import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author javiersolis
 */
public class TestRrdDB_2 {
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);    
    
    public static void main(String[] args) throws IOException
    {        
        String rrdPath = Util.getRrd4jDemoPath("demo" + ".rrd");
        System.out.println(rrdPath);
        
        int step=5;
        
        RrdDef rrdDef = new RrdDef(rrdPath, step);
        rrdDef.addDatasource("temp", GAUGE, step*2, 0, 1000);
        rrdDef.addDatasource("hume", GAUGE, step*2, 0, 100);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 50);
        System.out.println(rrdDef.dump());
                
        System.out.println("Estimated file size: " + rrdDef.getEstimatedSize());

        RrdDb rrdDb = new RrdDb(rrdDef);
        System.out.println("== RRD file created.");
        if (rrdDb.getRrdDef().equals(rrdDef)) {
            System.out.println("Checking RRD file structure... OK");
        } else {
            System.out.println("Invalid RRD file created. This is a serious bug, bailing out");
            return;
        }
        rrdDb.close();
        System.out.println("== RRD file closed.");
                
        rrdDb = new RrdDb(rrdPath);
        //rrdDb = new RrdDb(rrdDef);        
        Sample sample = rrdDb.createSample();

        long t=Util.getTimestamp(new Date());
        
        for(int x=0;x<1000;x++)
        {
            sample.setTime(t+x);
            if(x%2==0)            
                sample.setValue("temp", (double)x);
            else 
                sample.setValue("hume", RANDOM.nextDouble()*100);    
            //System.out.println(sample.dump());
            sample.update();
        }
        rrdDb.close();
        
        rrdDb = new RrdDb(rrdPath, true);
        println("File reopen in read-only mode");
        println("== Last update time was: " + rrdDb.getLastUpdateTime());
        println("== Last info was: " + rrdDb.getInfo());

        println("ExportXml:"+rrdDb.dump());
/*        
        // fetch data
        println("== Fetching data for the whole month");
        FetchRequest request = rrdDb.createFetchRequest(AVERAGE, start, end);
        println(request.dump());
        FetchData fetchData = request.fetchData();
        println("== Data fetched. " + fetchData.getRowCount() + " points obtained");
        println(fetchData.toString());
        println("== Dumping fetched data to XML format");
        println(fetchData.exportXml());
        println("== Fetch completed");
*/        

        // close files
        rrdDb.close();        
    }
    
    static void println(String txt)
    {
        System.out.println(txt);
    }
    
    
    
}
