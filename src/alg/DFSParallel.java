package alg;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import alg.cost.CostFunction;
import io.ScheduleEvent;
import io.ScheduleListener;
import util.PartialScheduleGrph;
import util.ScheduleGrph;
import util.StaticUtils;

/**
 * This is an entry point for the DFS parallelized version. Invokes thread pool
 * on the DFSTask objects.
 * 
 * @author Matt Frost
 *
 */
public class DFSParallel implements Algorithm {

	private final CostFunction _cost;
	private final ScheduleGrph _input;
	private final int _numProcessors;
	private AtomicLong _lowerBound = new AtomicLong();
	private PartialScheduleGrph _bestState;
	private ForkJoinPool forkJoinPool;
	private HashSet<String> _closed;
	private ScheduleListener _listen;
	private boolean _vis;
	private AtomicLong _iterations = new AtomicLong();

	private PartialScheduleGrph _start = new PartialScheduleGrph(0);

	/**
	 * 
	 * @param input
	 * @param cost
	 * @param numProcessors
	 * @param numCores
	 */
	public DFSParallel(ScheduleGrph input, CostFunction cost, int numProcessors, int numCores) {
		this.forkJoinPool = new ForkJoinPool(numCores);
		this._cost = cost;
		this._input = input;
		this._numProcessors = numProcessors;
		this._lowerBound.set(Long.MAX_VALUE);
		this._bestState = new PartialScheduleGrph(0);
		this._bestState.addVertices(input.getVertices());
		this._bestState.setVertexWeightProperty(input.getVertexWeightProperty());
		this._bestState.setVerticesLabel(input.getVertexLabelProperty());

		this._closed = new HashSet<String>();
		_vis = false;

	}

	/**
	 * 
	 * @param input
	 * @param cost
	 * @param numProcessors
	 * @param numCores
	 * @param listen
	 */
	public DFSParallel(ScheduleGrph input, CostFunction cost, int numProcessors, int numCores,
			ScheduleListener listen) {
		this(input, cost, numProcessors, numCores);
		this._listen = listen;
		_vis = true;
	}

	/**
	 * 
	 * @param finished
	 */
	private void getSetupOutput(PartialScheduleGrph finished) {
		finished.setEdgeWeightProperty(_input.getEdgeWeightProperty());
		for (int edge : _input.getEdges()) {
			int head = _input.getDirectedSimpleEdgeHead(edge);
			int tail = _input.getTheOtherVertex(edge, head);
			finished.addDirectedSimpleEdge(tail, head);
		}
	}

	/**
	 * 
	 */
	public PartialScheduleGrph runAlg() {
		ScheduledExecutorService executor = null;
		if (_vis) {
			executor = Executors.newScheduledThreadPool(1);
			executor.scheduleAtFixedRate(statsRunnable, 0, 200, TimeUnit.MILLISECONDS);
		}

		_bestState.setVerticesLabel(_input.getVertexLabelProperty());
		if (_vis) {
			forkJoinPool.invoke(
					new DFSTask(_input, _start, _bestState, _cost, _closed, _numProcessors, -1, _lowerBound, _iterations,_listen));
		}
		else {
			forkJoinPool.invoke(
					new DFSTask(_input, _start, _bestState, _cost, _closed, _numProcessors, -1, _lowerBound, _iterations));
		}
		
		getSetupOutput(_bestState);
		
		if (_vis) {
			executor.execute(statsRunnable);
			executor.shutdown();
		}

		return _bestState;
	}

	/**
	 * 
	 */
	Runnable statsRunnable = new Runnable() {
		public void run() {

			_listen.update(new ScheduleEvent(ScheduleEvent.EventType.NewState), _iterations.intValue(),
					StaticUtils.getUsedMemory());

		}
	};
	
	
}
