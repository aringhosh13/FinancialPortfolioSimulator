package portfolio;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public class PortfolioGUI extends JFrame {

    private JSlider stockSlider;
    private JLabel stockWeightLabel;
    private JLabel statsLabel;
    private HistogramPanel histogramPanel;

    public PortfolioGUI() {
        setTitle("ISC Financial Portfolio Risk Simulator");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(new JLabel("Stock Allocation (%):"));
        
        stockSlider = new JSlider(0, 100, 60);
        stockSlider.setMajorTickSpacing(20);
        stockSlider.setPaintTicks(true);
        stockSlider.setPaintLabels(true);
        controlPanel.add(stockSlider);

        stockWeightLabel = new JLabel("60% Stocks / 40% Bonds");
        controlPanel.add(stockWeightLabel);

        JButton runBtn = new JButton("Run Simulation");
        controlPanel.add(runBtn);
        add(controlPanel, BorderLayout.NORTH);

        // Center Panel: Histogram
        histogramPanel = new HistogramPanel();
        add(histogramPanel, BorderLayout.CENTER);

        // Bottom Panel: Output Stats
        statsLabel = new JLabel(" Adjust parameters and click Run Simulation.", SwingConstants.CENTER);
        statsLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        statsLabel.setPreferredSize(new Dimension(800, 50));
        add(statsLabel, BorderLayout.SOUTH);

        // Listeners
        stockSlider.addChangeListener(e -> {
            int val = stockSlider.getValue();
            stockWeightLabel.setText(val + "% Stocks / " + (100 - val) + "% Bonds");
        });

        runBtn.addActionListener(e -> executeSimulation());

        setLocationRelativeTo(null);
    }

    private void executeSimulation() {
        double stockWeight = stockSlider.getValue() / 100.0;
        double bondWeight = 1.0 - stockWeight;

        Asset stocks = new Asset("US Equities", 0.10, 0.18);
        Asset bonds = new Asset("Govt Bonds", 0.04, 0.06);

        Portfolio portfolio = new Portfolio(0.02); // 2% Risk-Free Rate
        portfolio.addAsset(stocks, stockWeight);
        portfolio.addAsset(bonds, bondWeight);

        int iterations = 2000;
        int years = 5;
        double initialPrincipal = 10000.0;
        double correlation = 0.10;

        MonteCarloSimulator.SimulationResult result = MonteCarloSimulator.runSimulation(
            portfolio, initialPrincipal, years, iterations, correlation
        );

        double expReturn = portfolio.calculateExpectedReturn();
        double volatility = portfolio.calculateVolatility(correlation);
        double sharpe = portfolio.calculateSharpeRatio(expReturn, volatility);

        String stats = String.format(
            "<html><center>Expected Return: <b>%.2f%%</b> | Volatility: <b>%.2f%%</b> | Sharpe Ratio: <b>%.2f</b><br>" +
            "5-Year Loss Probability: <b>%.1f%%</b> | 95%% Value at Risk (VaR): <b>$%.2f</b> | Median Value: <b>$%.2f</b></center></html>",
            expReturn * 100, volatility * 100, sharpe, result.probabilityOfLoss, result.valueAtRisk95, result.medianFinalValue
        );
        statsLabel.setText(stats);

        histogramPanel.setData(result.finalValues, initialPrincipal);
    }

    // Custom Component to paint distribution histogram
    private static class HistogramPanel extends JPanel {
        private double[] data;
        private double initialPrincipal;

        public void setData(double[] data, double initialPrincipal) {
            this.data = data;
            this.initialPrincipal = initialPrincipal;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.length == 0) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 40;

            int numBins = 30;
            int[] bins = new int[numBins];
            double min = data[0];
            double max = data[data.length - 1];
            double binWidth = (max - min) / numBins;

            for (double val : data) {
                int binIndex = (int) ((val - min) / binWidth);
                if (binIndex >= numBins) binIndex = numBins - 1;
                bins[binIndex]++;
            }

            int maxCount = Arrays.stream(bins).max().getAsInt();

            // Draw Bars
            double barWidthPixels = (double) (width - 2 * padding) / numBins;

            for (int i = 0; i < numBins; i++) {
                double binStartVal = min + (i * binWidth);
                int barHeight = (int) (((double) bins[i] / maxCount) * (height - 2 * padding));

                int x = (int) (padding + i * barWidthPixels);
                int y = height - padding - barHeight;

                // Red for loss territory, Green for profit
                if (binStartVal < initialPrincipal) {
                    g2.setColor(new Color(220, 53, 69, 180));
                } else {
                    g2.setColor(new Color(40, 167, 69, 180));
                }

                g2.fillRect(x, y, (int) barWidthPixels - 1, barHeight);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(x, y, (int) barWidthPixels - 1, barHeight);
            }

            // Draw Base Axes
            g2.setColor(Color.BLACK);
            g2.drawLine(padding, height - padding, width - padding, height - padding);
            g2.drawString(String.format("Min: $%.0f", min), padding, height - 15);
            g2.drawString(String.format("Max: $%.0f", max), width - padding - 80, height - 15);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PortfolioGUI().setVisible(true));
    }
}