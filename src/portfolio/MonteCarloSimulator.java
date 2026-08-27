package portfolio;

import java.util.Arrays;
import java.util.Random;

public class MonteCarloSimulator {

    public static class SimulationResult {
        public double[] finalValues;
        public double probabilityOfLoss;
        public double valueAtRisk95; // 5th percentile worst outcome
        public double medianFinalValue;

        public SimulationResult(double[] finalValues, double probabilityOfLoss, double valueAtRisk95, double medianFinalValue) {
            this.finalValues = finalValues;
            this.probabilityOfLoss = probabilityOfLoss;
            this.valueAtRisk95 = valueAtRisk95;
            this.medianFinalValue = medianFinalValue;
        }
    }

    public static SimulationResult runSimulation(Portfolio portfolio, double initialBalance, int years, int iterations, double avgCorr) {
        double mu = portfolio.calculateExpectedReturn();
        double sigma = portfolio.calculateVolatility(avgCorr);
        
        double[] finalValues = new double[iterations];
        Random random = new Random();
        int lossCount = 0;

        for (int i = 0; i < iterations; i++) {
            double currentBalance = initialBalance;
            for (int year = 0; year < years; year++) {
                double z = random.nextGaussian(); // Standard Normal Variable Z ~ N(0,1)
                
                // Geometric Brownian Motion step
                double simulatedReturn = (mu - 0.5 * sigma * sigma) + sigma * z;
                currentBalance *= Math.exp(simulatedReturn);
            }
            finalValues[i] = currentBalance;
            if (currentBalance < initialBalance) {
                lossCount++;
            }
        }

        Arrays.sort(finalValues);

        double probLoss = ((double) lossCount / iterations) * 100.0;
        
        // 5th Percentile calculation for 95% VaR
        int index5th = (int) (iterations * 0.05);
        double valAtRisk = initialBalance - finalValues[index5th];
        
        // Median outcome
        double medianValue = finalValues[iterations / 2];

        return new SimulationResult(finalValues, probLoss, valAtRisk, medianValue);
    }
}