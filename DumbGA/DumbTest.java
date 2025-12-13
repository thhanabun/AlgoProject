package DumbGA;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DumbTest {
    public static void main(String[] args) {
        // --------------------------------------------------------
        // 1. Load Map
        // --------------------------------------------------------
        System.out.println("Loading Map...");
        Mazereader mr = new Mazereader();
        ArrayList<ArrayList<Integer>> maze = mr.read("MAZE/m15_15.txt"); 
        
        if (maze == null) {
            System.err.println("Error: Could not read maze file!");
            return;
        }

        MazeMap map = new MazeMap(maze);
        System.out.println("Map Created! Size: " + map.rows + "x" + map.cols);
        
        // --------------------------------------------------------
        // BENCHMARK 1: Pure A* (The Optimal Target)
        // --------------------------------------------------------
        System.out.println("\n==========================================");
        System.out.println("BENCHMARK 1: Pure A* (Global Optimum)");
        System.out.println("==========================================");
        
        long startA = System.currentTimeMillis();
        double optimalCost = DumbDecoder.runPureAStar(map); 
        long endA = System.currentTimeMillis();
        
        System.out.println(">> Optimal Cost: " + optimalCost);
        System.out.println(">> Time: " + (endA - startA) + " ms");

        // --------------------------------------------------------
        // BENCHMARK 2: Greedy Search (The Baseline to Beat)
        // --------------------------------------------------------
        System.out.println("\n==========================================");
        System.out.println("BENCHMARK 2: Greedy Best-First (Speed King)");
        System.out.println("==========================================");
        
        List<Point> greedyPath = DumbDecoder.getGreedyPath(map);
        double greedyCost = calculatePathCost(map, greedyPath); // คำนวณ Cost ของ Greedy
        
        System.out.println(">> Greedy Cost: " + greedyCost);
        System.out.println(">> Steps: " + greedyPath.size());
        
        if (greedyCost > optimalCost) {
            System.out.println("(Note: Greedy is sub-optimal. GA should try to beat this score!)");
        } else {
            System.out.println("(Note: Greedy found optimal/near-optimal path. GA has a hard job!)");
        }

        // --------------------------------------------------------
        // CHALLENGER: Genetic Algorithm (Pure Random Start)
        // --------------------------------------------------------
        System.out.println("\n==========================================");
        System.out.println("CHALLENGER: Genetic Algorithm (Pure Random)");
        System.out.println("==========================================");
        
        int popSize = 100;         
        double mutationRate = 0.1; 
        double crossoverRate = 0.9;
        int elitismCount = 5;
        int maxGenerations = 300;
        int mutationMode = Chromosome.MUTATION_RANDOM;

        GeneticAlgorithm ga = new GeneticAlgorithm(map, popSize, mutationRate, crossoverRate, elitismCount);

        System.out.println("Initializing Pure Random Population (No Seeds)...");
        ArrayList<Chromosome> population = ga.initPopulation(null); 

        double lastBestFitness = Double.MAX_VALUE;
        int stagnationCount = 0;
        double defaultMutationRate = mutationRate;
        boolean useHeuristic = false; 

        for (int gen = 1; gen <= maxGenerations; gen++) {
            
            // ครึ่งหลังเปิดตา A*
            if (gen > maxGenerations / 2) {
                //useHeuristic = true;
            } 

            population = ga.evolve(population, useHeuristic, mutationMode);
            
            Collections.sort(population);
            Chromosome best = population.get(0);

            // --- Adaptive Mutation Logic ---
            if (Math.abs(best.fitness - lastBestFitness) < 0.0001) {
                stagnationCount++; 
            } else {
                stagnationCount = 0; 
                lastBestFitness = best.fitness; 
                ga.setMutationRate(defaultMutationRate); 
            }

            if (stagnationCount > 50) ga.setMutationRate(0.4); 
            // -------------------------------

            // Print Status
            if (gen % 10 == 0 || gen == 1 || best.fitness <= optimalCost) {
                System.out.printf("Gen %4d: Best = %7.2f", gen, best.fitness);
                
                if (best.fitness < 10000) {
                    // เปรียบเทียบกับ Greedy ให้ดูด้วย
                    if (best.fitness < greedyCost) System.out.print(" [Beats Greedy!]");
                    else System.out.print(" [Trailing Greedy...]");
                } else {
                    System.out.print(" [Searching...]");
                }
                
                if (useHeuristic) System.out.print(" [A* Mode]");
                if (stagnationCount > 50) System.out.print(" [Boost]");
                
                System.out.println();
            }

            if (best.fitness <= optimalCost + 0.001) {
                System.out.println("\n>>> Found Optimal Solution at Gen " + gen + " <<<");
                break;
            }
        }

        // --------------------------------------------------------
        // Final Result
        // --------------------------------------------------------
        Chromosome solution = population.get(0);
        System.out.println("------------------------------");
        System.out.println("Final Solution Found!");
        System.out.println("Best Fitness: " + solution.fitness);
        System.out.println(population.get(50).fitness);
        System.out.println("A* Optimal  : " + optimalCost);
        System.out.println("Greedy Cost : " + greedyCost);
        
        List<Point> solutionPath = DumbDecoder.getPath(map, solution, true);
        if (!solutionPath.isEmpty()) {
            System.out.print("Path Sequence: ");
            int limit = 4;

            if (solutionPath.size() <= limit * 2) {
                for (int i = 0; i < solutionPath.size(); i++) {
                    System.out.print(solutionPath.get(i));
                    if (i < solutionPath.size() - 1) System.out.print(" -> ");
                }
                System.out.println();
            } else {
                for (int i = 0; i < limit; i++) {
                    System.out.print(solutionPath.get(i) + " -> ");
                }

                int skipped = solutionPath.size() - (limit * 2);
                System.out.print("... (" + skipped + " steps) ... -> ");

                for (int i = solutionPath.size() - limit; i < solutionPath.size(); i++) {
                    System.out.print(solutionPath.get(i));
                    if (i < solutionPath.size() - 1) System.out.print(" -> ");
                }
                System.out.println();
            }

            if (map.rows <= 90) {
                 printVisualMap(map, solutionPath);
            } else {
                 System.out.println("(Map too large to visualize path in console)");
            }
        }
    }

    public static double calculatePathCost(MazeMap map, List<Point> path) {
        if (path.isEmpty()) return 20000.0;
        double cost = 0;
        for (int i = 1; i < path.size(); i++) {
            Point p = path.get(i);
            cost += map.getWeight(p.r, p.c);
        }
        return cost;
    }

    public static void printVisualMap(MazeMap map, List<Point> path) {
        boolean[][] isPath = new boolean[map.rows][map.cols];
        for (Point p : path) isPath[p.r][p.c] = true;

        for (int r = 0; r < map.rows; r++) {
            for (int c = 0; c < map.cols; c++) {
                if (map.getWeight(r, c) == -1) System.out.print(" # ");
                else if (r == map.start.r && c == map.start.c) System.out.print(" S ");
                else if (r == map.goal.r && c == map.goal.c) System.out.print(" G ");
                else if (isPath[r][c]) System.out.print(" * ");
                else System.out.print(" . ");
            }
            System.out.println();
        }
    }
}