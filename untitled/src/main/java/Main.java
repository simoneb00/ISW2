import model.Ticket;
import org.codehaus.jettison.json.JSONException;

import java.io.IOException;
import java.util.List;

public class Main {

    public static String projName = "STORM";
    public static void main(String[] args) {
        try {
            List<Ticket> allTickets = TicketRetriever.retrieveTickets(projName);
            CommitRetriever.retrieveCommits(projName, allTickets);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}
