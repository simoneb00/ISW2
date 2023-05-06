package utils;

import model.Class;
import model.Release;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSV {
    private static String FIRST_ROW = "Version, File Name, LOC, NAuth, NFix, Revisions, LOCAdded, MaxLOCAdded, averageLOCAdded, Churn, MaxChurn, AverageChurn, Age, Buggy";

    public static void generateCSV(List<Class> classes, String projName) throws IOException {

        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(projName + ".csv");
            fileWriter.append(FIRST_ROW);
            fileWriter.append("\n");

            for (Class c : classes) {
                String row = c.getRelease().getId() + 1 + ", " +
                        c.getName() + ", " +
                        c.getSize() + ", " +
                        c.getNAuth() + ", " +
                        c.getNFix() + ", " +
                        c.getNR() + ", " +
                        c.getLOCAdded() + ", " +
                        c.getMaxLOCAdded() + ", " +
                        c.getAverageLOCAdded() + ", " +
                        c.getChurn() + ", " +
                        c.getMaxChurn() + ", " +
                        c.getAverageChurn() + ", " +
                        c.getAge() + ", " +
                        c.isBuggy();

                fileWriter.append(row);
                fileWriter.append("\n");
            }

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
