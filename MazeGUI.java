import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MazeGUI extends JFrame {
    // Container Layouts
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private JPanel[][] gridCells;
    
    private static final String MENU_PANEL = "MENU";
    private static final String MAZE_PANEL = "MAZE";
    private static final int CELL_SIZE = 5; // Assuming a cell size
    // --- Fields ---
    private MazeMap currentMap;
    private AlgorithmStatusPanel statusGreedy;
    private AlgorithmStatusPanel statusAStar;
    private AlgorithmStatusPanel statusGA;

    private List<Point> lastGreedyPath = null; 
    private List<Point> lastAStarPath = null;  
    private List<Point> lastGAPath = null;
    private double cachedGAFitness = 0;
    private List<List<Point>> genPath = new ArrayList<>();
    private List<Double> genFitness = new ArrayList<>();

    // GA Settings
    private int gaPopSize = 20;
    private double gaMutationRate = 0.1;
    private double gacCossoverRate = 0.9;
    private int gaElitismCount = 5;
    private int gaMaxGenerations = 50;
    private int gaMutationMode = 0;

    // GUI Components
    private JComboBox<String> generationSelector;
    private JCheckBox chkShowGreedy;
    private JCheckBox chkShowAStar;
    private JCheckBox chkShowGA;
    private JButton btnBack;
    private JButton btnReset;
    private JButton btnGreedy;
    private JButton btnAStar;
    private JButton btnGA;
    private JButton btnSettings;
    
    

    public MazeGUI() {
        setTitle("Maze Solver (Locking Interface)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMenuScreen(), MENU_PANEL);
        mainContainer.add(new JPanel(), MAZE_PANEL);

        add(mainContainer);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createMenuScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        JButton startButton = new JButton("Select Maze File");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.addActionListener(e -> handleFileSelection());
        panel.add(startButton);
        return panel;
    }

    private void handleFileSelection() {
        JFileChooser fileChooser = new JFileChooser();
        File mazeDir = new File("MAZE");
        if(mazeDir.exists()) fileChooser.setCurrentDirectory(mazeDir);
        else fileChooser.setCurrentDirectory(new File("."));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Mazereader mr = new Mazereader();
            this.currentMap = new MazeMap(mr.read(fileChooser.getSelectedFile().getAbsolutePath()));
            lastGAPath = null;
            lastGreedyPath = null;
            lastAStarPath = null;
            genPath.clear();
            if (currentMap != null) buildAndShowMaze(currentMap);
        }
    }

    private void buildAndShowMaze(MazeMap map) {
        JPanel mazeGridPanel = new JPanel(new GridLayout(map.rows, map.cols));
        mazeGridPanel.setPreferredSize(new Dimension(map.cols * CELL_SIZE, map.rows * CELL_SIZE));
        gridCells = new JPanel[map.rows][map.cols];
        renderMaze(mazeGridPanel, map);

        JScrollPane scrollPane = new JScrollPane(mazeGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnBack = new JButton("Back");
        btnReset = new JButton("Clear / Reset");
        btnGreedy = new JButton("Run Greedy");
        btnAStar = new JButton("Run Pure A*");
        btnGA = new JButton("Run GA");
        btnSettings = new JButton("Settings");

        JPanel layersPanel = new JPanel(new GridLayout(3, 1));
        layersPanel.setBorder(BorderFactory.createTitledBorder("Show Layers"));
        JPanel replayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        replayPanel.setBorder(BorderFactory.createTitledBorder("Generation Replay"));

        chkShowGreedy = new JCheckBox("Greedy (Blue)");
        chkShowGreedy.setForeground(Color.BLUE);
        chkShowGreedy.setSelected(true);
        chkShowGreedy.addActionListener(e -> refreshMazeView());

        chkShowAStar = new JCheckBox("A* (Orange)");
        chkShowAStar.setForeground(Color.ORANGE.darker());
        chkShowAStar.setSelected(true);
        chkShowAStar.addActionListener(e -> refreshMazeView());

        chkShowGA = new JCheckBox("GA (Green)");
        chkShowGA.setForeground(Color.GREEN.darker());
        chkShowGA.setSelected(true);
        chkShowGA.addActionListener(e -> refreshMazeView());

        layersPanel.add(chkShowGreedy);
        layersPanel.add(chkShowAStar);
        layersPanel.add(chkShowGA);

        generationSelector = new JComboBox<>();
        generationSelector.setPreferredSize(new Dimension(150, 25));
        generationSelector.setEnabled(false);
        generationSelector.addItem("Run GA first...");
        generationSelector.addActionListener(e -> {
            if (generationSelector.isEnabled() && generationSelector.getItemCount() > 0) {
                int index = generationSelector.getSelectedIndex();
                if (index >= 0 && index < genPath.size()) {
                    updateMazeToGeneration(index);
                }
            }
        });
        replayPanel.add(generationSelector);

        JPanel dashboard = new JPanel();
        dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));
        dashboard.setPreferredSize(new Dimension(220, 0));
        dashboard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        statusGA = new AlgorithmStatusPanel("Genetic Algorithm");
        statusGreedy = new AlgorithmStatusPanel("Greedy");
        statusAStar = new AlgorithmStatusPanel("Pure A*");

        dashboard.add(statusGreedy);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusAStar);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusGA);
        dashboard.add(Box.createVerticalGlue());

        controlPanel.add(btnBack);
        controlPanel.add(btnReset);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(btnGreedy);
        controlPanel.add(btnAStar);
        controlPanel.add(btnGA);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(layersPanel); // Add Checkboxes
        controlPanel.add(replayPanel);
        controlPanel.add(btnSettings);

        btnGreedy.addActionListener(e -> {
            resetGridColors();
            List<Point> path = DumbDecoder.getGreedyPath(map);
            lastGreedyPath = path; // Save to specific variable
            refreshMazeView();
            statusGreedy.updateStats(path, map);
        });

        btnAStar.addActionListener(e -> {
            resetGridColors();
            List<Point> path = DumbDecoder.getPureAStarPath(map);
            lastAStarPath = path;  // Save to specific variable
            refreshMazeView();
            statusAStar.updateStats(path, map);
        });

        btnGA.addActionListener(e -> {
            resetGridColors();
            if (lastGAPath != null) {
                populateDropdownAndSelectLast();
                refreshMazeView();
                statusGA.updateStatsLive(gaMaxGenerations, gaMaxGenerations, lastGAPath, map, cachedGAFitness, "Cached Result");
            } else {
                runRealTimeGA(map);
            }
        });

        btnReset.addActionListener(e -> {
            resetGridColors();
            lastGAPath = null;
            genPath.clear();
            genFitness.clear();
            generationSelector.removeAllItems();
            generationSelector.addItem("Run GA first...");
            generationSelector.setEnabled(false);
            statusGA.setLoading("Memory Cleared");
        });

        btnBack.addActionListener(e -> cardLayout.show(mainContainer, MENU_PANEL));
        btnSettings.addActionListener(e -> showGASettings());

        JPanel View = new JPanel(new BorderLayout());
        View.add(controlPanel, BorderLayout.NORTH);
        View.add(scrollPane, BorderLayout.CENTER);
        View.add(dashboard, BorderLayout.EAST);

        mainContainer.add(View, MAZE_PANEL);
        cardLayout.show(mainContainer, MAZE_PANEL);
    }

    private void refreshMazeView() {
        resetGridColors();
        if (chkShowGA.isSelected() && lastGAPath != null) {
            drawPath(lastGAPath, Color.GREEN.darker());
        }
        if (chkShowGreedy.isSelected() && lastGreedyPath != null) {
            drawPath(lastGreedyPath, Color.BLUE);
        }
        
        if (chkShowAStar.isSelected() && lastAStarPath != null) {
            drawPath(lastAStarPath, Color.ORANGE);
        }
        
    }

    private void setControlsEnabled(boolean enabled) {
        btnBack.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnGreedy.setEnabled(enabled);
        btnAStar.setEnabled(enabled);
        btnGA.setEnabled(enabled);
        btnSettings.setEnabled(enabled);
        
        chkShowGreedy.setEnabled(enabled);
        chkShowAStar.setEnabled(enabled);
        chkShowGA.setEnabled(enabled);
        
        generationSelector.setEnabled(enabled && !genPath.isEmpty()); 
    }

    private void populateDropdownAndSelectLast() {
        generationSelector.setEnabled(false);
        generationSelector.removeAllItems();
        for(int i = 0; i < genPath.size(); i++) {
            generationSelector.addItem("Gen " + (i + 1));
        }
        generationSelector.setEnabled(true);
        if (generationSelector.getItemCount() > 0) {
            generationSelector.setSelectedIndex(generationSelector.getItemCount() - 1);
        }
    }

    private void updateMazeToGeneration(int index) {
        // Temporarily update the "cached" path to the historical one so the checkbox logic works
        lastGAPath = genPath.get(index);
        double historicalFit = genFitness.get(index);
        
        refreshMazeView(); // Uses the updated cachedGAPath
        
        statusGA.updateStatsLive(index + 1, gaMaxGenerations, lastGAPath, currentMap, historicalFit, "Replay Mode");
    }

    private void showGASettings() {
        JTextField txtPop = new JTextField(String.valueOf(gaPopSize));
        JTextField txtMut = new JTextField(String.valueOf(gaMutationRate));
        JTextField txtGen = new JTextField(String.valueOf(gaMaxGenerations));
        JTextField txtEli = new JTextField(String.valueOf(gaElitismCount));
        JTextField txtCro = new JTextField(String.valueOf(gacCossoverRate));
        JTextField txtMode = new JTextField(String.valueOf(gaMutationMode));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Population Size (e.g., 100):")); panel.add(txtPop);
        panel.add(new JLabel("Mutation Rate (0.0 - 1.0):")); panel.add(txtMut);
        panel.add(new JLabel("Cossover Rate (0.0 - 1.0):")); panel.add(txtCro);
        panel.add(new JLabel("Max Generations (e.g., 1000):")); panel.add(txtGen);
        panel.add(new JLabel("Elitism Count (e.g., 5):")); panel.add(txtEli);
        panel.add(new JLabel("Mutation Mode (0-2):")); panel.add(txtMode);

        int result = JOptionPane.showConfirmDialog(this, panel, "GA Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                gaPopSize = Integer.parseInt(txtPop.getText());
                gaMutationRate = Double.parseDouble(txtMut.getText());
                gaMaxGenerations = Integer.parseInt(txtGen.getText());
                gaElitismCount = Integer.parseInt(txtEli.getText());
                gacCossoverRate = Double.parseDouble(txtCro.getText());
                gaMutationMode = Integer.parseInt(txtMode.getText());
                
                lastGAPath = null;
                genPath.clear();
                generationSelector.removeAllItems();
                generationSelector.addItem("Settings Changed");
                generationSelector.setEnabled(false);
                statusGA.setLoading("Settings Updated");
                JOptionPane.showMessageDialog(this, "Settings Saved! Cache cleared.");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number entered!");
            }
        }
    }

    private void runRealTimeGA(MazeMap map) {
        setControlsEnabled(false);
        statusGA.setLoading("Initializing Population...");
        generationSelector.setEnabled(false);
        generationSelector.removeAllItems();
        generationSelector.addItem("Running...");
        genPath.clear();
        genFitness.clear();

        Thread gaThread = new Thread(() -> {
            int popSize = gaPopSize;        
            double mutationRate = gaMutationRate;
            double crossoverRate = gacCossoverRate;
            int elitismCount = gaElitismCount;
            int maxGenerations = gaMaxGenerations;
            int mutationMode = gaMutationMode;

            GeneticAlgorithm ga = new GeneticAlgorithm(map, popSize, mutationRate, crossoverRate, elitismCount);
            ArrayList<Chromosome> population = ga.initPopulation(null);
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMutationRate = mutationRate;
            boolean useHeuristic = false;

            for (int gen = 1; gen <= maxGenerations; gen++) {
                population = ga.evolve(population, useHeuristic, mutationMode);
                Collections.sort(population);
                Chromosome best = population.get(0);

                List<Point> rawPath = DumbDecoder.getPath(map, best, true);
                List<Point> visualPath = new ArrayList<>(rawPath);
                
                synchronized(genPath) {
                    genPath.add(visualPath);
                    genFitness.add(best.fitness);
                }

                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) {
                    stagnationCount++;
                } else {
                    stagnationCount = 0;
                    lastBestFitness = best.fitness;
                    ga.setMutationRate(defaultMutationRate);
                }
                if (stagnationCount > 50) ga.setMutationRate(0.4);

                String extraInfo = "";
                if (useHeuristic) extraInfo += "[A* Mode] ";
                if (stagnationCount > 50) extraInfo += "[Boost] ";
                final String statusText = extraInfo;
                final int currentGen = gen;
                final double currentFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    lastGAPath = visualPath;
                    refreshMazeView(); 
                    statusGA.updateStatsLive(currentGen, maxGenerations, visualPath, map, currentFit, statusText);
                });

                try { Thread.sleep(100); } catch (InterruptedException e) {} 
            }
            
            Chromosome finalBest = population.get(0);
            List<Point> finalPath = DumbDecoder.getPath(map, finalBest, true);
            lastGAPath = new ArrayList<>(finalPath);
            cachedGAFitness = finalBest.fitness;
            
            SwingUtilities.invokeLater(() -> {
                resetGridColors();
                drawPath(finalPath, Color.GREEN.darker());
                statusGA.updateStatsLive(maxGenerations, maxGenerations, finalPath, map, finalBest.fitness, "COMPLETED!");
                populateDropdownAndSelectLast();
                setControlsEnabled(true);
            });
        });
        gaThread.start();
    }

    private void renderMaze(JPanel panel, MazeMap map) {
        for (int r = 0; r < map.rows; r++) {
            for (int c = 0; c < map.cols; c++) {
                int val = map.getWeight(r,c);
                JPanel cell = new JPanel(new BorderLayout());
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                gridCells[r][c] = cell;
                setCellColor(cell, val);
                
                if(val > 0) {
                    JLabel lbl = new JLabel(String.valueOf(val));
                    lbl.setFont(new Font("Arial", Font.PLAIN, 5));
                    lbl.setHorizontalAlignment(JLabel.CENTER);
                    cell.add(lbl);
                }
                panel.add(cell);
            }
        }
    }

    private void drawPath(List<Point> path, Color c) {
        if(path == null) return;
        for(Point p : path) {
            if(p.r >= 0 && p.r < currentMap.rows && p.c >= 0 && p.c < currentMap.cols) {
                JPanel cell = gridCells[p.r][p.c];
                if(cell.getBackground() != Color.GREEN && cell.getBackground() != Color.RED)
                    cell.setBackground(c);
            }
        }
        repaint();
    }

    private void resetGridColors() {
        if (currentMap == null) return;
        for (int r = 0; r < currentMap.rows; r++) {
            for (int c = 0; c < currentMap.cols; c++) {
                setCellColor(gridCells[r][c], currentMap.getWeight(r,c));
            }
        }
        repaint();
    }

    private void setCellColor(JPanel cell, int val) {
        if (val == -1) cell.setBackground(Color.BLACK);
        else if (val == -2) cell.setBackground(Color.RED);
        else if (val == 0) cell.setBackground(Color.GREEN);
        else cell.setBackground(Color.WHITE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeGUI());
    }
}