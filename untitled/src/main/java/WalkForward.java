import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class WalkForward {

    public static void initSets(String projName) throws JSONException, IOException, GitAPIException {
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, true);

        // releases splitting
        //releases = releases.subList(0, Math.round((float)releases.size() / 2));

        // we have to create different CSV files: for each iteration, starting just with the first release, we add the next release
        for (int i = 1; i <= releases.size(); i++) {

            System.out.println("\n\nRetrieving tickets for the first " + i + " releases");
            List<Ticket> tickets = TicketRetriever.retrieveTickets(projName, i);

            CommitRetriever.retrieveCommits(projName, tickets, i);
        }
    }
}
