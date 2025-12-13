import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class MazeGen2 {
    static File dir = new File("MAZE");
    
    private static int ROWS;
    private static int COLS;
    private static int[][] grid;
    
    private static final Random rand = new Random();

    // Coordinates
    private static int START_ROW = 1;
    private static int START_COL = 1;
    private static int GOAL_ROW;
    private static int GOAL_COL;

    public static void main(String[] args) {
        if (!dir.exists()) dir.mkdirs();

        Scanner sc = new Scanner(System.in);
        System.out.println("--- Maze Generator (Any Size) ---");
        
        System.out.print("Enter Rows (e.g., 50): ");
        ROWS = sc.nextInt();
        
        System.out.print("Enter Cols (e.g., 50): ");
        COLS = sc.nextInt(); 

        System.out.print("Enter Extra Paths % (0-100): ");
        int densityInput = sc.nextInt();
        double loopFactor = densityInput / 100.0;
        
        sc.close();

        // Safety: Minimum size 4x4 to prevent crash
        if (ROWS < 4) ROWS = 4;
        if (COLS < 4) COLS = 4;

        // Set Goal to bottom-right (inside the walls)
        GOAL_ROW = ROWS - 2;
        GOAL_COL = COLS - 2;

        grid = new int[ROWS][COLS];


        generateMazeOptimized(START_ROW, START_COL);
        addLoops(loopFactor);

        // This step is CRITICAL for Even dimensions:
        // It drills through the "double wall" that even numbers create.
        connectGoalToMaze();


        String filename = "m" + ROWS + "_" + COLS + ".txt";
        saveMazeOptimized(filename);
    }

    private static int randomWeight() {
        return rand.nextInt(10) + 1;
    }

    // --- ALGORITHM 1: Base DFS Structure ---
    private static void generateMazeOptimized(int startR, int startC) {
        int[] stackR = new int[ROWS * COLS];
        int[] stackC = new int[ROWS * COLS];
        int top = 0;

        grid[startR][startC] = randomWeight();
        stackR[top] = startR;
        stackC[top] = startC;
        top++;

        // Jump 2 steps at a time
        int[] dr = {-2, 2, 0, 0};
        int[] dc = {0, 0, -2, 2};
        int[] idx = {0, 1, 2, 3}; 

        while (top > 0) {
            int r = stackR[top - 1];
            int c = stackC[top - 1];

            // Shuffle directions
            for (int i = 0; i < 4; i++) {
                int rnd = rand.nextInt(4);
                int temp = idx[i];
                idx[i] = idx[rnd];
                idx[rnd] = temp;
            }

            boolean found = false;
            for (int i = 0; i < 4; i++) {
                int dirIndex = idx[i];
                int nr = r + dr[dirIndex];
                int nc = c + dc[dirIndex];

                // Boundary Check
                if (nr > 0 && nr < ROWS - 1 && nc > 0 && nc < COLS - 1) {
                    if (grid[nr][nc] == 0) {
                        grid[r + dr[dirIndex] / 2][c + dc[dirIndex] / 2] = randomWeight();
                        grid[nr][nc] = randomWeight();
                        stackR[top] = nr;
                        stackC[top] = nc;
                        top++;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) top--; 
        }
    }

    // --- ALGORITHM 2: Add Loops ---
    private static void addLoops(double factor) {
        for (int r = 1; r < ROWS - 1; r++) {
            for (int c = 1; c < COLS - 1; c++) {
                if (grid[r][c] == 0) {
                    boolean v = (grid[r-1][c] > 0 && grid[r+1][c] > 0);
                    boolean h = (grid[r][c-1] > 0 && grid[r][c+1] > 0);
                    if (v || h) {
                        if (rand.nextDouble() < factor) {
                            grid[r][c] = randomWeight(); 
                        }
                    }
                }
            }
        }
    }

    // --- ALGORITHM 3: Force Goal Connection ---
    private static void connectGoalToMaze() {
        int r = GOAL_ROW;
        int c = GOAL_COL;
        
        // Even dimensions often leave the Goal cell as a wall (0). 
        // We forcibly open it here.
        if (grid[r][c] == 0) grid[r][c] = randomWeight();

        // Drill Up/Left/Right/Down until we find a path
        while (r > 1) {
            if ((r > 0 && grid[r - 1][c] > 0) || 
                (c > 0 && grid[r][c - 1] > 0) ||
                (c < COLS - 1 && grid[r][c + 1] > 0) || 
                (r < ROWS - 1 && grid[r + 1][c] > 0)) {
                break; 
            }
            r--; // Move Up
            if (r > 0) grid[r][c] = randomWeight();
        }
    }

    // --- FILE SAVING ---
    private static void saveMazeOptimized(String filename) {
        File file = new File(dir, filename);
        if (file.exists()) file = getUniqueFile(file);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file), 32768)) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < ROWS; i++) {
                sb.setLength(0);
                for (int j = 0; j < COLS; j++) {
                    // Force strict borders
                    if (i == 0 || i == ROWS - 1 || j == 0 || j == COLS - 1) {
                        sb.append('#');
                    } else if (i == START_ROW && j == START_COL) {
                        sb.append('S');
                    } else if (i == GOAL_ROW && j == GOAL_COL) {
                        sb.append('G');
                    } else if (grid[i][j] == 0) {
                        sb.append('#');
                    } else {
                        sb.append('"').append(grid[i][j]).append('"');
                    }
                }
                bw.write(sb.toString());
                bw.newLine();
            }
            System.out.println("Saved: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getUniqueFile(File file) {
        if (!file.exists()) return file;
        String name = file.getName();
        String base = name, ext = "";
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        int count = 1;
        File newFile;
        do {
            newFile = new File(file.getParent(), base + " (" + count + ")" + ext);
            count++;
        } while (newFile.exists());
        return newFile;
    }
}