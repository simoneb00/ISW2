package main;

import exceptions.ExecutionException;
import weka.WalkForward;

public class Main {

    public static void main(String[] args) throws ExecutionException {
        try {

            WalkForward.initSets("BOOKKEEPER");
            WalkForward.classify("BOOKKEEPER");
            WalkForward.initSets("STORM");
            WalkForward.classify("STORM");


        } catch (Exception e) {
            throw new ExecutionException();
        }

    }
}
