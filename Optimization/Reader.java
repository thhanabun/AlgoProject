package Optimization;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Reader {

    public int[][] read(String filePath) {
        try {
            // Read all bytes at once (Fastest method)
            byte[] data = Files.readAllBytes(Paths.get(filePath));
            
            // Dynamic storage for rows
            RowList rows = new RowList();
            IntList currentRow = new IntList();

            int i = 0;
            int len = data.length;

            while (i < len) {
                byte b = data[i];

                // 1. Handle Newlines (End of a row)
                if (b == '\n' || b == '\r') {
                    if (currentRow.size > 0) {
                        rows.add(currentRow.toArray());
                        currentRow.clear();
                    }
                    i++;
                    continue;
                }

                // 2. Handle Quotes (Just skip them)
                if (b == '"') {
                    i++;
                    continue;
                }

                // 3. Handle Numbers (Positive or Negative)
                // Checks for digits '0'-'9' or minus symbol '-'
                if ((b >= '0' && b <= '9') || b == '-') {
                    int val = 0;
                    boolean negative = false;
                    
                    if (b == '-') {
                        negative = true;
                        i++;
                        if (i < len) b = data[i]; // Move to digit
                    }

                    // Parse the full number
                    while (i < len && (data[i] >= '0' && data[i] <= '9')) {
                        val = (val * 10) + (data[i] - '0');
                        i++;
                    }
                    
                    currentRow.add(negative ? -val : val);
                    
                    // Do NOT increment i here, the inner loop already moved it past the number
                    continue; 
                }

                // 4. Handle Symbols (Walls, Start, Goal)
                if (b == '#') {
                    currentRow.add(-1); // Wall
                } else if (b == 'S' || b == 'G') {
                    currentRow.add(0);  // Start/Goal are walkable paths (0)
                } 
                
                // If it's a space or weird character, we just ignore it and move on
                i++;
            }

            // Add the final row if the file didn't end with a newline
            if (currentRow.size > 0) {
                rows.add(currentRow.toArray());
            }

            return rows.toArray();

        } catch (IOException e) {
            System.err.println("File could not be read: " + e.getMessage());
            return new int[0][0];
        }
    }

    // --- FAST HELPER CLASSES (To avoid ArrayList overhead) ---

    // Resizable array for integers (a single row)
    private static class IntList {
        int[] data = new int[100];
        int size = 0;

        void add(int val) {
            if (size == data.length) {
                // Resize: Create double size array and copy
                int[] newData = new int[data.length * 2];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            }
            data[size++] = val;
        }

        int[] toArray() {
            // Return exactly the size needed
            return Arrays.copyOf(data, size);
        }

        void clear() { size = 0; }
    }

    // Resizable array for the rows (the whole maze)
    private static class RowList {
        int[][] data = new int[100][];
        int size = 0;

        void add(int[] row) {
            if (size == data.length) {
                int[][] newData = new int[data.length * 2][];
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            }
            data[size++] = row;
        }

        int[][] toArray() {
            return Arrays.copyOf(data, size);
        }
    }
    
    // Test Main Method
    public static void main(String[] args) {
        Reader mr = new Reader();
        // Replace with your actual path
        int[][] maze = mr.read("MAZE\\maze1000_1000.txt"); 
        MazeMapOp mz = new MazeMapOp(maze);
        System.out.println(mz.cols);
        System.out.println(mz.rows);
        for (int i = 0; i < mz.rows; i++) {
            for (int j = 0; j < mz.cols; j++) {
                System.out.print(maze[i][j] + " ");
            }
            System.out.println(i);
        }
        // if (maze.length > 0) {
        //     System.out.println("Success! Read " + maze.length + " rows.");
        //     System.out.println("First row width: " + maze[0].length);
            
        //     // Print a small sample of the top-left corner to verify
        //     System.out.println("Top-Left 10x10 Sample:");
        //     for(int r=0; r<Math.min(10, maze.length); r++) {
        //         for(int c=0; c<Math.min(10, maze[r].length); c++) {
        //             int val = maze[r][c];
        //             if(val == -1) System.out.print("# ");
        //             else System.out.print(val + " ");
        //         }
        //         System.out.println();
        //     }
        // } else {
        //     System.out.println("Maze is empty or file not found.");
        // }
    }
}