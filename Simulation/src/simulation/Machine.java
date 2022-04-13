package simulation;

/**
 *	Machine in a factory
 *	@author Joel Karel
 *	@version %I%, %G%
 */
public class Machine implements CProcess,ProductAcceptor
{
	/** Product that is being handled  */
	private Product product;
	/** Eventlist that will manage events */
	private final CEventList eventlist;
	/** Queue(s) from which the machine has to take products */
	private Queue queue;
	private Queue[] queueList;
	/** Sink to dump products */
	private ProductAcceptor sink;
	/** Status of the machine (b=busy, i=idle) */
	private char status;
	/** Machine name */
	private final String name;
	/** Parameters regular*/
	private double meanProcTime;
	private double sdReg;
	private double minReg;
	/** Parameters service desk*/
	private double meanProcTimeSD;
	private double sdSD;
	private double minSD;
	/** Processing times (in case pre-specified) */
	private double[] processingTimes;
	/** Processing time iterator */
	private int procCnt;
	

	/**
	*	Constructor
	*        Service times are exponentially distributed with mean 30
	*	@param q	Queue from which the machine has to take products
	*	@param s	Where to send the completed products
	*	@param e	Eventlist that will manage events
	*	@param n	The name of the machine
	*/
	public Machine(Queue q, ProductAcceptor s, CEventList e, String n)
	{
		status='i';
		queue=q;
		sink=s;
		eventlist=e;
		name=n;
		meanProcTime=30;
		queue.askProduct(this);
	}

	/**
	*	Constructor
	*        Service times are exponentially distributed with specified mean
	*	@param q	Queue from which the machine has to take products
	*	@param s	Where to send the completed products
	*	@param e	Eventlist that will manage events
	*	@param n	The name of the machine
	*        @param m	Mean processing time
	*/
	public Machine(Queue q, ProductAcceptor s, CEventList e, String n, double m)
	{
		status='i';
		queue=q;
		sink=s;
		eventlist=e;
		name=n;
		meanProcTime=m;
		queue.askProduct(this);
	}

	/**
	 *	Constructor Regular Register
	 *        Service times are exponentially distributed with specified mean
	 *	@param q	Queue from which the machine has to take products
	 *	@param s	Where to send the completed products
	 *	@param e	Eventlist that will manage events
	 *	@param n	The name of the machine
	 *  @param m	Mean processing time
	 * @param sd   Standard Deviation
	 * @param min Minimum processing time
	 */
	public Machine(Queue q, ProductAcceptor s, CEventList e, String n, double m, double sd, double min)
	{
		status='i';
		queue=q;
		sink=s;
		eventlist=e;
		name=n;
		meanProcTime=m;
		sdReg=sd;
		minReg=min;
		queue.askProduct(this);
	}

	/**
	 *	Constructor
	 *        Service times are exponentially distributed with specified mean
	 *	@param q	Queues from which the machine has to take products
	 *	@param s	Where to send the completed products
	 *	@param e	Eventlist that will manage events
	 *	@param n	The name of the machine
	 *  @param m1	Mean processing time
	 * 	@param sd1   Standard Deviation
	 *  @param min1 Minimum processing time
	 */
	public Machine(Queue[] q, ProductAcceptor s, CEventList e, String n, double m1, double sd1, double min1, double m2, double sd2, double min2)
	{
		status='i';
		queueList=q;
		sink=s;
		eventlist=e;
		name=n;
		meanProcTime=m1;
		sdReg=sd1;
		minReg=min1;
		meanProcTimeSD=m2;
		sdSD=sd2;
		minSD=min2;
		// I've honestly just randomly chosen this; doesn't matter much at construction time
		queueList[0].askProduct(this);
	}
	
	/**
	*	Constructor
	*        Service times are pre-specified
	*	@param q	Queue from which the machine has to take products
	*	@param s	Where to send the completed products
	*	@param e	Eventlist that will manage events
	*	@param n	The name of the machine
	*        @param st	service times
	*/
	public Machine(Queue q, ProductAcceptor s, CEventList e, String n, double[] st)
	{
		status='i';
		queue=q;
		sink=s;
		eventlist=e;
		name=n;
		meanProcTime=-1;
		processingTimes=st;
		procCnt=0;
		queue.askProduct(this);
	}

	/**
	*	Method to have this object execute an event
	*	@param type	The type of the event that has to be executed
	*	@param tme	The current time
	*/
	public void execute(int type, double tme)
	{
		// show arrival
		System.out.println("Product finished at time = " + tme);
		// Remove product from system
		product.stamp(tme,"Production complete",name);
		sink.giveProduct(product);
		product=null;
		// set machine status to idle
		status='i';
		// Ask the queue for products
		// Give priority to service desk queue
		if (queueList != null) {
			if (!queueList[1].askProduct(this)) {
				queueList[0].askProduct(this);
			}
		}
		else {
			queue.askProduct(this);
		}
	}
	
	/**
	*	Let the machine accept a product and let it start handling it
	*	@param p	The product that is offered
	*	@return	true if the product is accepted and started, false in all other cases
	*/
        @Override
	public boolean giveProduct(Product p)
	{
		// Only accept something if the machine is idle
		if(status=='i')
		{
			// accept the product
			product=p;
			// mark starting time
			product.stamp(eventlist.getTime(),"Production started",name);
			// start production
			startProduction(p);
			// Flag that the product has arrived
			return true;
		}
		// Flag that the product has been rejected
		else return false;
	}
	
	/**
	*	Starting routine for the production
	*	Start the handling of the current product with an exponentionally distributed processingtime with average 30
	*	This time is placed in the eventlist
	*/
	private void startProduction(Product p)
	{
		// generate duration
		if(meanProcTime>0) {
			double duration;
			if (p.getType() == 0) {
				duration = drawRandomNormal(meanProcTime, sdReg);
			}
			else {
				duration = drawRandomNormal(meanProcTimeSD, sdSD);
			}
			if (duration < minReg) {
				duration = minSD;
			}
			// Create a new event in the eventlist
			double tme = eventlist.getTime();
			eventlist.add(this,p.getType(),tme+duration); //target,type,time
			// set status to busy
			status='b';
		}
		else
		{
			if(processingTimes.length>procCnt)
			{
				eventlist.add(this,0,eventlist.getTime()+processingTimes[procCnt]); //target,type,time
				// set status to busy
				status='b';
				procCnt++;
			}
			else
			{
				eventlist.stop();
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

	public static double drawRandomNormal(double mean, double sd)
	{
		//TODO
		double res = 1.0;
		return res;
	}

}