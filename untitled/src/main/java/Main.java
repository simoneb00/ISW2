import java.io.File;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
        /*
        List<Ticket> allTickets = TicketRetriever.retrieveTickets("BOOKKEEPER", 0);
        CommitRetriever.retrieveCommits("BOOKKEEPER", allTickets, 0);

        allTickets = TicketRetriever.retrieveTickets("STORM", 0);
        CommitRetriever.retrieveCommits("STORM", allTickets, 0);

         */

            List<List<File>> files = WalkForward.initSets("STORM");
            //WalkForward.initSets("STORM");

            WalkForward.classify("STORM");


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
