import model.Class;
import model.Ticket;
import model.TicketCommit;
import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ComputeMetrics {

    /*
     *  This class has the responsibility to compute all metrics for all classes
     *  The metrics here considered are:
     *  - buggyness
     *  - size
     *  - LOC touched
     *  - NAuth
     *  - NFix
     *  - NR
     *  - LOC added
     *  - max LOC added
     *  - average LOC added
     *  - churn
     *  - changeSetSize
     *
     */

    public boolean isBuggy(Class cls, List<TicketCommit> filteredCommits) {
        boolean isBuggy = false;
        List<RevCommit> assCommits = cls.getAssociatedCommits();
        List<TicketCommit> filteredReleaseCommits = new ArrayList<>();

        System.out.println(cls.getName());

        System.out.println("filteredCommits initial size: " + filteredCommits.size());

        for (RevCommit associatedCommit : assCommits) {
            System.out.println(associatedCommit.getShortMessage());
        }

        filteredCommits = commitIntersection(filteredCommits, assCommits);

        System.out.println("filteredCommits size after intersection: " + filteredCommits.size());

        for (TicketCommit ticketCommit : filteredCommits) {
            if (TicketRetriever.getRelease(ticketCommit.getCommit().getAuthorIdent().getWhen().toInstant().atZone(ticketCommit.getCommit().getAuthorIdent().getZoneId()).toLocalDateTime()).getId() == cls.getRelease().getId())
                filteredReleaseCommits.add(ticketCommit);
        }

        System.out.println("filteredCommits size after release filtering: " + filteredReleaseCommits.size() + "\n");

        if (filteredReleaseCommits.isEmpty())
            return false;

        for (TicketCommit ticketCommit : filteredReleaseCommits) {
            LocalDateTime commitDate = ticketCommit.getCommit().getAuthorIdent().getWhen().toInstant().atZone(ticketCommit.getCommit().getAuthorIdent().getZoneId()).toLocalDateTime();

            if (TicketRetriever.getRelease(commitDate).getId() == ticketCommit.getTicket().fixVersion.getId())
                isBuggy = false;
            else if (TicketRetriever.getRelease(commitDate).getId() < ticketCommit.getTicket().fixVersion.getId()) {
                isBuggy = true;
            }
        }

        return isBuggy;
    }

    private List<TicketCommit> commitIntersection(List<TicketCommit> list1, List<RevCommit> list2) {
        List<TicketCommit> intersection = new ArrayList<>();

        for (TicketCommit ticketCommit : list1) {
            if (list2.contains(ticketCommit.getCommit()) && !intersection.contains(ticketCommit.getCommit()))
                intersection.add(ticketCommit);
        }

        return intersection;
    }
}
