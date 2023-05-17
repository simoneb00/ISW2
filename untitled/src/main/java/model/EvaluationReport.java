package model;


public class EvaluationReport {

    public enum Classifiers {
        RANDOM_FOREST,
        IBK,
        NAIVE_BAYES
    }

    private int iteration;
    private Classifiers classifier;
    private String dataset;
    private double precision;
    private double recall;
    private double AUC;
    private double kappa;
    private boolean featureSelection;

    public EvaluationReport(int iteration, Classifiers classifier, String dataset, double precision, double recall, double AUC, double kappa, boolean featureSelection) {
        this.iteration = iteration;
        this.classifier = classifier;
        this.dataset = dataset;
        this.precision = precision;
        this.recall = recall;
        this.AUC = AUC;
        this.kappa = kappa;
        this.featureSelection = featureSelection;
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

    public Classifiers getClassifier() {
        return classifier;
    }

    public void setClassifier(Classifiers classifier) {
        this.classifier = classifier;
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
