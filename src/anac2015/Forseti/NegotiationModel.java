package anac2015.Forseti;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import negotiator.Bid;
import negotiator.issue.ISSUETYPE;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.EvaluatorInteger;
import negotiator.utility.EvaluatorReal;
import negotiator.utility.UtilitySpace;

public class NegotiationModel {
	private List<AbstractNegotiationParty> opponents = new ArrayList<AbstractNegotiationParty>();
	private UtilitySpace baseUtilSpace; // A standardized utility space with all
										// weights and utilities equal.
	private UtilitySpace myUtilSpace;
	private HashMap<AbstractNegotiationParty, OpponentModel> opponentModels = new HashMap<AbstractNegotiationParty, OpponentModel>(); // One
																																		// model
																																		// per
																																		// opponent.

	public NegotiationModel(UtilitySpace us) {
		myUtilSpace = us;
		baseUtilSpace = resetUtilitySpace(new UtilitySpace(us));
	}

	// Add a new opponent to the model, using the standardized utility space.
	public void addOpponent(AbstractNegotiationParty opponent) {
		opponents.add(opponent);
		opponentModels.put(opponent, new OpponentModel(baseUtilSpace));
	}

	// Standardizes the provided utility space.
	private UtilitySpace resetUtilitySpace(UtilitySpace util) {

		int numberOfObjectives = util.getNrOfEvaluators();

		// Iterates over all issues and resets the weights and utilities.
		for (int i = 0; i < numberOfObjectives; i++) {
			Objective obj = util.getDomain().getObjective(i);
			util.setWeight(obj, 1.0 / ((double) numberOfObjectives));
			ISSUETYPE type = obj.getType();

			// One case for each possible issue type:
			switch (type) {
			case DISCRETE:
				EvaluatorDiscrete ed = (EvaluatorDiscrete) util.getEvaluator(i);
				Object[] valuesD = ed.getValues().toArray();
				for (int j = 0; j < ed.getValues().size(); j++) {
					try {
						ed.setEvaluation((Value) valuesD[j], 1);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				util.addEvaluator(obj, ed);
				break;

			case INTEGER:
				EvaluatorInteger ei = (EvaluatorInteger) util.getEvaluator(i);
				ei.setSlope(1.0 / (ei.getUpperBound() - ei.getLowerBound()));
				ei.setOffset(0.0);
				util.addEvaluator(obj, ei);
				break;

			case REAL:
				EvaluatorReal er = (EvaluatorReal) util.getEvaluator(i);
				er.setLinearParam(1 / (er.getUpperBound() - er.getLowerBound()));
				util.addEvaluator(obj, er);
				break;

			default:
				break;
			}
		}

		return util;
	}

	public void addBid(AbstractNegotiationParty opponent, Bid bid) {
		opponentModels.get(opponent).addBid(bid);
	}

	// Returns the social welfare of a bid, according to the opponent models.
	public double getEstimatedSocialWelfare(Bid bid) {
		double socialWelfare = 0;
		try {
			socialWelfare = socialWelfare + myUtilSpace.getUtility(bid);
		} catch (Exception e) {
			socialWelfare = 0;
		}
		for (AbstractNegotiationParty o : opponents) {
			socialWelfare = socialWelfare
					+ opponentModels.get(o).getUtility(bid);
		}
		return socialWelfare;
	}

	// Returns true if the new bid is worse than the last bid for all
	// participants.
	public boolean isUnfortunateStep(Bid newBid, Bid lastBid) {
		boolean unfortunate = true;
		for (AbstractNegotiationParty o : opponents) {
			OpponentModel model = opponentModels.get(o);
			if (model.getUtility(newBid) > model.getUtility(lastBid))
				unfortunate = false;
		}
		return unfortunate;
	}
}
