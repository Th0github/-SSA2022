package simulation;

import static simulation.Simulation.stream;

/**
 *	A source of products
 *	This class implements CProcess so that it can execute events.
 *	By continuously creating new events, the source keeps busy.
 *	@author Joel Karel
 *	@version %I%, %G%
 */
public class Source implements CProcess
{
	/** Eventlist that will be requested to construct events */
	private CEventList list;
	/** Queue that buffers products for the machine */
	private ProductAcceptor queue;
	private ProductAcceptor[] queueList;
	/** Name of the source */
	private String name;
	/** Mean interarrival times */
	private double meanArrTime;
	private double meanArrTime2;
	/** Interarrival times (in case pre-specified) */
	private double[] interarrivalTimes;
	/** Interarrival time iterator */
	private int interArrCnt;

	/**
	*	Constructor, creates objects
	*        Interarrival times are exponentially distributed with mean 33
	*	@param q	The receiver of the products
	*	@param l	The eventlist that is requested to construct events
	*	@param n	Name of object
	*/
	public Source(ProductAcceptor q,CEventList l,String n)
	{
		list = l;
		queue = q;
		name = n;
		meanArrTime=33;
		// put first event in list for initialization
		list.add(this,0,drawRandomExponential(meanArrTime)); //target,type,time
	}

	/**
	*	Constructor, creates objects
	*        Interarrival times are exponentially distributed with specified mean
	*	@param q	The receiver of the products
	*	@param l	The eventlist that is requested to construct events
	*	@param n	Name of object
	*	@param m	Mean arrival time
	*/
	public Source(ProductAcceptor q,CEventList l,String n,double m)
	{
		list = l;
		queue = q;
		name = n;
		meanArrTime=m;
		// put first event in list for initialization
		list.add(this,0,drawRandomExponential(meanArrTime)); //target,type,time
	}

	/**
	 *	Constructor, creates objects
	 *        Interarrival times are exponentially distributed with specified mean, can join multiple queues
	 *	@param q	List of receivers of the products
	 *	@param l	The eventlist that is requested to construct events
	 *	@param n	Name of object
	 *	@param m1	Mean arrival time regular customer
	 *  @param m2  	Mean arrival time service desk customer
	 */
	public Source(ProductAcceptor[] q,CEventList l,String n,double m1, double m2)
	{
		list = l;
		queueList = q;
		name = n;
		meanArrTime=m1;
		meanArrTime2=m2;
		// put first event in list for initialization
		list.add(this,0,drawRandomExponential(meanArrTime)); //regular customer initialization
		list.add(this,1,drawRandomExponential(meanArrTime2)); // service desk initialization
	}

	/**
	*	Constructor, creates objects
	*        Interarrival times are prespecified
	*	@param q	The receiver of the products
	*	@param l	The eventlist that is requested to construct events
	*	@param n	Name of object
	*	@param ia	interarrival times
	*/
	public Source(ProductAcceptor q,CEventList l,String n,double[] ia)
	{
		list = l;
		queue = q;
		name = n;
		meanArrTime=-1;
		interarrivalTimes=ia;
		interArrCnt=0;
		// put first event in list for initialization
		list.add(this,0,interarrivalTimes[0]); //target,type,time
	}
	
        @Override
	public void execute(int type, double tme)
	{
		// show arrival
		stream.println("Arrival at time = " + tme);
		// give arrived product to queue
		Product p = new Product(type);
		p.stamp(tme,"Creation",name);
		if (queueList == null) {
			queue.giveProduct(p);
		}
		// This will be the implementation of putting customers in the right queue
		// IMPORTANT NOTE: WE TAKE queueList(5) and queueList(6) as the combined queue
		// With 5 = regular queue and 6 = service desk queue
		else {

			// For clarity
			Queue combinedReg = (Queue) queueList[5];
			Queue combinedSD = (Queue) queueList[6];

			// Service desk customer can only join the service desk queue
			if (type == 1) {
				combinedSD.giveProduct(p);
			}
			// For other customers, a little more complicated
			else {
				int fullQueues = 0; // Queues of length >= 4
				int occupiedQueues = 0; // Queues of length > 0
				int occupiedRegQueues = 0; // needed to ensure 2 regular queues are open
				int minQueueLength = 100; // arbitrarily large
				Queue minQueue = null;
				Queue zeroQueue = null;
				// We first go through the regular queues
				for (int i = 0; i < 5; i++) {
					Queue q = (Queue) queueList[i];
					if (q.getQueueLength() > 0) {
						occupiedRegQueues++;
						occupiedQueues++;
						if (q.getQueueLength() >= 4) {
							fullQueues++;
						}
					}
					if (q.getQueueLength() < minQueueLength && q.getQueueLength() > 0) {
						minQueueLength = q.getQueueLength();
						minQueue = q;
					}
					if (q.getQueueLength() == 0) {
						zeroQueue = q;
					}
				}
				// Add combined queue logic
				if (combinedReg.getQueueLength() + combinedSD.getQueueLength() > 0) {
					occupiedQueues++;
				}
				if (combinedReg.getQueueLength() + combinedSD.getQueueLength() >= 4) {
					fullQueues++;
				}
				if (combinedReg.getQueueLength() + combinedSD.getQueueLength() < minQueueLength) {
					minQueue = combinedReg;
				}
				// ONLY ADD TO QUEUES WHO ALREADY HAVE CUSTOMERS (NOT TO QUEUELENGTH 0), EXCEPT IF THE QUEUES WITH CUSTOMERS HAVE OVER >=4 CUSTOMERS
				// In this case, all queues have at least 4 customers -> new queue can be opened if possible
				// Also note that at least 2 regular queues need to be open
				if (occupiedQueues == fullQueues || occupiedRegQueues < 2) {

					if (zeroQueue != null) {
						zeroQueue.giveProduct(p);
					} else {
						minQueue.giveProduct(p); // Case where all queues are full
					}
				}
				// Otherwise, select the shortest queue
				else {
					if (minQueue != null) {
						minQueue.giveProduct(p);
					}
					else {
						zeroQueue.giveProduct(p);
					}
				}
			}
		}
		// generate duration
		if(meanArrTime>0 && meanArrTime2 > 0)
		{
			// Select the right interarrival time, add right type
			if (type == 1) {
				double duration = drawRandomExponential(meanArrTime2);
				// Create a new event in the eventlist
				list.add(this,1,tme+duration); //target,type,time
			}
			else {
				double duration = drawRandomExponential(meanArrTime);
				// Create a new event in the eventlist
				list.add(this,0,tme+duration); //target,type,time
			}

		}
		else if (meanArrTime > 0) {
			double duration = drawRandomExponential(meanArrTime);
			// Create a new event in the eventlist
			list.add(this,0,tme+duration); //target,type,time
		}
		else
		{
			interArrCnt++;
			if(interarrivalTimes.length>interArrCnt)
			{
				list.add(this,0,tme+interarrivalTimes[interArrCnt]); //target,type,time
			}
			else
			{
				list.stop();
			}
		}

	}
	
	public static double drawRandomExponential(double mean)
	{
		// draw a [0,1] uniform distributed number
		double u = Math.random();
		// Convert it into a exponentially distributed random variate with mean 33
		double res = -mean*Math.log(u);

		return res;
	}
}