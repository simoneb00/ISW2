package weka;

import exceptions.EmptyARFFException;
import model.EvaluationReport;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Weka {

    // Random Forest, Naive Bayes, IBk

    public List<EvaluationReport> classify(String ARFFTrainingSet, String ARFFTestingSet, int iteration, String projName) throws IOException, EmptyARFFException {

        List<EvaluationReport> reports = new ArrayList<>();

        File file = new File(ARFFTrainingSet);
        FileReader fileReader = new FileReader(file);
        ArffLoader.ArffReader arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();

        if (arffReader.getStructure().attribute(arffReader.getStructure().numAttributes() - 1).numValues() == 1)
            throw new EmptyARFFException();

        file = new File(ARFFTestingSet);
        fileReader = new FileReader(file);
        arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();

        try {

            // without feature selection

            System.out.println(ARFFTrainingSet);
            System.out.println(ARFFTestingSet);

            DataSource trainSource = new DataSource(ARFFTrainingSet);
            DataSource testSource = new DataSource(ARFFTestingSet);

            Instances trainData = trainSource.getDataSet();
            Instances testData = testSource.getDataSet();

            trainData.setClassIndex(trainData.numAttributes() - 1);
            testData.setClassIndex(trainData.numAttributes() - 1);

            Evaluation eval = new Evaluation(trainData);

            reports.add(NBClassification(trainData, testData, eval, iteration, projName, false));
            reports.add(IBkClassification(trainData, testData, eval, iteration, projName, false));
            reports.add(RFClassification(trainData, testData, eval, iteration, projName, false));


            // feature selection

            weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();
            CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();
            GreedyStepwise search = new GreedyStepwise();
            search.setSearchBackwards(true);

            filter.setEvaluator(cfsSubsetEval);
            filter.setSearch(search);

            filter.setInputFormat(trainData);
            Instances filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

            Instances filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

            reports.add(NBClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true));
            reports.add(IBkClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true));
            reports.add(RFClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true));


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileReader.close();
        }

        return reports;
    }

    private EvaluationReport NBClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection) throws Exception {
        NaiveBayes naiveBayes = new NaiveBayes();
        naiveBayes.buildClassifier(train);

        // evaluation
        eval.evaluateModel(naiveBayes, test);
        return new EvaluationReport(iteration, EvaluationReport.Classifiers.NAIVE_BAYES, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection);
    }

    private EvaluationReport RFClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection) throws Exception {

        RandomForest randomForest = new RandomForest();
        randomForest.buildClassifier(train);

        // evaluation
        System.out.println("Random Forest results: ");
        eval.evaluateModel(randomForest, test);
        return new EvaluationReport(iteration, EvaluationReport.Classifiers.RANDOM_FOREST, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection);

    }

    private EvaluationReport IBkClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection) throws Exception {
        IBk iBk = new IBk();
        iBk.buildClassifier(train);

        // evaluation
        System.out.println("IBk results: ");
        eval.evaluateModel(iBk, test);
        return new EvaluationReport(iteration, EvaluationReport.Classifiers.IBK, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection);
    }
}


