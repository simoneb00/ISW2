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
import java.util.ArrayList;
import java.util.List;

public class WalkForward {

    private static final Logger logger = LoggerFactory.getLogger(WalkForward.class);

    /*
     *  Walk-Forward: for each iteration i = 1, ..., n-1 (if the releases are n), we have a different dataset:
     *  -  training set i : all releases up to i - 1;
     *  -  testing set i: release i;
     *
     *  In order to create the training set (e.g. releases 1, 2, ..., k), we must use all data available until the date corresponding to the release k.
     *  Instead, in order to create the testing set, we can use all the available data (ASSUMPTION).
     *
     */
    public static List<List<File>> initSets(String projName) throws JSONException, IOException, GitAPIException, ExecutionException {
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

            String filenameCSV = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_training-set.csv";

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

    public static void classify(String projName) throws Exception {
        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, true, 0, true);
        Weka weka = new Weka();

        for (int i = 2; i <= releases.size(); i++) {

            String trainSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_training-set.arff";
            String testSetPath = "/home/simoneb/ISW2/" + projName + "_" + i + "/" + projName + "_" + i + "_testing-set.arff";

            try {
                weka.classify(trainSetPath, testSetPath, i, projName);
            } catch (EmptyARFFException e) {
                logger.warn("empty arff");
                // ignore, empty ARFF file
            }
        }

        weka.generateFiles();
    }

    /*
    private static void getDominantClassifier(List<EvaluationReport> reports) {
        EvaluationReportUtils evaluationReportUtils = new EvaluationReportUtils();
        List<EvaluationReport> meanReports = new ArrayList<>();

        List<EvaluationReport> reportsWithoutFS = evaluationReportUtils.getReportsWithoutFS(reports);

        try {

            // evaluating reports with no FS
            List<List<EvaluationReport>> reportsDividedByClassifierNoFS = evaluationReportUtils.divideReportsByClassifier(reportsWithoutFS);

            for (List<EvaluationReport> list : reportsDividedByClassifierNoFS) {
                meanReports.add(evaluationReportUtils.getMeanValuesForClassifier(list));
            }

            // evaluating reports with FS
            List<EvaluationReport> reportsWithFS = evaluationReportUtils.getReportsWithFS(reports);

            List<List<EvaluationReport>> divReportsWithFS = evaluationReportUtils.divideReportsBySearchMethod(reportsWithFS);
            for (List<EvaluationReport> list : divReportsWithFS) {
                List<List<EvaluationReport>> reportsDividedByClassifierFS = evaluationReportUtils.divideReportsByClassifier(list);
                for (List<EvaluationReport> listDivByClassifier : reportsDividedByClassifierFS) {
                    meanReports.add(evaluationReportUtils.getMeanValuesForClassifier(listDivByClassifier));
                }
            }

            // evaluating reports with FS and sampling
            List<EvaluationReport> reportsWithSampling = evaluationReportUtils.getReportsWithSampling(reports);
            List<List<EvaluationReport>> reportsDividedByClassifierSampling = evaluationReportUtils.divideReportsByClassifier(reportsWithSampling);
            for (List<EvaluationReport> list : reportsDividedByClassifierSampling) {
                meanReports.add(evaluationReportUtils.getMeanValuesForClassifier(list));
            }

            for (EvaluationReport report : meanReports) {
                if (report.isFeatureSelection()) {
                    if (report.getSamplingMethod() == null)
                        System.out.println("\nEvaluation for classifier " + report.getClassifier().toString().toLowerCase() + " with FS search method " + report.getFsSearchMethod().toString().toLowerCase());
                    else
                        System.out.println("\nEvaluation for classifier " + report.getClassifier().toString().toLowerCase() + " with FS search method " + report.getFsSearchMethod().toString().toLowerCase() + "and with " + report.getSamplingMethod().toString().toLowerCase());

                }
                else
                    System.out.println("\nEvaluation for classifier " + report.getClassifier().toString().toLowerCase() + " without feature selection");

                System.out.println("Mean precision = " + report.getPrecision());
                System.out.println("Mean recall = " + report.getRecall());
                System.out.println("Mean AUC = " + report.getAuc());
                System.out.println("Mean kappa = " + report.getKappa());
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     */
}
