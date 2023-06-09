package utils;

import exceptions.ExecutionException;
import model.Class;

import java.io.*;
import java.util.List;

public class ARFF {

    public static void generateARFF(String filename, List<Class> classes) throws ExecutionException {
        File file = new File(filename);


        try (FileWriter fileWriter = new FileWriter(file)) {

            fileWriter.append(
                    "@relation " + filename + "\n" +
                            "\n" +
                            "@attribute ' LOC' numeric\n" +
                            "@attribute ' NAuth' numeric\n" +
                            "@attribute ' Fan-Out' numeric\n" +
                            "@attribute ' Revisions' numeric\n" +
                            "@attribute ' LOCAdded' numeric\n" +
                            "@attribute ' MaxLOCAdded' numeric\n" +
                            "@attribute ' averageLOCAdded' numeric\n" +
                            "@attribute ' Churn' numeric\n" +
                            "@attribute ' MaxChurn' numeric\n" +
                            "@attribute ' AverageChurn' numeric\n" +
                            "@attribute ' Time Span (days)' numeric\n" +
                            "@attribute Buggy {'true','false'}\n\n" +
                            "@data\n"
            );

            for (Class c : classes) {
                fileWriter.append(
                        c.getSize() + "," +
                        c.getnAuth() + "," +
                        c.getFanOut() + "," +
                        c.getnR() + "," +
                        c.getLocAdded() + "," +
                        c.getMaxLOCAdded() + "," +
                        c.getAverageLOCAdded() + "," +
                        c.getChurn() + "," +
                        c.getMaxChurn() + "," +
                        c.getAverageChurn() + "," +
                        c.getTimeSpan() + "," +
                        c.isBuggy()
                );

                fileWriter.append("\n");
            }

        } catch (IOException e) {
            throw new ExecutionException(e);
        }
    }

    public int countNumOccurrences(File arff, String word) throws ExecutionException {
        String line;
        int count = 0;

        try (
            FileReader fileReader = new FileReader(arff);
            BufferedReader bufferedReader = new BufferedReader(fileReader)
            ) {

            while ((line = bufferedReader.readLine()) != null) {
                String[] words = line.split(",");
                for (String w : words) {
                    if (w.equals(word))
                        count++;
                }
            }

        } catch (IOException e) {
            throw new ExecutionException(e);
        }

        return count;
    }
}
