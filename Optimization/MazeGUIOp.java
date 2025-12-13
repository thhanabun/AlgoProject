package Optimization;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;

public class MazeGUIOp extends JFrame {

    // --- CardLayout ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private static final String MENU_PANEL = "Menu";
    private static final String MAZE_PANEL = "Maze";

    // --- Maze Data ---
    private MazeMapOp currentMap;
    
    // --- ZOOMABLE RENDERER ---
    private MazeRenderPanel mazeCanvas; 
    private JScrollPane mazeScrollPane; // Reference needed for centering zoom

    // --- Panels & Components ---
    private AlgorithmStatusPanel statusGreedy;
    private AlgorithmStatusPanel statusAStar;
    private AlgorithmStatusPanel statusDijk;
    private AlgorithmStatusPanel statusGA;

    // --- Data Storage ---
    private List<Point> lastGreedyPath = null; 
    private List<Point> lastAStarPath = null;  
    private List<Point> lastGAPath = null;
    private List<Point> lastDijkPath = null;  
    
    private double cachedGAFitness = 0;
    private List<List<Point>> genPath = new ArrayList<>();
    private List<Double> genFitness = new ArrayList<>();

    // --- GA Parameters ---
    private int gaPopSize = 20;         
    private double gaMutationRate = 0.1; 
    private double gaCrossoverRate = 0.9;
    private int gaElitismCount = 5;
    private int gaMaxGenerations = 50;
    private int gaMutationMode = 0;
    private int simulationSpeed = 100; 

    private JCheckBox chkShowGreedy;
    private JCheckBox chkShowAStar;
    private JCheckBox chkShowDijk;
    private JCheckBox chkShowGA;
    
    private JSlider sliderSpeed;
    private JTextField txtSpeed;

    private JSlider sliderGenerations;
    private JComboBox<String> cmbGenerations; 
    private JLabel lblGenVal;

    private JButton btnBack;
    private JButton btnReset;
    private JButton btnGreedy;
    private JButton btnAStar;
    private JButton btnDijk;
    private JButton btnGA;
    private JButton btnReplay;
    private JButton btnSettings;

    public MazeGUIOp() {
        setTitle("Maze Solver Ultimate (Zoomable + Optimized)");
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
            Reader r = new Reader();
            int[][] rawData = r.read(fileChooser.getSelectedFile().getAbsolutePath());
            this.currentMap = new MazeMapOp(rawData);
            
            lastGAPath = null;
            lastGreedyPath = null;
            lastAStarPath = null;
            lastDijkPath = null;
            genPath.clear();
            
            if (currentMap != null) buildAndShowMaze(currentMap);
        }
    }

    private void buildAndShowMaze(MazeMapOp map) {
        // --- ZOOM SETUP ---
        // 1. We create the canvas based on 1:1 pixel ratio initially.
        mazeCanvas = new MazeRenderPanel(map.cols, map.rows);
        
        // 2. Determine initial Zoom level so it fits nicely on screen
        // If map is huge (1000x1000), zoom is 1.0 (fits in scroll).
        // If map is small (20x20), zoom is 20.0 (big).
        double initialZoom = 1.0;
        if (map.rows < 50) initialZoom = 15.0;
        else if (map.rows < 100) initialZoom = 8.0;
        else if (map.rows < 500) initialZoom = 2.0;
        
        mazeCanvas.setZoom(initialZoom);

        // Run rendering in background to avoid UI blip on load
        new Thread(() -> {
            mazeCanvas.renderBaseMap(map); 
            SwingUtilities.invokeLater(mazeCanvas::repaint);
        }).start();
        
        mazeScrollPane = new JScrollPane(mazeCanvas);
        mazeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mazeScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        // Add MouseWheelListener to the ScrollPane to handle Zooming
        mazeScrollPane.addMouseWheelListener(e -> {
            if (e.isControlDown() || true) { // Default to zoom without Ctrl if desired
                int notches = e.getWheelRotation();
                double zoomFactor = 1.1;
                if (notches < 0) {
                    mazeCanvas.zoomIn(zoomFactor); // Scroll Up -> Zoom In
                } else {
                    mazeCanvas.zoomOut(zoomFactor); // Scroll Down -> Zoom Out
                }
                
                // Optional: Stop scrollbar from moving when zooming
                e.consume(); 
            }
        });

        // --- TOP CONTROLS ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        btnBack = new JButton("Back");
        btnReset = new JButton("Clear");
        btnGreedy = new JButton("Greedy");
        btnAStar = new JButton("A*");
        btnDijk = new JButton("Dijkstra");
        btnGA = new JButton("Run GA");
        btnReplay = new JButton("Replay GA");
        btnSettings = new JButton("Settings");

        // Layers
        JPanel layersPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        layersPanel.setBorder(BorderFactory.createTitledBorder("Layers"));
        chkShowGreedy = new JCheckBox("Greedy"); chkShowGreedy.setForeground(Color.BLUE); chkShowGreedy.setSelected(true);
        chkShowAStar = new JCheckBox("A*"); chkShowAStar.setForeground(Color.ORANGE.darker()); chkShowAStar.setSelected(true);
        chkShowDijk = new JCheckBox("Dijkstra"); chkShowDijk.setForeground(Color.PINK.darker()); chkShowDijk.setSelected(true);
        chkShowGA = new JCheckBox("GA"); chkShowGA.setForeground(Color.GREEN.darker()); chkShowGA.setSelected(true);
        
        chkShowGreedy.addActionListener(e -> refreshMazeView());
        chkShowAStar.addActionListener(e -> refreshMazeView());
        chkShowDijk.addActionListener(e-> refreshMazeView());
        chkShowGA.addActionListener(e -> refreshMazeView());
        
        layersPanel.add(chkShowGreedy);
        layersPanel.add(chkShowAStar);
        layersPanel.add(chkShowDijk);
        layersPanel.add(chkShowGA);

        // --- SPEED CONTROL ---
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        speedPanel.setBorder(BorderFactory.createTitledBorder("Speed (ms delay)"));
        
        sliderSpeed = new JSlider(0, 500, simulationSpeed);
        sliderSpeed.setPreferredSize(new Dimension(100, 20));
        
        txtSpeed = new JTextField(String.valueOf(simulationSpeed), 3);
        txtSpeed.setHorizontalAlignment(JTextField.CENTER);
        
        sliderSpeed.addChangeListener(e -> {
            simulationSpeed = sliderSpeed.getValue();
            txtSpeed.setText(String.valueOf(simulationSpeed));
        });
        
        txtSpeed.addActionListener(e -> {
            try {
                int val = Integer.parseInt(txtSpeed.getText());
                val = Math.max(0, Math.min(1000, val)); 
                sliderSpeed.setValue(val);
                simulationSpeed = val;
            } catch (NumberFormatException ex) {
                txtSpeed.setText(String.valueOf(sliderSpeed.getValue()));
            }
        });

        speedPanel.add(sliderSpeed);
        speedPanel.add(txtSpeed);

        topPanel.add(btnBack);
        topPanel.add(btnReset);
        topPanel.add(speedPanel);
        topPanel.add(btnGreedy);
        topPanel.add(btnAStar);
        topPanel.add(btnDijk);
        topPanel.add(btnGA);
        topPanel.add(btnReplay);
        topPanel.add(btnSettings);
        topPanel.add(layersPanel);

        // BOTTOM PANEL
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Generation Replay History"));
        
        sliderGenerations = new JSlider(0, 0, 0);
        sliderGenerations.setEnabled(false);
        sliderGenerations.setMajorTickSpacing(10);
        sliderGenerations.setPaintTicks(true);
        
        cmbGenerations = new JComboBox<>();
        cmbGenerations.setPreferredSize(new Dimension(120, 25));
        cmbGenerations.setEnabled(false);
        
        lblGenVal = new JLabel("Gen: 0 / 0  ");
        lblGenVal.setFont(new Font("Arial", Font.BOLD, 14));
        
        sliderGenerations.addChangeListener(e -> {
            if (!sliderGenerations.getValueIsAdjusting() && !genPath.isEmpty()) {
                int index = sliderGenerations.getValue();
                if (index >= 0 && index < genPath.size()) {
                    cmbGenerations.removeItemListener(this::generationDropdownAction); 
                    if (index < cmbGenerations.getItemCount()) cmbGenerations.setSelectedIndex(index);
                    cmbGenerations.addItemListener(this::generationDropdownAction); 
                    updateMazeToGeneration(index);
                }
            }
        });

        cmbGenerations.addItemListener(this::generationDropdownAction);

        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftBottom.add(cmbGenerations);
        leftBottom.add(lblGenVal);

        bottomPanel.add(sliderGenerations, BorderLayout.CENTER);
        bottomPanel.add(leftBottom, BorderLayout.EAST);

        // DASHBOARD
        JPanel dashboard = new JPanel();
        dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));
        dashboard.setPreferredSize(new Dimension(200, 0));
        dashboard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        statusGreedy = new AlgorithmStatusPanel("Greedy");
        statusAStar = new AlgorithmStatusPanel("Pure A*");
        statusDijk = new AlgorithmStatusPanel("Dijkstra");
        statusGA = new AlgorithmStatusPanel("Genetic Algorithm");
        
        dashboard.add(statusGreedy);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusAStar);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusDijk);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusGA);
        dashboard.add(Box.createVerticalStrut(5));

        setupButtonActions(map);

        JPanel View = new JPanel(new BorderLayout());
        View.add(topPanel, BorderLayout.NORTH);
        View.add(mazeScrollPane, BorderLayout.CENTER); // Changed to scrollPane
        View.add(dashboard, BorderLayout.EAST);
        View.add(bottomPanel, BorderLayout.SOUTH);

        mainContainer.add(View, MAZE_PANEL);
        cardLayout.show(mainContainer, MAZE_PANEL);
    }

    private void generationDropdownAction(java.awt.event.ItemEvent e) {
        if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            if (cmbGenerations.isEnabled() && !genPath.isEmpty()) {
                int index = cmbGenerations.getSelectedIndex();
                if (index >= 0) {
                    sliderGenerations.setValue(index); 
                }
            }
        }
    }

    private void runBackgroundAlgo(String name, Supplier<List<Point>> algorithm, AlgorithmStatusPanel statusPanel, java.util.function.Consumer<List<Point>> onDone) {
        setControlsEnabled(false);
        statusPanel.setLoading("Calculating...");
        
        new Thread(() -> {
            //long startTime = System.currentTimeMillis();
            List<Point> path = algorithm.get();
            // long duration = System.currentTimeMillis() - startTime;
            
            SwingUtilities.invokeLater(() -> {
                onDone.accept(path);
                refreshMazeView();
                statusPanel.updateStats(path, currentMap);
                setControlsEnabled(true);
            });
        }).start();
    }

    private void setupButtonActions(MazeMapOp map) {
        btnGreedy.addActionListener(e -> {
            runBackgroundAlgo("Greedy", 
                () -> DumbDecoder.getGreedyPath(map), 
                statusGreedy, 
                path -> lastGreedyPath = path
            );
        });

        btnAStar.addActionListener(e -> {
            runBackgroundAlgo("A*", 
                () -> DumbDecoder.getPureAStarPath(map), 
                statusAStar, 
                path -> lastAStarPath = path
            );
        });

        btnDijk.addActionListener(e -> {
            runBackgroundAlgo("Dijkstra", 
                () -> DumbDecoder.getDijkstraPath(map), 
                statusDijk, 
                path -> lastDijkPath = path
            );
        });

        btnGA.addActionListener(e -> {
            if (lastGAPath != null) {
                setupReplayControls();
                refreshMazeView();
                statusGA.updateStatsLive(gaMaxGenerations, gaMaxGenerations, lastGAPath, map, cachedGAFitness, "Cached Result");
            } else {
                runRealTimeGA(map); 
            }
        });

        btnReplay.addActionListener(e -> {
            if (genPath != null && !genPath.isEmpty()) {
                runReplayAnimation();
            } else {
                JOptionPane.showMessageDialog(this, "No GA history to replay. Run GA first.");
            }
        });

        btnReset.addActionListener(e -> {
            lastGreedyPath = null;
            lastAStarPath = null;
            lastDijkPath = null;
            lastGAPath = null;
            genPath.clear();
            genFitness.clear();
            
            refreshMazeView();
            
            sliderGenerations.setValue(0);
            sliderGenerations.setEnabled(false);
            cmbGenerations.removeAllItems();
            cmbGenerations.setEnabled(false);
            lblGenVal.setText("Gen: 0 / 0");
            
            statusGA.setLoading("Memory Cleared");
            statusGreedy.setLoading("Waiting...");
            statusAStar.setLoading("Waiting...");
            statusDijk.setLoading("Waiting...");
        });
        
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, MENU_PANEL));
        btnSettings.addActionListener(e -> showGASettings());
    }

    private void setupReplayControls() {
        cmbGenerations.removeItemListener(this::generationDropdownAction); 
        cmbGenerations.removeAllItems();
        for(int i=0; i<genPath.size(); i++) {
            cmbGenerations.addItem("Gen " + (i+1));
        }
        cmbGenerations.setEnabled(true);
        cmbGenerations.addItemListener(this::generationDropdownAction); 

        sliderGenerations.setMaximum(genPath.size() - 1);
        sliderGenerations.setValue(genPath.size() - 1);
        sliderGenerations.setEnabled(true);
        
        if (cmbGenerations.getItemCount() > 0) {
            cmbGenerations.setSelectedIndex(cmbGenerations.getItemCount() - 1);
        }
    }

    private void refreshMazeView() {
        if (mazeCanvas == null) return;
        mazeCanvas.resetToBase();
        if (chkShowGreedy.isSelected() && lastGreedyPath != null) mazeCanvas.overlayPath(lastGreedyPath, Color.BLUE);
        if (chkShowAStar.isSelected() && lastAStarPath != null) mazeCanvas.overlayPath(lastAStarPath, Color.ORANGE);
        if (chkShowDijk.isSelected() && lastDijkPath != null) mazeCanvas.overlayPath(lastDijkPath, Color.PINK);
        if (chkShowGA.isSelected() && lastGAPath != null) mazeCanvas.overlayPath(lastGAPath, Color.GREEN.darker());
        mazeCanvas.repaint();
    }

    private void runReplayAnimation() {
        setControlsEnabled(false); 
        statusGA.setLoading("Replaying History...");

        Thread replayThread = new Thread(() -> {
            for (int i = 0; i < genPath.size(); i++) {
                final int index = i;
                SwingUtilities.invokeLater(() -> sliderGenerations.setValue(index));
                try { Thread.sleep(simulationSpeed); } catch (InterruptedException e) {}
            }
            SwingUtilities.invokeLater(() -> {
                statusGA.setLoading("Replay Finished");
                setControlsEnabled(true); 
            });
        });
        replayThread.start();
    }

    private void updateMazeToGeneration(int index) {
        lastGAPath = genPath.get(index);
        double historicalFit = genFitness.get(index);
        refreshMazeView();
        lblGenVal.setText("Gen: " + (index + 1) + " / " + genPath.size() + "  ");
        statusGA.updateStatsLive(index + 1, gaMaxGenerations, lastGAPath, currentMap, historicalFit, "Replay Mode");
    }

    private void runRealTimeGA(MazeMapOp map) {
        setControlsEnabled(false);
        statusGA.setLoading("Initializing...");
        
        genPath.clear();
        genFitness.clear();
        sliderGenerations.setValue(0);
        sliderGenerations.setEnabled(false);
        cmbGenerations.removeAllItems();
        cmbGenerations.setEnabled(false);

        Thread gaThread = new Thread(() -> {
            GeneticAlgorithm ga = new GeneticAlgorithm(map, gaPopSize, gaMutationRate, gaCrossoverRate, gaElitismCount);
            ArrayList<Chromosome> population = ga.initPopulation(null);
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMutationRate = gaMutationRate;
            boolean useHeuristic = false;

            for (int gen = 1; gen <= gaMaxGenerations; gen++) {
                population = ga.evolve(population, useHeuristic, gaMutationMode);
                Collections.sort(population);
                Chromosome best = population.get(0);

                List<Point> rawPath = DumbDecoder.getPath(map, best, true);
                List<Point> visualPath = new ArrayList<>(rawPath); 
                
                synchronized(genPath) {
                    genPath.add(visualPath);
                    genFitness.add(best.fitness);
                }

                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) stagnationCount++; 
                else {
                    stagnationCount = 0; 
                    lastBestFitness = best.fitness; 
                    ga.setMutationRate(defaultMutationRate); 
                }
                if (stagnationCount > 50) ga.setMutationRate(0.4); 

                final String statusText = (stagnationCount > 50) ? "[Boost]" : "";
                final int currentGen = gen;
                final double currentFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    lastGAPath = visualPath;
                    refreshMazeView();
                    statusGA.updateStatsLive(currentGen, gaMaxGenerations, visualPath, map, currentFit, statusText);
                    sliderGenerations.setMaximum(currentGen - 1);
                    sliderGenerations.setValue(currentGen - 1);
                    lblGenVal.setText("Gen: " + currentGen + "  ");
                });
                try { Thread.sleep(simulationSpeed); } catch (InterruptedException e) {} 
            }
            
            Chromosome finalBest = population.get(0);
            List<Point> finalPath = DumbDecoder.getPath(map, finalBest, true);
            lastGAPath = new ArrayList<>(finalPath);
            cachedGAFitness = finalBest.fitness;

            SwingUtilities.invokeLater(() -> {
                refreshMazeView();
                statusGA.updateStatsLive(gaMaxGenerations, gaMaxGenerations, finalPath, map, finalBest.fitness, "COMPLETED!");
                setupReplayControls();
                setControlsEnabled(true);
            });
        });
        gaThread.start();
    }

    private void setControlsEnabled(boolean enabled) {
        btnBack.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnGreedy.setEnabled(enabled);
        btnAStar.setEnabled(enabled);
        btnDijk.setEnabled(enabled);
        btnGA.setEnabled(enabled);
        btnSettings.setEnabled(enabled);
        sliderSpeed.setEnabled(enabled);
        txtSpeed.setEnabled(enabled);
        chkShowGreedy.setEnabled(enabled);
        chkShowAStar.setEnabled(enabled);
        chkShowDijk.setEnabled(enabled);
        chkShowGA.setEnabled(enabled);
        
        boolean hasHistory = !genPath.isEmpty();
        sliderGenerations.setEnabled(enabled && hasHistory);
        cmbGenerations.setEnabled(enabled && hasHistory);
    }

    private void showGASettings() {
        JTextField txtPop = new JTextField(String.valueOf(gaPopSize));
        JTextField txtMut = new JTextField(String.valueOf(gaMutationRate));
        JTextField txtGen = new JTextField(String.valueOf(gaMaxGenerations));
        JTextField txtEli = new JTextField(String.valueOf(gaElitismCount));
        JTextField txtCro = new JTextField(String.valueOf(gaCrossoverRate));
        JTextField txtMode = new JTextField(String.valueOf(gaMutationMode));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Population Size:")); panel.add(txtPop);
        panel.add(new JLabel("Mutation Rate:")); panel.add(txtMut);
        panel.add(new JLabel("Crossover Rate:")); panel.add(txtCro);
        panel.add(new JLabel("Max Generations:")); panel.add(txtGen);
        panel.add(new JLabel("Elitism Count:")); panel.add(txtEli);
        panel.add(new JLabel("Mutation Mode (0-2):")); panel.add(txtMode);

        int result = JOptionPane.showConfirmDialog(this, panel, "GA Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
             try {
                gaPopSize = Integer.parseInt(txtPop.getText());
                gaMutationRate = Double.parseDouble(txtMut.getText());
                gaMaxGenerations = Integer.parseInt(txtGen.getText());
                gaElitismCount = Integer.parseInt(txtEli.getText());
                gaCrossoverRate = Double.parseDouble(txtCro.getText());
                gaMutationMode = Integer.parseInt(txtMode.getText());
                
                lastGAPath = null; 
                genPath.clear();
                sliderGenerations.setEnabled(false);
                sliderGenerations.setValue(0);
                cmbGenerations.removeAllItems();
                cmbGenerations.setEnabled(false);
                lblGenVal.setText("Gen: 0 / 0");
                
                statusGA.setLoading("Settings Updated");
                JOptionPane.showMessageDialog(this, "Settings Saved! Cache cleared.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid Input!");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MazeGUIOp());
    }

}