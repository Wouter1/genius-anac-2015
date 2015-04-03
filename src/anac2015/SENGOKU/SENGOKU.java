package anac2015.SENGOKU;

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
import anac2015.SENGOKU.etc.bidSearch;
import anac2015.SENGOKU.etc.negotiatingInfo;
import anac2015.SENGOKU.etc.strategy;

/**
 * This is your negotiation party.
 */
public class SENGOKU extends AbstractNegotiationParty {
	private negotiatingInfo negotiatingInfo; // 交渉情報
	private bidSearch bidSearch; // Bid探索
	private strategy strategy; // 交渉戦略
	private Bid offeredBid = null; // 提案された合意案候補

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

	// コンストラクター　初期化
	public SENGOKU(UtilitySpace utilitySpace, Deadline deadlines,
			Timeline timeline, long randomSeed) throws Exception {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);

		if (isPrinting) {
			System.out.println("*** SAOPMN_SampleAgent ***");
		}

		negotiatingInfo = new negotiatingInfo(utilitySpace);
		bidSearch = new bidSearch(utilitySpace, negotiatingInfo);
		strategy = new strategy(utilitySpace, negotiatingInfo, bidSearch);
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

	// validActions　自分に行動圏があるか！　その行動がAcceptのとき！
	@SuppressWarnings("rawtypes")
	@Override
	// Actionの選択
	public Action chooseAction(List<Class> validActions) {
		if (isPrinting) {
			System.out.println("-----------------MyTern----------------------"
					+ timeline.getTime() * 180);
		}
		double time = timeline.getTime(); // 現在の交渉時刻を取得
		negotiatingInfo.resetAcceptRate(); // アクセプト率の更新
		negotiatingInfo.laststrategy(time); // 最後に妥協するとき
		negotiatingInfo.myActionAccept(); // 自分の行動をアクセプトにする
		// Acceptのアクションを要求されているとき

		// 一度最大の閾値を保存しておく
		negotiatingInfo.updateMaxThreshold(strategy.maxthreshold(0));
		if (time > 0.05) {
			try {
				if (isPrinting) {
					System.out.println("有効値："
							+ utilitySpace.getUtility(offeredBid));
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// 最後の妥協処理
		if (negotiatingInfo.lastFlag) {
			if (isPrinting) {
				System.out
						.println("last-------------------------------------------");
			}
			return lastAction();
		}

		// アクセプトの処理
		if (validActions.contains(Accept.class)
				&& strategy.selectAccept(offeredBid, time)) {
			if (isPrinting) {
				System.out.println("MyAction:Accept");
			}
			return new Accept();
		}

		// EndNegotiation
		// selectEndNegotiation 時間切れ！
		if (strategy.selectEndNegotiation(time) || strategy.endNegotieitionFlag) {
			// System.out.println("flag:"+strategy.endNegotieitionFlag
			// +"タイム"+strategy.selectEndNegotiation(time));
			// System.out.println("MyAction:EndNegociation");
			return new EndNegotiation();
		}

		// Offer
		// System.out.println("MyAction:Offer");
		negotiatingInfo.myActionOffer();
		return OfferAction();
	}

	// 自分でオファーするときの選択！
	public Action OfferAction() {
		Bid offerBid = bidSearch.shiftBidSerch(generateRandomBid(),
				strategy.getThreshold(timeline.getTime()));
		negotiatingInfo.updateMyBidHistory(offerBid);
		return new Offer(offerBid);
	}

	// 最後の処理のメソッド
	public Action lastAction() {
		if (isPrinting) {
			System.out.println("ラストリストの数"
					+ negotiatingInfo.getLastOfferBidNum());
		}

		if (negotiatingInfo.getLastOfferBidNum() == 0) { // 最後のオファー用リストがなくなったとき
			if (isPrinting) {
				System.out.println("カラリストでアクセプト");
			}
			return new Accept();
		} else { //
			Bid lastBid = negotiatingInfo.getLastOfferBidHistry();
			double offerUtil = 0.0;
			double lastUtil = 0.0;
			try {
				offerUtil = utilitySpace.getUtility(offeredBid);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				lastUtil = utilitySpace.getUtility(lastBid);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			negotiatingInfo.removeLastOfferBidHistry();

			if (isPrinting) {
				// System.out.println("自分のオファー"+ lastUtil);
				// System.out.println("相手オファー" + offerUtil);
			}

			if (offerUtil > lastUtil) {
				if (isPrinting) {
					System.out.println("lastAccept");
				}
				return new Accept();
			} else {
				if (isPrinting) {
					System.out.println("lastOffer");
				}
				return new Offer(lastBid);
			}

		}
	}

	// 非協力状態でのオファーのときに閾値調節してからオファーに行く
	/*
	 * private Bid noCooperateOffer(){ ArrayList<Bid> MyBidHistory =
	 * negotiatingInfo.getMyBidHistory(); while(true){ int flag = 0; Bid
	 * offerBid = bidSearch.getBid(generateRandomBid(), strategy.myThreshold);
	 * for (Bid mybid:MyBidHistory){ if(offerBid.equals(mybid) ){ flag = 1;
	 * continue; } } if(flag == 1){ strategy.myThreshold = strategy.myThreshold
	 * -0.0005; }else{ negotiatingInfo.updateMyBidHistory(offerBid); return
	 * offerBid; } } }
	 */

	/***
	 ** All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 **
	 ** @param sender
	 *            The party that did the action.
	 ** @param action
	 *            The action that party did.
	 ***/

	/*
	 * @Override // 自身以外の交渉参加者のActionを受信 // senderが相手　相手名
	 * //　Actionはどんなアクションでオファーのときは中身もわかる！ //　絶えず相手の情報がわかる　なにもしているのか！ public void
	 * receiveMessage(Object sender, Action action) {
	 * //System.out.println("Sender:"+sender+", Action:"+action);
	 * 
	 * // 親クラスのメッソドを実行する　いじらなくてよし！ super.receiveMessage(sender, action);
	 * 
	 * // Here you can listen to other parties' messages //　表示するのみ！
	 * if(isPrinting){ System.out.println("Sender:"+sender+", Action:"+action);
	 * }
	 * 
	 * //System.out.println("Sender:"+sender+", Action:"+action);
	 * 
	 * //アクションがあるとき if (action != null ) {
	 * 
	 * //アクセプトをとき！ actionの中にAcceptがある if(action instanceof Accept){
	 * negotiatingInfo.updateAccept(sender);
	 * //System.out.println("アクセプト数"+negotiatingInfo.getAccept());
	 * //System.out.println("アクセプト率"+negotiatingInfo.getAcceptRate());
	 * if(!negotiatingInfo.getOpponents().contains(sender)){// 初出の交渉者は初期化
	 * 相手のリストにないから最初いれていく！ negotiatingInfo.initOpponent(sender);
	 * //negotiatingInfoのopperatersインスタンスに保存している } }
	 * 
	 * //相手がオファーを出した時 else if(action instanceof Offer) {
	 * if(!negotiatingInfo.getOpponents().contains(sender)){
	 * negotiatingInfo.initOpponent(sender); } // 初出の交渉者は初期化 offeredBid =
	 * ((Offer) action).getBid(); // 提案された合意案候補 try {
	 * negotiatingInfo.updateInfo(sender, offeredBid); } // 交渉情報を更新 catch
	 * (Exception e) { System.out.println("交渉情報の更新に失敗しました");
	 * e.printStackTrace(); } } else {//最初に参加人数の情報を出す Object obj =
	 * ((Object)action); int opponentsNum =
	 * Integer.parseInt(obj.toString().replaceAll("[^0-9]","")); //相手のナンバーを取る
	 * negotiatingInfo.updateOpponentsNum(opponentsNum); if(isPrinting){
	 * System.out.println("NumberofNegotiator:" +
	 * negotiatingInfo.getNegotiatorNum());} } } }
	 */

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
			} else if (action instanceof Offer) {
				if (!negotiatingInfo.getOpponents().contains(sender)) {
					negotiatingInfo.initOpponent(sender);
				} // 初出の交渉者は初期化
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
		}
	}

}
