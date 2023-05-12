package utils;

import model.Class;
import model.Release;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSV {
    private static String FIRST_ROW = "Version, File Name, LOC, NAuth, Fan-Out, Revisions, LOCAdded, MaxLOCAdded, averageLOCAdded, Churn, MaxChurn, AverageChurn, Time Span (days), Buggy";

    public static void generateCSV(List<Class> classes, String projName, int numVersions) throws IOException {
        System.out.println("Generating the CSV file for " + projName + ", numVersions = " + numVersions);
        File file = new File(projName + ".csv");
        PopulateFile(classes, file);
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

        }

        finally {
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

    public static List<File> generateCSVForWF(List<Class> classesForTrainSet, List<Class> classesForTestSet, String projName, int iteration) throws IOException {

        // outputFiles[0] = training set file, outputFiles[1] = testing set file
        List<File> outputFiles = new ArrayList<>();

        if (!classesForTrainSet.isEmpty()) {
            File trainFile = new File("/home/simoneb/ISW2/" + projName + "_" + iteration + "/" + projName + "_" + iteration + "_training-set.csv");
            if (trainFile.getParentFile().mkdirs())
                System.out.println("Training directory created");
            else
                System.out.println("Training directory cannot be created");
            trainFile.createNewFile();
            PopulateFile(classesForTrainSet, trainFile);
            outputFiles.add(0, trainFile);
        }

        File testFile = new File("/home/simoneb/ISW2/" + projName + "_" + iteration + "/" + projName + "_" +  iteration + "_testing-set.csv");
        if (testFile.getParentFile().mkdirs()) {
            System.out.println("Testing directory created");
        }
        else {
            System.out.println("Testing directory cannot be created");
        }
        testFile.createNewFile();
        outputFiles.add(testFile);

        PopulateFile(classesForTestSet, testFile);

        return outputFiles;
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
        }
        finally {
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
