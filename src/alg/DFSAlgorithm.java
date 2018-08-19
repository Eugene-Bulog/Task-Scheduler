package alg;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import alg.cost.CostFunction;
import io.ScheduleEvent;
import io.ScheduleListener;
import util.PartialScheduleGrph;
import util.ScheduleGrph;
import util.StaticUtils;

public class DFSAlgorithm implements Algorithm {

	private final CostFunction _cost;
	private final ScheduleGrph _input;
	private final int _numProcessors;
	private int _lowerBound;
	private PartialScheduleGrph _bestState;
	private HashSet<String> _closed = new HashSet<String>();
	
	private ScheduleListener _listen;
	private int _iterations = 0;
	
	public DFSAlgorithm(ScheduleGrph input, CostFunction cost, int numProcessors, ScheduleListener listen) {
		this._cost = cost;
		this._input = input;
		this._numProcessors = numProcessors;
		this._listen = listen;
	}

	private void getSetupOutput(PartialScheduleGrph finished) {
		finished.setEdgeWeightProperty(_input.getEdgeWeightProperty());
		for (int edge : _input.getEdges()) {
			int head = _input.getDirectedSimpleEdgeHead(edge);
			int tail = _input.getTheOtherVertex(edge, head);
			finished.addDirectedSimpleEdge(tail, head);
		}
	}


	public PartialScheduleGrph runAlg() {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(guiRunnable, 0, 1, TimeUnit.SECONDS);
		
		_lowerBound = Integer.MAX_VALUE;

		_bestState = new PartialScheduleGrph(0);
		_bestState.setVerticesLabel(_input.getVertexLabelProperty());

		recursiveSolve(_bestState, -1);

		getSetupOutput(_bestState);
		executor.shutdown();
		return _bestState;

	}

	private void recursiveSolve(PartialScheduleGrph p, int lastAdded) {
		_iterations++;
		if (p.getScore() >= _lowerBound
				|| _closed.contains(p.getNormalizedCopy(_numProcessors).serialize().getSerialString())
				|| (lastAdded != -1 && p.equivalenceCheck(_input, lastAdded, _numProcessors))) {
			return;
		}
		
		if (p.getVertices().size() == _input.getVertices().size()) {

			updateCurrentBest(p);
			return;
		}
		
		_closed.add(p.getNormalizedCopy(_numProcessors).serialize().getSerialString());
		
		for (int freeTask : p.getFree(_input)) {
			for (int proc = 1; proc <= this._numProcessors; proc++) {

				PartialScheduleGrph next = p.copy();
				next.setVerticesLabel(_bestState.getVertexLabelProperty());
				next.addFreeTask(_input, freeTask, proc);
				_cost.applyCost(next, freeTask, _numProcessors);
				recursiveSolve(next, freeTask);

			}
		}
	}

	private void updateCurrentBest(PartialScheduleGrph s) {

		int underestimate = s.getScore();

		if (underestimate < _lowerBound) {
			_lowerBound = underestimate;
			_bestState = s;
		}
	}
	
	Runnable guiRunnable = new Runnable() {
		public void run() {
			_listen.updateGraph(new ScheduleEvent(ScheduleEvent.EventType.NewState), _iterations,_bestState);
			_listen.update(new ScheduleEvent(ScheduleEvent.EventType.NewState), _iterations, StaticUtils.getUsedMemory());
		}
	};

}
