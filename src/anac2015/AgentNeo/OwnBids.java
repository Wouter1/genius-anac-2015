package anac2015.AgentNeo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.utility.UtilitySpace;


public class OwnBids {

    private ArrayList<Bid> BidHistory;
    private Bid minBidInHistory;

    public OwnBids() {
        BidHistory = new ArrayList<Bid>();
    }

    protected void addBid(Bid bid, UtilitySpace utilitySpace) {
       // if (BidHistory.indexOf(bid) == -1) {
            BidHistory.add(bid);
            System.out.println(minBidInHistory + "OWN BIDS"); 
       // }
        try {
            if (BidHistory.size() == 1) {
                this.minBidInHistory = BidHistory.get(0);
                
            } else {
                
                if (utilitySpace.getUtility(bid) < utilitySpace.getUtility(this.minBidInHistory)) {
                    this.minBidInHistory = bid;
                    
                }
            }
        } catch (Exception e) {
            System.out.println("error in addBid method of OwnBidHistory class " + e.getMessage());
        }
    }

    protected Bid GetMinBidInHistory() {

        return this.minBidInHistory;
    }

    protected Bid getLastBid() {
        if (BidHistory.size() >= 1) {
            return BidHistory.get(BidHistory.size() - 1);
        } else {
            return null;
        }
    }

    protected int numOfBidsProposed() {
    	System.out.println("No. of bids proposed = " + BidHistory.size());
        return BidHistory.size();
    }

    protected Bid chooseLowestBidInHistory(UtilitySpace utilitySpace) {
        double minUtility = 100;
        Bid minBid = null;
        try {
            for (Bid bid : BidHistory) {
                if (utilitySpace.getUtility(bid) < minUtility) {
                    minUtility = utilitySpace.getUtility(bid);
                    minBid = bid;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception in chooseLowestBidInHistory");
        }
        return minBid;
    }
}


