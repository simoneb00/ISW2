package utils;

import model.Class;
import model.EvaluationReport;
import model.Release;
import weka.Weka;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSV {

    public enum Type {
        TRAINING_SET,
        TESTING_SET
    }

    private static final String FIRST_ROW = "Version, File Name, LOC, NAuth, Fan-Out, Revisions, LOCAdded, MaxLOCAdded, averageLOCAdded, Churn, MaxChurn, AverageChurn, Time Span (days), Buggy";

    public static void generateCSV(List<Class> classes, String projName, int numVersions) throws IOException {
        System.out.println("Generating the CSV file for " + projName + ", numVersions = " + numVersions);
        File file = new File(projName + ".csv");
        PopulateFile(classes, file);
        System.out.println("CSV generated for " + projName + ", numVersions = " + numVersions);
    }

    private static void PopulateFile(List<Class> classes, File file) throws IOException {

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            fileWriter = new FileWriter(file);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append(FIRST_ROW);
            bufferedWriter.append("\n");

            for (Class c : classes) {
                String row = c.getRelease().getId() + ", " +
                        c.getName() + ", " +
                        c.getSize() + ", " +
                        c.getNAuth() + ", " +
                        c.getFanOut() + ", " +
                        c.getNR() + ", " +
                        c.getLOCAdded() + ", " +
                        c.getMaxLOCAdded() + ", " +
                        c.getAverageLOCAdded() + ", " +
                        c.getChurn() + ", " +
                        c.getMaxChurn() + ", " +
                        c.getAverageChurn() + ", " +
                        c.getTimeSpan() + ", " +
                        c.isBuggy();

                bufferedWriter.append(row);
                bufferedWriter.append("\n");
            }

        } finally {
            try {
                fileWriter.flush();
                bufferedWriter.flush();
                fileWriter.close();
                bufferedWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }

    }

    public static File generateCSVForWF(Type type, List<Class> classes, String projName, int iteration) throws IOException {
        if (type == Type.TRAINING_SET) {
            String filenameCSV = "/home/simoneb/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.csv";
            String filenameARFF = "/home/simoneb/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.arff";
            File trainFile = new File(filenameCSV);
            trainFile.getParentFile().mkdirs();
            trainFile.createNewFile();
            PopulateFile(classes, trainFile);
            ARFF.generateARFF(filenameARFF, classes);
            return trainFile;
        } else if (type == Type.TESTING_SET) {
            String filenameCSV = "/home/simoneb/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_testing-set.csv";
            String filenameARFF = "/home/simoneb/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_testing-set.arff";
            File testFile = new File(filenameCSV);
            testFile.getParentFile().mkdirs();
            testFile.createNewFile();
            PopulateFile(classes, testFile);
            ARFF.generateARFF(filenameARFF, classes);
            return testFile;
        }

        return null;
    }

    public static void generateCSVForReports(List<EvaluationReport> reports) {
        FileWriter fileWriterNoFS = null;
        EvaluationReportUtils evaluationReportUtils = new EvaluationReportUtils();

        try {
            fileWriterNoFS = new FileWriter(new File(reports.get(0).getDataset() + "-report-withoutFS.csv"));
            fileWriterNoFS.append("Dataset, Iteration, Classifier, Precision, Recall, AUC, Kappa");
            fileWriterNoFS.append("\n");

            List<EvaluationReport> reportsWithFS = evaluationReportUtils.getReportsWithFS(reports);
            List<List<EvaluationReport>> partitionedReports = evaluationReportUtils.divideReportsBySearchMethod(reportsWithFS);

            for (List<EvaluationReport> reportsList : partitionedReports) {
                generateCSVForReportsWithFS(reportsList);
            }

            for (EvaluationReport report : reports) {
                if (!report.isFeatureSelection()) {
                    String row = report.getDataset().substring(0, report.getDataset().length() - 3).toLowerCase() + ", "
                            + report.getIteration() + ", "
                            + report.getClassifier().toString().toLowerCase() + ", "
                            + report.getPrecision() + ", "
                            + report.getRecall() + ", "
                            + report.getAUC() + ", "
                            + report.getKappa();

                    fileWriterNoFS.append(row);
                    fileWriterNoFS.append("\n");
                }
            }

        } catch (
                IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                fileWriterNoFS.flush();
                fileWriterNoFS.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }

    }

    private static void generateCSVForReportsWithFS(List<EvaluationReport> reports) {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(new File(reports.get(0).getDataset() + "-report-withFS-" + reports.get(0).getFSSearchMethod().toString().toLowerCase() + ".csv"));

            fileWriter.append("Dataset, Iteration, Classifier, Precision, Recall, AUC, Kappa, Search Method").append("\n");

            for (EvaluationReport report : reports) {
                fileWriter.append(report.getDataset().substring(0, report.getDataset().length() - 3).toLowerCase())
                        .append(", ").append(String.valueOf(report.getIteration()))
                        .append(", ").append(report.getClassifier().toString().toLowerCase())
                        .append(", ").append(String.valueOf(report.getPrecision()))
                        .append(", ").append(String.valueOf(report.getRecall()))
                        .append(", ").append(String.valueOf(report.getAUC()))
                        .append(", ").append(String.valueOf(report.getKappa()))
                        .append(", ").append(String.valueOf(report.getFSSearchMethod()))
                        .append("\n");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void generateCSVForVersions(List<Release> releases, String projName) {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(projName + "VersionInfo.csv");
            fileWriter.append("Index, Version Name, Date");
            fileWriter.append("\n");

            for (Release release : releases) {
                fileWriter.append(
                        release.getId() + ", " +
                                release.getName() + ", " +
                                release.getDate() + ", "
                );
                fileWriter.append("\n");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }
}
