package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

public class Class {

    private String name;

    private String implementation;
    private Release release;
    private List<RevCommit> associatedCommits;
    private boolean isBuggy;

    private int size;
    private int nAuth;
    private int fanOut;
    private int nR;
    private int locAdded;
    private int maxLOCAdded;
    private float averageLOCAdded;
    private int churn;
    private int maxChurn;
    private float averageChurn;
    private long timeSpan;


    public Class(String name, String implementation, Release release) {
        this.name = name;
        this.implementation = implementation;
        this.release = release;
        this.associatedCommits = new ArrayList<>();
        this.isBuggy = false;
        this.size = 0;
        this.nAuth = 0;
        this.nR = 0;
        this.locAdded = 0;
        this.maxLOCAdded = 0;
        this.averageLOCAdded = 0;
        this.churn = 0;
        this.maxChurn = 0;
        this.averageChurn = 0;
        this.timeSpan = 0;
    }

    public long getTimeSpan() {
        return timeSpan;
    }

    public void setTimeSpan(long timeSpan) {
        this.timeSpan = timeSpan;
    }

    public int getMaxChurn() {
        return maxChurn;
    }

    public void setMaxChurn(int maxChurn) {
        this.maxChurn = maxChurn;
    }

    public float getAverageChurn() {
        return averageChurn;
    }

    public void setAverageChurn(float averageChurn) {
        this.averageChurn = averageChurn;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public List<RevCommit> getAssociatedCommits() {
        return associatedCommits;
    }

    public void setAssociatedCommits(List<RevCommit> associatedCommits) {
        this.associatedCommits = associatedCommits;
    }

    public boolean isBuggy() {
        return isBuggy;
    }

    public void setBuggy(boolean buggy) {
        isBuggy = buggy;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public int getnAuth() {
        return nAuth;
    }

    public void setnAuth(int nAuth) {
        this.nAuth = nAuth;
    }

    public int getnR() {
        return nR;
    }

    public void setnR(int nR) {
        this.nR = nR;
    }

    public int getLocAdded() {
        return locAdded;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public int getMaxLOCAdded() {
        return maxLOCAdded;
    }

    public void setMaxLOCAdded(int maxLOCAdded) {
        this.maxLOCAdded = maxLOCAdded;
    }

    public float getAverageLOCAdded() {
        return averageLOCAdded;
    }

    public void setAverageLOCAdded(float averageLOCAdded) {
        this.averageLOCAdded = averageLOCAdded;
    }

    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public int getFanOut() {
        return fanOut;
    }

    public void setFanOut(int fanOut) {
        this.fanOut = fanOut;
    }
}

