import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class MazeGUI extends JFrame {

    // --- CardLayout Components ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    
    // Names for our screens
    private static final String MENU_PANEL = "Menu";
    private static final String MAZE_PANEL = "Maze";
    

    // --- Your Maze Settings ---
    private int CELL_SIZE = 5;
    private JPanel[][] gridCells; 
    private MazeMap currentMap;

    private AlgorithmStatusPanel statusGreedy;
    private AlgorithmStatusPanel statusAStar;
    private AlgorithmStatusPanel statusGA;

    private List<Point> cachedGAPath = null;
    private double cachedGAFitness = 0;

    private int gaPopSize = 20;         
    private double gaMutationRate = 0.1; 
    private double gacCossoverRate = 0.9;
    private int gaElitismCount = 5;
    private int gaMaxGenerations = 50;


    public MazeGUI() {
        setTitle("Maze Solver");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);

        // 1. Setup CardLayout (The Manager)
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
            // Assuming Mazereader returns the raw 2D ArrayList, and MazeMap constructor takes it
            this.currentMap = new MazeMap(mr.read(fileChooser.getSelectedFile().getAbsolutePath()));
            cachedGAPath = null;
            if (currentMap != null) buildAndShowMaze(currentMap);
        }
    }

    // ==========================================
    // SCREEN 2: BUILDING THE MAZE UI
    // ==========================================
    private void buildAndShowMaze(MazeMap map) {
        JPanel mazeGridPanel = new JPanel(new GridLayout(map.rows, map.cols));
        mazeGridPanel.setPreferredSize(new Dimension(map.cols * CELL_SIZE, map.rows * CELL_SIZE));
        gridCells = new JPanel[map.rows][map.cols];
        renderMaze(mazeGridPanel, map);
        
        JScrollPane scrollPane = new JScrollPane(mazeGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // 4. Create a container for the Game View (Top Bar + ScrollPane)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnBack = new JButton("Back");
        JButton btnReset = new JButton("Clear / Reset");
        JButton btnGreedy = new JButton("Run Greedy");
        JButton btnAStar = new JButton("Run Pure A*");
        JButton btnGA = new JButton("Run GA");

        JButton btnSettings = new JButton("⚙ Settings");

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
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(btnGreedy);
        controlPanel.add(btnAStar);
        controlPanel.add(btnGA);

        controlPanel.add(btnSettings);
        
        btnGreedy.addActionListener(e -> {
            resetGridColors();
            List<Point> path = DumbDecoder.getGreedyPath(map);
            drawPath(path, Color.BLUE);
            statusGreedy.updateStats(path, map);
        });

        btnAStar.addActionListener(e -> {
            resetGridColors();
            List<Point> path = DumbDecoder.getPureAStarPath(map); 
            drawPath(path, Color.MAGENTA);
            statusAStar.updateStats(path, map);
        });

        btnGA.addActionListener(e -> {
            resetGridColors();
            if (cachedGAPath != null) {
                // DATA EXISTS: Show it instantly (No loading)
                drawPath(cachedGAPath, Color.GREEN.darker());
                statusGA.updateStatsLive(1000, 1000, cachedGAPath, map, cachedGAFitness, "Cached Result");
            } else {
                // NO DATA: Run simulation
                runRealTimeGA(map); 
            }
        });

        btnReset.addActionListener(e -> {
            resetGridColors();
            cachedGAPath = null; // Wipe memory
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

    private void showGASettings() {
        // Create input fields
        JTextField txtPop = new JTextField(String.valueOf(gaPopSize));
        JTextField txtMut = new JTextField(String.valueOf(gaMutationRate));
        JTextField txtGen = new JTextField(String.valueOf(gaMaxGenerations));
        JTextField txtEli = new JTextField(String.valueOf(gaElitismCount));
        JTextField txtCro = new JTextField(String.valueOf(gacCossoverRate));

        // Create a nice panel layout for the inputs
        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Population Size (e.g., 100):"));
        panel.add(txtPop);
        panel.add(new JLabel("Mutation Rate (0.0 - 1.0):"));
        panel.add(txtMut);
        panel.add(new JLabel("Cossover Rate (0.0 - 1.0):"));
        panel.add(txtCro);
        panel.add(new JLabel("Max Generations (e.g., 1000):"));
        panel.add(txtGen);
        panel.add(new JLabel("Elitism Count (e.g., 5):"));
        panel.add(txtEli);

        int result = JOptionPane.showConfirmDialog(this, panel, "GA Configuration",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                // Parse and Save
                gaPopSize = Integer.parseInt(txtPop.getText());
                gaMutationRate = Double.parseDouble(txtMut.getText());
                gaMaxGenerations = Integer.parseInt(txtGen.getText());
                gaElitismCount = Integer.parseInt(txtEli.getText());
                
                // CRITICAL: Clear cache because settings changed!
                cachedGAPath = null; 
                statusGA.setLoading("Settings Updated");
                
                JOptionPane.showMessageDialog(this, "Settings Saved! Cache cleared.");
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number entered!");
            }
        }
    }

    private void runRealTimeGA(MazeMap map) {
        statusGA.setLoading("Initializing Population...");
        
        Thread gaThread = new Thread(() -> {
            // --- 1. Parameters ---
            int popSize = gaPopSize;         
            double mutationRate = gaMutationRate; 
            double crossoverRate = gacCossoverRate;
            int elitismCount = gaElitismCount;
            int maxGenerations = gaMaxGenerations;

            // --- 2. Initialize GA ---
            GeneticAlgorithm ga = new GeneticAlgorithm(map, popSize, mutationRate, crossoverRate, elitismCount);
            ArrayList<Chromosome> population = ga.initPopulation(null);
            
            double lastBestFitness = Double.MAX_VALUE;
            int stagnationCount = 0;
            double defaultMutationRate = mutationRate;
            boolean useHeuristic = false;

            // --- 3. Evolution Loop ---
            for (int gen = 1; gen <= maxGenerations; gen++) {
                
                // Heuristic Switching
                if (gen > maxGenerations / 2) {
                    useHeuristic = true;
                    DumbDecoder.ALPHA = 1.2;
                } else {
                    DumbDecoder.ALPHA = 10.0; 
                }

                population = ga.evolve(population, useHeuristic);
                Collections.sort(population);
                Chromosome best = population.get(0);

                // Adaptive Mutation
                if (Math.abs(best.fitness - lastBestFitness) < 0.0001) {
                    stagnationCount++; 
                } else {
                    stagnationCount = 0; 
                    lastBestFitness = best.fitness; 
                    ga.setMutationRate(defaultMutationRate); 
                }
                if (stagnationCount > 50) ga.setMutationRate(0.4); 

                // --- 4. Update Visualization (Every 5 gens to be faster, or 1 for smooth) ---
                // We update every frame here for smoothness
                List<Point> rawPath = DumbDecoder.getPath(map, best, true);
                List<Point> visualPath = new ArrayList<>(rawPath); // Copy path

                // Status Text
                String extraInfo = "";
                if (useHeuristic) extraInfo += "[A* Mode] ";
                if (stagnationCount > 50) extraInfo += "[Boost] ";
                final String statusText = extraInfo;
                final int currentGen = gen;
                final double currentFit = best.fitness;
                
                SwingUtilities.invokeLater(() -> {
                    resetGridColors();
                    drawPath(visualPath, Color.GREEN.darker());
                    statusGA.updateStatsLive(currentGen, maxGenerations, visualPath, map, currentFit, statusText);
                });

                try { Thread.sleep(500); } catch (InterruptedException e) {} // Fast animation
            }
            
            // --- 5. FINISHED: Show Final Result ---
            // Get the absolute best from the final population
            Chromosome finalBest = population.get(0);
            List<Point> finalPath = DumbDecoder.getPath(map, finalBest, true);
            cachedGAPath = new ArrayList<>(finalPath);
            cachedGAFitness = finalBest.fitness;
            SwingUtilities.invokeLater(() -> {
                resetGridColors();
                drawPath(finalPath, Color.GREEN.darker());
                // Update the status panel one last time with "FINISHED"
                statusGA.updateStatsLive(maxGenerations, maxGenerations, finalPath, map, finalBest.fitness, "COMPLETED!");
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
            // Safety check for bounds
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
        // Good practice to run Swing on its own thread
        SwingUtilities.invokeLater(() -> new MazeGUI());
    }
}