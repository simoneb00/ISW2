package retrievers;

import exceptions.ExecutionException;
import exceptions.InvalidTicketException;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import proportion.Proportion;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static utils.JSON.readJsonFromUrl;

public class TicketRetriever {


    public static ArrayList<Ticket> retrieveTickets(String projName, int numReleases) throws JSONException, IOException, ExecutionException {
        int startAt = 0;

        ArrayList<Ticket> tickets = new ArrayList<>();

        JSONObject json;

        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, false, numReleases, false);

        LocalDateTime lastDate = releases.get(releases.size() - 1).getDate();

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
            if (ticket.getProportion() != 0) {
                count++;
            }
        }

        System.out.println("Tickets having proportion (project " + projName + "):" + count + " over " + tickets.size() + " tickets");

        Proportion.coldStartProportion(tickets, projName);

        count = 0;

        System.out.println("Tickets having injected version: " + count);

        return tickets;

    }

    public static Ticket getTicket(JSONObject ticketInfo, List<Release> rels, LocalDateTime lastDate) throws JSONException, InvalidTicketException, IOException {

        Ticket ticket = new Ticket();

        JSONObject fields = ticketInfo.getJSONObject("fields");
        if (!fields.has("resolutiondate")) {
            throw new InvalidTicketException();
        }

        LocalDateTime creationDate = LocalDateTime.parse(fields.get("created").toString().substring(0, 21));

        LocalDateTime resolutionDate = LocalDateTime.parse(fields.get("resolutiondate").toString().substring(0, 21));


        if (resolutionDate.isAfter(rels.get(rels.size() - 1).getDate())) {
            /* this ticket has been resolved in a release that hasn't been released yet, so we do not know the fix version */
            ticket.setFixVersion(null);
        } else {
            ticket.setFixVersion(getRelease(resolutionDate));
        }

        ticket.setId(ticketInfo.get("id").toString());
        ticket.setKey(ticketInfo.get("key").toString());

        // walk-forward
        if (creationDate.isAfter(lastDate)) {
            throw new InvalidTicketException();
        }

        ticket.setOpeningVersion(getRelease(creationDate));

        /* we're taking the 'component' value in order to set the classes buggyness when the tickets have not associated commits (walk forward) */
        JSONArray components = fields.getJSONArray("components");

        for (int i = 0; i < components.length(); i++) {
            ticket.getAffectedComponents().add(components.getJSONObject(i).get("name").toString());
        }

        JSONArray versions = fields.getJSONArray("versions");

        if (versions.isNull(0)) {
            // this ticket does not have the affected versions field
            ticket.setAffectedVersions(null);
        } else {
            for (int i = 0; i < versions.length(); i++) {
                if (versions.getJSONObject(i).has("releaseDate"))
                    ticket.getAffectedVersions().add(getRelease(LocalDate.parse(versions.getJSONObject(i).get("releaseDate").toString()).atStartOfDay()));
            }
        }

        if (ticket.getAffectedVersions() != null && !ticket.getAffectedVersions().isEmpty()) {

            // ASSUMPTION: we take as IV the earliest AV (i.e., the release in AV with the lowest id)
            Release injVersion = ticket.getAffectedVersions().get(0);

            for (Release affVersion : ticket.getAffectedVersions()) {
                if (affVersion.getId() < injVersion.getId())
                    injVersion = affVersion;
            }

            /* the code below avoids the cases (e.g. ticket BOOKKEEPER-313) in which the affected versions are all subsequents to the opening version */
            if (injVersion.getId() <= ticket.getOpeningVersion().getId()) {
                ticket.setInjectedVersion(injVersion);
                Proportion.computeProportion(ticket);
            }
        }

        return ticket;
    }

    public static float getProportionMean(List<Ticket> tickets) {
        float sum = 0;
        int count = 0;
        for (Ticket ticket : tickets) {
            if (ticket.getProportion() > 0) {
                sum += ticket.getProportion();
                count++;
            }
        }

        return sum / count;
    }


    public static Release getRelease(LocalDateTime date) {

        int i = 0;
        List<Release> rels = GetReleaseInfo.getReleases();

        while (rels.get(i).getDate().isBefore(date)) {
            i++;
        }

        return rels.get(i);
    }


    public static List<Ticket> getTicketsWithAV(List<Ticket> tickets) {

        //List<Ticket> filteredTickets = filterTickets(tickets, releases);
        List<Ticket> ticketsWithAV = new ArrayList<>();

        //for (Ticket ticket : filteredTickets) {
        for (Ticket ticket : tickets) {
            if (ticket.getFixVersion() == null) {
                ticketsWithAV.add(ticket);
                continue;
            }
            if (ticket.getFixVersion().getId() > ticket.getInjectedVersion().getId()) {
                // these tickets have fix version different from the injected version, so they have AV
                ticketsWithAV.add(ticket);
            }

        }

        return ticketsWithAV;
    }

    private static List<Ticket> filterTickets(List<Ticket> tickets, List<Release> releases) {
        List<Ticket> filteredList = new ArrayList<>();

        for (Ticket ticket : tickets) {
            //if (!ticket.injectedVersion.getDate().isAfter(releases.get(releases.size() - 1).getDate()))
            if (!ticket.getOpeningVersion().getDate().isAfter(releases.get(releases.size() - 1).getDate()))
                filteredList.add(ticket);

        }

        return filteredList;
    }

}

