package DumbestGA;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DumbestGUI extends JFrame {

    // --- Layout Components ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private static final String MENU_PANEL = "Menu";
    private static final String MAZE_PANEL = "Maze";

    // --- Maze Data ---
    private int CELL_SIZE = 5; 
    private JPanel[][] gridCells; 
    private MazeMap currentMap;

    // --- Status Panels ---
    private AlgorithmStatusPanel statusGreedy;
    private AlgorithmStatusPanel statusAStar;
    private AlgorithmStatusPanel statusGA;

    // --- Data Storage ---
    private List<Point> lastGreedyPath = null; 
    private List<Point> lastAStarPath = null;   
    private List<Point> lastGAPath = null;
    private boolean[] lastJunctionBlocks = null; // เก็บ Junction Block ของตัว Best
    
    // เก็บประวัติทุก Gen เพื่อทำ Slider Replay
    private List<List<Point>> genPathHistory = new ArrayList<>();
    private List<boolean[]> genJunctionBlockHistory = new ArrayList<>(); 
    private List<Double> genFitnessHistory = new ArrayList<>();

    // --- GA Parameters ---
    private int gaPopSize = 100;        
    private double gaMutationRate = 0.1; 
    private double gaCrossoverRate = 0.9;
    private int gaElitismCount = 5;
    private int gaMaxGenerations = 1000;
    private int gaMutationMode = Chromosome2.MUTATION_HYBRID;
    private int simulationSpeed = 20; // ms

    // --- UI Controls ---
    private JCheckBox chkShowGreedy;
    private JCheckBox chkShowAStar;
    private JCheckBox chkShowGA;
    private JCheckBox chkShowGlobal;   // [NEW] Global Dead Ends
    private JCheckBox chkShowJunction; // [NEW] Local Junction Blocks
    
    private JSlider sliderSpeed;
    private JTextField txtSpeed;
    private JSlider sliderGenerations;
    private JLabel lblGenVal;

    private JButton btnBack;
    private JButton btnReset;
    private JButton btnGreedy;
    private JButton btnAStar;
    private JButton btnGA;
    private JButton btnSettings;

    public DumbestGUI() {
        setTitle("Dumbest GUI (Hybrid Memory Visualizer)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 900); 

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
            
            resetData(); // Reset ทุกอย่างรวมถึง GlobalKnowledge
            
            if (currentMap != null) buildAndShowMaze(currentMap);
        }
    }

    private void resetData() {
        lastGAPath = null;
        lastJunctionBlocks = null;
        lastGreedyPath = null;
        lastAStarPath = null;
        genPathHistory.clear();
        genJunctionBlockHistory.clear();
        genFitnessHistory.clear();
        
        // *** สำคัญ: Reset Global Knowledge ***
        if (currentMap != null) {
            GlobalKnowledge.init(currentMap.rows, currentMap.cols);
        }
    }

    private void buildAndShowMaze(MazeMap map) {
        // 1. GRID PANEL
        JPanel mazeGridPanel = new JPanel(new GridLayout(map.rows, map.cols));
        if (map.rows > 100) CELL_SIZE = 4;
        else if (map.rows > 50) CELL_SIZE = 8;
        else CELL_SIZE = 15;

        mazeGridPanel.setPreferredSize(new Dimension(map.cols * CELL_SIZE, map.rows * CELL_SIZE));
        gridCells = new JPanel[map.rows][map.cols];
        renderMaze(mazeGridPanel, map);
        
        JScrollPane scrollPane = new JScrollPane(mazeGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // 2. CONTROL PANEL
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        btnBack = new JButton("Back");
        btnReset = new JButton("Reset");
        btnGreedy = new JButton("Greedy");
        btnAStar = new JButton("A*");
        btnGA = new JButton("Run GA2");
        btnSettings = new JButton("Settings");

        // Layers Checkboxes
        JPanel layersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        layersPanel.setBorder(BorderFactory.createTitledBorder("Layers"));
        
        chkShowGreedy = new JCheckBox("Greedy"); 
        chkShowGreedy.setForeground(Color.BLUE); 
        chkShowGreedy.setSelected(true);
        
        chkShowAStar = new JCheckBox("A*"); 
        chkShowAStar.setForeground(Color.ORANGE.darker()); 
        chkShowAStar.setSelected(true);
        
        chkShowGA = new JCheckBox("GA Path"); 
        chkShowGA.setForeground(Color.GREEN.darker()); 
        chkShowGA.setSelected(true);
        
        chkShowGlobal = new JCheckBox("Global Dead Ends"); 
        chkShowGlobal.setForeground(new Color(139, 0, 0)); // Dark Red
        chkShowGlobal.setSelected(true);

        chkShowJunction = new JCheckBox("Junction Blocks"); 
        chkShowJunction.setForeground(new Color(255, 100, 100)); // Light Red
        chkShowJunction.setSelected(true);
        
        Runnable refreshAction = this::refreshMazeView;
        chkShowGreedy.addActionListener(e -> refreshAction.run());
        chkShowAStar.addActionListener(e -> refreshAction.run());
        chkShowGA.addActionListener(e -> refreshAction.run());
        chkShowGlobal.addActionListener(e -> refreshAction.run());
        chkShowJunction.addActionListener(e -> refreshAction.run());
        
        layersPanel.add(chkShowGreedy);
        layersPanel.add(chkShowAStar);
        layersPanel.add(chkShowGA);
        layersPanel.add(chkShowGlobal);
        layersPanel.add(chkShowJunction);

        // Speed
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        speedPanel.setBorder(BorderFactory.createTitledBorder("Delay (ms)"));
        sliderSpeed = new JSlider(0, 100, simulationSpeed);
        sliderSpeed.setPreferredSize(new Dimension(80, 20));
        txtSpeed = new JTextField(String.valueOf(simulationSpeed), 3);
        
        sliderSpeed.addChangeListener(e -> {
            simulationSpeed = sliderSpeed.getValue();
            txtSpeed.setText(String.valueOf(simulationSpeed));
        });
        speedPanel.add(sliderSpeed);
        speedPanel.add(txtSpeed);

        topPanel.add(btnBack);
        topPanel.add(btnReset);
        topPanel.add(speedPanel);
        topPanel.add(layersPanel);
        topPanel.add(btnGreedy);
        topPanel.add(btnAStar);
        topPanel.add(btnGA);
        topPanel.add(btnSettings);

        // 3. TIMELINE
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Generation Timeline"));
        
        sliderGenerations = new JSlider(0, 0, 0);
        sliderGenerations.setEnabled(false);
        lblGenVal = new JLabel("Gen: 0 / 0  ");
        lblGenVal.setFont(new Font("Arial", Font.BOLD, 14));
        
        sliderGenerations.addChangeListener(e -> {
            if (!sliderGenerations.getValueIsAdjusting() && !genPathHistory.isEmpty()) {
                int index = sliderGenerations.getValue();
                if (index >= 0 && index < genPathHistory.size()) {
                    updateMazeToGeneration(index);
                }
            }
        });

        bottomPanel.add(sliderGenerations, BorderLayout.CENTER);
        bottomPanel.add(lblGenVal, BorderLayout.EAST);

        // 4. DASHBOARD
        JPanel dashboard = new JPanel();
        dashboard.setLayout(new BoxLayout(dashboard, BoxLayout.Y_AXIS));
        dashboard.setPreferredSize(new Dimension(220, 0));
        dashboard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        statusGreedy = new AlgorithmStatusPanel("Greedy");
        statusAStar = new AlgorithmStatusPanel("Pure A*");
        statusGA = new AlgorithmStatusPanel("Genetic Algorithm 2");
        
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

    private void setupButtonActions(MazeMap map) {
        btnGreedy.addActionListener(e -> {
            List<Point> path = DumbestDecoder.getGreedyPath(map);
            lastGreedyPath = path;
            refreshMazeView();
            statusGreedy.updateStats(path, map);
        });

        btnAStar.addActionListener(e -> {
            List<Point> path = DumbestDecoder.getPureAStarPath(map); 
            lastAStarPath = path;
            refreshMazeView();
            statusAStar.updateStats(path, map);
        });

        btnGA.addActionListener(e -> runRealTimeGA(map));

        btnReset.addActionListener(e -> {
            resetData();
            refreshMazeView();
            sliderGenerations.setValue(0);
            sliderGenerations.setEnabled(false);
            lblGenVal.setText("Gen: 0 / 0");
            statusGA.setLoading("Reset");
            statusGreedy.setLoading("Waiting...");
            statusAStar.setLoading("Waiting...");
        });
        
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, MENU_PANEL));
        btnSettings.addActionListener(e -> showGASettings());
    }

    // --- VISUALIZATION LOGIC ---
    private void refreshMazeView() {
        resetGridColors();
        
        // 1. Draw Global Dead Ends (พื้นหลังสุด - สีแดงเข้ม)
        if (chkShowGlobal.isSelected()) {
            drawGlobalDeadEnds(new Color(139, 0, 0)); 
        }

        // 2. Draw Junction Blocks (ชั้นกลาง - สีชมพู)
        if (chkShowJunction.isSelected() && lastJunctionBlocks != null) {
            drawJunctionBlocks(lastJunctionBlocks, new Color(255, 105, 180)); 
        }

        // 3. Draw Paths (ชั้นบนสุด)
        if (chkShowGreedy.isSelected() && lastGreedyPath != null) {
            drawPath(lastGreedyPath, new Color(173, 216, 230)); 
        }
        if (chkShowAStar.isSelected() && lastAStarPath != null) {
            drawPath(lastAStarPath, new Color(255, 200, 0)); 
        }
        if (chkShowGA.isSelected() && lastGAPath != null) {
            drawPath(lastGAPath, Color.GREEN.darker()); 
        }
    }

    private void updateMazeToGeneration(int index) {
        if (index < 0 || index >= genPathHistory.size()) return;
        
        lastGAPath = genPathHistory.get(index);
        lastJunctionBlocks = genJunctionBlockHistory.get(index); 
        double historicalFit = genFitnessHistory.get(index);
        
        refreshMazeView(); 
        
        lblGenVal.setText("Gen: " + (index + 1) + " / " + genPathHistory.size() + "  ");
        statusGA.updateStatsLive(index + 1, gaMaxGenerations, lastGAPath, currentMap, historicalFit, "Replay Mode");
    }

    private void runRealTimeGA(MazeMap map) {
        setControlsEnabled(false);
        statusGA.setLoading("Initializing...");
        
        genPathHistory.clear();
        genJunctionBlockHistory.clear(); 
        genFitnessHistory.clear();
        sliderGenerations.setValue(0);
        sliderGenerations.setEnabled(false);

        Thread gaThread = new Thread(() -> {
            int popSize = gaPopSize;        
            double mutationRate = gaMutationRate; 
            double crossoverRate = gaCrossoverRate;
            int elitismCount = gaElitismCount;
            int maxGenerations = gaMaxGenerations;
            int mutationMode = gaMutationMode;

            GeneticAlgorithm2 ga = new GeneticAlgorithm2(map, popSize, mutationRate, crossoverRate, elitismCount);
            ArrayList<Chromosome2> population = ga.initPopulation(null); 
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMutationRate = mutationRate;
            boolean useHeuristic = false;

            for (int gen = 1; gen <= maxGenerations; gen++) {
                
                population = ga.evolve(population, useHeuristic, mutationMode);
                Collections.sort(population);
                Chromosome2 best = population.get(0);

                // Clone Data for UI
                List<Point> visualPath = new ArrayList<>(best.path); 
                boolean[] visualJunctions = best.junctionBlocks.clone(); 
                
                synchronized(genPathHistory) {
                    genPathHistory.add(visualPath);
                    genJunctionBlockHistory.add(visualJunctions); 
                    genFitnessHistory.add(best.fitness);
                }

                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) {
                    stagnationCount++; 
                } else {
                    stagnationCount = 0; 
                    lastBestFitness = best.fitness; 
                    ga.setMutationRate(defaultMutationRate); 
                }
                
                if (stagnationCount > 50) ga.setMutationRate(0.4); 

                // if (stagnationCount > 50) { 
                //     Chromosome2 survivor = population.get(0);
                    
                //     // *** สำคัญ: ต้องมั่นใจว่า survivor มี Path ติดตัวไปก่อนจะโดน Reset ***
                //     if (survivor.path == null || survivor.path.isEmpty()) {
                //         // ถ้าไม่มี ให้คำนวณใหม่เดี๋ยวนี้เลย ไม่งั้นกราฟิกหาย
                //         survivor.path = new ArrayList<>();
                //         DumbestDecoder.calculateFitness(map, survivor, survivor.path);
                //     }

                //     population = ga.initPopulation(null); 
                //     population.set(0, survivor);
                //     stagnationCount = 0;
                // }

                final String statusText = (stagnationCount > 50) ? "[Boost]" : "";
                final int currentGen = gen;
                final double currentFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    lastGAPath = visualPath;
                    lastJunctionBlocks = visualJunctions; 
                    
                    refreshMazeView(); 
                    statusGA.updateStatsLive(currentGen, maxGenerations, visualPath, map, currentFit, statusText);
                    
                    sliderGenerations.setMaximum(currentGen - 1);
                    sliderGenerations.setValue(currentGen - 1);
                    sliderGenerations.setEnabled(true);
                    lblGenVal.setText("Gen: " + currentGen + "  ");
                });

                try { Thread.sleep(simulationSpeed); } catch (InterruptedException e) {} 
            }
            
            SwingUtilities.invokeLater(() -> {
                setControlsEnabled(true);
                statusGA.setLoading("Done! Best Fitness: " + genFitnessHistory.get(genFitnessHistory.size()-1));
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
        chkShowGreedy.setEnabled(enabled);
        chkShowAStar.setEnabled(enabled);
        chkShowGA.setEnabled(enabled);
        chkShowGlobal.setEnabled(enabled);
        chkShowJunction.setEnabled(enabled);
        
        boolean hasHistory = !genPathHistory.isEmpty();
        sliderGenerations.setEnabled(enabled && hasHistory);
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
                JOptionPane.showMessageDialog(this, "Settings Saved!");
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
                cell.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230))); 
                gridCells[r][c] = cell;
                setCellColor(cell, val);
                
                if(val > 5 && map.rows <= 30) {
                   JLabel lbl = new JLabel(String.valueOf(val));
                   lbl.setFont(new Font("Arial", Font.PLAIN, 9));
                   lbl.setForeground(Color.DARK_GRAY);
                   lbl.setHorizontalAlignment(JLabel.CENTER);
                   cell.add(lbl);
                }
                panel.add(cell);
            }
        }
    }

    // --- วาด Global Dead Ends (Static) ---
    private void drawGlobalDeadEnds(Color c) {
        if (GlobalKnowledge.deadEnds == null) return;
        for (int r = 0; r < currentMap.rows; r++) {
            for (int col = 0; col < currentMap.cols; col++) {
                if ((r == currentMap.start.r && col == currentMap.start.c) || 
                    (r == currentMap.goal.r && col == currentMap.goal.c)) continue;

                if (GlobalKnowledge.isDeadEnd(r, col)) {
                    gridCells[r][col].setBackground(c);
                }
            }
        }
    }

    // --- วาด Junction Blocks (Individual) ---
    private void drawJunctionBlocks(boolean[] blocks, Color c) {
        if (blocks == null) return;
        for (int r = 0; r < currentMap.rows; r++) {
            for (int col = 0; col < currentMap.cols; col++) {
                int idx = r * currentMap.cols + col;
                if (idx < blocks.length && blocks[idx]) {
                    if ((r == currentMap.start.r && col == currentMap.start.c) || 
                        (r == currentMap.goal.r && col == currentMap.goal.c)) continue;
                    
                    // ถ้าตรงนี้เป็น Global Dead End ไปแล้ว ไม่ต้องวาดทับ
                    if (GlobalKnowledge.isDeadEnd(r, col)) continue;
                    
                    gridCells[r][col].setBackground(c);
                }
            }
        }
    }

    private void drawPath(List<Point> path, Color c) {
        if(path == null) return;
        for(Point p : path) {
            if(p.r >= 0 && p.r < currentMap.rows && p.c >= 0 && p.c < currentMap.cols) {
                JPanel cell = gridCells[p.r][p.c];
                Color bg = cell.getBackground();
                // ห้ามทับสีสำคัญ (Start, Goal, Global, Junction)
                if(!bg.equals(Color.GREEN) && !bg.equals(Color.RED) && 
                   !bg.equals(new Color(139, 0, 0)) && !bg.equals(new Color(255, 100, 100))) {
                    cell.setBackground(c);
                }
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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new DumbestGUI());
    }
}