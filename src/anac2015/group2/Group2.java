package anac2015.group2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import negotiator.Bid;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group2 extends AbstractNegotiationParty {
	Map<Object, G2OpponentModel> opponentModels;
	G2Bid _previousBid;
	G2UtilitySpace ourUtilitySpace;
	G2Logger logger = new G2Logger();
	
	G2CSVLogger csvLogger_real;
	G2CSVLogger csvLogger_model;
	double proposedUtility = 0;
	
	boolean loggingOn = false;
	
	int partyNumber = 1;
	int round = 0;
	static final int MAX_ROUNDS = 180;
	final double reservationValue;
	
	/**
	 * Please keep this constructor. This is called by genius.
	 *
	 * @param utilitySpace Your utility space.
	 * @param deadlines The deadlines set for this negotiation.
	 * @param timeline Value counting from 0 (start) to 1 (end).
	 * @param randomSeed If you use any randomization, use this seed for it.
	 */
	public Group2(UtilitySpace utilitySpace,
				  Map<DeadlineType, Object> deadlines,
				  Timeline timeline,
				  long randomSeed) {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);
		
		opponentModels = new HashMap<Object, G2OpponentModel>();
		ourUtilitySpace = new G2UtilitySpace(utilitySpace);
		reservationValue = utilitySpace.getReservationValueUndiscounted();
		if(loggingOn){
			logger.log("Our utilityspace");
			logger.log(this.ourUtilitySpace.allDataString());
			csvLogger_real = new G2CSVLogger();
			csvLogger_model = new G2CSVLogger();
		}
		
	}
	
	/**
	 * Calculates maximum utility bid and returns it as an Action
	 * @return The maximum utility Action
	 */
	private Action calcMaxUtilityAction() {
		// How hard can it be to take the maximum option for each issue??
		// seems a bit absurd to accept an offer in case of an exception... 
		try {
			Bid bid = getUtilitySpace().getMaxUtilityBid();
			_previousBid = new G2Bid(bid);
			logBid(_previousBid, false);
			return new Offer(bid);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.out.println("Error: Our UtilitySpace is empty.");
			return new Accept();
		}
	}
	
	private double calcMinUtility() {
		double time = getTimeLine().getTime();
		if(round == 0)
			return 1;
		double roundTime = time/round;
		//System.out.println(round + ">"+roundTime);
		if(roundTime > (1.0/20))
			return 1 - (1-reservationValue) * time * time;
		else if (time > (1-roundTime*20)){
			double newTime = (time - (1-roundTime*20)) / (roundTime*20);
			//System.out.println("newTime:" + newTime);
			return 1 - (1-reservationValue) * newTime * newTime;
		}
		else {
			return 0.95;
		}
	}
	
	/**
	 * Calculates next Action based on the previous bid
	 * @return the next Action of the agent
	 */
	private Action calcNextAction() {
		ArrayList<G2UtilitySpace> opponentUtilitySpaces = new ArrayList<G2UtilitySpace>();
		for(G2OpponentModel opponent: opponentModels.values()){
			opponentUtilitySpaces.add(opponent.getUtilitySpace());
		}
		G2ParetoFinder paretoFinder = new G2ParetoFinder(ourUtilitySpace, opponentUtilitySpaces);
		
		ArrayList<G2Bid> bids = paretoFinder.findParetoOptimalBids();
		
		double minUtility = calcMinUtility();
		
		//Random bid picking algorithm
		int i=0;
		while(i < bids.size()-1 && ourUtilitySpace.calculateUtility(bids.get(i))< minUtility)
			i++;
		Random rand = new Random();
	    int randomNum = rand.nextInt((bids.size() - 1 - i) + 1) + i;
	    G2Bid nextBid = bids.get(randomNum);
	    //System.out.println("size:" + bids.size() + ", i:" + i + ", rand:" + randomNum);

		
		// Generate the Bid from the G2Bid
		Bid returnBid = null;
		try {
			returnBid = nextBid.convertToBid(getUtilitySpace().getDomain(), getUtilitySpace().getEvaluators());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Accept if the proposedUtility is higher than our minimum or if the Bid was incorrectly generated 
		if (returnBid == null || calcMinUtility() < proposedUtility) {
			logBid(nextBid, true);

			return new Accept();
		}
		else { //Else make the Bid we just calculated
			logBid(nextBid, false);
		
			return new Offer(returnBid);
		}
	}
	public void logBid(G2Bid bid, boolean accept){
		
		ArrayList<Double> utilities = new ArrayList<Double>();
		for(G2OpponentModel opponentModel : opponentModels.values()){
			if(accept){
				utilities.add(-1.0);
			}else{
				utilities.add(opponentModel.getUtility(bid));
			}
		}
		if(opponentModels.size() == 0){
			utilities.add(0.0);
		}
		if(loggingOn){
			csvLogger_model.log(utilities);
			csvLogger_real.log(ourUtilitySpace.calculateUtility(bid));
		}
	}
	/**
	 * Each round this method gets called and ask you to accept or offer. The first party in
	 * the first round is a bit different, it can only propose an offer.
	 *
	 * @param validActions Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class> validActions) {
		//Initialize the logger with your partynumber once it is known
		if (!logger.isInitialized() && loggingOn) {
			logger.init(partyNumber);
			csvLogger_real.init(partyNumber, "real");
			csvLogger_model.init(partyNumber, "model");
		}
		
		//Increase round timer
		round++;
		

		//In the first round return the bid with maximum utility for ourself
		if(_previousBid == null){
			return calcMaxUtilityAction();
		//Else make a bid based on out previous bid
		}else{
			return calcNextAction();
		}
	}


	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict their utility.
	 *
	 * @param sender The party that did the action.
	 * @param action The action that party did.
	 */
	@Override
	public void receiveMessage(Object sender, Action action) {
		if(round == 0) {
			partyNumber++;
		}
		// don't update opponent when action is accept, and thus contains no bid
		if(Action.getBidFromAction(action) != null){
			// If the map does not contain a UtilitySpace for this opponent then make a new one 		
			if (!opponentModels.containsKey(sender))
			{
				opponentModels.put(sender, new G2OpponentModel(getUtilitySpace()));
			}
			G2Bid bid = new G2Bid(Action.getBidFromAction(action));
			opponentModels.get(sender).updateModel(bid);
			
			proposedUtility = ourUtilitySpace.calculateUtility(bid);
			if(loggingOn){
				csvLogger_real.log(proposedUtility);
				logger.log("received bid from: " + sender);
				logger.log(bid.toString());
				logger.log("updated opponentModel");
				logger.log(opponentModels.get(sender).getUtilitySpaceString());
			}
		}else{
			if(loggingOn){
				csvLogger_real.log(-1.0);
			}
		}
	}
	static public List<G2Bid> getAlternativeBids(List<G2Bid> bids, Set<Map.Entry<String, G2Issue>> issues){
		List<G2Bid> alternativeBids = new ArrayList<G2Bid>(bids);
		for(G2Bid bid:bids){
			alternativeBids.addAll(getAlternativeBids(bid,issues));
		}
		return alternativeBids;
	}
	static public List<G2Bid> getAlternativeBids(G2Bid bid, Set<Map.Entry<String, G2Issue>> issues){
		
		List<G2Bid> bids = new ArrayList<G2Bid>();
        Map<String, String> originalOptions = bid.getChoices();
        for(Map.Entry<String,G2Issue> entry:issues){
        	//get all alternative options for current issue
        	Set<String> otherOptions = entry.getValue().getOtherOptions(bid.getChoice(entry.getKey()));
        	for(String option:otherOptions){
        		//create a new bid, equal to the original one
        		G2Bid newBid = new G2Bid(originalOptions);
        		//set one of the issue to an alternative option
        		newBid.setChoice(entry.getKey(), option);
        		//save it in the array
        		bids.add(newBid);
        	}
        }
        
		return bids;
	}

}
