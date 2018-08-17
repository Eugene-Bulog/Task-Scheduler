package alg;

import alg.cost.CostFunction;
import util.PartialScheduleGrph;
import util.ScheduleGrph;

public class DFSAlgorithm implements Algorithm {

	private final CostFunction _cost;
	private final ScheduleGrph _input;
	private final int _numProcessors;
	private int _lowerBound;
	private int _upperBound;
	private PartialScheduleGrph _bestState;

	public DFSAlgorithm(ScheduleGrph input, CostFunction cost, int numProcessors) {
		this._cost = cost;
		this._input = input;
		this._numProcessors = numProcessors;
	}

	private void getSetupOutput(PartialScheduleGrph finished) {
		finished.setEdgeWeightProperty(_input.getEdgeWeightProperty());
		for (int edge : _input.getEdges()) {
			int head = _input.getDirectedSimpleEdgeHead(edge);
			int tail = _input.getTheOtherVertex(edge, head);
			finished.addDirectedSimpleEdge(tail, head);
		}
	}

	@Override
	public PartialScheduleGrph runAlg() {

		_lowerBound = Integer.MAX_VALUE;

		PartialScheduleGrph initial = new PartialScheduleGrph(0);
		initial.setVerticesLabel(_input.getVertexLabelProperty());

		recursiveSolve(initial);

		getSetupOutput(_bestState);
		return _bestState;

	}

	private void recursiveSolve(PartialScheduleGrph p) {

		for (int freeTask : p.getFree(_input)) {
			for (int proc = 1; proc <= this._numProcessors; proc++) {
				PartialScheduleGrph next = p.copy();
				next.addFreeTask(_input, freeTask, proc);
				_cost.applyCost(next, freeTask, _numProcessors);

				if (next.getScore() >= _lowerBound) {
					return;
				}

				if (next.getVertices().size() == _input.getVertices().size()) {
					updateCurrentBest(next);
					return;

				}

				recursiveSolve(next);

			}
		}
	}

	private void updateCurrentBest(PartialScheduleGrph s) {
		int underestimate = s.getScore();
		if (underestimate < _lowerBound) {
			log.info(s.getScore());
			_lowerBound = underestimate;
			_bestState = s;
		}
	}

}
