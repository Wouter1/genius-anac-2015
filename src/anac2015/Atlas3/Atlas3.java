package anac2015.Atlas3;

import java.util.ArrayList;
import java.util.List;

import negotiator.Bid;
import negotiator.Deadline;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Inform;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;
import anac2015.Atlas3.etc.bidSearch;
import anac2015.Atlas3.etc.negotiatingInfo;
import anac2015.Atlas3.etc.strategy;

/**
 * This is your negotiation party.
 */
public class Atlas3 extends AbstractNegotiationParty {
	private negotiatingInfo negotiatingInfo; // 交渉情報
	private bidSearch bidSearch; // Bid探索
	private strategy strategy; // 交渉戦略
	private double rv; // 留保価格
	private Bid offeredBid = null; // 提案された合意案候補
	private int supporter_num = 0; // 支持者数
	private int CList_index = 0; // CListのインデックス：最終提案フェーズにおける遡行を行うために利用(ConcessionList)

	// デバッグ用
	public static boolean isPrinting = false; // メッセージを表示する

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
	public Atlas3(UtilitySpace utilitySpace, Deadline deadlines,
			Timeline timeline, long randomSeed) throws Exception {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);

		if (isPrinting) {
			System.out.println("*** Atlas3 v1.0 ***");
		}

		negotiatingInfo = new negotiatingInfo(utilitySpace);
		bidSearch = new bidSearch(utilitySpace, negotiatingInfo);
		strategy = new strategy(utilitySpace, negotiatingInfo);
		rv = utilitySpace.getReservationValue();
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
	@SuppressWarnings("rawtypes")
	@Override
	// Actionの選択
	public Action chooseAction(List<Class> validActions) {
		double time = timeline.getTime(); // 現在の交渉時刻を取得
		negotiatingInfo.updateTimeScale(time); // 自身の手番が回ってくる時間間隔を記録

		// 最終提案フェーズにおけるアクション
		ArrayList<Bid> CList;
		CList = negotiatingInfo.getPBList();
		if (time > 1.0 - negotiatingInfo.getTimeScale() * (CList.size() + 1)) {
			try {
				return chooseFinalAction(offeredBid, CList);
			} catch (Exception e) {
				System.out.println("最終提案フェーズにおけるActionの選択に失敗しました");
				e.printStackTrace();
			}
		}

		// Accept
		if (validActions.contains(Accept.class)
				&& strategy.selectAccept(offeredBid, time)) {
			return new Accept();
		}

		// EndNegotiation
		if (strategy.selectEndNegotiation(time)) {
			return new EndNegotiation();
		}

		// Offer
		return OfferAction();
	}

	public Action chooseFinalAction(Bid offeredBid, ArrayList<Bid> CList)
			throws Exception {
		double offeredBid_util = 0;
		if (offeredBid != null) {
			offeredBid_util = utilitySpace.getUtility(offeredBid);
		}
		if (CList_index >= CList.size()) {
			if (offeredBid_util >= rv) {
				return new Accept();
			} // 遡行を行っても合意が失敗する場合，Acceptする
			else {
				return OfferAction();
			}
		}

		// CListの遡行
		Bid CBid = CList.get(CList_index);
		double CBid_util = utilitySpace.getUtility(CBid);
		if (CBid_util > offeredBid_util && CBid_util > rv) {
			CList_index++;
			negotiatingInfo.updateMyBidHistory(CBid);
			return new Offer(CBid);
		} else if (offeredBid_util > rv) {
			return new Accept();
		}

		return OfferAction();
	}

	public Action OfferAction() {
		Bid offerBid = bidSearch.getBid(generateRandomBid(),
				strategy.getThreshold(timeline.getTime()));
		negotiatingInfo.updateMyBidHistory(offerBid);
		return new Offer(offerBid);
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
	// 自身以外の交渉参加者のActionを受信
	public void receiveMessage(Object sender, Action action) {
		super.receiveMessage(sender, action);
		// Here you can listen to other parties' messages
		if (isPrinting) {
			System.out.println("Sender:" + sender + ", Action:" + action);
		}

		if (action != null) {
			if (action instanceof Inform
					&& ((Inform) action).getName() == "NumberOfAgents"
					&& ((Inform) action).getValue() instanceof Integer) {
				Integer opponentsNum = (Integer) ((Inform) action).getValue();
				negotiatingInfo.updateOpponentsNum(opponentsNum);
				if (isPrinting) {
					System.out.println("NumberofNegotiator:"
							+ negotiatingInfo.getNegotiatorNum());
				}
			} else if (action instanceof Accept) {
				if (!negotiatingInfo.getOpponents().contains(sender)) {
					negotiatingInfo.initOpponent(sender);
				} // 初出の交渉者は初期化
				supporter_num++;
			} else if (action instanceof Offer) {
				if (!negotiatingInfo.getOpponents().contains(sender)) {
					negotiatingInfo.initOpponent(sender);
				} // 初出の交渉者は初期化
				supporter_num = 1; // supporterをリセット
				offeredBid = ((Offer) action).getBid(); // 提案された合意案候補
				try {
					negotiatingInfo.updateInfo(sender, offeredBid);
				} // 交渉情報を更新
				catch (Exception e) {
					System.out.println("交渉情報の更新に失敗しました");
					e.printStackTrace();
				}
			} else if (action instanceof EndNegotiation) {
			}

			// 自身以外が賛成している合意案候補を記録（自身以外のエージェントを1つの交渉者とみなす．そもそも自身以外のエージェントが二人以上非協力であれば，自身の選択に関わらず合意は不可能である）
			if (supporter_num == negotiatingInfo.getNegotiatorNum() - 1) {
				if (offeredBid != null) {
					try {
						negotiatingInfo.updatePBList(offeredBid);
					} catch (Exception e) {
						System.out.println("PBListの更新に失敗しました"); // PopularBidHistoryを更新
						e.printStackTrace();
					}
				}
			}
		}
	}

}
