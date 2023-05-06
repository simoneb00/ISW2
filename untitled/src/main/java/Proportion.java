import exceptions.InvalidTicketException;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static utils.JSON.readJsonFromUrl;


public class Proportion {

    /*
     *  Cold start: we compute the proportion value for the project as the average between the proportion values for other similar projects (i.e. other Apache projects).
     *  In particular, for each other project, we compute the average proportion value across all defects within the project. Then, we take the median between these values as the proportion value for this project.
     */

    // Predicted IV = FV - (FV - OV) * P

    public static void coldStartProportion(ArrayList<Ticket> tickets, String projName) throws JSONException, IOException {

        List<String> allProjects = Arrays.asList(
                "AVRO",
                "OPENJPA",
                "STORM",
                "ZOOKEEPER",
                "SYNCOPE",
                "TAJO",
                "BOOKKEEPER"
        );

        ArrayList<Integer> proportionValues = new ArrayList<>();

        for (String project : allProjects) {
            if (!project.equals(projName)) {
                proportionValues.add(Math.round(getProportionForProject(project)));
            }
        }

        Collections.sort(proportionValues);

        System.out.println("Proportion values = " + proportionValues);

        int proportionValue = Math.round(median(proportionValues));

        System.out.println("Proportion value for " + projName + "= " + proportionValue);

        for (Ticket ticket : tickets) {
            if (ticket.proportion == 0) {
                ticket.injectedVersion = TicketRetriever.releases.get(
                        Math.max(0, Math.round((float)ticket.fixVersion.getId() - (ticket.fixVersion.getId() - ticket.openingVersion.getId()) * proportionValue) - 1)
                );
            }
        }
    }

    private static float median(List<Integer> list) {
        float sum;
        if (list.size() % 2 == 0) {
            sum = (float)list.get(list.size() / 2 - 1) + (float)list.get(list.size() / 2);
            return sum/2;
        } else {
            return (float)list.get(list.size()/2);
        }
    }

    /*
     *  ASSUMPTION: we consider only tickets with consistent AV, i.e. with IV <= OV. In the case of FV = OV, in order to avoid denominator going to infinity, we assume FV - AV = 1
     */
    public static void computeProportion(Ticket ticket) {

        if (ticket.injectedVersion.getId() <= ticket.openingVersion.getId() && ticket.fixVersion.getId() == ticket.openingVersion.getId())
            ticket.proportion = ticket.fixVersion.getId() - ticket.injectedVersion.getId();
        else if (ticket.injectedVersion.getId() <= ticket.openingVersion.getId() && ticket.openingVersion.getId() < ticket.fixVersion.getId()) {
            ticket.proportion = (float)(ticket.fixVersion.getId() - ticket.injectedVersion.getId())/(ticket.fixVersion.getId() - ticket.openingVersion.getId());
            System.out.println(ticket.proportion);
        }
    }

    public static float getProportionForProject(String projName) throws JSONException, IOException {

        int startAt = 0;
        ArrayList<Ticket> tickets = new ArrayList<>();
        JSONObject json;

        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName);

        System.out.println("Retrieving tickets for project " + projName);

        do {
            String query = "search?jql=project=" + projName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000&startAt=" + startAt;
            String url = "https://issues.apache.org/jira/rest/api/2/" + query;

            json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");

            for (int i = 0; i < issues.length(); i++) {
                try {
                    tickets.add(TicketRetriever.getTicket(issues.getJSONObject(i), releases));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (InvalidTicketException e) {
                    // ignore: invalid ticket
                }
            }

            startAt += 1000;

        } while (json.getJSONArray("issues").length() != 0);

        int count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.proportion != 0) {
                count++;
            }
        }

        System.out.println("Tickets having proportion (project " + projName + "):"  + count + " over " + tickets.size() + " tickets");
        System.out.println("proportion mean (project " + projName + "):" + TicketRetriever.getProportionMean(tickets));

        return TicketRetriever.getProportionMean(tickets);

    }
}
