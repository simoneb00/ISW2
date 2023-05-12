import model.Class;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import utils.CSV;
import weka.WekaUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalkForward {

    public static void initSets(String projName) throws JSONException, IOException, GitAPIException {
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, true);
        WekaUtils wekaUtils = new WekaUtils();
        // releases splitting
        //releases = releases.subList(0, Math.round((float)releases.size() / 2));

        // we have to create different CSV files: for each iteration, starting just with the first release, we add the next release
        for (int i = 1; i <= releases.size(); i++) {
            System.out.println("\n\nRetrieving tickets for the first " + i + " releases");
            List<Ticket> tickets = TicketRetriever.retrieveTickets(projName, i);
            List<Class> allClasses = CommitRetriever.retrieveCommits(projName, tickets, i);

            if (i > 1) {
                List<Class> classesForTrainingSet = new ArrayList<>();
                List<Class> classesForTestingSet = new ArrayList<>();
                for (Class c : allClasses) {
                    if (c.getRelease().getId() < i)
                        classesForTrainingSet.add(c);
                    else
                        classesForTestingSet.add(c);
                }
                List<File> outputFiles = CSV.generateCSVForWF(classesForTrainingSet, classesForTestingSet, projName, i);
                File trainFile = outputFiles.get(0);
                File testFile = outputFiles.get(1);

                wekaUtils.CSVToARFF(trainFile);
                wekaUtils.CSVToARFF(testFile);

            } else {
                List<File> outputFiles =  CSV.generateCSVForWF(Collections.emptyList(), allClasses, projName, i);
                File testFile = outputFiles.get(0);
                wekaUtils.CSVToARFF(testFile);
            }

        }
    }
}
