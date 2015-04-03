package anac2015.Forseti;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import negotiator.Bid;
import negotiator.Deadline;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.EVALUATORTYPE;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.EvaluatorInteger;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Forseti extends AbstractNegotiationParty {

	private Bid lastBid = null; // Last bid made by anyone.
	private Bid myLastBid = null; // Last bid made by this agent.
	private AbstractNegotiationParty currentAgent = null; // The agent currently
															// bidding/accepting
	private NegotiationModel model = new NegotiationModel(getUtilitySpace()); // Our
																				// model
																				// of
																				// the
																				// negotiation.
	private List<AbstractNegotiationParty> opponents = new ArrayList<AbstractNegotiationParty>();
	private boolean opponentsListFull = false;
	private List<Bid> goodBids = new ArrayList<Bid>(); // List of bids with
														// utility above our
														// utility cutoff value
	private Bid bestSocialBid; // The bid that maximizes the Nash product,
								// according to the current model.
	private int round = 1;
	private double timeFactor = 0.05; // Used for discounting the reservation
										// value
	private Random rng;
	private int noOfPossibleBids;
	private double utilityCutoff = 0.8;

	/**
	 * Please keep this constructor. This is called by genius.
	 *
	 * @param utilitySpace
	 *            Your utility space.
	 * @param deadlines
	 *            The deadlines set for this negotiation.
	 * @param timeline
	 *            Value counting from 0 (start) to 1 (end).
	 * @param randomSeed
	 *            If you use any randomization, use this seed for it.
	 */
	public Forseti(UtilitySpace utilitySpace, Deadline deadlines,
			Timeline timeline, long randomSeed) {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);
		rng = new Random(randomSeed);

		// If the domain is discounted, the time factor is increased.
		if (utilitySpace.isDiscounted())
			timeFactor = 0.2 / Math.pow(utilitySpace.getDiscountFactor(), 2.5);

		// Calculating the total number of possible bids.
		noOfPossibleBids = 1;
		for (int i = 0; i < utilitySpace.getNrOfEvaluators(); i++) {
			Evaluator eva = utilitySpace.getEvaluator(i + 1);
			if (eva.getType() == EVALUATORTYPE.DISCRETE) {
				noOfPossibleBids = noOfPossibleBids
						* ((EvaluatorDiscrete) eva).getValues().size();
			} else if (eva.getType() == EVALUATORTYPE.INTEGER) {
				EvaluatorInteger evai = (EvaluatorInteger) eva;
				noOfPossibleBids = noOfPossibleBids
						* (evai.getUpperBound() - evai.getLowerBound());
			}
		}
		findGoodBids(utilityCutoff);
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions
	 *            Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class> validActions) {
		// Method is called every time it's this agent's turn.
		currentAgent = this;

		// First offer is always the maximum utility offer.
		if (!validActions.contains(Accept.class) || round == 1) {
			try {
				round++;
				return new Offer(utilitySpace.getMaxUtilityBid());
			} catch (Exception e) {
				// Do nothing.
			}
		}

		// If the last bid is good enough, accept it.
		double utilityOfLastBid = utilitySpace.getUtilityWithDiscount(lastBid,
				timeline);
		if (utilityOfLastBid >= getReservationValue()) {
			return new Accept();
		}

		// In the next phase, the agent tries different bids to build the model.
		if (round <= 0.2 * goodBids.size()) {
			round++;
			return new Offer(goodBids.get(round));
		}

		// For the rest of the negotiation: Offer the bid with the highest
		// estimated Nash product.
		// However, don't offer the same bid two times in a row. Also don't
		// offer a bid that is
		// unfortunate (worse for all participants than the last bid) according
		// to the model.
		else {
			updateBestSocialBid();
			Bid newBid = bestSocialBid;
			if (newBid == myLastBid) {
				while (!model.isUnfortunateStep(newBid, lastBid)) {
					newBid = getRandomGoodBid();
				}
			}
			lastBid = newBid;
			myLastBid = newBid;
			return new Offer(newBid);
		}
	}

	// Finds (almost) all bids with utility above the cutoff and stores the in
	// the goodBids list.
	// The size of the list should be within 5% to 20% of the total number of
	// bids; if this is not the case,
	// it is built again with an adjusted cutoff value.
	private void findGoodBids(double cutoff) {
		goodBids.clear();

		Set<Bid> allBids = new HashSet<Bid>();
		for (int i = 0; i < 10 * noOfPossibleBids; i++) {
			allBids.add(generateRandomBid());
		}
		// Makes sure that the bid with max utility is included.
		try {
			allBids.add(utilitySpace.getMaxUtilityBid());
		} catch (Exception e) {
			// Do nothing.
		}

		// All bids with utility larger than the cutoff is added to the list.
		for (Bid b : allBids) {
			double util = 0;
			try {
				util = utilitySpace.getUtility(b);
			} catch (Exception e) {
				// Do nothing.
			}
			if (util >= cutoff && !goodBids.contains(b)) {
				goodBids.add(b);
			}
		}

		double cutoffAdjustment = 0.05;

		// If the total number of bids is relatively low, we need a finer
		// adjustment
		if (noOfPossibleBids <= 200) {
			cutoffAdjustment = 0.02;
		} else if (noOfPossibleBids <= 100) {
			cutoffAdjustment = 0.01;
		}

		// If the size of the list is not within 5% to 20% of the total number
		// of bids,
		// the method is run again with an adjusted cutoff.
		if (goodBids.size() >= 0.2 * noOfPossibleBids) {
			findGoodBids(cutoff + cutoffAdjustment);
		} else if (goodBids.size() < 0.05 * noOfPossibleBids) {
			findGoodBids(cutoff - cutoffAdjustment);
		}

	}

	// Returns a random bid from the goodBids list.
	private Bid getRandomGoodBid() {
		int i = rng.nextInt(goodBids.size());
		return goodBids.get(i);
	}

	// Finds the bid with highest estimated social welfare and stores it in the
	// bestSocialBid variable.
	private void updateBestSocialBid() {
		double bestSocialWelfare;
		bestSocialWelfare = model.getEstimatedSocialWelfare(bestSocialBid);
		for (Bid b : goodBids) {
			if (model.getEstimatedSocialWelfare(b) > bestSocialWelfare) {
				bestSocialBid = b;
				bestSocialWelfare = model.getEstimatedSocialWelfare(b);
			}
		}
	}

	// Returns the reservation value, discounted by time.
	private double getReservationValue() {
		return utilityCutoff - utilityCutoff
				* Math.pow((timeline.getTime()), (1 / timeFactor));
	}

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 *
	 * @param sender
	 *            The party that did the action.
	 * @param action
	 *            The action that party did.
	 */

	@Override
	public void receiveMessage(Object sender, Action action) {
		super.receiveMessage(sender, action);

		boolean noSender = false;
		try {
			currentAgent = (AbstractNegotiationParty) sender;
		} catch (Exception e1) {
			noSender = true;
		}

		// In the first round, add the opponents to the model.
		if (!opponentsListFull && !noSender) {
			opponents.add(currentAgent);
			model.addOpponent(currentAgent);
			if (opponents.size() >= getNumberOfParties() - 1) {
				opponentsListFull = true;
			}
		}

		// Add the bid to the model. If the message is an accept, add the
		// accepted bid.
		if (!noSender) {
			if (action instanceof Offer) {
				Bid bid = ((Offer) action).getBid();
				lastBid = bid;
			} else if (action instanceof Accept) {
				// The accepted bid is used.
			}
			model.addBid(currentAgent, lastBid);
		}
	}
}
