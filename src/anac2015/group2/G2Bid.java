package anac2015.group2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.utility.Evaluator;

class G2Bid {
	private Map<String, String> choices;
	
	private Map<String, String> extractChoicesFromBid(Bid bid) {
		Map<String, String> bidChoices = new HashMap<String, String>();
		ArrayList<negotiator.issue.Issue> issues = bid.getIssues();
		
		for(negotiator.issue.Issue issue : issues) {
			try {
				bidChoices.put(issue.getName(), bid.getValue(issue.getNumber()).toString());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return bidChoices;
	}
	
	public G2Bid(Bid bid) {
		choices = extractChoicesFromBid(bid);
	}
	
	public G2Bid(Map<String, String> options) {
		choices = new HashMap<String, String> (options);
	}
	
	public String getChoice(String issue) {
		return choices.get(issue);
	}
	
	public String setChoice(String issue, String choice) {
		return choices.put(issue, choice);
	}
	
	public Map<String,String> getChoices() {
		return choices;
	}
	
	public Set<Entry<String, String>> getEntrySet(){
		return choices.entrySet();
	}
	
	public String toString() {
		String s = "";
		for(Entry<String, String> entry : getEntrySet()) {
			s += "["+entry.getKey()+":"+entry.getValue()+"]";
		}
		return s;
	}
	
	public Bid convertToBid(Domain domain, Set<Entry<Objective,Evaluator>> evaluators) throws Exception{
		HashMap<Integer, Value> map = new HashMap<Integer, Value>();
		
		for(Entry<Objective, Evaluator> entry : evaluators) {
			IssueDiscrete issue = (IssueDiscrete) entry.getKey();
			Integer index = issue.getNumber();
			ValueDiscrete value = issue.getValue(issue.getValueIndex(getChoice(issue.getName())));
			map.put(index, value);
		}
		
		
		return new Bid(domain, map);
	}
	
	boolean equals(G2Bid otherBid) {
		Set<String> keySet = choices.keySet();
		if(!(keySet.size() == otherBid.choices.keySet().size())) {
			return false;
		}
		for(String issue: keySet) {
			if(!choices.get(issue).equals(otherBid.getChoice(issue))) {
				return false;
			}
		}
		return true;
	}
}