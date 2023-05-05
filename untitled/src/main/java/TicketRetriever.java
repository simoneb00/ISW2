import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jgit.api.errors.GitAPIException;


import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static utils.JSON.readJsonFromUrl;

public class TicketRetriever {
    public static ArrayList<Ticket> tickets;
    public static ArrayList<Release> releases = new ArrayList<>();

    public static void main(String[] args) throws JSONException, IOException, GitAPIException {
        String projectName = "BOOKKEEPER";
        String query = "search?jql=project=" + projectName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000";
        String url = "https://issues.apache.org/jira/rest/api/2/" + query;

        JSONObject json = readJsonFromUrl(url);
        JSONArray issues = json.getJSONArray("issues");
        System.out.println(issues.length());

        tickets = new ArrayList<>();

        releases = GetReleaseInfo.getReleaseInfo(projectName);

        for (int i = 0; i < releases.size(); i++) {
            System.out.println(releases.get(i).getId());
            System.out.println(releases.get(i).getName());
            System.out.println(releases.get(i).getDate());
            System.out.println("____________________________");
        }



        for (int i = 0; i < issues.length(); i++) {
            try {
                tickets.add(getTicket(issues.getJSONObject(i)));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        System.out.println("-------- tickets having proportion ----------------------");
        for (Ticket ticket : tickets) {
            if (ticket.proportion != 0)
                System.out.println(ticket.fixVersion.getName());
        }


        System.out.println("proportion mean: " + getProportionMean());
        Proportion.estimateInjectedVersion(tickets, getProportionMean());

        for (Ticket ticket : tickets) {

            ArrayList<String> affVersionsNames = new ArrayList<>();

            if (ticket.affectedVersions != null) {
                for (int i = 0; i < ticket.affectedVersions.size(); i++) {
                    affVersionsNames.add(ticket.affectedVersions.get(i).getName());
                }
            }

            System.out.println("id: " + ticket.id);
            System.out.println(ticket.key);
            System.out.println("Affected versions: " + affVersionsNames);
            System.out.println("Opening version: " + ticket.openingVersion.getName());
            if (ticket.injectedVersion != null)
                System.out.println("Injected version: " + ticket.injectedVersion.getName());
            else System.out.println("Injected version: " + null);
            System.out.println("Fix version: " + ticket.fixVersion.getName());
            System.out.println('\n');
        }

        CommitRetriever.retrieveCommits();


    }

    public static Ticket getTicket(JSONObject ticketInfo) throws JSONException {

        Ticket ticket = new Ticket();

        ticket.id = ticketInfo.get("id").toString();
        ticket.key = ticketInfo.get("key").toString();


        JSONObject fields = ticketInfo.getJSONObject("fields");

        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        ticket.fixVersion = getRelease(resolutionDate);

        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        ticket.openingVersion = getRelease(creationDate);


        JSONArray versions = fields.getJSONArray("versions");

        if (versions.isNull(0)) {
            // this ticket does not have the affected versions field
            ticket.affectedVersions = null;
        } else {
            for (int i = 0; i < versions.length(); i++) {
                ticket.affectedVersions.add(getRelease(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay()));
            }
        }

        if (ticket.affectedVersions != null) {

            Release injVersion = ticket.affectedVersions.get(0);

            for (Release affVersion: ticket.affectedVersions) {
                if (affVersion.getId() < injVersion.getId())
                    injVersion = affVersion;
            }

            ticket.injectedVersion = injVersion;
            computeProportion(ticket);
        }

        return ticket;
    }

    public static float getProportionMean() {
        float sum = 0;
        int count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.proportion > 0) {
                sum += ticket.proportion;
                count++;
            }
        }

        return sum/count;
    }


    public static Release getRelease(LocalDateTime date) {

        int i = 0;

        while (releases.get(i).getDate().isBefore(date)) {
           i++;
        }

        return releases.get(i);
    }



    /*
     *   proportion = (fixVersion - injectedVersion)/(fixVersion - openingVersion)
     *
     *   ASSUMPTION: we're considering in proportion's computation only the tickets with fixVersion > openingVersion > injectedVersion, in order to:
     *   - avoid fixVersion = openingVersion -> proportion = infinity
     *   - avoid fixVersion = injectedVersion -> proportion = 0
     *   - discard invalid affectedVersions, i.e. with openingVersion > fixVersion, injectedVersion > fixVersion and injectedVersion > openingVersion
     */


    public static void computeProportion(Ticket ticket) {
        if (ticket.fixVersion.getId() > ticket.openingVersion.getId()
                && ticket.fixVersion.getId() > ticket.injectedVersion.getId()
                && ticket.injectedVersion.getId() < ticket.openingVersion.getId()) {
            ticket.proportion = (float)(ticket.fixVersion.getId() - ticket.injectedVersion.getId())/(ticket.fixVersion.getId() - ticket.openingVersion.getId());
            System.out.println(ticket.proportion);

        }

    }


    public static List<Ticket> getTicketsWithAV() {

        List<Ticket> filteredTickets = filterTickets();

        System.out.println(filteredTickets.size());

        List<Ticket> ticketsWithAV = new ArrayList<>();

        for (Ticket ticket : filteredTickets) {
            if (ticket.fixVersion.getId() != ticket.injectedVersion.getId()) {
                // these tickets have fix version different from the injected version, so they have AV
                ticketsWithAV.add(ticket);
            }
        }

        return ticketsWithAV;
    }

    private static List<Ticket> filterTickets() {
        List<Ticket> filteredList = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.injectedVersion.getId() < Math.round(releases.size()/2))
                filteredList.add(ticket);
        }

        return filteredList;
    }


}

