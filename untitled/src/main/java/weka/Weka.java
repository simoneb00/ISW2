package weka;

import exceptions.EmptyARFFException;
import model.Classifier;
import model.EvaluationReport;
import weka.attributeSelection.BestFirst;
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

    public enum SearchMethods {
        BACKWARD_SEARCH,
        FORWARD_SEARCH,
        BEST_FIRST
    }

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

            reports.add(NBClassification(trainData, testData, eval, iteration, projName, false, null));
            reports.add(IBkClassification(trainData, testData, eval, iteration, projName, false, null));
            reports.add(RFClassification(trainData, testData, eval, iteration, projName, false, null));


            // feature selection: backward greedyStepwise

            weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();

            /*  CFSSubsetEval evaluates the worth of a subset of attributes by considering the individual predictive ability of each feature along with the degree of redundancy between them.
             *  It selects a subset of attributes highly correlated with the class but with low inter-correlation.
             */
            CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();

            /* Backward Search */
            GreedyStepwise greedyStepwise = new GreedyStepwise();
            greedyStepwise.setSearchBackwards(true);

            filter.setEvaluator(cfsSubsetEval);
            filter.setSearch(greedyStepwise);

            reports.addAll(classificationWithFS(trainData, testData, filter, eval, iteration, projName, SearchMethods.BACKWARD_SEARCH));

            // feature selection: forward greedyStepwise
            greedyStepwise.setConservativeForwardSelection(true);
            filter.setSearch(greedyStepwise);

            reports.addAll(classificationWithFS(trainData, testData, filter, eval, iteration, projName, SearchMethods.FORWARD_SEARCH));

            // feature selection: best first
            BestFirst bestFirst = new BestFirst();
            filter.setSearch(bestFirst);

            reports.addAll(classificationWithFS(trainData, testData, filter, eval, iteration, projName, SearchMethods.BEST_FIRST));


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileReader.close();
        }

        return reports;
    }

    private List<EvaluationReport> classificationWithFS(Instances train, Instances test, Filter filter ,Evaluation eval, int iteration, String projName, SearchMethods method) throws Exception {

        List<EvaluationReport> reports = new ArrayList<>();

        filter.setInputFormat(train);
        Instances filteredTrainingData = Filter.useFilter(train, filter);
        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

        Instances filteredTestingData = Filter.useFilter(test, filter);
        filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

        reports.add(NBClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true, method));
        reports.add(IBkClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true, method));
        reports.add(RFClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true, method));

        return reports;
    }

    private EvaluationReport NBClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection, SearchMethods method) throws Exception {
        NaiveBayes naiveBayes = new NaiveBayes();
        naiveBayes.buildClassifier(train);

        // evaluation
        eval.evaluateModel(naiveBayes, test);
        return new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection, method);
    }

    private EvaluationReport RFClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection, SearchMethods method) throws Exception {

        RandomForest randomForest = new RandomForest();
        randomForest.buildClassifier(train);

        // evaluation
        System.out.println("Random Forest results: ");
        eval.evaluateModel(randomForest, test);
        return new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection, method);

    }

    private EvaluationReport IBkClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection, SearchMethods method) throws Exception {
        IBk iBk = new IBk();
        iBk.buildClassifier(train);

        // evaluation
        System.out.println("IBk results: ");
        eval.evaluateModel(iBk, test);
        return new EvaluationReport(iteration, Classifier.Type.IBK, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection, method);
    }
}


