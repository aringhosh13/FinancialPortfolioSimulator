package portfolio;

import java.util.ArrayList;
import java.util.List;

public class Portfolio {
    private List<Asset> assets;
    private List<Double> weights;
    private double riskFreeRate;

    public Portfolio(double riskFreeRate) {
        this.assets = new ArrayList<>();
        this.weights = new ArrayList<>();
        this.riskFreeRate = riskFreeRate;
    }

    public void addAsset(Asset asset, double weight) {
        assets.add(asset);
        weights.add(weight);
    }

    public double calculateExpectedReturn() {
        double totalReturn = 0;
        for (int i = 0; i < assets.size(); i++) {
            totalReturn += weights.get(i) * assets.get(i).getExpectedReturn();
        }
        return totalReturn;
    }

    /**
     * Calculates portfolio volatility using the multi-asset variance formula.
     * Assumes correlation matrix = avg correlation for simplicity.
     */
    public double calculateVolatility(double averageCorrelation) {
        double varianceSum = 0;
        int n = assets.size();

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double wI = weights.get(i);
                double wJ = weights.get(j);
                double sigI = assets.get(i).getVolatility();
                double sigJ = assets.get(j).getVolatility();

                double covIJ;
                if (i == j) {
                    covIJ = sigI * sigI; // Variance when i == j
                } else {
                    covIJ = averageCorrelation * sigI * sigJ; // Covariance
                }
                varianceSum += wI * wJ * covIJ;
            }
        }
        return Math.sqrt(varianceSum);
    }

    public double calculateSharpeRatio(double expectedReturn, double volatility) {
        if (volatility == 0) return 0;
        return (expectedReturn - riskFreeRate) / volatility;
    }

    public List<Asset> getAssets() { return assets; }
    public List<Double> getWeights() { return weights; }
}