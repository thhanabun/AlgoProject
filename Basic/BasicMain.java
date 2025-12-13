package Basic;

public class BasicMain {
    public static void main(String[] args) {
        MazeReader mr = new MazeReader();
      
        MazeMap maze = new MazeMap(mr.read("MAZE\\m100_100.txt"));

        int populationSize = 200;
        int geneLength = maze.rows * maze.cols * 2;
        double mutationRate = 0.03;

        BasicGenetic ga = new BasicGenetic(populationSize, geneLength, mutationRate, maze);
        BasicChromosome finalBest = null;

        for (int gen = 0; gen < 1000; gen++) {

            ga.evaluatePopulation();
            BasicChromosome best = ga.getBest();

            if (finalBest == null || best.fitness > finalBest.fitness) {
                finalBest = best.copy();
            }

            if (best.solved) {
                System.out.println("Generation " + gen + " has a solution. Fitness: " + best.fitness);
            }

            if (gen % 100 == 0) {
                System.out.println("Gen " + gen + " best fitness = " + best.fitness);
            }

            ga.evolve();
        }

        System.out.println("Finished all generations.");
        System.out.println("Best fitness overall = " + finalBest.fitness);
        System.out.println("Solved = " + finalBest.solved);

        printPath(maze, finalBest);
    }

    private static void printPath(MazeMap maze, BasicChromosome best) {
        int r = maze.start.r;
        int c = maze.start.c;

        char[][] vis = new char[maze.rows][maze.cols];
        for (int i = 0; i < maze.rows; i++) {
            for (int j = 0; j < maze.cols; j++) {
                if (maze.grid[i][j] == -1) vis[i][j] = '#';
                else vis[i][j] = '.';
            }
        }

        vis[r][c] = 'S';

        for (int move : best.genes) {

            int nr = r, nc = c;

            if (move == 0) nr--;
            else if (move == 1) nr++;
            else if (move == 2) nc--;
            else if (move == 3) nc++;

            if (!maze.isValid(nr, nc)) continue;

            r = nr; c = nc;
            vis[r][c] = '*';

            if (r == maze.goal.r && c == maze.goal.c) {
                vis[r][c] = 'G';
                break;
            }
        }

        for (int i = 0; i < vis.length; i++) {
            for (int j = 0; j < vis[0].length; j++) {
                System.out.print(vis[i][j] + " ");
            }
            System.out.println();
        }
    }
}
