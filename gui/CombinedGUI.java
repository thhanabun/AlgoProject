package gui;
import javax.swing.*;

import GA_DIjksDecoder.DijksChromosome;
import GA_DIjksDecoder.DijksDecoder;
import GA_DIjksDecoder.DijksGA;

import GA_StocasticDecoder.StocasticChromosome;
import GA_StocasticDecoder.StocasticGA;
import GA_StocasticDecoder.StocasticGlobalKnowledge;

import GA_DepthFirstSearch.DFSChromosome;
import GA_DepthFirstSearch.DFSGA;
import GA_DepthFirstSearch.DFSGlobalKnowledge;

import MazeSolverAlgo.MyAlgorithm;

import Struct.MazeMap;
import Struct.Point;
import Struct.Reader;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.function.Consumer;

// Assumes existence of: MazeMap, MazeRenderPanel, AlgorithmStatusPanel, Reader/Mazereader
// Assumes existence of: GeneticAlgorithm (for Dijk) and GeneticAlgorithm2 (for SCT)
// Assumes existence of: GlobalKnowledge (for SCT)

public class CombinedGUI extends JFrame {

    // --- CardLayout ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private static final String MENU_PANEL = "Menu";
    private static final String MAZE_PANEL = "Maze";

    // --- Maze Data ---
    private MazeMap currentMap;
    
    // --- ZOOMABLE RENDERER ---
    private MazeRenderPanel mazeCanvas; 
    private JScrollPane mazeScrollPane; 

    // --- Panels & Components ---
    private AlgorithmStatusPanel statusGreedy;
    private AlgorithmStatusPanel statusAStar;
    private AlgorithmStatusPanel statusDijk;
    private AlgorithmStatusPanel statusGADijk; // Formerly GA in MazeGUI
    private AlgorithmStatusPanel statusGADFS;
    private AlgorithmStatusPanel statusGASCT;  // Formerly GA in DumbestGUI

    // --- Data Storage: Standard Algos ---
    private List<Point> lastGreedyPath = null; 
    private List<Point> lastAStarPath = null;  
    private List<Point> lastDijkPath = null;  

    // --- Data Storage: GA Dijk ---
    private List<Point> lastGADijkPath = null;
    private List<List<Point>> historyGADijkPath = new ArrayList<>();
    private List<Double> historyGADijkFitness = new ArrayList<>();

    
    // --- Data Storage: GA SCT ---
    private List<Point> lastGASCTPath = null;
    private List<Point> lastGASCTJunctionBlocks = null; // To store junction blocks for rendering
    private List<List<Point>> historyGASCTPath = new ArrayList<>();
    private List<List<Point>> historyGASCTJunctions = new ArrayList<>(); // Store junctions as points for replay
    private List<Double> historyGASCTFitness = new ArrayList<>();
    private List<Point> lastGlobalDeadEndsSCT = null; // To store calculated dead ends for rendering

    // --- Data Storage: GA DFS ---
    private List<Point> lastGADFSPath = null;
    private List<Point> lastGADFSJunctionBlocks = null; // To store junction blocks for rendering
    private List<List<Point>> historyGADFSPath = new ArrayList<>();
    private List<List<Point>> historyGADFSJunctions = new ArrayList<>(); // Store junctions as points for replay
    private List<Double> historyGADFSTFitness = new ArrayList<>();
    private List<Point> lastGlobalDeadEndsDFS = null;

    // --- State Tracking ---
    private enum LastRun { NONE, GA_DIJK, GA_SCT, GA_DFS }
    private LastRun lastRunMode = LastRun.NONE;

    // --- GA Parameters (Shared) ---
    private int gaPopSize = 500;         
    private double gaMutationRate = 0.01; 
    private double gaCrossoverRate = 0.9;
    private int gaElitismCount = 10;
    private int gaMaxGenerations = 1000;
    //private int gaMutationMode = 0; // 0 for Dijk, Hybrid for SCT
    private int simulationSpeed = 20; 
    private int earlyStopStagnationLimit = gaMaxGenerations;

    // --- UI Controls ---
    private JCheckBox chkShowGreedy;
    private JCheckBox chkShowAStar;
    private JCheckBox chkShowDijk;
    private JCheckBox chkShowGADijk;
    private JCheckBox chkShowGASCT;
    private JCheckBox chkShowJunctionSCT; 
    private JCheckBox chkShowDFS;
    private JCheckBox chkShowJunctionDFS;
    private JCheckBox chkShowGlobalSCT;   
    private JCheckBox chkShowGlobalDFS;
    
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
    private JButton btnRunGADijk;
    private JButton btnRunGASCT;
    private JButton btnRunDFS;
    private JButton btnReplay;
    private JButton btnSettings;
    private JButton btnStop;
    private volatile boolean stopRequested = false;

    public CombinedGUI() {
        setTitle("Maze Solver Ultimate: Combined Edition (Dijk + SCT)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1480, 800); 

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
            // Assuming Reader/Mazereader are compatible
            Reader r = new Reader(); 
            int[][] rawData = r.read(fileChooser.getSelectedFile().getAbsolutePath());
            this.currentMap = new MazeMap(rawData);
            
            resetAllData();
            
            if (currentMap != null) buildAndShowMaze(currentMap);
        }
    }

    private void resetAllData() {
        lastGreedyPath = null;
        lastAStarPath = null;
        lastDijkPath = null;
        lastGADijkPath = null;
        lastGASCTPath = null;
        lastGASCTJunctionBlocks = null;
        lastGlobalDeadEndsSCT = null;
        lastGADFSPath = null;
        lastGADFSJunctionBlocks = null;
        
        historyGADijkPath.clear();
        historyGADijkFitness.clear();
        historyGASCTPath.clear();
        historyGASCTJunctions.clear();
        historyGASCTFitness.clear();
        historyGADFSPath.clear();
        historyGADFSJunctions.clear();
        historyGADFSTFitness.clear();
        
        lastRunMode = LastRun.NONE;
        
        // ** IMPORTANT: Initialize GlobalKnowledge for SCT **
        if (currentMap != null) {
            StocasticGlobalKnowledge.init(currentMap.rows, currentMap.cols);
            DFSGlobalKnowledge.init(currentMap.rows, currentMap.cols);
            // Pre-calculate global dead ends for visualization
            lastGlobalDeadEndsSCT = calculateGlobalDeadEndsPointsSCT();
        }
    }

    private List<Point> calculateGlobalDeadEndsPointsSCT() {
        List<Point> currentDeadEnds = new ArrayList<>();
        if (currentMap == null) return currentDeadEnds;
        
        for(int r = 0; r < currentMap.rows; r++) {
            for(int c = 0; c < currentMap.cols; c++) {
                // Skip Start/Goal
                if ((r == currentMap.start.r && c == currentMap.start.c) || 
                    (r == currentMap.goal.r && c == currentMap.goal.c)) continue;

                if(StocasticGlobalKnowledge.isDeadEnd(r, c)) {
                    currentDeadEnds.add(new Point(r, c)); 
                }
            }
        }
        return currentDeadEnds;
    }

    private List<Point> calculateGlobalDeadEndsDFS() {
        List<Point> currentDeadEnds = new ArrayList<>();
        if (currentMap == null) return currentDeadEnds;
        
        for(int r = 0; r < currentMap.rows; r++) {
            for(int c = 0; c < currentMap.cols; c++) {
                if ((r == currentMap.start.r && c == currentMap.start.c) || 
                    (r == currentMap.goal.r && c == currentMap.goal.c)) continue;

                // Check DFSKnowledge instead of Stocastic
                if(DFSGlobalKnowledge.isDeadEnd(r, c)) {
                    currentDeadEnds.add(new Point(r, c)); 
                }
            }
        }
        return currentDeadEnds;
    }

    private void buildAndShowMaze(MazeMap map) {
        // --- ZOOM SETUP ---
        mazeCanvas = new MazeRenderPanel(map.cols, map.rows);
        
        double initialZoom = 1.0;
        if (map.rows < 50) initialZoom = 15.0;
        else if (map.rows < 100) initialZoom = 8.0;
        else if (map.rows < 500) initialZoom = 2.0;
        
        mazeCanvas.setZoom(initialZoom);

        new Thread(() -> {
            mazeCanvas.renderBaseMap(map); 
            SwingUtilities.invokeLater(mazeCanvas::repaint);
        }).start();
        
        mazeScrollPane = new JScrollPane(mazeCanvas);
        mazeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mazeScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        
        mazeScrollPane.addMouseWheelListener(e -> {
            if (e.isControlDown() || true) { 
                int notches = e.getWheelRotation();
                double zoomFactor = 1.1;
                if (notches < 0) mazeCanvas.zoomIn(zoomFactor); 
                else mazeCanvas.zoomOut(zoomFactor); 
                e.consume(); 
            }
        });

        // --- TOP CONTROLS ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Initialize Buttons and Apply Small Style
        btnBack = new JButton("Back");           styleSmallButton(btnBack);
        btnReset = new JButton("Clear");         styleSmallButton(btnReset);
        btnGreedy = new JButton("Greedy");       styleSmallButton(btnGreedy);
        btnAStar = new JButton("A*");            styleSmallButton(btnAStar);
        btnDijk = new JButton("Dijkstra");       styleSmallButton(btnDijk);
        
        btnRunGADijk = new JButton("Run GA (Dijk)"); styleSmallButton(btnRunGADijk);
        btnRunGASCT = new JButton("Run GA (SCT)");   styleSmallButton(btnRunGASCT);
        btnRunDFS = new JButton("Run GA (DFS)");     styleSmallButton(btnRunDFS);
        
        btnStop = new JButton("STOP");           styleSmallButton(btnStop);
        btnReplay = new JButton("Replay");       styleSmallButton(btnReplay);
        btnSettings = new JButton("Settings");   styleSmallButton(btnSettings);

        // Layers
        JPanel layersPanel = new JPanel(new GridLayout(2, 5, 5, 0)); 
        layersPanel.setBorder(BorderFactory.createTitledBorder("Layers"));
        
        chkShowGreedy = new JCheckBox("Greedy"); chkShowGreedy.setForeground(Color.BLUE); chkShowGreedy.setSelected(true);
        chkShowAStar = new JCheckBox("A*"); chkShowAStar.setForeground(Color.ORANGE.darker()); chkShowAStar.setSelected(true);
        chkShowDijk = new JCheckBox("Dijk"); chkShowDijk.setForeground(Color.MAGENTA.darker()); chkShowDijk.setSelected(true);
        chkShowGADijk = new JCheckBox("GA Dijk"); chkShowGADijk.setForeground(Color.GREEN.darker()); chkShowGADijk.setSelected(true);
        
        chkShowGASCT = new JCheckBox("GA SCT"); chkShowGASCT.setForeground(new Color(0, 100, 0)); chkShowGASCT.setSelected(true);
        chkShowJunctionSCT = new JCheckBox("SCT Junction"); chkShowJunctionSCT.setForeground(new Color(255, 105, 180)); chkShowJunctionSCT.setSelected(true);
        chkShowGlobalSCT = new JCheckBox("SCT DeadEnds"); chkShowGlobalSCT.setForeground(new Color(139, 0, 0)); chkShowGlobalSCT.setSelected(true);

        chkShowDFS = new JCheckBox("GA DFS"); chkShowDFS.setForeground(Color.RED); chkShowDFS.setSelected(true);
        chkShowJunctionDFS = new JCheckBox("DFS Junc"); chkShowJunctionDFS.setForeground(Color.CYAN.darker()); chkShowJunctionDFS.setSelected(true);
        chkShowGlobalDFS = new JCheckBox("DFS DeadEnd"); chkShowGlobalDFS.setForeground(new Color(139, 139, 139)); chkShowGlobalDFS.setSelected(true);
        // Reduce font size for checkboxes too if you want them smaller
        Font chkFont = new Font("Arial", Font.PLAIN, 10); // Slightly smaller to fit 6 columns
        chkShowGreedy.setFont(chkFont); chkShowAStar.setFont(chkFont); chkShowDijk.setFont(chkFont);
        chkShowGADijk.setFont(chkFont); 
        chkShowGASCT.setFont(chkFont); chkShowJunctionSCT.setFont(chkFont); chkShowGlobalSCT.setFont(chkFont);
        chkShowDFS.setFont(chkFont); chkShowJunctionDFS.setFont(chkFont); chkShowGlobalDFS.setFont(chkFont);


        Runnable refresh = this::refreshMazeView;
        chkShowGreedy.addActionListener(e -> refresh.run());
        chkShowAStar.addActionListener(e -> refresh.run());
        chkShowDijk.addActionListener(e -> refresh.run());
        chkShowGADijk.addActionListener(e -> refresh.run());
        
        chkShowGASCT.addActionListener(e -> refresh.run());
        chkShowJunctionSCT.addActionListener(e -> refresh.run());
        chkShowGlobalSCT.addActionListener(e -> refresh.run());
        
        chkShowDFS.addActionListener(e -> refresh.run());          
        chkShowJunctionDFS.addActionListener(e -> refresh.run());
        chkShowGlobalDFS.addActionListener(e -> refresh.run());
        
        layersPanel.add(chkShowGreedy); 
        layersPanel.add(chkShowAStar); 
        layersPanel.add(chkShowDijk);
        layersPanel.add(chkShowGADijk);
        layersPanel.add(new JLabel(""));
         layersPanel.add(new JLabel(""));
        // Row 2: SCT and DFS aligned
        
        layersPanel.add(chkShowGASCT); 
        layersPanel.add(chkShowJunctionSCT);
        layersPanel.add(chkShowGlobalSCT);
        layersPanel.add(chkShowDFS); 
        layersPanel.add(chkShowJunctionDFS);
        layersPanel.add(chkShowGlobalDFS);

        // --- SPEED CONTROL ---
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        speedPanel.setBorder(BorderFactory.createTitledBorder("Speed"));
        sliderSpeed = new JSlider(0, 500, simulationSpeed);
        sliderSpeed.setPreferredSize(new Dimension(80, 20));
        txtSpeed = new JTextField(String.valueOf(simulationSpeed), 3);
        
        sliderSpeed.addChangeListener(e -> {
            simulationSpeed = sliderSpeed.getValue();
            txtSpeed.setText(String.valueOf(simulationSpeed));
        });
        speedPanel.add(sliderSpeed); speedPanel.add(txtSpeed);

        btnStop.addActionListener(e -> {
            stopRequested = true;
            btnStop.setText("Stop...");
            btnStop.setEnabled(false);
        });

        // Add components to top panel
        topPanel.add(btnBack);
        topPanel.add(btnReset);
        topPanel.add(speedPanel);
        topPanel.add(btnGreedy);
        topPanel.add(btnAStar);
        topPanel.add(btnDijk);
        topPanel.add(btnRunGADijk);
        topPanel.add(btnRunGASCT);
        topPanel.add(btnRunDFS);
        topPanel.add(btnStop);
        topPanel.add(btnReplay);
        topPanel.add(btnSettings);
        topPanel.add(layersPanel);

        // BOTTOM PANEL (Timeline)
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Timeline Replay"));
        
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
            if (!sliderGenerations.getValueIsAdjusting()) {
                handleSliderChange();
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
        dashboard.setPreferredSize(new Dimension(220, 0));
        dashboard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        statusGreedy = new AlgorithmStatusPanel("Greedy");
        statusAStar = new AlgorithmStatusPanel("Pure A*");
        statusDijk = new AlgorithmStatusPanel("Dijkstra");
        statusGADijk = new AlgorithmStatusPanel("GA (Dijkstra)");
        statusGASCT = new AlgorithmStatusPanel("GA (SCT)");
        statusGADFS = new AlgorithmStatusPanel("GA (DFS)");
        
        dashboard.add(statusGreedy); dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusAStar); dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusDijk); dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusGADijk); dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusGASCT); dashboard.add(Box.createVerticalStrut(5));
        dashboard.add(statusGADFS); dashboard.add(Box.createVerticalStrut(5));

        setupButtonActions(map);

        JPanel View = new JPanel(new BorderLayout());
        View.add(topPanel, BorderLayout.NORTH);
        View.add(mazeScrollPane, BorderLayout.CENTER); 
        View.add(dashboard, BorderLayout.EAST);
        View.add(bottomPanel, BorderLayout.SOUTH);

        mainContainer.add(View, MAZE_PANEL);
        cardLayout.show(mainContainer, MAZE_PANEL);
    }

    private void styleSmallButton(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 11)); // Smaller font (11pt)
        btn.setMargin(new Insets(1, 1, 1, 1));         // Tighter padding (Top, Left, Bottom, Right)
        btn.setFocusPainted(false);                    // Removes the focus box line for a cleaner look
    }

    // --- LOGIC: Slider & Dropdown ---
    private void generationDropdownAction(java.awt.event.ItemEvent e) {
        if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
            if (cmbGenerations.isEnabled()) {
                int index = cmbGenerations.getSelectedIndex();
                if (index >= 0) sliderGenerations.setValue(index);
            }
        }
    }

    private void handleSliderChange() {
        int index = sliderGenerations.getValue();
        
        if (lastRunMode == LastRun.GA_DIJK && !historyGADijkPath.isEmpty()) {
            if (index < historyGADijkPath.size()) {
                updateMazeToGenerationDijk(index);
                syncDropdown(index);
            }
        } else if (lastRunMode == LastRun.GA_SCT && !historyGASCTPath.isEmpty()) {
            if (index < historyGASCTPath.size()) {
                updateMazeToGenerationSCT(index);
                syncDropdown(index);
            }
        }else if (lastRunMode == LastRun.GA_DFS && !historyGADFSPath.isEmpty()) { // <-- Add Block
            if (index < historyGADFSPath.size()) {
                updateMazeToGenerationDFS(index);
                syncDropdown(index);
            }
        }
    }
    
    private void syncDropdown(int index) {
        cmbGenerations.removeItemListener(this::generationDropdownAction);
        if (index < cmbGenerations.getItemCount()) cmbGenerations.setSelectedIndex(index);
        cmbGenerations.addItemListener(this::generationDropdownAction);
    }

    private void runBackgroundAlgo(String name, Supplier<List<Point>> algorithm, AlgorithmStatusPanel statusPanel, Consumer<List<Point>> onDone) {
        setControlsEnabled(false);
        statusPanel.setLoading("Calculating...");
        new Thread(() -> {
            List<Point> path = algorithm.get();
            SwingUtilities.invokeLater(() -> {
                onDone.accept(path);
                refreshMazeView();
                statusPanel.updateStats(path, currentMap);
                setControlsEnabled(true);
            });
        }).start();
    }

    private void setupButtonActions(MazeMap map) {
        btnGreedy.addActionListener(e -> runBackgroundAlgo("Greedy", () -> MyAlgorithm.getGreedyPath(map), statusGreedy, path -> lastGreedyPath = path));
        btnAStar.addActionListener(e -> runBackgroundAlgo("A*", () -> MyAlgorithm.getPureAStarPath(map), statusAStar, path -> lastAStarPath = path));
        btnDijk.addActionListener(e -> runBackgroundAlgo("Dijkstra", () -> MyAlgorithm.getDijkstraPath(map), statusDijk, path -> lastDijkPath = path));

        btnRunGADijk.addActionListener(e -> {
            if (lastGADijkPath != null && lastRunMode == LastRun.GA_DIJK) {
                // Already have data, just replay setup
                setupReplayControls(historyGADijkPath.size());
            } else {
                runRealTimeGADijk(map);
            }
        });

        btnRunGASCT.addActionListener(e -> {
            if (lastGASCTPath != null && lastRunMode == LastRun.GA_SCT) {
                setupReplayControls(historyGASCTPath.size());
            } else {
                runRealTimeGASCT(map);
            }
        });

        btnRunDFS.addActionListener(e -> {
            if (lastGADFSPath != null && lastRunMode == LastRun.GA_DFS) {
                setupReplayControls(historyGADFSPath.size());
            } else {
                runRealTimeGADFS(map);
            }
        });

        btnReplay.addActionListener(e -> {
            if (lastRunMode == LastRun.GA_DIJK && !historyGADijkPath.isEmpty()) runReplayAnimation();
            else if (lastRunMode == LastRun.GA_SCT && !historyGASCTPath.isEmpty()) runReplayAnimation();
            else if (lastRunMode == LastRun.GA_DFS && !historyGADFSPath.isEmpty()) runReplayAnimation();
            else JOptionPane.showMessageDialog(this, "No valid history to replay. Run a GA first.");
        });

        btnReset.addActionListener(e -> {
            resetAllData();
            refreshMazeView();
            sliderGenerations.setValue(0);
            sliderGenerations.setEnabled(false);
            cmbGenerations.removeAllItems();
            cmbGenerations.setEnabled(false);
            lblGenVal.setText("Gen: 0 / 0");
            
            statusGADijk.setLoading("Cleared");
            statusGASCT.setLoading("Cleared");
            statusGADFS.setLoading("Cleared");
            statusGreedy.setLoading("Waiting...");
            statusAStar.setLoading("Waiting...");
            statusDijk.setLoading("Waiting...");
        });
        
        btnBack.addActionListener(e -> cardLayout.show(mainContainer, MENU_PANEL));
        btnSettings.addActionListener(e -> showGASettings());
    }

    private void setupReplayControls(int size) {
        cmbGenerations.removeItemListener(this::generationDropdownAction); 
        cmbGenerations.removeAllItems();
        for(int i=0; i<size; i++) cmbGenerations.addItem("Gen " + (i+1));
        cmbGenerations.setEnabled(true);
        cmbGenerations.addItemListener(this::generationDropdownAction); 

        sliderGenerations.setMaximum(size - 1);
        sliderGenerations.setValue(size - 1);
        sliderGenerations.setEnabled(true);
        
        if (cmbGenerations.getItemCount() > 0) cmbGenerations.setSelectedIndex(cmbGenerations.getItemCount() - 1);
    }

    // --- VISUALIZATION ---
private void refreshMazeView() {
        if (mazeCanvas == null) return;
        mazeCanvas.resetToBase();

        // 1. Draw SCT Layers (DeadEnds and Junctions)
        if (chkShowGlobalSCT.isSelected() && lastGlobalDeadEndsSCT != null) {
            mazeCanvas.overlayPoints(lastGlobalDeadEndsSCT, new Color(139, 0, 0)); // Dark Red
        }
        if (chkShowJunctionSCT.isSelected() && lastGASCTJunctionBlocks != null) {
            mazeCanvas.overlayPoints(lastGASCTJunctionBlocks, new Color(255, 105, 180)); // Pink
        }

        // 2. Draw DFS Layers (DeadEnds and Junctions)
        if (chkShowGlobalDFS.isSelected() && lastGlobalDeadEndsDFS != null) {
            mazeCanvas.overlayPoints(lastGlobalDeadEndsDFS, new Color(139, 139, 139)); // Dark Red
        }
        if (chkShowJunctionDFS.isSelected() && lastGADFSJunctionBlocks != null) {
            mazeCanvas.overlayPoints(lastGADFSJunctionBlocks, Color.CYAN.darker());
        }

        // 3. Paths
        if (chkShowGreedy.isSelected() && lastGreedyPath != null) mazeCanvas.overlayPath(lastGreedyPath, Color.BLUE);
        if (chkShowAStar.isSelected() && lastAStarPath != null) mazeCanvas.overlayPath(lastAStarPath, Color.ORANGE);
        if (chkShowDijk.isSelected() && lastDijkPath != null) mazeCanvas.overlayPath(lastDijkPath, Color.MAGENTA.darker());
        
        if (chkShowDFS.isSelected() && lastGADFSPath != null) mazeCanvas.overlayPath(lastGADFSPath, Color.RED);
        if (chkShowGADijk.isSelected() && lastGADijkPath != null) mazeCanvas.overlayPath(lastGADijkPath, Color.GREEN.darker());
        if (chkShowGASCT.isSelected() && lastGASCTPath != null) mazeCanvas.overlayPath(lastGASCTPath, new Color(0, 100, 0)); 
   
        mazeCanvas.repaint();
    }
    private void runReplayAnimation() {
        setControlsEnabled(false); 
        int max = (lastRunMode == LastRun.GA_DIJK) ? historyGADijkPath.size() : historyGASCTPath.size();
        
        Thread replayThread = new Thread(() -> {
            for (int i = 0; i < max; i++) {
                final int index = i;
                SwingUtilities.invokeLater(() -> sliderGenerations.setValue(index));
                try { Thread.sleep(simulationSpeed); } catch (InterruptedException e) {}
            }
            SwingUtilities.invokeLater(() -> setControlsEnabled(true));
        });
        replayThread.start();
    }

    private void updateMazeToGenerationDijk(int index) {
        lastGADijkPath = historyGADijkPath.get(index);
        double fit = historyGADijkFitness.get(index);
        refreshMazeView();
        lblGenVal.setText("Gen: " + (index + 1) + " / " + historyGADijkPath.size());
        statusGADijk.updateStatsLive(index + 1, gaMaxGenerations, lastGADijkPath, currentMap, fit, "Replay");
    }

    private void updateMazeToGenerationSCT(int index) {
        lastGASCTPath = historyGASCTPath.get(index);
        lastGASCTJunctionBlocks = historyGASCTJunctions.get(index);
        double fit = historyGASCTFitness.get(index);
        refreshMazeView();
        lblGenVal.setText("Gen: " + (index + 1) + " / " + historyGASCTPath.size());
        statusGASCT.updateStatsLive(index + 1, gaMaxGenerations, lastGASCTPath, currentMap, fit, "Replay");
    }

    private void updateMazeToGenerationDFS(int index) {
        lastGADFSPath = historyGADFSPath.get(index);
        lastGADFSJunctionBlocks = historyGADFSJunctions.get(index);
        double fit = historyGADFSTFitness.get(index);
        refreshMazeView();
        lblGenVal.setText("Gen: " + (index + 1) + " / " + historyGADFSPath.size());
        statusGADFS.updateStatsLive(index + 1, gaMaxGenerations, lastGADFSPath, currentMap, fit, "Replay");
    }

    // --- GA LOGIC: DIJKSTRA BASED (From MazeGUI) ---
    private void runRealTimeGADijk(MazeMap map) {
        setControlsEnabled(false);
        stopRequested = false;
        lastRunMode = LastRun.GA_DIJK;
        statusGADijk.setLoading("Init...");
        
        historyGADijkPath.clear();
        historyGADijkFitness.clear();
        sliderGenerations.setValue(0);
        
        Thread gaThread = new Thread(() -> {
            DijksGA ga = new DijksGA(map, gaPopSize, gaMutationRate, gaCrossoverRate, gaElitismCount);
            ArrayList<DijksChromosome> population = ga.initPopulation(null);
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMut = gaMutationRate;

            for (int gen = 1; gen <= gaMaxGenerations; gen++) {
                population = ga.evolve(population, false, 0); // 0 = standard logic
                Collections.sort(population);
                DijksChromosome best = population.get(0);
                List<Point> path = DijksDecoder.getPath(map, best, true);
                List<Point> visualPath = new ArrayList<>(path);
                final double currentFit = best.fitness;
                final int currentLen = path.size();
                if (stopRequested) {
                    SwingUtilities.invokeLater(() -> statusGADijk
                            .setLoading(String.format("Stopped (Fit: %.2f, Step: %d)", currentFit, currentLen)));
                    break;
                }
                
                synchronized(historyGADijkPath) {
                    historyGADijkPath.add(visualPath);
                    historyGADijkFitness.add(best.fitness);
                }

                // Adaptive Mutation Logic (Simplified)
                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) stagnationCount++;
                else { stagnationCount = 0; lastBestFitness = best.fitness; ga.setMutationRate(defaultMut); }
                if (stagnationCount > 50) ga.setMutationRate(0.4);
                if (stagnationCount >= earlyStopStagnationLimit) {
                    SwingUtilities.invokeLater(() -> statusGADijk
                            .setLoading(String.format("Converged (Fit: %.2f, Step: %d)", currentFit, currentLen)));
                    break;
                }

                final int cGen = gen;
                final double cFit = best.fitness;
                SwingUtilities.invokeLater(() -> {
                    lastGADijkPath = visualPath;
                    refreshMazeView();
                    statusGADijk.updateStatsLive(cGen, gaMaxGenerations, visualPath, map, cFit, (cGen % 50 == 0 ? "Running" : ""));
                    sliderGenerations.setMaximum(cGen - 1);
                    sliderGenerations.setValue(cGen - 1);
                    lblGenVal.setText("Gen: " + cGen);
                });
                try { Thread.sleep(simulationSpeed); } catch (Exception e) {}
            }  
            final int finalStagnationCount = stagnationCount;
            SwingUtilities.invokeLater(() -> {
                setupReplayControls(historyGADijkPath.size());
                setControlsEnabled(true);
                if(!stopRequested && finalStagnationCount < earlyStopStagnationLimit) {
                    if (!historyGADijkPath.isEmpty()) {
                        double f = historyGADijkFitness.get(historyGADijkFitness.size() - 1);
                        int l = historyGADijkPath.get(historyGADijkPath.size() - 1).size();
                        statusGADijk.setLoading(String.format("Done (Fit: %.2f, Step: %d)", f, l));
                    } else {
                        statusGADijk.setLoading("Done");
                    }
                }
            });
        });
        gaThread.start();
    }

    // --- GA LOGIC: SCT BASED (From DumbestGUI) ---
    private void runRealTimeGASCT(MazeMap map) {
        setControlsEnabled(false);
        stopRequested = false;
        lastRunMode = LastRun.GA_SCT;
        statusGASCT.setLoading("Init...");

        historyGASCTPath.clear();
        historyGASCTJunctions.clear();
        historyGASCTFitness.clear();
        sliderGenerations.setValue(0);

        Thread gaThread = new Thread(() -> {
            StocasticGA ga = new StocasticGA(map, gaPopSize, gaMutationRate, gaCrossoverRate, gaElitismCount);
            ArrayList<StocasticChromosome> population = ga.initPopulation(null);
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMut = gaMutationRate;

            for (int gen = 1; gen <= gaMaxGenerations; gen++) {
                population = ga.evolve(population, false, StocasticChromosome.MUTATION_HYBRID);
                Collections.sort(population);
                StocasticChromosome best = population.get(0);
                

                List<Point> calculatedDeadEnds = calculateGlobalDeadEndsPointsSCT();
                List<Point> visualPath = new ArrayList<>(best.path);
                final double currentFit = best.fitness;
                final int currentLen = visualPath.size();

                // 2. Update Stop Button Logic
                if (stopRequested) {
                    SwingUtilities.invokeLater(() -> statusGASCT
                            .setLoading(String.format("Stopped (Fit: %.2f, Step: %d)", currentFit, currentLen)));
                    break;
                }
                List<Point> blockPoints = convertBlocksToPoints(best.junctionBlocks, map);
                
                synchronized(historyGASCTPath) {
                    historyGASCTPath.add(visualPath);
                    historyGASCTJunctions.add(blockPoints);
                    historyGASCTFitness.add(best.fitness);
                }

                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) stagnationCount++;
                else { stagnationCount = 0; lastBestFitness = best.fitness; ga.setMutationRate(defaultMut); }
                //if (stagnationCount > 50) ga.setMutationRate(0.01);
                if (stagnationCount >= earlyStopStagnationLimit) {
                    SwingUtilities.invokeLater(() -> statusGASCT
                            .setLoading(String.format("Converged (Fit: %.2f, Step: %d)", currentFit, currentLen)));
                    break;
                }
                final int cGen = gen;
                final double cFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    lastGASCTPath = visualPath;
                    lastGASCTJunctionBlocks = blockPoints;
                    lastGlobalDeadEndsSCT = calculatedDeadEnds;
                    
                    refreshMazeView();
                    statusGASCT.updateStatsLive(cGen, gaMaxGenerations, visualPath, map, cFit, (cGen % 50 == 0 ? "Running" : ""));
                    sliderGenerations.setMaximum(cGen - 1);
                    sliderGenerations.setValue(cGen - 1);
                    lblGenVal.setText("Gen: " + cGen);
                });
                try { Thread.sleep(simulationSpeed); } catch (Exception e) {}
            }
            final int finalStagnationCount = stagnationCount;
            SwingUtilities.invokeLater(() -> {
                setupReplayControls(historyGASCTPath.size());
                setControlsEnabled(true);
                if (!stopRequested && finalStagnationCount < earlyStopStagnationLimit) {
                    if (!historyGASCTPath.isEmpty()) {
                        double f = historyGASCTFitness.get(historyGASCTFitness.size() - 1);
                        int l = historyGASCTPath.get(historyGASCTPath.size() - 1).size();
                        statusGASCT.setLoading(String.format("Done (Fit: %.2f, Step: %d)", f, l));
                    } else {
                        statusGASCT.setLoading("Done");
                    }
                }
            });
        });
        gaThread.start();
    }

    private void runRealTimeGADFS(MazeMap map) {
        setControlsEnabled(false);
        stopRequested = false;
        lastRunMode = LastRun.GA_DFS;
        statusGADFS.setLoading("Init...");
    
        historyGADFSPath.clear();
        historyGADFSJunctions.clear();
        historyGADFSTFitness.clear();
        sliderGenerations.setValue(0);
    
        Thread gaThread = new Thread(() -> {
            DFSGA ga = new DFSGA(map, gaPopSize, gaMutationRate, gaCrossoverRate, gaElitismCount);
            ArrayList<DFSChromosome> population = ga.initPopulation(null);
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMut = gaMutationRate;
    
            for (int gen = 1; gen <= gaMaxGenerations; gen++) {
                population = ga.evolve(population, false, 0); 
                Collections.sort(population);
                DFSChromosome best = population.get(0);
                
                // --- ADDED: Calculate Dead Ends Live ---
                List<Point> currentDeadEnds = calculateGlobalDeadEndsDFS();
                // ---------------------------------------

                if (stopRequested) {
                    SwingUtilities.invokeLater(() -> statusGADFS.setLoading("Stopped by User"));
                    break; 
                }
    
                List<Point> visualPath = new ArrayList<>(best.path);
                List<Point> blockPoints = convertBlocksToPoints(best.junctionBlocks, map);
                final double currentFit = best.fitness;
                final int currentLen = visualPath.size();
                
                synchronized(historyGADFSPath) {
                    historyGADFSPath.add(visualPath);
                    historyGADFSJunctions.add(blockPoints);
                    historyGADFSTFitness.add(best.fitness);
                }
    
                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) stagnationCount++;
                else { stagnationCount = 0; lastBestFitness = best.fitness; ga.setMutationRate(defaultMut); }
                
                if (stagnationCount >= earlyStopStagnationLimit) {
                    SwingUtilities.invokeLater(() -> statusGADFS
                            .setLoading(String.format("Converged (Fit: %.2f, Step: %d)", currentFit, currentLen)));
                    break;
                }
                
                final int cGen = gen;
                final double cFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    lastGADFSPath = visualPath;
                    lastGADFSJunctionBlocks = blockPoints;
                    
                    // --- ADDED: Update the UI variable ---
                    lastGlobalDeadEndsDFS = currentDeadEnds;
                    // -------------------------------------
                    
                    refreshMazeView();
                    statusGADFS.updateStatsLive(cGen, gaMaxGenerations, visualPath, map, cFit, (cGen % 50 == 0 ? "Running" : ""));
                    sliderGenerations.setMaximum(cGen - 1);
                    sliderGenerations.setValue(cGen - 1);
                    lblGenVal.setText("Gen: " + cGen);
                });
                try { Thread.sleep(simulationSpeed); } catch (Exception e) {}
            }
            
            final int finalStagnationCount = stagnationCount;
            SwingUtilities.invokeLater(() -> {
                setupReplayControls(historyGADFSPath.size());
                setControlsEnabled(true);
                if (!stopRequested && finalStagnationCount < earlyStopStagnationLimit) {
                    if (!historyGADFSPath.isEmpty()) {
                        double f = historyGADFSTFitness.get(historyGADFSTFitness.size() - 1);
                        int l = historyGADFSPath.get(historyGADFSPath.size() - 1).size();
                        statusGADFS.setLoading(String.format("Done (Fit: %.2f, Len: %d)", f, l));
                    } else {
                        statusGADFS.setLoading("Done");
                    }
                }
            });
        });
        gaThread.start();
    }

    private List<Point> convertBlocksToPoints(boolean[] blocks, MazeMap map) {
        List<Point> points = new ArrayList<>();
        if (blocks == null) return points;
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i]) {
                int r = i / map.cols;
                int c = i % map.cols;
                if (!StocasticGlobalKnowledge.isDeadEnd(r, c)) {
                    points.add(new Point(c, r));
                }
            }
        }
        return points;
    }

    private void setControlsEnabled(boolean enabled) {
        btnBack.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnGreedy.setEnabled(enabled);
        btnAStar.setEnabled(enabled);
        btnDijk.setEnabled(enabled);
        btnRunGADijk.setEnabled(enabled);
        btnRunGASCT.setEnabled(enabled);
        btnSettings.setEnabled(enabled);
        btnRunDFS.setEnabled(enabled);
        btnStop.setEnabled(!enabled);
        if (enabled) {
            btnStop.setText("STOP");
        }
        boolean hasHistory = (lastRunMode == LastRun.GA_DIJK && !historyGADijkPath.isEmpty()) ||
                             (lastRunMode == LastRun.GA_SCT && !historyGASCTPath.isEmpty() ||
                             (lastRunMode == LastRun.GA_DFS && !historyGADFSPath.isEmpty()));
        sliderGenerations.setEnabled(enabled && hasHistory);
        cmbGenerations.setEnabled(enabled && hasHistory);
    }

    private void showGASettings() {
        JTextField txtPop = new JTextField(String.valueOf(gaPopSize));
        JTextField txtMut = new JTextField(String.valueOf(gaMutationRate));
        JTextField txtGen = new JTextField(String.valueOf(gaMaxGenerations));
        JTextField txtEli = new JTextField(String.valueOf(gaElitismCount));
        JTextField txtStag = new JTextField(String.valueOf(earlyStopStagnationLimit));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Population Size:")); panel.add(txtPop);
        panel.add(new JLabel("Mutation Rate:")); panel.add(txtMut);
        panel.add(new JLabel("Max Generations:")); panel.add(txtGen);
        panel.add(new JLabel("Elitism Count:")); panel.add(txtEli);
        panel.add(new JLabel("Early Stop (Stagnation Limit):")); panel.add(txtStag);

        int result = JOptionPane.showConfirmDialog(this, panel, "GA Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
             try {
                gaPopSize = Integer.parseInt(txtPop.getText());
                gaMutationRate = Double.parseDouble(txtMut.getText());
                gaMaxGenerations = Integer.parseInt(txtGen.getText());
                gaElitismCount = Integer.parseInt(txtEli.getText());
                earlyStopStagnationLimit = Integer.parseInt(txtStag.getText());

                resetAllData();
                refreshMazeView();
                JOptionPane.showMessageDialog(this, "Settings Saved! Data Reset.");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Invalid Input!");
            }
        }
    }

}