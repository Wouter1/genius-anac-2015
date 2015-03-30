package misc;

import negotiator.session.TournamentManager;

public class AutomatedTournamentRunner {

    public static void main(String[] args) {

        int amountOfScenarios = AutomatedTournamentConfiguration.getAllPartyProfileItems().size() / AutomatedTournamentConfiguration.PROFILES_PER_RUN;

        for (int i = 0; i < amountOfScenarios; i++) {
            TournamentManager tournamentManager = new TournamentManager(new AutomatedTournamentConfiguration(i));
            System.out.println("Loaded custom tournament " + (i + 1) + "/" + amountOfScenarios);
            tournamentManager.run();
        }
    }
}
