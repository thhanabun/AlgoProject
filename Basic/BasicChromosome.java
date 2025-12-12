package Basic;
import java.util.Random;

public class BasicChromosome implements Comparable <BasicChromosome> {
    
    public static final int MOVE_UP = 0;
    public static final int MOVE_DOWN = 1;
    public static final int MOVE_LEFT = 2;
    public static final int MOVE_RIGHT = 3;

    public int[] genes;
    public int fitness;
    public boolean solved = false;

    private static Random rand = new Random();

    public BasicChromosome(int geneLength) {
        genes = new int[geneLength];
        for (int i = 0; i < geneLength; i++) {
            genes[i] = rand.nextInt(4);
        }
    }

    public BasicChromosome(int[] genes) {
        this.genes = genes.clone();
    }

    public BasicChromosome copy() {
        return new BasicChromosome(genes);
    }

    public void mutate(double mutationRate) {
        for (int i = 0; i < genes.length; i++) {
            if (rand.nextDouble() < mutationRate) {
                genes[i] = rand.nextInt(4);
            }
        }
    }

    public void evaluate(MazeMap maze) {
        int r = maze.start.r;
        int c = maze.start.c;
        boolean[][] visited = new boolean[maze.rows][maze.cols];

        int score = 0;
        int wallHits = 0;

        for (int move : genes) {

            int nr = r, nc = c;

            if (move == MOVE_UP) nr--;
            else if (move == MOVE_DOWN) nr++;
            else if (move == MOVE_LEFT) nc--;
            else if (move == MOVE_RIGHT) nc++;

            if (!maze.isValid(nr, nc)) {
                wallHits++;
                score -= 3;         // penalty
                continue;
            }

            r = nr;
            c = nc;

            if (!visited[r][c]) {
                visited[r][c] = true;
                score += 5;        // reward exploration
                score += maze.getWeight(r, c);  // reward low-weight paths
            }

            if (maze.grid[r][c] == 0 && r == maze.goal.r && c == maze.goal.c) {
                solved = true;
                score += 10000;    // massive reward
                break;
            }
        }

        score -= wallHits * 2;
        this.fitness = score;
    }

    @Override
    public int compareTo(BasicChromosome other) {
        return Double.compare(other.fitness, this.fitness);
    }
}

