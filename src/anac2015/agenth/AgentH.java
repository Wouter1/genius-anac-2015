package anac2015.agenth;

import java.util.HashMap;
import java.util.List;

import negotiator.Bid;
import negotiator.Deadline;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class AgentH extends AbstractNegotiationParty {

	/** 現在の bid */
	protected Bid mCurrentBid;
	/** 現在の bid での効用値 */
	protected double mCurrentUtility;
	/** estimatorMap */
	protected HashMap<Object, BidStrategy> mEstimatorMap;
	/** bid 履歴 */
	protected BidHistory mBidHistory;
	/** ヘルパー */
	protected BidHelper mBidHelper;

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
	 * @throws Exception
	 */
	public AgentH(UtilitySpace utilitySpace, Deadline deadlines,
			Timeline timeline, long randomSeed) throws Exception {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);

		mEstimatorMap = new HashMap<Object, BidStrategy>();
		mBidHistory = new BidHistory(getUtilitySpace());
		mBidHelper = new BidHelper(this);
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
		// 経過時間 [0,1] を取得
		final double time = getTimeLine().getTime();

		// トップバッターなら適当に bid // FIXME
		if (!validActions.contains(Accept.class)) {
			final Bid bid = generateRandomBid();
			mBidHistory.offer(this, bid, getUtility(bid));
			mCurrentBid = new Bid(bid);
			return new Offer(bid);
		}

		final double v = mCurrentUtility * time;
		// System.out.println("OreoreAgent#chooseAction(): v="+v);

		// 時間と共に
		if (v < 0.45) {
			final Bid bid = generateNextBid(time);
			mBidHistory.offer(this, bid, getUtility(bid));
			mCurrentBid = new Bid(bid);
			return new Offer(bid);
		} else {
			return new Accept();
		}
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
		// Here you can listen to other parties' messages

		// 現在の bid を更新
		if (action instanceof Offer) {
			mCurrentBid = ((Offer) action).getBid();
			mCurrentUtility = getUtility(mCurrentBid);

			// 記録
			mBidHistory.offer(sender, mCurrentBid, mCurrentUtility);
		} else if (action instanceof Accept) {
			// 記録
			mBidHistory.accept(sender, mCurrentBid);
		}
	}

	/**
	 * 次に自分が出す bid を生成する
	 * 
	 * @return
	 */
	protected Bid generateNextBid(double time) {
		Bid bid;

		bid = mBidHelper.generateFromRelativeUtilitySearch(1.0 * time);
		if (bid == null) {
			bid = mBidHelper.generateFromHistory(1.0 * time);
		}
		if (bid == null) {
			bid = generateRandomBid();
		}

		return bid;
	}

}
