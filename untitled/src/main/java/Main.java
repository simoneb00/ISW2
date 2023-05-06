import model.Ticket;
import org.codehaus.jettison.json.JSONException;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            //List<Ticket> allTickets = TicketRetriever.retrieveTickets("BOOKKEEPER");
            //CommitRetriever.retrieveCommits("BOOKKEEPER", allTickets);

            List<Ticket> allTickets = TicketRetriever.retrieveTickets("STORM");
            CommitRetriever.retrieveCommits("STORM", allTickets);

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }
}
