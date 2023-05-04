import model.Ticket;

import java.util.ArrayList;

public class Proportion {

    /*
    *
    *   here a hybrid technique between cold start and increment is used.
    *   In particular, cold start is used when the number of previous (usable) tickets is less or equal to 5,
    *   otherwise increment is used.
    *
     */

    // Predicted IV = FV - (FV - OV) * P

    public static void estimateInjectedVersion(ArrayList<Ticket> tickets, float proportion) {

        for (Ticket ticket : tickets) {

            if (ticket.injectedVersion == null) {

                System.out.println("----------------- computing proportion for ticket " + ticket.key + "--------------------");

                ArrayList<Ticket> previousTickets = new ArrayList<>();

                for (Ticket t : tickets) {
                    if (t.fixVersion.getId() < ticket.fixVersion.getId() && t.proportion != 0) {
                        previousTickets.add(t);
                    }
                }

                System.out.println(previousTickets.size());

                if (previousTickets.size() < 5) {
                    coldStartProportion(ticket);
                } else {
                    incrementProportion(previousTickets, ticket);
                }

                //ticket.injectedVersion = TicketRetriever.releases.get(
                   //     Math.max(0, Math.round(ticket.fixVersion.getId() - (ticket.fixVersion.getId() - ticket.openingVersion.getId()) * proportion)));
            }
        }
    }

    private static void incrementProportion(ArrayList<Ticket> previousTickets, Ticket ticket) {

        System.out.println("---------------- increment for ticket " + ticket.key + "-------------------------");

        float p = 0;

        for (Ticket pTicket: previousTickets) {
            p += pTicket.proportion;
            System.out.println(pTicket.proportion);
        }

        p = p / previousTickets.size();

        System.out.println(p);

        ticket.injectedVersion = TicketRetriever.releases.get(
                Math.max(0, Math.round(ticket.fixVersion.getId() - (ticket.fixVersion.getId() - ticket.openingVersion.getId()) * p)));
    }


    private static void coldStartProportion(Ticket ticket) {

        System.out.println("---------------- cold start for ticket " + ticket.key + "-------------------------");

        /* TODO find a significant value for proportion */
        int proportion = 2;

        ticket.injectedVersion = TicketRetriever.releases.get(
                Math.max(0, Math.round(ticket.fixVersion.getId() - (ticket.fixVersion.getId() - ticket.openingVersion.getId()) * proportion)));

    }
}
