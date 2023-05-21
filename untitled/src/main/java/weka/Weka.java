package weka;

import com.sun.xml.bind.v2.runtime.unmarshaller.IntArrayData;
import exceptions.EmptyARFFException;
import model.Classifier;
import model.EvaluationReport;
import org.eclipse.jgit.util.FS;
import utils.ARFF;
import utils.CSV;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SpreadSubsample;

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

    public enum SamplingMethod {
        UNDERSAMPLING,
        OVERSAMPLING
    }

    private final List<EvaluationReport> reportsWithoutFS = new ArrayList<>();
    private final List<EvaluationReport> reportsWithFS = new ArrayList<>();
    private final List<EvaluationReport> reportsWithSampling = new ArrayList<>();

    /*
     *  Classifiers: Naive Bayes, Random Forest, IBk
     *  Configurations:
     *  - without any filter
     *  - feature selection:
     *     - backward search
     *     - forward search
     *     - best first
     *  - feature selection (best first) + oversampling
     *  - feature selection (best first) + undersampling
     *
     */

    public void classify(String ARFFTrainingSet, String ARFFTestingSet, int iteration, String projName) throws IOException, EmptyARFFException {

        File trainFile = new File(ARFFTrainingSet);
        FileReader fileReader = new FileReader(trainFile);
        ArffLoader.ArffReader arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();

        if (arffReader.getStructure().attribute(arffReader.getStructure().numAttributes() - 1).numValues() == 1)
            throw new EmptyARFFException();

        File testFile = new File(ARFFTestingSet);
        fileReader = new FileReader(testFile);
        arffReader = new ArffLoader.ArffReader(fileReader);
        if (arffReader.getData().isEmpty())
            throw new EmptyARFFException();

        try {

            /* NO FILTER */

            DataSource trainSource = new DataSource(ARFFTrainingSet);
            DataSource testSource = new DataSource(ARFFTestingSet);

            Instances trainData = trainSource.getDataSet();
            Instances testData = testSource.getDataSet();

            trainData.setClassIndex(trainData.numAttributes() - 1);
            testData.setClassIndex(trainData.numAttributes() - 1);

            Evaluation eval = new Evaluation(trainData);

            NaiveBayes nb = new NaiveBayes();
            nb.buildClassifier(trainData);
            eval.evaluateModel(nb, testData);
            reportsWithoutFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), false, null, null));

            RandomForest rf = new RandomForest();
            rf.buildClassifier(trainData);
            eval.evaluateModel(rf, testData);
            reportsWithoutFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), false, null, null));

            IBk iBk = new IBk();
            iBk.buildClassifier(trainData);
            eval.evaluateModel(iBk, testData);
            reportsWithoutFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), false, null, null));



            /* FEATURE SELECTION */

            weka.filters.supervised.attribute.AttributeSelection filter = new AttributeSelection();

            /*  CFSSubsetEval evaluates the worth of a subset of attributes by considering the individual predictive ability of each feature along with the degree of redundancy between them.
             *  It selects a subset of attributes highly correlated with the class but with low inter-correlation.  */
            CfsSubsetEval cfsSubsetEval = new CfsSubsetEval();

            /* Backward Search */
            GreedyStepwise greedyStepwise = new GreedyStepwise();
            greedyStepwise.setSearchBackwards(true);

            filter.setEvaluator(cfsSubsetEval);
            filter.setSearch(greedyStepwise);
            filter.setInputFormat(trainData);

            Instances filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);
            Instances filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

            nb = new NaiveBayes();
            nb.buildClassifier(filteredTrainingData);
            eval.evaluateModel(nb, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BACKWARD_SEARCH, null));

            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BACKWARD_SEARCH, null));

            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BACKWARD_SEARCH, null));


            /* Forward Search */
            greedyStepwise.setSearchBackwards(false);
            greedyStepwise.setConservativeForwardSelection(true);
            filter.setSearch(greedyStepwise);

            filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

            filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

            nb = new NaiveBayes();
            nb.buildClassifier(filteredTrainingData);
            eval.evaluateModel(nb, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.FORWARD_SEARCH, null));

            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.FORWARD_SEARCH, null));

            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.FORWARD_SEARCH, null));


            /* Best First */
            BestFirst bestFirst = new BestFirst();
            filter.setSearch(bestFirst);

            filteredTrainingData = Filter.useFilter(trainData, filter);
            filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

            filteredTestingData = Filter.useFilter(testData, filter);
            filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

            nb = new NaiveBayes();
            nb.buildClassifier(filteredTrainingData);
            eval.evaluateModel(nb, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, null));

            rf = new RandomForest();
            rf.buildClassifier(filteredTrainingData);
            eval.evaluateModel(rf, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, null));

            iBk = new IBk();
            iBk.buildClassifier(filteredTrainingData);
            eval.evaluateModel(iBk, filteredTestingData);
            reportsWithFS.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, null));


            /* SAMPLING (+ Best First Feature Selection) */

            /* Undersampling */
            Filter samplingFilter = new SpreadSubsample();
            String[] spreadSubSampleOptions = new String[2];
            spreadSubSampleOptions[0] = "-M";
            spreadSubSampleOptions[1] = "1.0";

            FilteredClassifier filteredClassifier = new FilteredClassifier();
            filteredClassifier.setFilter(samplingFilter);

            nb = new NaiveBayes();
            filteredClassifier.setClassifier(nb);
            filteredClassifier.buildClassifier(trainData);
            eval.evaluateModel(filteredClassifier, testData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, SamplingMethod.UNDERSAMPLING));

            rf = new RandomForest();
            filteredClassifier.setClassifier(rf);
            filteredClassifier.buildClassifier(trainData);
            eval.evaluateModel(filteredClassifier, testData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, SamplingMethod.UNDERSAMPLING));

            iBk = new IBk();
            filteredClassifier.setClassifier(iBk);
            filteredClassifier.buildClassifier(trainData);
            eval.evaluateModel(filteredClassifier, testData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, SamplingMethod.UNDERSAMPLING));

            /* Oversampling */
            samplingFilter = new Resample();

            ARFF arff = new ARFF();
            int countTrue = arff.countNumOccurrences(trainFile, "true");
            int countFalse = arff.countNumOccurrences(trainFile, "false");

            System.out.println(projName + " - " + iteration + ": " + "num of true = " + countTrue + ", num of false = " + countFalse);

            double percentage;
            if (countTrue < countFalse)
                percentage = (double) (countFalse - countTrue) / Math.max(countTrue, 1);
            else
                percentage = (double) (countTrue - countFalse) / Math.max(countFalse, 1);

            String[] resampleOptions = new String[4];
            resampleOptions[0] = "-B";
            resampleOptions[1] = "1.0";
            resampleOptions[2] = "-Z";
            resampleOptions[3] = String.valueOf(percentage);

            samplingFilter.setOptions(resampleOptions);

            filteredClassifier.setFilter(samplingFilter);

            filteredClassifier.setClassifier(nb);
            filteredClassifier.buildClassifier(trainData);
            eval.evaluateModel(filteredClassifier, testData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, SamplingMethod.OVERSAMPLING));

            filteredClassifier.setClassifier(rf);
            filteredClassifier.buildClassifier(trainData);
            eval.evaluateModel(filteredClassifier, testData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, SamplingMethod.OVERSAMPLING));

            filteredClassifier.setClassifier(iBk);
            filteredClassifier.buildClassifier(trainData);
            eval.evaluateModel(filteredClassifier, testData);
            reportsWithSampling.add(new EvaluationReport(iteration, Classifier.Type.IBK, projName, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), true, SearchMethods.BEST_FIRST, SamplingMethod.OVERSAMPLING));


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            fileReader.close();
        }
    }

    public void generateFiles() {

        CSV.generateCSVForReportsWithoutFS(reportsWithoutFS);
        CSV.generateCSVForReportsWithFS(reportsWithFS);
        CSV.generateCSVForReportsWithSampling(reportsWithSampling);
    }

    /*
    private List<EvaluationReport> classificationWithFS(Instances train, Instances test, Filter FSFilter, Evaluation eval, int iteration, String projName, SearchMethods searchMethod) throws Exception {

        List<EvaluationReport> reports = new ArrayList<>();

        FSFilter.setInputFormat(train);
        Instances filteredTrainingData = Filter.useFilter(train, FSFilter);
        filteredTrainingData.setClassIndex(filteredTrainingData.numAttributes() - 1);

        Instances filteredTestingData = Filter.useFilter(test, FSFilter);
        filteredTestingData.setClassIndex(filteredTestingData.numAttributes() - 1);

        reports.add(NBClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true, searchMethod, null));
        reports.add(IBkClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true, searchMethod, null));
        reports.add(RFClassification(filteredTrainingData, filteredTestingData, eval, iteration, projName, true, searchMethod, null));


        return reports;
    }



    private EvaluationReport NBClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection, SearchMethods searchMethod, SamplingMethod samplingMethod) throws Exception {
        NaiveBayes naiveBayes = new NaiveBayes();
        naiveBayes.buildClassifier(train);

        // evaluation
        eval.evaluateModel(naiveBayes, test);
        return new EvaluationReport(iteration, Classifier.Type.NAIVE_BAYES, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection, searchMethod, samplingMethod);
    }

    private EvaluationReport RFClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection, SearchMethods searchMethod, SamplingMethod samplingMethod) throws Exception {

        RandomForest randomForest = new RandomForest();
        randomForest.buildClassifier(train);

        // evaluation
        System.out.println("Random Forest results: ");
        eval.evaluateModel(randomForest, test);
        return new EvaluationReport(iteration, Classifier.Type.RANDOM_FOREST, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection, searchMethod, samplingMethod);

    }

    private EvaluationReport IBkClassification(Instances train, Instances test, Evaluation eval, int iteration, String projName, boolean featureSelection, SearchMethods searchMethods, SamplingMethod samplingMethod) throws Exception {
        IBk iBk = new IBk();
        iBk.buildClassifier(train);

        // evaluation
        System.out.println("IBk results: ");
        eval.evaluateModel(iBk, test);
        return new EvaluationReport(iteration, Classifier.Type.IBK, projName + " - " + iteration, eval.precision(0), eval.recall(0), eval.areaUnderROC(0), eval.kappa(), featureSelection, searchMethods, samplingMethod);
    }

     */
}


