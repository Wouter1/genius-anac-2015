package misc;

import java.util.ArrayList;
import java.util.List;

import negotiator.Deadline;
import negotiator.DeadlineType;
import negotiator.config.Configuration;
import negotiator.protocol.Protocol;
import negotiator.repository.DomainRepItem;
import negotiator.repository.MultiPartyProtocolRepItem;
import negotiator.repository.PartyRepItem;
import negotiator.repository.ProfileRepItem;
import negotiator.repository.RepItem;
import negotiator.repository.Repository;
import negotiator.session.Session;
import negotiator.tournament.TournamentGenerator;
import negotiator.utility.TournamentIndicesGenerator;

public class AutomatedTournamentConfiguration extends Configuration {

	/**
	 * Number of agents in each test run (make sure that this is reflected in
	 * partyrepository.xml
	 */
	public static final int AGENTS_PER_RUN = 3;

	/**
	 * Number of profiles in each test run (make sure that this is reflected in
	 * domainrepository.xml
	 */
	public static final int PROFILES_PER_RUN = 3;

	/**
	 * If set to true, agents also play against themselves
	 */
	public static final boolean REPETITION_ALLOWED = false;

	/**
	 * Number of times to run this setting. (use more than 1 for statistically
	 * significant results).
	 */
	public static final int NUMBER_OF_RUNS = 1;

	/**
	 * Deadline for each negotiation round
	 */
	public static final int DEADLINE_TIME = 180;
	public static final int DEADLINE_ROUNDS = 0;

	/**
	 * Type of deadline for each negotiation round
	 */
	public static final DeadlineType DEADLINE_TYPE = DeadlineType.TIME;

	public int runNumber = 0;
	private Session session = null;

	public AutomatedTournamentConfiguration(int runNumber) {
		this.runNumber = runNumber;
	}

	@Override
	public Session getSession() {
		if (session == null)
			session = new Session(getDeadlines());
		return session;
	}

	@Override
	public Protocol getProtocol() throws Exception {
		MultiPartyProtocolRepItem protocolRepItem = (MultiPartyProtocolRepItem) Repository
				.getMultiPartyProtocolRepository().getItems().get(0);
		return Configuration.createFrom(protocolRepItem);
	}

	@Override
	public Deadline getDeadlines() {
		return new Deadline(DEADLINE_TIME, DEADLINE_ROUNDS);
	}

	public static List<ProfileRepItem> getAllPartyProfileItems() {
		try {
			Repository domainrep = Repository.get_domain_repos();
			ArrayList<ProfileRepItem> profiles = new ArrayList<ProfileRepItem>();
			for (RepItem domain : domainrep.getItems()) {
				if (!(domain instanceof DomainRepItem))
					throw new IllegalStateException(
							"Found a non-DomainRepItem in domain repository:"
									+ domain);
				for (ProfileRepItem profile : ((DomainRepItem) domain)
						.getProfiles())
					profiles.add(profile);
			}
			return profiles;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	@Override
	public List<ProfileRepItem> getPartyProfileItems() {
		return getAllPartyProfileItems().subList(runNumber * PROFILES_PER_RUN,
				(runNumber + 1) * PROFILES_PER_RUN);
	}

	@Override
	public List<PartyRepItem> getPartyItems() {
		final ArrayList<RepItem> items = Repository.get_party_repository()
				.getItems();
		ArrayList<PartyRepItem> parties = new ArrayList<PartyRepItem>();
		for (RepItem item : items)
			parties.add((PartyRepItem) item);
		return parties;
	}

	@Override
	public TournamentGenerator getPartiesGenerator() {

		List<Integer> indices = new ArrayList<Integer>(getPartyItems().size());
		for (int i = 0; i < getPartyItems().size(); i++)
			indices.add(i);

		TournamentIndicesGenerator indicesGenerator = new TournamentIndicesGenerator(
				AGENTS_PER_RUN, PROFILES_PER_RUN, REPETITION_ALLOWED, indices);
		return new TournamentGenerator(this, indicesGenerator);
	}

	@Override
	public int getNumTournaments() {
		return NUMBER_OF_RUNS;
	}

	@Override
	public int getMediatorIndex() {
		return 0;
	}

	@Override
	public ProfileRepItem getMediatorProfile() {
		return null;
	}

	@Override
	public PartyRepItem getMediatorItem() {
		return null;
	}
}
