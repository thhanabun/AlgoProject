import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class MazeGUI extends JFrame {

    private ArrayList<ArrayList<Integer>> mazeData;    
    private JPanel mazePanel;
    private int CELL_SIZE = 20;

    public MazeGUI() {
        setTitle("Maze Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);

        // 1. Initialize the 2D ArrayList Data
        initializeMazeData();

        // 2. Setup the Main Panel
        mazePanel = new JPanel();
        // GridLayout(rows, cols) - We use the size of our ArrayList
        int rows = mazeData.size();
        int cols = mazeData.get(0).size();
        mazePanel.setLayout(new GridLayout(rows, cols));
        mazePanel.setPreferredSize(new Dimension(cols * CELL_SIZE, rows * CELL_SIZE));
        renderMaze(mazePanel, mazeData, rows, cols);
        JScrollPane scrollPane = new JScrollPane(mazePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane);
        
        // Center the window on screen
        setLocationRelativeTo(null); 
        setVisible(true);
    }

    private void initializeMazeData() {
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("MAZE"));
        fileChooser.setDialogTitle("Select Maze Text File");
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            Mazereader mr = new Mazereader();
            
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getAbsolutePath(); // <--- THIS IS THE STRING

            mazeData = mr.read(fileName);

            if (mazeData != null) {
                return;
            } else {
                System.out.println("Error reading file.");
            }
            
        } else {
            System.out.println("No file selected.");
            System.exit(0);
        }
        
    }
    private void renderMaze(JPanel panel, ArrayList<ArrayList<Integer>> data, int rows, int cols) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cellValue = data.get(r).get(c);

                JPanel cell = new JPanel();
                cell.setLayout(new BorderLayout());
                cell.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY)); // Lighter border for dense grids

                // Color Logic
                if (cellValue == -1) cell.setBackground(Color.BLACK);
                else if (cellValue == -2) cell.setBackground(Color.RED);
                else if (cellValue == 0) cell.setBackground(Color.GREEN);
                else cell.setBackground(Color.WHITE);
                
                if (cellValue <= 0) {
                    panel.add(cell);
                    continue;
                }
                // Score Logic
                JLabel scoreLabel = new JLabel(String.valueOf(cellValue));
                scoreLabel.setHorizontalAlignment(JLabel.CENTER);
                // Smaller font for 100x100 grid so it fits
                scoreLabel.setFont(new Font("Arial", Font.PLAIN, 7)); 
                
                if (cellValue == -1) scoreLabel.setForeground(Color.WHITE);
                else scoreLabel.setForeground(Color.BLACK);

                cell.add(scoreLabel, BorderLayout.CENTER);
                panel.add(cell);
            }
        }
    }
   

    public static void main(String[] args) {
        new MazeGUI();
    }
}