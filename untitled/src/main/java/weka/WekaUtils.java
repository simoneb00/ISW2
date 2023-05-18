package weka;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.RenameNominalValues;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WekaUtils {

    public File CSVToARFF(File CSVFile) {

        System.out.println("path: " + CSVFile.getAbsolutePath());
        String arffFileName = CSVFile.getPath().substring(0, CSVFile.getPath().length() - 4) + ".arff";
        File arff = new File(arffFileName);

        try {

            CSVLoader loader = new CSVLoader();
            loader.setSource(CSVFile);
            Instances data = loader.getDataSet();

            ArffSaver saver = new ArffSaver();
            saver.setInstances(data);
            saver.setFile(new File(arffFileName));
            saver.writeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return arff;
    }

    public void removeAttribute(File arff) {

        System.out.println("Removing attribute for file " + arff.getAbsolutePath());

        // loading the arff file in order to remove the attribute 'File Name'
        ArffLoader arffLoader = new ArffLoader();
        try {
            arffLoader.setSource(arff);

            Instances arffData = arffLoader.getDataSet();

            String options[] = new String[2];
            options[0] = "-R";
            options[1] = "1, 2";


            Remove remove = new Remove();
            remove.setOptions(options);
            remove.setInputFormat(arffData);

            Instances newData = Filter.useFilter(arffData, remove);

            // saving the new arff file
            ArffSaver saver = new ArffSaver();
            saver.setInstances(newData);
            saver.setFile(arff);
            saver.writeBatch();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void adjustAttributes(File arff) {

        ArffLoader arffLoader = new ArffLoader();

        try {
            arffLoader.setSource(arff);

            Instances arffData = arffLoader.getDataSet();

            int index = arffData.numAttributes() - 1;

            List<String> values = new ArrayList<>();
            values.add(0, "true");
            values.add(1, "false");
            Attribute attribute = new Attribute("Buggy", values);

            arffData.replaceAttributeAt(attribute, index);

            ArffSaver saver = new ArffSaver();
            saver.setInstances(arffData);
            saver.setFile(arff);
            saver.writeBatch();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
