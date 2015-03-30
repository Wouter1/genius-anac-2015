package anac2015.agentBuyogV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.bidding.BidDetails;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.Objective;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.ValueInteger;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.EvaluatorInteger;
import negotiator.utility.UtilitySpace;

public class OpponentInfo {

	private String agentID;
	private BidHistory agentBidHistory, bestBids;
	private UtilitySpace utilitySpace;
	private Double leniency, domainCompetitiveness, agentDifficulty;
	private Bid bestBid;
	private List<Integer> bidPointWeights;
	
	public OpponentInfo(String agentID, UtilitySpace utilitySpace){
		this.agentID = agentID;
		this.agentBidHistory = new BidHistory();
		this.bestBids = new BidHistory();
		this.domainCompetitiveness = null;
		this.leniency = null;
		this.utilitySpace = new UtilitySpace(utilitySpace);
		this.bidPointWeights = new ArrayList<Integer>();
		this.agentDifficulty = null;
		
		initializeOpponentUtilitySpace();
		
	}
	
	private void initializeOpponentUtilitySpace() {
		int numberOfIssues = utilitySpace.getDomain().getIssues().size();
		double commonWeight = 1D/numberOfIssues;
		//An evaluator for an issue contains a list of issue value evaluations as well as the weight of that issue.
		//Each issue has one evaluator.
		for(Entry<Objective, Evaluator> issueEvaluatorEntry : utilitySpace.getEvaluators()){
			utilitySpace.unlock(issueEvaluatorEntry.getKey());
			issueEvaluatorEntry.getValue().setWeight(commonWeight);
			
			if(issueEvaluatorEntry.getKey() instanceof IssueDiscrete){
				for(ValueDiscrete value: ((IssueDiscrete) issueEvaluatorEntry.getKey()).getValues()){
					try {
						((EvaluatorDiscrete) issueEvaluatorEntry.getValue()).setEvaluation(value, 1);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else if(issueEvaluatorEntry.getKey() instanceof IssueInteger){				
				((EvaluatorInteger) issueEvaluatorEntry.getValue()).setLinearFunction(0.5, 0.5);
			}
		}
		
	}

	/**
	 * @return the agentID
	 */
	public String getAgentID() {
		return agentID;
	}
	/**
	 * @param agentID the agentID to set
	 */
	public void setAgentID(String agentID) {
		this.agentID = agentID;
	}
	/**
	 * @return the agentBidHistory
	 */
	public BidHistory getAgentBidHistory() {
		return agentBidHistory;
	}
	/**
	 * @param agentBidHistory the agentBidHistory to set
	 */
	public void setAgentBidHistory(BidHistory agentBidHistory) {
		this.agentBidHistory = agentBidHistory;
	}
	/**
	 * @return the bestBids
	 */
	public BidHistory getBestBids() {
		return bestBids;
	}
	/**
	 * @param bestBids the bestBids to set
	 */
	public void setBestBids(BidHistory bestBids) {
		this.bestBids = bestBids;
	}
	/**
	 * @return the opponentUtilitySpace
	 */
	public UtilitySpace getOpponentUtilitySpace() {
		return utilitySpace;
	}
	/**
	 * @param opponentUtilitySpace the opponentUtilitySpace to set
	 */
	public void setOpponentUtilitySpace(UtilitySpace opponentUtilitySpace) {
		this.utilitySpace = opponentUtilitySpace;
	}
	/**
	 * @return the leniency
	 */
	public Double getLeniency() {
		return leniency;
	}
	/**
	 * @param leniency the leniency to set
	 */
	public void setLeniency(Double leniency) {
		this.leniency = leniency;
	}
	/**
	 * @return the domainCompetitiveness
	 */
	public Double getDomainCompetitiveness() {
		return domainCompetitiveness;
	}
	/**
	 * @param domainCompetitiveness the domainCompetitiveness to set
	 */
	public void setDomainCompetitiveness(Double domainCompetitiveness) {
		this.domainCompetitiveness = domainCompetitiveness;
	}

	/**
	 * @return the bestBi
	 */
	public Bid getBestBid() {
		return bestBid;
	}

	/**
	 * @param bestBidUtility the bestBidUtility to set
	 */
	public void setBestBid(Bid bestBid) {
		this.bestBid = bestBid;
	}

	
	/**
	 * @return the weights
	 */
	public List<Integer> getBidPointWeights() {
		return bidPointWeights;
	}

	/**
	 * @param weights the weights to set
	 */
	public void setBidPointWeights(List<Integer> weights) {
		this.bidPointWeights = weights;
	}

	
	
	/**
	 * @return the agentDifficulty
	 */
	public Double getAgentDifficulty() {
		return agentDifficulty;
	}

	/**
	 * @param agentDifficulty the agentDifficulty to set
	 */
	public void setAgentDifficulty(Double agentDifficulty) {
		this.agentDifficulty = agentDifficulty;
	}

	public boolean containsBid(Bid bid) {
		for(BidDetails bidDetails: this.agentBidHistory.getHistory()){
			if(bidDetails.getBid().equals(bid)){
				return true;
			}
		}
		return false;
	}

	
	
	
	
	
}
