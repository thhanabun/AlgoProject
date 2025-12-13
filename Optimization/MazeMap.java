package Optimization;


public class MazeMap {
    public int[][] grid;
    public int rows;
    public int cols;
    public Point start;
    public Point goal;

    public MazeMap(int[][] rawData) {
        this.rows = rawData.length;
        this.cols = rawData[0].length;
        this.grid = rawData;


        this.start = new Point(1, 1);
        this.goal = new Point(rows - 2, cols - 2);
        if (grid[start.r][start.c] != 0 || grid[goal.r][goal.c] != 0) {
            System.err.println("Warning Start and Goal Wrong Point.");
        }
    }

    public boolean isValid(int r, int c) {
        if (r < 0 || c < 0 || r >= rows || c >= cols) return false;
        if (grid[r][c] == -1) return false;
        return true;
    }

    public int getWeight(int r, int c) {
        return grid[r][c];
    }
}