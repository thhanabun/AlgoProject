import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MazeGUI extends JFrame {

    // --- CardLayout Components ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    
    // Names for our screens
    private static final String MENU_PANEL = "Menu";
    private static final String MAZE_PANEL = "Maze";
    

    // --- Your Maze Settings ---
    private int CELL_SIZE = 20;
    private JPanel[][] gridCells; 
    private MazeMap currentMap;

    public MazeGUI() {
        setTitle("Maze Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 800); // Window size

        // 1. Setup CardLayout (The Manager)
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // 2. Create the Screens
        JPanel menuScreen = createMenuScreen();
        // We create an empty panel for the maze now, we will fill it later
        JPanel mazeScreenPlaceholder = new JPanel(); 

        // 3. Add screens to the container
        mainContainer.add(menuScreen, MENU_PANEL);
        mainContainer.add(mazeScreenPlaceholder, MAZE_PANEL);

        // 4. Add main container to Frame
        add(mainContainer);
        
        // Center on screen
        setLocationRelativeTo(null); 
        setVisible(true);
    }

    private JPanel createMenuScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        JButton startButton = new JButton("Select Maze File to Start");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.addActionListener(e -> handleFileSelection());
        panel.add(startButton);
        return panel;
    }

    // ==========================================
    // LOGIC: FILE CHOOSER & MAZE GENERATION
    // ==========================================
    private void handleFileSelection() {
        JFileChooser fileChooser = new JFileChooser();
        
        // Try to set directory to "MAZE", fallback to current dir if missing
        File mazeDir = new File("MAZE");
        if(mazeDir.exists()) {
            fileChooser.setCurrentDirectory(mazeDir);
        } else {
            fileChooser.setCurrentDirectory(new File("."));
        }
        
        fileChooser.setDialogTitle("Select Maze Text File");
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getAbsolutePath();

            // --- CALL YOUR READER CLASS ---
            Mazereader mr = new Mazereader();
            this.currentMap = new MazeMap(mr.read(fileName));
            //List<Point> Path = DumbDecoder.getPureAStarPath(currentMap);
            
            if (currentMap != null) {
                // If data is good, Build the Interface and Switch!
                buildAndShowMaze(currentMap.rows, currentMap.cols, currentMap);
            } else {
                JOptionPane.showMessageDialog(this, "Error reading file or empty data.");
            }
        }
    }

    // ==========================================
    // SCREEN 2: BUILDING THE MAZE UI
    // ==========================================
    private void buildAndShowMaze(int rows, int cols, MazeMap map) {
        JPanel mazeGridPanel = new JPanel();
        mazeGridPanel.setLayout(new GridLayout(map.rows, map.cols));
        mazeGridPanel.setPreferredSize(new Dimension(map.cols * CELL_SIZE, map.rows * CELL_SIZE));
        gridCells = new JPanel[map.rows][map.cols];
        renderMaze(mazeGridPanel, rows, cols,map);

        // 3. Wrap in ScrollPane
        JScrollPane scrollPane = new JScrollPane(mazeGridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // 4. Create a container for the Game View (Top Bar + ScrollPane)
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        JButton btnAStar = new JButton("Dijkstra/A*");
        JButton btnGreed = new JButton("Greedy");
        JButton btnReset = new JButton("Clear");
        JButton backBtn = new JButton("← Back");

        btnAStar.addActionListener(e -> {
            resetGridColors();
            // You can swap this with DumbDecoder.getPureAStarPath(map) if you prefer
            List<Point> path = DumbDecoder.getPureAStarPath(map); 
            drawPath(path, Color.MAGENTA);
        });
        btnGreed.addActionListener(e -> {
            resetGridColors();
            // You can swap this with DumbDecoder.getPureAStarPath(map) if you prefer
            List<Point> path = DumbDecoder.getGreedyPath(map); 
            drawPath(path, Color.MAGENTA);
        });
        btnReset.addActionListener(e -> resetGridColors());
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, MENU_PANEL));

        controlPanel.add(backBtn);
        controlPanel.add(Box.createHorizontalStrut(20)); // Spacer
        controlPanel.add(btnAStar);
        controlPanel.add(btnGreed);
        controlPanel.add(btnReset);


        JPanel View = new JPanel(new BorderLayout());
        View.add(controlPanel, BorderLayout.NORTH);
        View.add(scrollPane, BorderLayout.CENTER);

        // 6. Switch View
        mainContainer.add(View, MAZE_PANEL);
        cardLayout.show(mainContainer, MAZE_PANEL);
    }

    private void renderMaze(JPanel panel, int rows, int cols,MazeMap map) {
        for (int r = 0; r < map.rows; r++) {
            for (int c = 0; c < map.cols; c++) {
                
                int cellValue = map.getWeight(r,c);
        
                JPanel cell = new JPanel();
                cell.setLayout(new BorderLayout());
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

                gridCells[r][c] = cell;
                // Color Logic
                if (cellValue == -1) cell.setBackground(Color.BLACK);
                else if (cellValue == -2) cell.setBackground(Color.RED);
                else if (cellValue == 0) cell.setBackground(Color.GREEN);
                else cell.setBackground(Color.WHITE);
                
                // If Wall/Start/Goal, just add the colored cell and continue
                if (cellValue > 0) {
                    JLabel scoreLabel = new JLabel(String.valueOf(cellValue));
                    scoreLabel.setHorizontalAlignment(JLabel.CENTER);
                    scoreLabel.setFont(new Font("Arial", Font.PLAIN, 7)); 
                    scoreLabel.setForeground(Color.BLACK);
                    cell.add(scoreLabel, BorderLayout.CENTER);
                }
        
                panel.add(cell);
            }
        }
    }
    private void drawPath(List<Point> path, Color color) {
        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No path found!");
            return;
        }
        for (Point p : path) {
            JPanel cell = gridCells[p.r][p.c];
            // Don't color over Start or End
            if (cell.getBackground() != Color.GREEN && cell.getBackground() != Color.RED) {
                cell.setBackground(color);
            }
        }
        repaint();
    }
    private void resetGridColors() {
        for (int r = 0; r < currentMap.rows; r++) {
            for (int c = 0; c < currentMap.cols; c++) {
                int val = currentMap.getWeight(r, c);
                JPanel cell = gridCells[r][c];
                
                // Restore original colors
                if (val == -1) cell.setBackground(Color.BLACK);
                else if (val == -2) cell.setBackground(Color.RED);
                else if (val == 0) cell.setBackground(Color.GREEN);
                else cell.setBackground(Color.WHITE);
            }
        }
        repaint();
    }

    public static void main(String[] args) {
        new MazeGUI();
    }
}