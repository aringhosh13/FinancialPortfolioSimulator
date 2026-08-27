package portfolio;

public class Asset {
    private String name;
    private double expectedReturn; // Annualized mean return (e.g., 0.10 for 10%)
    private double volatility;     // Annualized standard deviation (e.g., 0.15 for 15%)

    public Asset(String name, double expectedReturn, double volatility) {
        this.name = name;
        this.expectedReturn = expectedReturn;
        this.volatility = volatility;
    }

    public String getName() { return name; }
    public double getExpectedReturn() { return expectedReturn; }
    public double getVolatility() { return volatility; }
}