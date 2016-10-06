
import java.io.IOException;
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
public class TestRrdDB {
    static final long SEED = 1909752002L;
    static final Random RANDOM = new Random(SEED);    
    
    static final long START = Util.getTimestamp(2010, 4, 1);
    static final long END = Util.getTimestamp(2010, 6, 1);
    static final int MAX_STEP = 300;    
    
    public static void main(String[] args) throws IOException
    {
        
        String rrdPath = Util.getRrd4jDemoPath("demo" + ".rrd");
        System.out.println(rrdPath);
        
        long start = START;
        long end = END;        
                
        RrdDef rrdDef = new RrdDef(rrdPath, start - 1, 300);
        rrdDef.setVersion(2);
        rrdDef.addDatasource("sun", GAUGE, 600, 0, Double.NaN);
        rrdDef.addDatasource("shade", GAUGE, 600, 0, Double.NaN);
        rrdDef.addArchive(AVERAGE, 0.5, 1, 600);
        rrdDef.addArchive(AVERAGE, 0.5, 6, 700);
        rrdDef.addArchive(AVERAGE, 0.5, 24, 775);
        rrdDef.addArchive(AVERAGE, 0.5, 288, 797);
        rrdDef.addArchive(TOTAL, 0.5, 1, 600);
        rrdDef.addArchive(TOTAL, 0.5, 6, 700);
        rrdDef.addArchive(TOTAL, 0.5, 24, 775);
        rrdDef.addArchive(TOTAL, 0.5, 288, 797);
        rrdDef.addArchive(MAX, 0.5, 1, 600);
        rrdDef.addArchive(MAX, 0.5, 6, 700);
        rrdDef.addArchive(MAX, 0.5, 24, 775);
        rrdDef.addArchive(MAX, 0.5, 288, 797);
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
        
        
        // update database
        GaugeSource sunSource = new GaugeSource(1200, 20);
        GaugeSource shadeSource = new GaugeSource(300, 10);
        System.out.println("== Simulating one month of RRD file updates with step not larger than " +
                MAX_STEP + " seconds (* denotes 1000 updates)");
        long t = start;
        int n = 0;
        rrdDb = new RrdDb(rrdPath);
        Sample sample = rrdDb.createSample();

        while (t <= end + 172800L) {
            sample.setTime(t);
            sample.setValue("sun", sunSource.getValue());
            sample.setValue("shade", shadeSource.getValue());
            System.out.println(sample.dump());
            sample.update();
            t += RANDOM.nextDouble() * MAX_STEP + 1;
            if (((++n) % 1000) == 0) {
                System.out.print("*");
            }
        }
        rrdDb.close();
        System.out.println("");
        System.out.println("== Finished. RRD file updated " + n + " times");                
        
        
        
        
        rrdDb = new RrdDb(rrdPath, true);
        println("File reopen in read-only mode");
        println("== Last update time was: " + rrdDb.getLastUpdateTime());
        println("== Last info was: " + rrdDb.getInfo());

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

        // dump to XML file
        //println("== Dumping RRD file to XML file " + xmlPath + " (can be restored with RRDTool)");
        //rrdDb.exportXml(xmlPath);
        //println("== Creating RRD file " + rrdRestoredPath + " from XML file " + xmlPath);
        //RrdDb rrdRestoredDb = new RrdDb(rrdRestoredPath, xmlPath);

        // close files
        println("== Closing both RRD files");
        rrdDb.close();
        //println("== First file closed");
        //rrdRestoredDb.close();
        //println("== Second file closed");        
        
        
    }
    
    static void println(String txt)
    {
        System.out.println(txt);
    }
    
    
    static class GaugeSource {
        private double value;
        private double step;

        GaugeSource(double value, double step) {
            this.value = value;
            this.step = step;
        }

        long getValue() {
            double oldValue = value;
            double increment = RANDOM.nextDouble() * step;
            if (RANDOM.nextDouble() > 0.5) {
                increment *= -1;
            }
            value += increment;
            if (value <= 0) {
                value = 0;
            }
            return Math.round(oldValue);
        }
    }    
    
}
