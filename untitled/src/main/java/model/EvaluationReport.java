package model;


import org.eclipse.jgit.util.FS;
import weka.Weka;

public class EvaluationReport {

    private int iteration;
    private Classifier.Type classifier;
    private String dataset;
    private double precision;
    private double recall;
    private double AUC;
    private double kappa;
    private boolean featureSelection;
    private Weka.SearchMethods FSSearchMethod;
    private Weka.SamplingMethod samplingMethod;
    private boolean costSensitiveClassification;

    public EvaluationReport(int iteration, Classifier.Type classifier, String dataset, double precision, double recall, double AUC, double kappa, boolean featureSelection, Weka.SearchMethods FSSearchMethod, Weka.SamplingMethod samplingMethod, boolean costSensitiveClassification) {
        this.iteration = iteration;
        this.classifier = classifier;
        this.dataset = dataset;
        this.precision = precision;
        this.recall = recall;
        this.AUC = AUC;
        this.kappa = kappa;
        this.featureSelection = featureSelection;
        this.FSSearchMethod = FSSearchMethod;
        this.samplingMethod = samplingMethod;
        this.costSensitiveClassification = costSensitiveClassification;
    }

    public boolean isCostSensitiveClassification() { return costSensitiveClassification;}

    public Weka.SamplingMethod getSamplingMethod() {
        return samplingMethod;
    }

    public Weka.SearchMethods getFSSearchMethod() {
        return FSSearchMethod;
    }

    public boolean isFeatureSelection() {
        return featureSelection;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public Classifier.Type getClassifier() {
        return classifier;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getAUC() {
        return AUC;
    }

    public void setAUC(double AUC) {
        this.AUC = AUC;
    }

    public double getKappa() {
        return kappa;
    }

    public void setKappa(double kappa) {
        this.kappa = kappa;
    }
}
