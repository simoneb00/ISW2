package weka;

import exceptions.EmptyARFFException;
import exceptions.ExecutionException;
import model.Class;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrievers.CommitRetriever;
import retrievers.GetReleaseInfo;
import retrievers.TicketRetriever;
import utils.CSV;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class WalkForward {

    private WalkForward() {}

    private static final Logger logger = LoggerFactory.getLogger(WalkForward.class);
    private static final int CSV_FILE = 0;
    private static final int ARFF_FILE = 1;
    private static final int TRAIN_SET = 0;
    private static final int TEST_SET = 1;


    /*
     *  Walk-Forward: for each iteration i = 1, ..., n-1 (if the releases are n), we have a different dataset:
     *  -  training set i : all releases up to i - 1
     *  -  testing set i: release i
     *
     *  In order to create the training set (e.g. releases 1, 2, ..., k), we must use all data available until the date corresponding to the release k.
     *  Instead, in order to create the testing set, we can use all the available data (ASSUMPTION).
     *
     */
    public static List<List<File>> initSets(String projName) throws JSONException, IOException, GitAPIException, ExecutionException, ParseException {
        List<List<File>> files = new ArrayList<>();
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, false);
        List<Class> allClasses;

        // here we retrieve all tickets and classes, in order to create the testing sets for all the iterations
        List<Ticket> allTickets = TicketRetriever.retrieveTickets(projName, releases.size());
        if (!new File(projName + ".csv").exists()) {
            allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size());
            CSV.generateCSV(allClasses, projName, releases.size());
        } else {
            allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size());
        }

        // we have to create different CSV files: for each iteration, starting just with the first release, we add the next release
        for (int i = 2; i <= Math.round((float) releases.size() / 2); i++) {

            /* i must be > 1: we skip the first iteration, as it doesn't have the training set -> inaccurate predictions */

            // training set
            logger.info("Retrieving tickets for the first {} releases", i);

            String filenameCSV = getPath(projName, i, TRAIN_SET, CSV_FILE);

            if (!new File(filenameCSV).exists()) {
                List<Ticket> ticketsForTrainSet = TicketRetriever.retrieveTickets(projName, i - 1);
                List<Class> classesForTrainSet = CommitRetriever.retrieveCommits(projName, ticketsForTrainSet, i - 1);

                CSV.generateCSVForWF(CSV.Type.TRAINING_SET, classesForTrainSet, projName, i);
            }

            // testing set
            List<Class> classesForTestSet = new ArrayList<>();
            for (Class c : allClasses) {
                if (c.getRelease().getId() == i)
                    classesForTestSet.add(c);
            }

            CSV.generateCSVForWF(CSV.Type.TESTING_SET, classesForTestSet, projName, i);

        }

        return files;
    }

    public static void classify(String projName) throws JSONException, ExecutionException, IOException {
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, true);
        Weka weka = new Weka();

        for (int i = 2; i <= releases.size(); i++) {

            String trainSetPath = getPath(projName, i, TRAIN_SET, ARFF_FILE);
            String testSetPath = getPath(projName, i, TEST_SET, ARFF_FILE);

            try {
                weka.classify(trainSetPath, testSetPath, i, projName);
            } catch (EmptyARFFException e) {
                logger.warn("empty arff");
                // ignore, empty ARFF file
            }
        }

        weka.generateFiles();
    }

    private static String getPath(String projName, int iteration, int setType, int fileType) {
        switch (setType) {
            case TRAIN_SET:
                switch (fileType) {
                    case CSV_FILE:
                        return projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.csv";
                    case ARFF_FILE:
                        return projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.arff";
                    default:
                        return null;
                }
            case TEST_SET:
                switch (fileType) {
                    case CSV_FILE:
                        return projName + "_" + iteration + "/" + projName + "_" + iteration + "_testing-set.csv";
                    case ARFF_FILE:
                        return projName + "_" + iteration + "/" + projName + "_" + iteration + "_testing-set.arff";
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

}
