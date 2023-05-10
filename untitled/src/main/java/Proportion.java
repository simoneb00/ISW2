import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import exceptions.InvalidTicketException;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import utils.CSV;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


    private static List<String> allProjects = Arrays.asList(
            "AVRO",
            "OPENJPA",
            "STORM",
            "ZOOKEEPER",
            "SYNCOPE",
            "TAJO",
            "BOOKKEEPER",
            "ACCUMULO",
            "KAFKA"
    );

    public static void coldStartProportion(ArrayList<Ticket> tickets, String projName) throws JSONException, IOException {

        ArrayList<Float> proportionValues = new ArrayList<>();

        FileWriter fileWriter = null;
        Path proportionFile = Paths.get("Proportion" + projName + ".csv");

        if (!Files.exists(proportionFile)) {
            try {
                System.out.println("Proportion.csv does not exist for " + projName);
                fileWriter = new FileWriter("Proportion" + projName + ".csv");

                fileWriter.append("Project, Proportion Value");
                fileWriter.append("\n");

                for (String project : allProjects) {
                    if (!project.equals(projName)) {
                        fileWriter.append(project).append(", ").append(String.valueOf(getProportionForProject(project)));
                        fileWriter.append("\n");
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fileWriter.close();
            }
        }


        proportionValues = readProportionFile(projName);

        Collections.sort(proportionValues);

        System.out.println("Proportion values: " + proportionValues);

        System.out.println(median(proportionValues));

        int proportionValue = Math.round(median(proportionValues));

        System.out.println("Proportion value: " + proportionValue);

        for (Ticket ticket : tickets) {
            if (ticket.proportion == 0) {
                ticket.injectedVersion = TicketRetriever.releases.get(
                        Math.max(0, Math.round((float) ticket.fixVersion.getId() - (ticket.fixVersion.getId() - ticket.openingVersion.getId()) * proportionValue) - 1)
                );
            }
        }
    }

    private static ArrayList<Float> readProportionFile(String projName) throws IOException {

        CSVReader csvReader = null;
        ArrayList<Float> proportionValues = new ArrayList<>();

        try {

            csvReader = new CSVReader(new FileReader("Proportion" + projName + ".csv"));
            List<String[]> r = csvReader.readAll();
            for (String[] row : r) {
                for (String proj : allProjects) {
                    if (Arrays.stream(row).toArray()[0].toString().equals(proj)) {
                        proportionValues.add(Float.parseFloat(Arrays.stream(row).toArray()[1].toString().substring(1)));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CsvException e) {
            throw new RuntimeException(e);
        } finally {
            csvReader.close();
        }

        return proportionValues;
    }

    private static float median(List<Float> list) {
        float sum;
        if (list.size() % 2 == 0) {
            sum = (float) list.get(list.size() / 2 - 1) + (float) list.get(list.size() / 2);
            return sum / 2;
        } else {
            return list.get(list.size() / 2);
        }
    }

    /*
     *  ASSUMPTION: we consider only tickets with consistent AV, i.e. with IV (earliest release in AV) <= OV. In the case of FV = OV, in order to avoid denominator going to infinity, we assume FV - AV = 1
     */
    public static void computeProportion(Ticket ticket) {
        if (ticket.injectedVersion.getId() <= ticket.openingVersion.getId() && ticket.fixVersion.getId() == ticket.openingVersion.getId())
            ticket.proportion = ticket.fixVersion.getId() - ticket.injectedVersion.getId();
        else if (ticket.injectedVersion.getId() <= ticket.openingVersion.getId() && ticket.openingVersion.getId() < ticket.fixVersion.getId()) {
            ticket.proportion = (float) (ticket.fixVersion.getId() - ticket.injectedVersion.getId()) / (ticket.fixVersion.getId() - ticket.openingVersion.getId());
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

        System.out.println("Tickets having proportion (project " + projName + "):" + count + " over " + tickets.size() + " tickets");
        System.out.println("proportion mean (project " + projName + "):" + TicketRetriever.getProportionMean(tickets));

        return TicketRetriever.getProportionMean(tickets);

    }
}
