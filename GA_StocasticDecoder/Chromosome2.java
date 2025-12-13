package GA_StocasticDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Struct.MazeMap;
import Struct.Point;

public class Chromosome2 implements Comparable<Chromosome2> {
    public double[] genes; 
    public double fitness = -1;
    public int rows, cols;

    public boolean[] junctionBlocks; // เก็บเฉพาะทางแยกส่วนตัว
    public List<Point> path = new ArrayList<>(); 
    
    private static final Random rand = new Random();

    public static final int MUTATION_RANDOM = 0;
    public static final int MUTATION_FLIP   = 1;
    public static final int MUTATION_HYBRID = 2;
    
    public Chromosome2(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.genes = new double[rows * cols];
        this.junctionBlocks = new boolean[rows * cols];
    }

    public void randomInit() {
        for (int i = 0; i < genes.length; i++) {
            genes[i] = rand.nextDouble();
            junctionBlocks[i] = false; 
        }
    }
    
    public void randomInit(Point p) { randomInit(); } 

    public boolean isMyBlock(int r, int c) {
        return junctionBlocks[r * cols + c];
    }

    public void addJunctionBlock(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            junctionBlocks[r * cols + c] = true;
        }
    }

    public double getPriority(int r, int c) {
        return genes[r * cols + c];
    }
    
    public void mutate(double mutationRate, int mode, List<Point> parentPath, MazeMap map) {
        for (int i = 0; i < genes.length; i++) {
            if (rand.nextDouble() < mutationRate) genes[i] = rand.nextDouble();
            if (junctionBlocks[i] && rand.nextDouble() < 0.15) junctionBlocks[i] = false; 
        }

        if (parentPath != null && parentPath.size() >= 5) {
            if (rand.nextDouble() < 0.6) {
                int targetBlocks = 1 + rand.nextInt(3); 
                int blocksDone = 0;
                int maxTries = 50; 
                while (blocksDone < targetBlocks && maxTries-- > 0) {
                    int idx = rand.nextInt(parentPath.size() - 1);
                    Point curr = parentPath.get(idx);
                    if ((curr.r == map.start.r && curr.c == map.start.c) || 
                        (curr.r == map.goal.r && curr.c == map.goal.c)) continue;
                    if (isJunction(map, curr.r, curr.c)) {
                        Point nextStep = parentPath.get(idx + 1);
                        if ((nextStep.r == map.goal.r && nextStep.c == map.goal.c) ||
                            isMyBlock(nextStep.r, nextStep.c)) {
                            continue;
                        }
                        addJunctionBlock(nextStep.r, nextStep.c);
                        blocksDone++;

                    }
                }
            }
        }
        this.fitness = -1; 
    }
    
    private boolean isJunction(MazeMap map, int r, int c) {
        int ways = 0;
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (map.isValid(nr, nc) && !GlobalKnowledge.isDeadEnd(nr, nc)) {
                ways++;
            }
        }
        return ways > 2; 
    }

    public void inheritWalls(Chromosome2 p1, Chromosome2 p2) {
        for(int i=0; i<junctionBlocks.length; i++) {
            boolean w1 = p1.junctionBlocks[i];
            boolean w2 = p2.junctionBlocks[i];
            if (w1 && w2) this.junctionBlocks[i] = true;
            else if (w1 || w2) this.junctionBlocks[i] = rand.nextBoolean();
            else this.junctionBlocks[i] = false;
        }
    }

    public Chromosome2 clone() {
        Chromosome2 c = new Chromosome2(rows, cols);
        System.arraycopy(this.genes, 0, c.genes, 0, genes.length);
        System.arraycopy(this.junctionBlocks, 0, c.junctionBlocks, 0, junctionBlocks.length);
        c.fitness = this.fitness;
        if (this.path != null) c.path = new ArrayList<>(this.path); 
        return c;
    }

    @Override
    public int compareTo(Chromosome2 other) {
        return Double.compare(this.fitness, other.fitness);
    }
}