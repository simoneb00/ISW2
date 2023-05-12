import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import weka.Weka;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

            List<List<File>> files = WalkForward.initSets("BOOKKEEPER");
            //WalkForward.initSets("STORM");


            WalkForward.classify("BOOKKEEPER");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

    }
}
