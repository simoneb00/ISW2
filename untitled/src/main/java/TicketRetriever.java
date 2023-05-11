import exceptions.InvalidTicketException;
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


    public static ArrayList<Ticket> retrieveTickets(String projName, int numReleases) throws JSONException, IOException {
        int startAt = 0;

        ArrayList<Ticket> tickets = new ArrayList<>();

        JSONObject json;

        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, false, numReleases, false);

        LocalDateTime lastDate = releases.get(releases.size()-1).getDate();

        System.out.println("Retrieving tickets for project " + projName);

        do {
            String query = "search?jql=project=" + projName + "+and+type=bug+and+(status=closed+or+status=resolved)+and+resolution=fixed&maxResults=1000&startAt=" + startAt;
            String url = "https://issues.apache.org/jira/rest/api/2/" + query;

            json = readJsonFromUrl(url);
            JSONArray issues = json.getJSONArray("issues");

            for (int i = 0; i < issues.length(); i++) {
                try {
                    tickets.add(getTicket(issues.getJSONObject(i), releases, lastDate));
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
        System.out.println("proportion mean (project " + projName + "):" + getProportionMean(tickets));


        Proportion.coldStartProportion(tickets, projName);

        return tickets;

    }

    public static Ticket getTicket(JSONObject ticketInfo, List<Release> rels, LocalDateTime lastDate) throws JSONException, InvalidTicketException, IOException {

        Ticket ticket = new Ticket();

        JSONObject fields = ticketInfo.getJSONObject("fields");
        if (!fields.has("resolutiondate")) {
            throw new InvalidTicketException();
        }

        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));
        if (resolutionDate.isAfter(rels.get(rels.size() - 1).getDate())) {
            /* this ticket has been resolved in a release that hasn't been released yet, so it is ignored */
            throw new InvalidTicketException();
        }


        ticket.id = ticketInfo.get("id").toString();
        ticket.key = ticketInfo.get("key").toString();


        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));
        ticket.openingVersion = getRelease(creationDate, rels);

        // walk-forward
        if (creationDate.isAfter(lastDate)) {
            System.out.println("issue found for ticket " + ticket.key + "; creation date = " + creationDate + " vs " + lastDate);
            throw new InvalidTicketException();
        }

        ticket.fixVersion = getRelease(resolutionDate, rels);


        JSONArray versions = fields.getJSONArray("versions");

        if (versions.isNull(0)) {
            // this ticket does not have the affected versions field
            ticket.affectedVersions = null;
        } else {
            for (int i = 0; i < versions.length(); i++) {
                if (versions.getJSONObject(i).has("releaseDate"))
                    ticket.affectedVersions.add(getRelease(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay(), rels));
            }
        }

        if (ticket.affectedVersions != null && !ticket.affectedVersions.isEmpty()) {

            // ASSUMPTION: we take as IV the earliest AV (i.e., the release in AV with the lowest id)
            Release injVersion = ticket.affectedVersions.get(0);

            for (Release affVersion: ticket.affectedVersions) {
                if (affVersion.getId() < injVersion.getId())
                    injVersion = affVersion;
            }

            ticket.injectedVersion = injVersion;
            Proportion.computeProportion(ticket);
        }

        return ticket;
    }

    public static float getProportionMean(List<Ticket> tickets) {
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


    public static Release getRelease(LocalDateTime date, List<Release> rels) throws JSONException, IOException {

        int i = 0;

        if (rels.size() == 1)
            return rels.get(0);

        while (i < rels.size() - 1 && rels.get(i).getDate().isBefore(date)) {
           i++;
        }

        return rels.get(i);
    }



    public static List<Ticket> getTicketsWithAV(List<Ticket> tickets, List<Release> releases) {

        List<Ticket> filteredTickets = filterTickets(tickets, releases);
        List<Ticket> ticketsWithAV = new ArrayList<>();

        for (Ticket ticket : filteredTickets) {
            if (ticket.fixVersion.getId() != ticket.injectedVersion.getId()) {
                // these tickets have fix version different from the injected version, so they have AV
                ticketsWithAV.add(ticket);
            }
        }

        return ticketsWithAV;
    }

    private static List<Ticket> filterTickets(List<Ticket> tickets, List<Release> releases) {
        List<Ticket> filteredList = new ArrayList<>();

        for (Ticket ticket : tickets) {
            //if (ticket.injectedVersion.getId() < Math.round(releases.size()/2))
            if (!ticket.injectedVersion.getDate().isAfter(releases.get(releases.size()-1).getDate()))
                filteredList.add(ticket);
        }

        return filteredList;
    }

}

