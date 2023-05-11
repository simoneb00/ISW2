import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            List<Ticket> allTickets = TicketRetriever.retrieveTickets("BOOKKEEPER");
            CommitRetriever.retrieveCommits("BOOKKEEPER", allTickets);

            allTickets = TicketRetriever.retrieveTickets("STORM");
            CommitRetriever.retrieveCommits("STORM", allTickets);

        } catch (JSONException | IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
