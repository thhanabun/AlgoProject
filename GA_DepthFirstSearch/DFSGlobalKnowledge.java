package GA_DepthFirstSearch;

public class DFSGlobalKnowledge {
    public static boolean[] deadEnds; 
    public static int rows, cols;

    public static void init(int r, int c) {
        rows = r;
        cols = c;
        deadEnds = new boolean[rows * cols];
    }
    
    public static boolean isDeadEnd(int r, int c) {
        if (deadEnds == null) return false;
        return deadEnds[r * cols + c];
    }
    
    public static void markDeadEnd(int r, int c) {
        if (deadEnds != null) deadEnds[r * cols + c] = true;
    }
}