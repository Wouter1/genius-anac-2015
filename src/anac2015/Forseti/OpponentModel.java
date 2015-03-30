package anac2015.Forseti;

import java.util.ArrayList;
import java.util.List;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.EvaluatorInteger;
import negotiator.utility.EvaluatorReal;
import negotiator.utility.UtilitySpace;

public class OpponentModel {
	private List<Bid> bids;
	private int round;
	private double weightLearningFactor; // The factor that decides how fast the weights will be updated.
	private UtilitySpace utilitySpace; //The estimate of the agent's utility space.
	
	public OpponentModel(UtilitySpace utilitySpace) {
		bids = new ArrayList<Bid>();
		round = 0;
		weightLearningFactor = 0.2;
		this.utilitySpace = utilitySpace;
	}
	
	public void addBid(Bid bid) {
		round++;
		bids.add(bid);
		updateUtilitySpace(bid);
	}
	
	// Updates the utility space using the new bid.
	// Utilities for all the bid's chosen alternatives are increased.
	// Weights for all issues with unchanged choices are increased.
	public void updateUtilitySpace(Bid bid) {
		List<Issue> issues = bid.getIssues();
		int changeMagnitude = 1; // Decides how much the utility of each chosen alternative will increase.
		double bidUtility = 0;
		try {
			bidUtility = utilitySpace.getUtility(bid);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// If the bid has a lower estimated utility than 0.5, the model is obviously some way off.
		// In that case, increase the magnitude of the change for this update.
		if (bidUtility < 0.5) {
			changeMagnitude = 5;
		}
		
		// Iterates over all issues and updates them.
		for (int i = 1; i < issues.size()+1; i++) {
			Evaluator eva = utilitySpace.getEvaluator(i);
			Evaluator evaNew = utilitySpace.getEvaluator(i);
			switch (eva.getType()) {
			
			case DISCRETE:
				ValueDiscrete vald = null;
				evaNew = null;
				try {
					vald = (ValueDiscrete) bid.getValue(i);
					evaNew = updateDiscreteEvaluator((EvaluatorDiscrete)eva,vald,changeMagnitude);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
				
			case INTEGER:
				ValueInteger vali = null;
				try {
					vali = (ValueInteger) bid.getValue(i);
				} catch (Exception e) {
					e.printStackTrace();
				}
				evaNew = updateIntegerEvaluator((EvaluatorInteger)eva,vali);
				break;
				
			case REAL:
				ValueReal valr = null;
				try {
					valr = (ValueReal) bid.getValue(i);
				} catch (Exception e) {
					e.printStackTrace();
				}
				evaNew = updateRealEvaluator((EvaluatorReal)eva,valr);
				break;
				
			default:
				break;
			}
			
			utilitySpace.addEvaluator(issues.get(i-1), evaNew);
		}
		
		// Update the weights.
		try {
			updateWeights(bid);
		} catch (Exception e) {
			//Do nothing.
		}
	}
	
	private Evaluator updateDiscreteEvaluator (EvaluatorDiscrete eva, ValueDiscrete val,int magnitude) throws Exception {
		eva.setEvaluation(val, (int)(eva.getValue(val)+magnitude));
		return eva;
	}
	
	private Evaluator updateIntegerEvaluator (EvaluatorInteger eva, ValueInteger val) {
		int middle = (eva.getUpperBound() - eva.getLowerBound())/2;
		if (val.getValue() > middle) {
			eva.setSlope(-1.0/(eva.getUpperBound() - eva.getLowerBound()));
		}
		else {
			eva.setSlope(1.0/(eva.getUpperBound() - eva.getLowerBound()));
		}
		return eva;
	}
	
	private Evaluator updateRealEvaluator (EvaluatorReal eva, ValueReal val) {
		double middle = (eva.getUpperBound() - eva.getLowerBound())/2;
		if (val.getValue() > middle) {
			eva.setLinearParam(1.0);
		}
		else {
			eva.setLinearParam(-1.0);
		}
		return eva;
	}
	
	private void updateWeights(Bid bid) throws Exception {
		List<Issue> issues = bid.getIssues();
		
		for (int i = 1; i < issues.size() + 1;i++) {
			if (round > 1 && bid.getValue(i).equals(bids.get(round-2).getValue(i))) {
				utilitySpace.setWeight(bid.getIssues().get(i-1), utilitySpace.getWeight(i) + weightLearningFactor);
			}
		}
	}
	
	public List<Bid> getBids() {
		return bids;
	}
	
	public double getUtility(Bid bid) {
		try {
			return utilitySpace.getUtility(bid);
		} catch (Exception e) {
			return 0;
		}
	}
	
	public Bid getMaxUtilityBid () {
		Bid maxBid=null;
		try {
			maxBid = utilitySpace.getMaxUtilityBid();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return maxBid;
	}
}
 