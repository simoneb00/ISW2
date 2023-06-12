package model;


import weka.Weka;

public class EvaluationReport {

    private int iteration;
    private Classifier.Type classifier;
    private String dataset;
    private double precision;
    private double recall;
    private double auc;
    private double kappa;
    private boolean featureSelection;
    private Weka.SearchMethods fsSearchMethod;
    private Weka.SamplingMethod samplingMethod;
    private boolean costSensitiveClassification;

    public EvaluationReport(int iteration, Classifier.Type classifier, String dataset, double precision, double recall, double auc, double kappa, boolean featureSelection, Weka.SearchMethods fsSearchMethod, Weka.SamplingMethod samplingMethod, boolean costSensitiveClassification) {
        this.iteration = iteration;
        this.classifier = classifier;
        this.dataset = dataset;
        this.precision = precision;
        this.recall = recall;
        this.auc = auc;
        this.kappa = kappa;
        this.featureSelection = featureSelection;
        this.fsSearchMethod = fsSearchMethod;
        this.samplingMethod = samplingMethod;
        this.costSensitiveClassification = costSensitiveClassification;
    }

    public boolean isCostSensitiveClassification() {
        return costSensitiveClassification;
    }

    public void setCostSensitiveClassification(boolean costSensitiveClassification) {
        this.costSensitiveClassification = costSensitiveClassification;
    }

    public Weka.SamplingMethod getSamplingMethod() {
        return samplingMethod;
    }

    public Weka.SearchMethods getFsSearchMethod() {
        return fsSearchMethod;
    }

    public boolean isFeatureSelection() {
        return featureSelection;
    }

    public int getIteration() {
        return iteration;
    }

    public Classifier.Type getClassifier() {
        return classifier;
    }

    public String getDataset() {
        return dataset;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getAuc() {
        return auc;
    }

    public double getKappa() {
        return kappa;
    }

}
