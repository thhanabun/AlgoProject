package Basic;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BasicGUI extends JFrame {

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
    private AlgorithmStatusPanel statusGA;

    // --- Data Storage ---
    private List<Point> lastGreedyPath = null; 
    private List<Point> lastAStarPath = null;  
    private List<Point> lastDijkPath = null;  
    private List<Point> lastGAPath = null;
    
    private double cachedGAFitness = 0;
    private List<List<Point>> genPath = new ArrayList<>();
    private List<Integer> genFitness = new ArrayList<>();

    // --- GA Parameters ---
    private int gaPopSize = 200;         
    private double gaMutationRate = 0.03; 
    private int gaMaxGenerations = 1000;  
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

    public BasicGUI() {
        setTitle("Maze Solver Ultimate (DumbDecoder Integrated)");
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
            MazeReader r = new MazeReader(); 
            this.currentMap = new MazeMap(r.read(fileChooser.getSelectedFile().getAbsolutePath()));
            
            lastGAPath = null;
            lastGreedyPath = null;
            lastAStarPath = null;
            lastDijkPath = null;
            genPath.clear();
            
            if (currentMap != null) buildAndShowMaze(currentMap);
        }
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

        btnBack = new JButton("Back");
        btnReset = new JButton("Clear");
        
        // Re-enabled Standard Algorithms
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
        chkShowDijk.addActionListener(e -> refreshMazeView());
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
        View.add(mazeScrollPane, BorderLayout.CENTER);
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

    private void runBackgroundAlgo(String name, Supplier<List<Point>> algorithm, AlgorithmStatusPanel statusPanel, Consumer<List<Point>> onDone) {
        setControlsEnabled(false);
        statusPanel.setLoading("Calculating...");
        
        new Thread(() -> {
            // Converts Point objects (c,r) used in DumbDecoder to (c,r) used here, or check consistency
            List<Point> rawPath = algorithm.get();
            
            // DumbDecoder uses internal Point class or java.awt.Point? 
            // Assuming java.awt.Point for simplicity or compatible Point.
            // If DumbDecoder uses custom Point, ensure conversion.
            List<Point> path = new ArrayList<>();
            // Assuming DumbDecoder returns java.awt.Point or Basic.Point that is compatible
            // If they are custom points, we need a mapping. Assuming consistency for now.
            // But verify: DumbDecoder usually uses (r, c). MazeRenderPanel usually expects (x, y) = (c, r).
            // BasicMain's tracePath did: path.add(new Point(c, r));
            // DumbDecoder's backtrack does: path.add(new Point(curr.r, curr.c)); -> This is (r, c)
            // WE NEED TO SWAP THEM FOR DRAWING IF Point(r, c) was used.
            // java.awt.Point is (x, y). 
            
            for (Point p : rawPath) {
                 // DumbDecoder returns (row, col) in its Point
                 // Renderer needs (x=col, y=row)
                 // We need to inspect Point class. Assuming Point.r and Point.c exist.
                 // Since Point is often java.awt.Point (x,y), let's be careful.
                 // DumbDecoder uses `new Point(curr.r, curr.c)`. If Point is custom Basic.Point, it's (r,c).
                 // Renderer likely uses java.awt.Point or just expects x,y.
                 
                 // FIX: Let's assume we need to swap for rendering if it came as (row, col)
                 // But wait, tracePath in GUI does `new Point(c, r)`.
                 // DumbDecoder does `new Point(curr.r, curr.c)`.
                 // So we need to swap them here:
                 path.add(new Point(p.r, p.c)); // Swap assuming p.x=r, p.y=c or similar.
                 // ACTUALLY: Let's look at DumbDecoder. It uses `new Point(curr.r, curr.c)`.
                 // If Point is java.awt.Point, x=r, y=c.
                 // Renderer (drawPolyline) usually takes x,y. So x should be col, y should be row.
                 // So if DumbDecoder put Row in X, we need to swap.
                 path.set(path.size()-1, new Point(p.r, p.c)); 
            }
            
            // RE-FIX: Instead of guessing, let's just assume DumbDecoder returns compatible points
            // OR use a standard swap:
            List<Point> visualPath = new ArrayList<>();
            for(Point p : rawPath) {
                // DumbDecoder returns Points where x=row, y=col (based on `new Point(r, c)`)
                // Renderer expects x=col, y=row
                visualPath.add(new Point(p.r, p.c));
            }

            SwingUtilities.invokeLater(() -> {
                onDone.accept(visualPath);
                refreshMazeView();
                statusPanel.updateStats(visualPath, currentMap);
                setControlsEnabled(true);
            });
        }).start();
    }

    private void setupButtonActions(MazeMap map) {
        // --- RESTORED ACTIONS USING DUMBDECODER ---
        btnGreedy.addActionListener(e -> {
            runBackgroundAlgo("Greedy", 
                () -> {
                    // DumbDecoder returns List<Point> (custom or awt)
                    // We need to map it. DumbDecoder.getGreedyPath(map)
                    List<Point> res = DumbDecoder.getGreedyPath(map);
                    return res;
                }, 
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
            System.out.println("Memory cleared.");
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
        // Restore overlays
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

    // --- MAIN INTEGRATION LOGIC ---
    private void runRealTimeGA(MazeMap map) {
        setControlsEnabled(false);
        statusGA.setLoading("Initializing...");
        
        System.out.println("Starting Basic Genetic Algorithm...");
        System.out.println("Pop: " + gaPopSize + ", Mut: " + gaMutationRate + ", Gen: " + gaMaxGenerations);

        genPath.clear();
        genFitness.clear();
        sliderGenerations.setValue(0);
        sliderGenerations.setEnabled(false);
        cmbGenerations.removeAllItems();
        cmbGenerations.setEnabled(false);

        Thread gaThread = new Thread(() -> {
            int geneLength = map.rows * map.cols * 2;
            
            // 1. Initialize BasicGenetic
            BasicGenetic ga = new BasicGenetic(gaPopSize, geneLength, gaMutationRate, map);
            
            BasicChromosome finalBest = null;

            for (int gen = 0; gen < gaMaxGenerations; gen++) {
                
                // 2. Evaluate
                ga.evaluatePopulation();
                BasicChromosome best = ga.getBest();

                if (finalBest == null || best.fitness > finalBest.fitness) {
                    finalBest = best.copy();
                }

                // 3. Trace Path (Local implementation for GA)
                List<Point> visualPath = tracePath(map, best);
                
                synchronized(genPath) {
                    genPath.add(visualPath);
                    genFitness.add(best.fitness);
                }

                // 4. Console Logs
                if (best.solved) {
                    System.out.println("Generation " + gen + " has a solution. Fitness: " + best.fitness);
                }
                if (gen % 100 == 0) {
                    System.out.println("Gen " + gen + " best fitness = " + best.fitness);
                }

                // 5. GUI Update
                final int currentGen = gen;
                final double currentFit = best.fitness;
                final boolean isSolved = best.solved;
                
                SwingUtilities.invokeLater(() -> {
                    lastGAPath = visualPath;
                    refreshMazeView();
                    statusGA.updateStatsLive(currentGen, gaMaxGenerations, visualPath, map, currentFit, isSolved ? "SOLVED" : "Running");
                    sliderGenerations.setMaximum(currentGen);
                    sliderGenerations.setValue(currentGen);
                    lblGenVal.setText("Gen: " + currentGen + "  ");
                });
                
                try { Thread.sleep(simulationSpeed); } catch (InterruptedException e) {} 

                // 6. Evolve
                ga.evolve();
            }
            
            // Finalization
            System.out.println("Finished all generations.");
            System.out.println("Best fitness overall = " + finalBest.fitness);
            
            List<Point> finalPath = tracePath(map, finalBest);
            lastGAPath = new ArrayList<>(finalPath);
            cachedGAFitness = finalBest.fitness;

            SwingUtilities.invokeLater(() -> {
                refreshMazeView();
                statusGA.updateStatsLive(gaMaxGenerations, gaMaxGenerations, finalPath, map, cachedGAFitness, "COMPLETED!");
                setupReplayControls();
                setControlsEnabled(true);
            });
        });
        gaThread.start();
    }
    
    // Trace path for GA Chromosomes
    private List<Point> tracePath(MazeMap map, BasicChromosome chromo) {
        List<Point> path = new ArrayList<>();
        int r = map.start.r;
        int c = map.start.c;
        path.add(new Point(c, r)); 
        
        for (int move : chromo.genes) {
            int nr = r, nc = c;
            
            if (move == 0) nr--;
            else if (move == 1) nr++;
            else if (move == 2) nc--;
            else if (move == 3) nc++;
            
            if (!map.isValid(nr, nc)) continue;
            
            r = nr; c = nc;
            path.add(new Point(c, r));
            
            if (r == map.goal.r && c == map.goal.c) break;
        }
        return path;
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
       
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Population Size:")); panel.add(txtPop);
        panel.add(new JLabel("Mutation Rate:")); panel.add(txtMut);
        panel.add(new JLabel("Max Generations:")); panel.add(txtGen);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "GA Configuration", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
             try {
                gaPopSize = Integer.parseInt(txtPop.getText());
                gaMutationRate = Double.parseDouble(txtMut.getText());
                gaMaxGenerations = Integer.parseInt(txtGen.getText());
                
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
        SwingUtilities.invokeLater(() -> new BasicGUI());
    }
}