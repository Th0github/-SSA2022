/**
 *	Example program for using eventlists
 *	@author Joel Karel
 *	@version %I%, %G%
 */

package simulation;

import java.io.*;
import java.util.ArrayList;


public class Simulation {

    public CEventList list;
    public Queue queue;
    public Source source;
    public Sink sink;
    public Machine mach;

    //to print the output
    public static PrintStream stream; //for mean for overall

        /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    	bonus_initialize();
    }

    public static void lab1_initialize() {
        CEventList l = new CEventList();
        // A queue for the machine
        Queue q = new Queue();
        // A source
        Source s = new Source(q,l,"Source 1", new double[]{0.4,1.2,0.5,1.7,0.2,1.6,0.2,1.4,1.9});
        // A sink
        Sink si = new Sink("Sink 1");
        // A machine
        Machine m = new Machine(q,si,l,"Machine 1", new double[]{2.0,0.7,0.2,1.1,3.7,0.6,4.0,4.0,4.0});
        // start the eventlist
        l.start(8.7); // 2000 is maximum time
        String[] ev = si.getEvents();
        double[] times = si.getTimes();
        for (int i = 0; i < ev.length; i++) {
            System.out.println("Event " + ev[i] + " at " + times[i]);
        }
    }

    /**
     * Initializes and runs simulation for bonus project
     * Uses 6 machines; 5 regular and 1 combined
     * 2 machines are initially open, others open as queuelength gets to 4
     */
    public static void bonus_initialize() {
        try  {
            //stream collects all terminal output to a txt file
            stream = new PrintStream("Output.txt");

            // Create an eventlist
            CEventList l = new CEventList();

            // All Queues
            Queue[] allQueues = new Queue[7];
            for (int i = 0; i < 7; i++) {
                allQueues[i] = new Queue();
            }

            // Source will generate interarrival time based on product type
            Source s = new Source(allQueues, l, "Customers", 1.0, 5.0);

            // A sink
            Sink si = new Sink("Sink 1");

            // Regular registers
            for (int i = 0; i < 5; i++) {
                Machine m = new Machine(allQueues[i], si, l, ("Regular Register" + i), 2.6, 1.1, (1.0 / 60.0));
            }
            // Combined register, needs to accept 2 queues and work with different service times
            // IMPORTANT NOTE: WE TAKE queueList(5) and queueList(6) as the combined queue
            // With 5 = regular queue and 6 = service desk queue
            Machine combRegister = new Machine(new Queue[]{allQueues[5], allQueues[6]}, si, l, "Combined Register", 2.6, 1.1, (1.0 / 60.0), 4.1, 1.1, (1.0 / 60.0));

            // Can decide on how many days to run the simulation for easily
            int days = 1;
            // Start simulation, specifying stopping criteria
            l.start(1440 * days); // One day has 1440 minutes

            //Sink
            //return Type: 0 is regular customer and 1 is service customer

            // Output for mean for service desk customers and regular customers.
            FileWriter writer = new FileWriter("Output.csv");
            //headers
            BufferedWriter bw = new BufferedWriter(writer);
            bw.write(si.getEvents()[0]);
            bw.write(" , ");
            bw.write(si.getEvents()[1]);
            bw.write(" , ");
            bw.write(si.getEvents()[2]);
            bw.write(" , ");
            bw.write("Type");

            bw.write("\n");

            for(int i =0; i < si.getProducts().size(); i++) {
                bw.write(String.valueOf(si.getProducts().get(i).getTimes().get(0)));
                bw.write(" , ");
                bw.write(String.valueOf(si.getProducts().get(i).getTimes().get(1)));
                bw.write(" , ");
                bw.write(String.valueOf(si.getProducts().get(i).getTimes().get(2)));
                bw.write(" , ");
                bw.write(String.valueOf(si.getProducts().get(i).getType()));
                bw.write("\n");
            }

            bw.close();

        } catch (IOException e) {
            File output = new File("Output.txt");
            File output2 = new File("Output.csv");
        }
    }
}
