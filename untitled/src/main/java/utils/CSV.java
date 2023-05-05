package utils;

import model.Class;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSV {

    private static String projName = "BOOKKEEPER";
    private static String FIRST_ROW = "Version, File Name, LOC, NAuth, NFix, Revisions, LOCAdded, MaxLOCAdded, averageLOCAdded, Churn, MaxChurn, AverageChurn, Age, Buggy";

    public static void generateCSV(List<Class> classes) throws IOException {

        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(projName + ".csv");
            fileWriter.append(FIRST_ROW);
            fileWriter.append("\n");

            for (Class c : classes) {
                String row = c.getRelease().getId() + ", " +
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
}
