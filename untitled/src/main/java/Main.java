import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

public class Main {

    public static String projName = "BOOKKEEPER";
    public static void main(String[] args) {
        try {
            List<Ticket> allTickets = TicketRetriever.retrieveTickets(projName);
            CommitRetriever.retrieveCommits(projName, allTickets);

        } catch (JSONException | IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
