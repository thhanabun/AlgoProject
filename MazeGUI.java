import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class MazeGUI extends JFrame {

    // --- CardLayout ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private static final String MENU_PANEL = "Menu";
    private static final String MAZE_PANEL = "Maze";

    // --- Maze Data ---
    private int CELL_SIZE = 5; 
    private JPanel[][] gridCells; 
    private MazeMap currentMap;

    // --- Panels & Components ---
    private AlgorithmStatusPanel statusGreedy;
    private AlgorithmStatusPanel statusAStar;
    private AlgorithmStatusPanel statusGA;

    // --- Data Storage ---
    private List<Point> lastGreedyPath = null; 
    private List<Point> lastAStarPath = null;  
    private List<Point> lastGAPath = null;
    
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
    private int simulationSpeed = 50; // Default delay (ms)

    // --- UI Controls ---
    private JCheckBox chkShowGreedy;
    private JCheckBox chkShowAStar;
    private JCheckBox chkShowGA;
    
    // Speed Controls
    private JSlider sliderSpeed;
    private JTextField txtSpeed;

    // Generation Controls
    private JSlider sliderGenerations;
    private JComboBox<String> cmbGenerations; // New Dropdown
    private JLabel lblGenVal;

    // Buttons
    private JButton btnBack;
    private JButton btnReset;
    private JButton btnGreedy;
    private JButton btnAStar;
    private JButton btnGA;
    private JButton btnReplay;
    private JButton btnSettings;

    public MazeGUI() {
        setTitle("Maze Solver Ultimate (Synced Controls)");
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
        // 1. GRID PANEL
        JPanel mazeGridPanel = new JPanel(new GridLayout(map.rows, map.cols));
        mazeGridPanel.setPreferredSize(new Dimension(map.cols * CELL_SIZE, map.rows * CELL_SIZE));
        gridCells = new JPanel[map.rows][map.cols];
        renderMaze(mazeGridPanel, map);
        
        JScrollPane scrollPane = new JScrollPane(mazeGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // 2. TOP CONTROL PANEL
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        btnBack = new JButton("Back");
        btnReset = new JButton("Clear");
        btnGreedy = new JButton("Greedy");
        btnAStar = new JButton("A*");
        btnGA = new JButton("Run GA");
        btnReplay = new JButton("Replay GA");
        btnSettings = new JButton("Settings");

        // Layers
        JPanel layersPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        layersPanel.setBorder(BorderFactory.createTitledBorder("Layers"));
        chkShowGreedy = new JCheckBox("Greedy"); chkShowGreedy.setForeground(Color.BLUE); chkShowGreedy.setSelected(true);
        chkShowAStar = new JCheckBox("A*"); chkShowAStar.setForeground(Color.ORANGE.darker()); chkShowAStar.setSelected(true);
        chkShowGA = new JCheckBox("GA"); chkShowGA.setForeground(Color.GREEN.darker()); chkShowGA.setSelected(true);
        
        chkShowGreedy.addActionListener(e -> refreshMazeView());
        chkShowAStar.addActionListener(e -> refreshMazeView());
        chkShowGA.addActionListener(e -> refreshMazeView());
        
        layersPanel.add(chkShowGreedy);
        layersPanel.add(chkShowAStar);
        layersPanel.add(chkShowGA);

        // --- SPEED CONTROL (Slider + Text) ---
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        speedPanel.setBorder(BorderFactory.createTitledBorder("Speed (ms delay)"));
        
        sliderSpeed = new JSlider(0, 500, simulationSpeed);
        sliderSpeed.setPreferredSize(new Dimension(100, 20));
        
        txtSpeed = new JTextField(String.valueOf(simulationSpeed), 3);
        txtSpeed.setHorizontalAlignment(JTextField.CENTER);
        
        // Sync Logic: Slider -> Text
        sliderSpeed.addChangeListener(e -> {
            simulationSpeed = sliderSpeed.getValue();
            txtSpeed.setText(String.valueOf(simulationSpeed));
        });
        
        // Sync Logic: Text -> Slider
        txtSpeed.addActionListener(e -> {
            try {
                int val = Integer.parseInt(txtSpeed.getText());
                val = Math.max(0, Math.min(500, val)); // Clamp 0-500
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
        topPanel.add(btnGA);
        topPanel.add(btnReplay);
        topPanel.add(btnSettings);
        topPanel.add(layersPanel);

        // 3. BOTTOM PANEL (REPLAY)
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
        
        // --- SYNC: Slider -> Dropdown ---
        sliderGenerations.addChangeListener(e -> {
            if (!sliderGenerations.getValueIsAdjusting() && !genPath.isEmpty()) {
                int index = sliderGenerations.getValue();
                if (index >= 0 && index < genPath.size()) {
                    // Update Dropdown without triggering its listener loop
                    cmbGenerations.removeItemListener(this::generationDropdownAction); // Detach
                    if (index < cmbGenerations.getItemCount()) cmbGenerations.setSelectedIndex(index);
                    cmbGenerations.addItemListener(this::generationDropdownAction); // Re-attach
                    
                    updateMazeToGeneration(index);
                }
            }
        });

        // --- SYNC: Dropdown -> Slider ---
        cmbGenerations.addItemListener(this::generationDropdownAction);

        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftBottom.add(cmbGenerations);
        leftBottom.add(lblGenVal);

        bottomPanel.add(sliderGenerations, BorderLayout.CENTER);
        bottomPanel.add(leftBottom, BorderLayout.EAST);

        // 4. DASHBOARD
        JPanel dashboard = new JPanel();
        dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));
        dashboard.setPreferredSize(new Dimension(200, 0));
        dashboard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        statusGreedy = new AlgorithmStatusPanel("Greedy");
        statusAStar = new AlgorithmStatusPanel("Pure A*");
        statusGA = new AlgorithmStatusPanel("Genetic Algorithm");
        
        dashboard.add(statusGreedy);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusAStar);
        dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusGA);
        dashboard.add(Box.createVerticalGlue());

        setupButtonActions(map);

        JPanel View = new JPanel(new BorderLayout());
        View.add(topPanel, BorderLayout.NORTH);
        View.add(scrollPane, BorderLayout.CENTER);
        View.add(dashboard, BorderLayout.EAST);
        View.add(bottomPanel, BorderLayout.SOUTH);

        mainContainer.add(View, MAZE_PANEL);
        cardLayout.show(mainContainer, MAZE_PANEL);
    }

    // Separate Listener method to easily attach/detach
    private void generationDropdownAction(java.awt.event.ItemEvent e) {
        if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            if (cmbGenerations.isEnabled() && !genPath.isEmpty()) {
                int index = cmbGenerations.getSelectedIndex();
                if (index >= 0) {
                    sliderGenerations.setValue(index); // This will trigger the slider listener
                }
            }
        }
    }

    private void setupButtonActions(MazeMap map) {
        btnGreedy.addActionListener(e -> {
            List<Point> path = DumbDecoder.getGreedyPath(map);
            lastGreedyPath = path;
            refreshMazeView();
            statusGreedy.updateStats(path, map);
        });

        btnAStar.addActionListener(e -> {
            List<Point> path = DumbDecoder.getPureAStarPath(map); 
            lastAStarPath = path;
            refreshMazeView();
            statusAStar.updateStats(path, map);
        });

        btnGA.addActionListener(e -> {
            if (lastGAPath != null) {
                // If cached, just setup UI
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
        });
        
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, MENU_PANEL));
        btnSettings.addActionListener(e -> showGASettings());
    }

    private void setupReplayControls() {
        // Fill Dropdown
        cmbGenerations.removeItemListener(this::generationDropdownAction); // Detach temporarily
        cmbGenerations.removeAllItems();
        for(int i=0; i<genPath.size(); i++) {
            cmbGenerations.addItem("Gen " + (i+1));
        }
        cmbGenerations.setEnabled(true);
        cmbGenerations.addItemListener(this::generationDropdownAction); // Re-attach

        // Setup Slider
        sliderGenerations.setMaximum(genPath.size() - 1);
        sliderGenerations.setValue(genPath.size() - 1);
        sliderGenerations.setEnabled(true);
        
        // Select Last in Dropdown
        if (cmbGenerations.getItemCount() > 0) {
            cmbGenerations.setSelectedIndex(cmbGenerations.getItemCount() - 1);
        }
    }

    private void refreshMazeView() {
        resetGridColors();
        if (chkShowGreedy.isSelected() && lastGreedyPath != null) drawPath(lastGreedyPath, Color.BLUE);
        if (chkShowAStar.isSelected() && lastAStarPath != null) drawPath(lastAStarPath, Color.ORANGE);
        if (chkShowGA.isSelected() && lastGAPath != null) drawPath(lastGAPath, Color.GREEN.darker());
    }

    private void runReplayAnimation() {
        setControlsEnabled(false); // Lock controls
        statusGA.setLoading("Replaying History...");

        Thread replayThread = new Thread(() -> {
            for (int i = 0; i < genPath.size(); i++) {
                final int index = i;
                SwingUtilities.invokeLater(() -> {
                    // Update slider (which triggers grid update)
                    sliderGenerations.setValue(index);
                });

                try { Thread.sleep(simulationSpeed); } catch (InterruptedException e) {}
            }

            SwingUtilities.invokeLater(() -> {
                statusGA.setLoading("Replay Finished");
                setControlsEnabled(true); // Unlock
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

    private void runRealTimeGA(MazeMap map) {
        setControlsEnabled(false);
        statusGA.setLoading("Initializing...");
        
        genPath.clear();
        genFitness.clear();
        sliderGenerations.setValue(0);
        sliderGenerations.setEnabled(false);
        cmbGenerations.removeAllItems();
        cmbGenerations.setEnabled(false);

        Thread gaThread = new Thread(() -> {
            int popSize = gaPopSize;         
            double mutationRate = gaMutationRate; 
            double crossoverRate = gaCrossoverRate;
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

                final String statusText = (stagnationCount > 50) ? "[Boost]" : "";
                final int currentGen = gen;
                final double currentFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    lastGAPath = visualPath;
                    refreshMazeView();
                    statusGA.updateStatsLive(currentGen, maxGenerations, visualPath, map, currentFit, statusText);
                    
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
                statusGA.updateStatsLive(maxGenerations, maxGenerations, finalPath, map, finalBest.fitness, "COMPLETED!");
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
        btnGA.setEnabled(enabled);
        btnSettings.setEnabled(enabled);
        sliderSpeed.setEnabled(enabled);
        txtSpeed.setEnabled(enabled);
        chkShowGreedy.setEnabled(enabled);
        chkShowAStar.setEnabled(enabled);
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
                   lbl.setFont(new Font("Arial", Font.PLAIN, 8));
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