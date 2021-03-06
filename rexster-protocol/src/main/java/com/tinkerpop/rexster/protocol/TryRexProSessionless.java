package com.tinkerpop.rexster.protocol;

import com.tinkerpop.rexster.client.RexsterClient;
import com.tinkerpop.rexster.client.RexsterClientFactory;
import com.tinkerpop.rexster.client.RexsterClientTokens;
import com.tinkerpop.rexster.protocol.msg.RexProChannel;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.msgpack.type.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.msgpack.template.Templates.TString;
import static org.msgpack.template.Templates.TValue;
import static org.msgpack.template.Templates.tList;
import static org.msgpack.template.Templates.tMap;

/**
 * A bit of an experiment.
 */
public class TryRexProSessionless implements Runnable {

    static {
        BasicConfigurator.configure();
    }

    private int cycle = 0;
    private final String host;
    private final int exerciseTime;

    public static void main(final String[] args) throws Exception {
        int c = Integer.parseInt(args[1]);
        final int exerciseTime = Integer.parseInt(args[2]) * 60 * 1000;

        for (int ix = 0; ix < c; ix++) {
            new Thread(new TryRexProSessionless(args[0],  exerciseTime)).start();
        }

        Thread.currentThread().join();
        System.exit(0);
    }

    public TryRexProSessionless(final String host, final int exerciseTime) {
        this.exerciseTime = exerciseTime;
        this.host = host;
    }

    @Override
    public void run()  {
        this.lotsOfCalls();
    }

    private void lotsOfCalls() {

        final long start = System.currentTimeMillis();
        long checkpoint = System.currentTimeMillis();
        final Random random = new Random();

        RexsterClient client = null;
        try {
            client = RexsterClientFactory.open(host, "gratefulgraph");

            while ((System.currentTimeMillis() - start) < exerciseTime) {
                cycle++;
                System.out.println("Exercise cycle: " + cycle);

                try {
                    int counter = 1;

                    final int vRequestCount = random.nextInt(500);
                    for (int iv = 1; iv < vRequestCount; iv++) {
                        final Map<String,Object> scriptArgs = new HashMap<String, Object>();
                        scriptArgs.put("id", random.nextInt(800));
                        final List<Map<String, Object>> innerResults = client.execute("g.v(id)", scriptArgs);
                        System.out.println(innerResults.get(0));
                        counter++;
                    }

                    final int eRequestCount = random.nextInt(500);
                    for (int ie = 1; ie < eRequestCount; ie++) {
                        final Map<String,Object> scriptArgs = new HashMap<String, Object>();
                        scriptArgs.put("id", random.nextInt(8000));
                        final List<Map<String, Object>> innerResults = client.execute("g.e(id)", scriptArgs);
                        System.out.println(innerResults.get(0));
                        counter++;
                    }

                    final int gRequestCount = random.nextInt(1000);
                    for (int ig = 1; ig < gRequestCount; ig++) {
                        final Map<String,Object> scriptArgs = new HashMap<String, Object>();
                        scriptArgs.put("id", random.nextInt(800));
                        final List<Map<String, Object>> innerResults = client.execute("g.v(id).out('followed_by').loop(1){it.loops<3}[0..10]", scriptArgs);
                        System.out.println(innerResults.size() > 0 ? innerResults.get(0) : "no results");
                        counter++;
                    }

                    final long end = System.currentTimeMillis() - checkpoint;
                    System.out.println((checkpoint - start) + ":" + end);
                    System.out.println(counter / (end / 1000));
                } catch (Exception ex) {
                    System.out.println("Error during TEST CYCLE (stack trace follows)");
                    ex.printStackTrace();
                    if (ex.getCause() != null) {
                        System.out.println("There is an inner exception (stack trace follows)");
                        ex.getCause().printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                try { client.close(); } catch(Exception e) {}
            }
        }
    }
}