package anac2015.meanBot;

import java.util.List;
import java.util.Map;

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
public class MeanBot extends AbstractNegotiationParty
{

    private Bid currentBid;

    /**
     * Please keep this constructor. This is called by genius.
     *
     * @param utilitySpace Your utility space.
     * @param deadlines    The deadlines set for this negotiation.
     * @param timeline     Value counting from 0 (start) to 1 (end).
     * @param randomSeed   If you use any randomization, use this seed for it.
     */
    public MeanBot(UtilitySpace utilitySpace,
                   Map<DeadlineType, Object> deadlines,
                   Timeline timeline,
                   long randomSeed)
    {
        // Make sure that this constructor calls it's parent.
        super(utilitySpace, deadlines, timeline, randomSeed);
    }

    /**
     * Each round this method gets called and ask you to accept or offer. The first party in
     * the first round is a bit different, it can only propose an offer.
     *
     * @param validActions Either a list containing both accept and offer or only offer.
     * @return The chosen action.
     */
    @Override
    public Action chooseAction(List<Class> validActions)
    {
        try
        {
            if (validActions.contains(Accept.class)
                    && timeline.getTime() >= .95
                    && utilitySpace.getUtility(currentBid) > .5)
            {
                return new Accept();
            } else
                return new Offer(utilitySpace.getMaxUtilityBid());

        } catch (Exception e)
        {
            return new Accept(); // Not sure what to put here... Don't know what error I would be hitting.
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
    public void receiveMessage(Object sender, Action action)
    {
        super.receiveMessage(sender, action);

        if (action instanceof Offer)
        {
            // Know what the most recent bid on the table is, for evaluation.
            currentBid = Action.getBidFromAction(action);
        }
        // Here you can listen to other parties' messages
    }

}
