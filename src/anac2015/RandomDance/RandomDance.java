package anac2015.RandomDance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import negotiator.Bid;
import negotiator.Deadline;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Inform;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class RandomDance extends AbstractNegotiationParty {
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

	/*
	 * パレート最適最速アタックを目指す(ひとまず)
	 */

	public RandomDance(UtilitySpace utilitySpace, Deadline deadlines,
			Timeline timeline, long randomSeed) {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);
	}

	final int NashCountMax = 200;
	final int NumberOfAcceptSafety = 5;
	final int NumberOfRandomTargetCheck = 3;

	private boolean init = false;
	private int playerNumber = 0;
	private Map<String, PlayerDataLib> utilityDatas = new HashMap<String, PlayerDataLib>();
	private PlayerData myData = null;
	private int offerCount = 0;

	private List<String> nash = new LinkedList<String>();
	private Map<String, Bid> olderBidMap = new HashMap<String, Bid>();

	private double discountFactor = 1.0;
	private double reservationValue = 0;

	private double olderTime = 0;

	@Override
	public Action chooseAction(List<Class> validActions) {

		offerCount++;

		if (!init) {
			init = true;
			myInit();
		}

		Map<String, PlayerData> utilityMap = new HashMap<String, PlayerData>();
		for (String str : utilityDatas.keySet()) {
			utilityMap.put(str, utilityDatas.get(str).getRandomPlayerData());
		}
		utilityMap.put("my", myData);

		/*
		 * 前回の相手のBidのうち、より他の相手に歩み寄っているプレイヤーを探す
		 */
		double maxval = -999;
		String maxPlayer = null;
		for (String string : olderBidMap.keySet()) {
			double utility = 1.0;
			for (String player : utilityMap.keySet()) {
				if (string.equals(player)) {
					continue;
				}
				utility *= utilityMap.get(player).GetUtility(
						olderBidMap.get(string));
			}
			if (utility > maxval) {
				maxval = utility;
				maxPlayer = string;
			}
		}
		if (maxPlayer != null) {
			nash.add(maxPlayer);
		}
		while (nash.size() > NashCountMax) {
			nash.remove(0);
		}

		Map<String, Double> playerWeight = getWeights();

		Action action = null;
		Offer offer = null;

		double target = GetTarget(utilityMap);
		double utility = 0;

		if (olderBid != null) {

			try {
				utility = utilitySpace.getUtility(olderBid);
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}

		try {
			offer = new Offer(SearchBid(target, utilityMap, playerWeight));
			action = offer;
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		if (action == null || IsAccept(target, utility)) {
			action = new Accept();
		}
		if (IsEndNegotiation(target)) {
			action = new EndNegotiation();
		}

		return action;
	}

	public Map<String, Double> getWeights() {
		/*
		 * プレイヤーウェイトの計算
		 */
		Map<String, Double> playerWeight = new HashMap<String, Double>();
		int rand = (int) (Math.random() * 3);

		switch (rand) {
		case 0:
			for (String string : utilityDatas.keySet()) {
				playerWeight.put(string, 0.0001);
			}
			for (String string : nash) {
				playerWeight.put(string, playerWeight.get(string) + 1.0);
			}
			break;

		case 1:
			for (String string : utilityDatas.keySet()) {
				playerWeight.put(string, 1.0);
			}
			break;
		case 2:
			boolean flag = Math.random() < 0.5;
			for (String string : utilityDatas.keySet()) {

				if (string.equals("my")) {
					continue;
				}

				if (flag) {
					playerWeight.put(string, 1.0);
				} else {
					playerWeight.put(string, 0.01);
				}
				flag = !flag;
			}
			break;
		default:
			for (String string : utilityDatas.keySet()) {
				playerWeight.put(string, 1.0);
			}
			break;
		}

		// System.err.println("PlayerWeight : " + playerWeight.toString());

		return playerWeight;
	}

	Bid olderBid = null;

	@Override
	public void receiveMessage(Object sender, Action action) {
		super.receiveMessage(sender, action);

		if (sender.toString().equals("Protocol")) {
			Inform inform = (Inform) action;
			// System.err.println(inform.getName());
			// System.err.println((int)inform.getValue());
			playerNumber = (Integer) inform.getValue();

		} else {
			if (utilityDatas.containsKey(sender.toString()) == false) {
				utilityDatas.put(sender.toString(), new PlayerDataLib(
						utilitySpace.getDomain().getIssues()));
			}

			if (action.getClass() == Offer.class) {
				Offer offer = (Offer) action;
				Bid bid = offer.getBid();
				olderBid = bid;
			}

			olderBidMap.put(sender.toString(), olderBid);

			try {
				utilityDatas.get(sender.toString()).AddBid(olderBid);
			} catch (Exception e) {
				// System.err.println("Error In AddBid");
				e.printStackTrace();
			}
			// System.err.println(sender.toString());
			// System.err.println(utilityDatas.get(sender.toString()).toString());

		}
		// Here you can listen to other parties' messages
	}

	private boolean IsAccept(double target, double utility) {
		double time = timeline.getTime();
		double d = time - olderTime;
		olderTime = time;
		// 時間ギリギリならAccept
		if (time + d * NumberOfAcceptSafety > 1.0) {
			// System.err.println("Accept Time");
			return true;
		}

		if (olderBid == null) {
			return false;
		}
		// targetより大きければAccept
		if (utility > target) {
			// System.err.println("Accept utility over target! " + target + " "
			// + utility);
			return true;
		}
		return false;
	}

	private boolean IsEndNegotiation(double target) {

		if (target < reservationValue) {
			return true;
		}

		return false;
	}

	private double GetTarget(Map<String, PlayerData> datas) {

		double max = 0;

		Map<String, Double> weights = new HashMap<String, Double>();

		/*
		 * for(String str:datas.keySet()){ weights.put(str, 1.0); } Bid bid =
		 * SearchBidWithWeights(datas, weights); try { max = Math.max(max,
		 * utilitySpace.getUtility(bid)); } catch (Exception e) { // TODO
		 * 自動生成された catch ブロック e.printStackTrace(); }
		 */

		for (int i = 0; i < NumberOfRandomTargetCheck; i++) {

			Map<String, PlayerData> utilityMap = new HashMap<String, PlayerData>();
			for (String str : utilityDatas.keySet()) {
				utilityMap
						.put(str, utilityDatas.get(str).getRandomPlayerData());
				weights.put(str, 1.0);
			}
			utilityMap.put("my", myData);
			weights.put("my", 1.0);

			Bid bid = SearchBidWithWeights(utilityMap, weights);
			try {
				max = Math.max(max, utilitySpace.getUtility(bid));
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}

		}

		double target = 1.0 - (1.0 - max)
				* (Math.pow(timeline.getTime(), discountFactor));

		if (discountFactor > 0.99) {
			target = 1.0 - (1.0 - max) * (Math.pow(timeline.getTime(), 3));
		}

		// System.err.println("time = " + timeline.getTime()+ "target = " +
		// target +" max = " + max);
		return target;
	}

	private void myInit() {
		PlayerData playerData = new PlayerData(utilitySpace.getDomain()
				.getIssues(), 1.0);

		try {
			playerData.SetMyUtility(utilitySpace);
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		myData = playerData;

		reservationValue = utilitySpace.getReservationValue();
		discountFactor = utilitySpace.getDiscountFactor();

	}

	private Bid SearchBidWithWeights(Map<String, PlayerData> datas,
			Map<String, Double> weights) {
		Bid ret = generateRandomBid();
		for (Issue issue : utilitySpace.getDomain().getIssues()) {

			List<ValueDiscrete> values = new ArrayList<ValueDiscrete>();
			IssueDiscrete id = (IssueDiscrete) issue;
			values = id.getValues();

			double max = -1;
			Value maxValue = null;

			for (Value value : values) {

				double v = 0;

				for (String string : datas.keySet()) {
					PlayerData data = datas.get(string);
					double weight = weights.get(string);
					v += data.GetValue(issue, value) * weight;
				}
				if (v > max) {
					max = v;
					maxValue = value;
				}
			}
			ret.setValue(id.getNumber(), maxValue);
		}
		return ret;
	}

	/*
	 * target以上で良い感じに
	 */
	private Bid SearchBid(double target, Map<String, PlayerData> datas,
			Map<String, Double> weights) throws Exception {

		/*
		 * 引数に渡すようのMapを作る
		 */
		Map<String, PlayerData> map = new HashMap<String, PlayerData>();
		map.putAll(datas);
		map.put("my", myData);

		Map<String, Double> weightbuf = new HashMap<String, Double>();
		/*
		 * 敵ウェイトを合計1になるようにする
		 */
		double sum = 0;
		for (Double d : weights.values()) {
			sum += d;
		}
		for (String key : weights.keySet()) {
			weightbuf.put(key, weights.get(key) / sum);
		}

		for (double w = 0; w < 9.999; w += 0.01) {

			double myweight = w / (1.0 - w);
			weightbuf.put("my", myweight);

			Bid bid = SearchBidWithWeights(map, weightbuf);

			if (utilitySpace.getUtility(bid) > target) {
				return bid;
			}
		}

		return utilitySpace.getMaxUtilityBid();
	}

}

class PlayerDataLib {

	ArrayList<PlayerData> playerDatas = new ArrayList<PlayerData>();

	public PlayerDataLib(ArrayList<Issue> issues) {
		playerDatas.add(new PlayerData(issues, 1.0));
		playerDatas.add(new PlayerData(issues, 1.05));
		playerDatas.add(new PlayerData(issues, 0.95));
	}

	public PlayerData getRandomPlayerData() {
		int rand = (int) (Math.random() * playerDatas.size());
		return playerDatas.get(rand);
	}

	public void AddBid(Bid bid) {
		for (PlayerData d : playerDatas) {
			try {
				d.AddBid(bid);
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	public ArrayList<PlayerData> getPlayerDataList() {
		return playerDatas;
	}

}

class PlayerData {

	Map<Issue, IssueData> map = new HashMap<Issue, IssueData>();
	Set<Bid> history = new HashSet<Bid>();
	double derta = 1.00;

	public PlayerData(ArrayList<Issue> issues, double derta) {
		for (Issue issue : issues) {
			map.put(issue, new IssueData(issue, derta));
		}
		this.derta = derta;
	}

	public double GetUtility(Bid bid) {
		double ret = 0;
		for (Issue issue : bid.getIssues()) {
			try {
				ret += GetValue(issue, bid.getValue(issue.getNumber()));
			} catch (Exception e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		return ret;
	}

	public double GetValue(Issue issue, Value value) {
		ValuePut(issue);
		return map.get(issue).GetValueWithWeight(value);
	}

	public void SetMyUtility(UtilitySpace utilitySpace) throws Exception {

		Bid bid = utilitySpace.getMinUtilityBid();
		ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
		double min = utilitySpace.getUtility(bid);

		for (Issue issue : issues) {
			bid = utilitySpace.getMinUtilityBid();
			IssueDiscrete id = (IssueDiscrete) issue;
			List<ValueDiscrete> values = id.getValues();
			IssueData issueData = map.get(issue);

			for (ValueDiscrete value : values) {
				bid.setValue(issue.getNumber(), value);
				double v = utilitySpace.getUtility(bid) - min;
				issueData.setValue(value, v);
			}
			issueData.setWeight(1.0 / (1.0 - min));
			issueData.Locked();
		}

		// System.err.println(this.toString());

	}

	public void AddBid(Bid bid) throws Exception {

		if (history.contains(bid)) {
			return;
		}
		history.add(bid);

		double countsum = 0;
		for (Issue issue : bid.getIssues()) {
			ValuePut(issue);
			map.get(issue).Update(bid.getValue(issue.getNumber()));
			countsum += map.get(issue).getMax();
		}

		for (Issue issue : bid.getIssues()) {
			map.get(issue).setWeight(map.get(issue).getMax() / countsum);
		}
	}

	/*
	 * Mapにキーがない時に追加する関数
	 */
	private void ValuePut(Issue issue) {
		if (!map.containsKey(issue)) {
			map.put(issue, new IssueData(issue, derta));
		}
	}

	@Override
	public String toString() {

		String ret = "";
		for (Issue issue : map.keySet()) {
			ret += issue.toString() + ":" + map.get(issue).toString() + "\n";
		}
		return ret;
	}

	/*
	 * 各Issueごとの数え上げデータ
	 */

	class IssueData {

		private Map<Value, Double> map = new HashMap<Value, Double>();
		private double weight = 1;
		private boolean locked = false;
		private double max = 1;
		private double derta = 1.0;
		private double adder = 1.0;

		public IssueData(Issue issue, double derta) {
			IssueDiscrete id = (IssueDiscrete) issue;

			List<ValueDiscrete> list = id.getValues();
			for (Value value : list) {
				setValue(value, 0);
			}
			this.derta = derta;
		}

		/*
		 * 更新禁止のロックをかける ロックは外せない
		 */
		public void Locked() {
			locked = true;
		}

		public double getWeight() {
			return weight;
		}

		public void setWeight(double weight) {
			this.weight = weight;
		}

		public void setValue(Value value, double util) {
			if (locked) {
				System.err.println("LockedAccess!!");
			} else {
				map.put(value, util);
			}
		}

		/*
		 * 相手のBidがきた時の更新関数 とりあえず1を足す
		 */
		public void Update(Value value) {
			if (locked) {
				System.err.println("LockedAccess!!");
				return;
			}
			ValuePut(value);
			map.put(value, map.get(value) + adder);
			max = Math.max(max, map.get(value));
			adder *= derta;
		}

		double GetValue(Value value) {
			ValuePut(value);
			return map.get(value) / max;
		}

		double GetValueWithWeight(Value value) {
			return GetValue(value) * weight;
		}

		/*
		 * Mapにキーがない時に追加する関数
		 */
		private void ValuePut(Value value) {
			if (!map.containsKey(value)) {
				map.put(value, 0.0);
			}
		}

		private double getMax() {
			return max;
		}

		@Override
		public String toString() {
			return "weight:" + weight + ":" + map.toString();
		}

	}
}
