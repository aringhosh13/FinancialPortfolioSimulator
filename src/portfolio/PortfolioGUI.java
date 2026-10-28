package portfolio;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;

public class PortfolioGUI extends JFrame {
    private static final Color BACKGROUND = new Color(241, 243, 245);
    private static final Color TEXT = new Color(40, 47, 55);
    private static final Color BLUE = new Color(66, 111, 151);
    private static final Color RED = new Color(185, 105, 91);
    private static final Font FONT = new Font("SansSerif", Font.PLAIN, 13);

    public PortfolioGUI() {
        super("Financial Portfolio Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new DashboardPanel(false));
        setSize(1000, 720);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
    }

    // The form uses standard Swing controls and layout managers.
    static class DashboardPanel extends JPanel {
        private final JSpinner amount = new JSpinner(new SpinnerNumberModel(10000, 1000, 100000000, 1000));
        private final JSpinner years = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
        private final JSlider stockSlider = new JSlider(0, 100, 60);
        private final JLabel allocation = new JLabel();
        private final JLabel status = new JLabel("Choose your inputs, then click Run simulation.");
        private final JLabel context = new JLabel("No simulation has been run yet.");
        private final JLabel expectedReturn = new JLabel("\u2014");
        private final JLabel volatility = new JLabel("\u2014");
        private final JLabel medianValue = new JLabel("\u2014");
        private final JLabel lossProbability = new JLabel("\u2014");
        private final JButton runButton = new JButton("Run simulation");
        private final JButton detailsButton = new JButton("More results");
        private final HistogramPanel histogram = new HistogramPanel();
        private double sharpeRatio;
        private double valueAtRisk;
        private int completedYears;

        DashboardPanel(boolean runOnOpen) {
            super(new BorderLayout(16, 16));
            setBackground(BACKGROUND);
            setBorder(new EmptyBorder(20, 20, 14, 20));

            JPanel heading = new JPanel(new BorderLayout());
            heading.setOpaque(false);
            JLabel title = new JLabel("Financial Portfolio Simulator");
            title.setFont(FONT.deriveFont(Font.BOLD, 24f));
            title.setForeground(TEXT);
            heading.add(title, BorderLayout.NORTH);
            add(heading, BorderLayout.NORTH);

            JPanel body = new JPanel(new BorderLayout(16, 0));
            body.setOpaque(false);
            body.add(buildInputs(), BorderLayout.WEST);
            body.add(buildOutput(), BorderLayout.CENTER);
            add(body, BorderLayout.CENTER);

            status.setName("simulationStatus");
            status.setFont(FONT.deriveFont(12f));
            add(status, BorderLayout.SOUTH);

            amount.addChangeListener(e -> inputsChanged());
            years.addChangeListener(e -> inputsChanged());
            stockSlider.addChangeListener(e -> {
                updateAllocation();
                inputsChanged();
            });
            runButton.addActionListener(e -> runSimulation());
            detailsButton.addActionListener(e -> showDetails());
            updateAllocation();
            if (runOnOpen) runSimulation();
        }

        private JPanel buildInputs() {
            JPanel inputs = group("Input values");
            inputs.setPreferredSize(new Dimension(260, 500));
            inputs.setLayout(new BoxLayout(inputs, BoxLayout.Y_AXIS));
            inputs.add(new JLabel("Starting amount (INR)"));
            gap(inputs, 7);
            amount.setName("startingAmount");
            amount.setEditor(new JSpinner.NumberEditor(amount, "#,##0"));
            prepareSpinner(amount);
            inputs.add(amount);
            gap(inputs, 17);
            inputs.add(new JLabel("Investment period (years)"));
            gap(inputs, 7);
            years.setName("investmentYears");
            years.setEditor(new JSpinner.NumberEditor(years, "0"));
            prepareSpinner(years);
            inputs.add(years);
            gap(inputs, 24);
            inputs.add(new JLabel("Percentage invested in stocks"));
            gap(inputs, 9);
            stockSlider.setName("stockAllocation");
            stockSlider.setOpaque(false);
            stockSlider.setMajorTickSpacing(25);
            stockSlider.setPaintTicks(true);
            stockSlider.setPaintLabels(true);
            stockSlider.setAlignmentX(LEFT_ALIGNMENT);
            stockSlider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
            inputs.add(stockSlider);
            gap(inputs, 8);
            inputs.add(allocation);
            gap(inputs, 10);
            inputs.add(new JLabel("The rest is invested in bonds."));
            gap(inputs, 24);
            runButton.setName("runSimulation");
            runButton.setAlignmentX(LEFT_ALIGNMENT);
            runButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            runButton.setFont(FONT.deriveFont(Font.BOLD));
            inputs.add(runButton);
            gap(inputs, 12);
            inputs.add(new JLabel("Each run simulates 2,000 outcomes."));
            inputs.add(Box.createVerticalGlue());
            JButton about = new JButton("About the model");
            about.setAlignmentX(LEFT_ALIGNMENT);
            about.addActionListener(e -> showAbout());
            inputs.add(about);
            return inputs;
        }

        private JPanel buildOutput() {
            JPanel output = new JPanel(new BorderLayout(0, 14));
            output.setOpaque(false);
            JPanel chart = group("Distribution of final values");
            chart.setLayout(new BorderLayout(0, 8));
            context.setName("resultContext");
            context.setFont(FONT.deriveFont(12f));
            chart.add(context, BorderLayout.NORTH);
            chart.add(histogram, BorderLayout.CENTER);
            JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
            legend.setOpaque(false);
            JLabel below = new JLabel("\u25a0 Below starting amount");
            below.setForeground(RED);
            JLabel above = new JLabel("\u25a0 At or above starting amount");
            above.setForeground(BLUE);
            below.setFont(FONT.deriveFont(11f));
            above.setFont(FONT.deriveFont(11f));
            legend.add(below);
            legend.add(above);
            chart.add(legend, BorderLayout.SOUTH);
            output.add(chart, BorderLayout.CENTER);

            JPanel results = group("Results");
            results.setLayout(new BorderLayout(0, 12));
            JPanel table = new JPanel(new GridLayout(4, 2, 12, 9));
            table.setOpaque(false);
            addResult(table, "Expected yearly return", expectedReturn, "annualReturn");
            addResult(table, "Yearly volatility", volatility, "volatility");
            addResult(table, "Median final value", medianValue, "medianValue");
            addResult(table, "Chance of losing money", lossProbability, "lossProbability");
            volatility.setToolTipText("The model's annual standard deviation; a higher value means more variation.");
            medianValue.setToolTipText("Half the simulated final values are below the median, and half are above.");
            lossProbability.setToolTipText("The percentage of runs ending below the starting amount.");
            results.add(table, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            buttons.setOpaque(false);
            detailsButton.setEnabled(false);
            detailsButton.setName("moreResults");
            buttons.add(detailsButton);
            results.add(buttons, BorderLayout.SOUTH);
            output.add(results, BorderLayout.SOUTH);
            return output;
        }

        private void updateAllocation() {
            int stocks = stockSlider.getValue();
            allocation.setText("Stocks: " + stocks + "%     Bonds: " + (100 - stocks) + "%");
        }

        private void inputsChanged() {
            status.setText("Inputs changed. Click Run simulation to update the results.");
        }

        private void runSimulation() {
            try {
                amount.commitEdit();
                years.commitEdit();
            } catch (java.text.ParseException ex) {
                status.setText("Enter a valid amount and number of years.");
                return;
            }
            double principal = ((Number) amount.getValue()).doubleValue();
            int period = ((Number) years.getValue()).intValue();
            double stockWeight = stockSlider.getValue() / 100.0;
            Portfolio portfolio = new Portfolio(0.02);
            portfolio.addAsset(new Asset("Stocks", 0.10, 0.18), stockWeight);
            portfolio.addAsset(new Asset("Bonds", 0.04, 0.06), 1 - stockWeight);

            MonteCarloSimulator.SimulationResult result = MonteCarloSimulator.runSimulation(
                    portfolio, principal, period, 2000, 0.10);
            double expected = portfolio.calculateExpectedReturn();
            double risk = portfolio.calculateVolatility(0.10);
            expectedReturn.setText(percent(expected * 100));
            volatility.setText(percent(risk * 100));
            medianValue.setText(money(result.medianFinalValue));
            lossProbability.setText(percent(result.probabilityOfLoss));
            sharpeRatio = portfolio.calculateSharpeRatio(expected, risk);
            valueAtRisk = Math.max(0, result.valueAtRisk95);
            completedYears = period;
            context.setText("2,000 runs | " + period + (period == 1 ? " year" : " years")
                    + " | Stocks " + stockSlider.getValue() + "% | Start " + money(principal));
            histogram.setData(result.finalValues, principal);
            detailsButton.setEnabled(true);
            status.setText("Simulation complete. Results will vary slightly each time you run it.");
        }

        private void showDetails() {
            String message = "Results for the last " + completedYears + "-year simulation\n\n"
                    + "Sharpe ratio: " + String.format(Locale.US, "%.2f", sharpeRatio) + "\n"
                    + "This compares the assumed return above a 2% risk-free rate with volatility.\n\n"
                    + "95% loss at risk: " + money(valueAtRisk) + "\n"
                    + "Starting amount minus the 5th-percentile final value, with a minimum of zero.\n"
                    + "About 5% of the simulated outcomes are worse than that percentile.";
            JOptionPane.showMessageDialog(this, message, "More results", JOptionPane.INFORMATION_MESSAGE);
        }

        private void showAbout() {
            String message = "This project uses Monte Carlo simulation to compare a mix of stocks and bonds.\n\n"
                    + "For each run, the program generates random yearly returns and compounds the balance.\n"
                    + "It repeats this 2,000 times. Each bar groups similar final amounts.\n\n"
                    + "Model assumptions (annual):\n"
                    + "Stocks: 10% return, 18% volatility\n"
                    + "Bonds: 4% return, 6% volatility\n"
                    + "Correlation: 0.10\n\n"
                    + "For example, 60% stocks and 40% bonds gives an assumed return of\n"
                    + "(0.60 x 10%) + (0.40 x 4%) = 7.6% per year.\n\n"
                    + "These are example rates, not live market data. The model ignores fees, taxes,\n"
                    + "inflation and extra deposits. Rupees are used to display the amounts.";
            JOptionPane.showMessageDialog(this, message, "About the model", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Draw the histogram directly, without a charting library.
    private static class HistogramPanel extends JPanel {
        private int[] bins;
        private double minimum, maximum, principal;

        HistogramPanel() { setBackground(Color.WHITE); }

        void setData(double[] values, double startingAmount) {
            principal = startingAmount;
            minimum = Math.min(values[0], principal) * 0.95;
            maximum = Math.max(values[values.length - 1], principal) * 1.03;
            bins = new int[25];
            double interval = (maximum - minimum) / bins.length;
            for (double value : values) {
                int index = Math.min(bins.length - 1, (int) ((value - minimum) / interval));
                bins[index]++;
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(FONT.deriveFont(11f));
            if (bins == null) {
                String prompt = "Run a simulation to draw the graph.";
                g.setColor(Color.GRAY);
                g.drawString(prompt, (getWidth() - g.getFontMetrics().stringWidth(prompt)) / 2, getHeight() / 2);
                g.dispose();
                return;
            }
            int left = 44, right = getWidth() - 24, top = 30, bottom = getHeight() - 48;
            int peak = 1;
            for (int count : bins) peak = Math.max(peak, count);
            int scale = ((peak + 39) / 40) * 40;
            for (int i = 0; i <= 4; i++) {
                int y = bottom - (bottom - top) * i / 4;
                g.setColor(new Color(225, 229, 233));
                g.drawLine(left, y, right, y);
                g.setColor(Color.GRAY);
                String tick = String.valueOf(scale * i / 4);
                g.drawString(tick, left - 7 - g.getFontMetrics().stringWidth(tick), y + 4);
            }
            g.setColor(TEXT);
            g.drawString("Number of runs", left, 15);
            double width = (right - left) / (double) bins.length;
            double startX = left + (principal - minimum) / (maximum - minimum) * (right - left);
            for (int i = 0; i < bins.length; i++) {
                int x = (int) (left + i * width);
                int barWidth = Math.max(1, (int) width - 2);
                int height = (int) (bins[i] / (double) scale * (bottom - top));
                int lossWidth = (int) Math.max(0, Math.min(barWidth, startX - x));
                g.setColor(RED);
                g.fillRect(x, bottom - height, lossWidth, height);
                g.setColor(BLUE);
                g.fillRect(x + lossWidth, bottom - height, barWidth - lossWidth, height);
            }
            g.setColor(Color.DARK_GRAY);
            g.drawLine((int) startX, top, (int) startX, bottom);
            g.drawString("Start", Math.min(right - 30, (int) startX + 4), top - 4);
            for (int i = 0; i <= 4; i++) {
                double value = minimum + (maximum - minimum) * i / 4;
                String tick = compactMoney(value);
                int x = left + (right - left) * i / 4;
                g.drawString(tick, x - g.getFontMetrics().stringWidth(tick) / 2, bottom + 19);
            }
            String axis = "Final value (INR)";
            g.drawString(axis, (getWidth() - g.getFontMetrics().stringWidth(axis)) / 2, getHeight() - 6);
            g.dispose();
        }
    }

    private static JPanel group(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(195, 202, 210)), title),
                new EmptyBorder(12, 12, 12, 12)));
        return panel;
    }

    private static void prepareSpinner(JSpinner spinner) {
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JFormattedTextField field = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        field.setHorizontalAlignment(SwingConstants.LEFT);
        field.setFont(FONT);
        field.setBorder(new EmptyBorder(3, 6, 3, 6));
    }

    private static void addResult(JPanel table, String title, JLabel value, String name) {
        table.add(new JLabel(title));
        value.setName(name);
        value.setFont(FONT.deriveFont(Font.BOLD, 14f));
        value.setHorizontalAlignment(SwingConstants.RIGHT);
        table.add(value);
    }

    private static void gap(JPanel panel, int height) { panel.add(Box.createVerticalStrut(height)); }
    private static String money(double value) { return "\u20b9" + NumberFormat.getIntegerInstance(Locale.US).format(value); }
    private static String percent(double value) { return String.format(Locale.US, "%.1f%%", value); }
    private static String compactMoney(double value) {
        if (value >= 10000000) return String.format(Locale.US, "%.1fcr", value / 10000000);
        if (value >= 100000) return String.format(Locale.US, "%.1fL", value / 100000);
        return String.format(Locale.US, "%.1fk", value / 1000);
    }

    public static void main(String[] args) throws Exception {
        String[] controls = {"Label", "Button", "Slider", "Spinner", "TitledBorder"};
        for (String control : controls) UIManager.put(control + ".font", FONT);
        if (args.length >= 2 && args.length <= 5 && "--preview".equals(args[0])) {
            int stockPercent = args.length > 2 ? Integer.parseInt(args[2]) : 60;
            int period = args.length > 3 ? Integer.parseInt(args[3]) : 5;
            int startingAmount = args.length > 4 ? Integer.parseInt(args[4]) : 10000;
            if (stockPercent < 0 || stockPercent > 100 || period < 1 || period > 30
                    || startingAmount < 1000 || startingAmount > 100000000) {
                throw new IllegalArgumentException("Preview values are outside the dashboard input ranges.");
            }
            SwingUtilities.invokeAndWait(() -> {
                try {
                    DashboardPanel panel = new DashboardPanel(false);
                    panel.stockSlider.setValue(stockPercent);
                    panel.years.setValue(period);
                    panel.amount.setValue(startingAmount);
                    panel.runSimulation();
                    panel.setSize(1000, 680);
                    layoutTree(panel);
                    BufferedImage image = new BufferedImage(1000, 680, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    panel.printAll(g);
                    g.dispose();
                    ImageIO.write(image, "png", new File(args[1]));
                } catch (Exception ex) { throw new RuntimeException(ex); }
            });
        } else {
            SwingUtilities.invokeLater(() -> new PortfolioGUI().setVisible(true));
        }
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container) layoutTree((Container) child);
        }
    }
}
