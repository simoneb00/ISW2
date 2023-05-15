import exceptions.EmptyARFFException;
import model.Class;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import utils.CSV;
import weka.Weka;
import weka.WekaUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WalkForward {

    /*
     *  Walk-Forward: for each iteration i = 1, ..., n-1 (if the releases are n), we have a different dataset:
     *  -  training set i : all releases up to i - 1;
     *  -  testing set i: release i;
     *
     *  In order to create the training set (e.g. releases 1, 2, ..., k), we must use all data available until the date corresponding to the release k.
     *  Instead, in order to create the testing set, we can use all the available data (ASSUMPTION).
     *
     */
    public static List<List<File>> initSets(String projName) throws JSONException, IOException, GitAPIException {
        List<List<File>> files = new ArrayList<>();
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, false);
        WekaUtils wekaUtils = new WekaUtils();
        // releases splitting
        //releases = releases.subList(0, Math.round((float)releases.size() / 2));

        // here we retrieve all tickets and classes, in order to create the testing sets for all the iterations
        List<Ticket> allTickets = TicketRetriever.retrieveTickets(projName, releases.size());
        List<Class> allClasses = CommitRetriever.retrieveCommits(projName, allTickets, releases.size());
        CSV.generateCSV(allClasses, projName, releases.size());

        // we have to create different CSV files: for each iteration, starting just with the first release, we add the next release
        for (int i = 1; i <= Math.round((float)releases.size() / 2); i++) {

            /* i must be > 1: we skip the first iteration, as it doesn't have the training set -> inaccurate predictions */
            if (i > 1) {

                // training set
                System.out.println("\n\nRetrieving tickets for the first " + i + " releases");
                List<Ticket> ticketsForTrainSet = TicketRetriever.retrieveTickets(projName, i - 1);
                List<Class> classesForTrainSet = CommitRetriever.retrieveCommits(projName, ticketsForTrainSet, i - 1);

                File trainFile = CSV.generateCSVForWF(CSV.Type.TRAINING_SET, classesForTrainSet, projName, i);

                // testing set
                List<Class> classesForTestSet = new ArrayList<>();
                for (Class c : allClasses) {
                    if (c.getRelease().getId() == i)
                        classesForTestSet.add(c);
                }

                File testFile = CSV.generateCSVForWF(CSV.Type.TESTING_SET, classesForTestSet, projName, i);

                File trainArff = wekaUtils.CSVToARFF(trainFile);
                File testArff = wekaUtils.CSVToARFF(testFile);

                wekaUtils.removeAttribute(trainArff);
                wekaUtils.removeAttribute(testArff);

                /*
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

                    File trainArff = wekaUtils.CSVToARFF(trainFile);
                    File testArff = wekaUtils.CSVToARFF(testFile);

                    wekaUtils.removeAttribute(trainArff);
                    wekaUtils.removeAttribute(testArff);

                    files.add(outputFiles);

                } else {
                    List<File> outputFiles = CSV.generateCSVForWF(Collections.emptyList(), allClasses, projName, i);
                    File testFile = outputFiles.get(0);
                    wekaUtils.CSVToARFF(testFile);
                }

                 */
            }
        }

        return files;
    }

    public static void classify(String projName) throws JSONException, IOException {
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, true);
        Weka weka = new Weka();

        for (int i = 2; i <= releases.size(); i++) {

            String trainSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_training-set.arff";
            String testSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_testing-set.arff";

            try {
                weka.classify(trainSetPath, testSetPath);
            } catch (EmptyARFFException e) {
                System.out.println("empty arff");
                // ignore, empty ARFF file
            }
        }
    }
}
