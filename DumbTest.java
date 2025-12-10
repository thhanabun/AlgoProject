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
        ArrayList<ArrayList<Integer>> maze = mr.read("MAZE/m70_60.txt"); // เช็คชื่อไฟล์ดีๆ นะครับ m100_90 หรือ m100_100
        
        if (maze == null) {
            System.err.println("Error: Could not read maze file!");
            return;
        }

        MazeMap map = new MazeMap(maze);
        System.out.println("Map Created! Size: " + map.rows + "x" + map.cols);
        System.out.println("Start: " + map.start + " -> Goal: " + map.goal);

        // --------------------------------------------------------
        // 2. Run Pure A* (Benchmark)
        // --------------------------------------------------------
        System.out.println("\n--- Running Pure A* Benchmark ---");
        long startA = System.currentTimeMillis();

        double optimalCost = DumbDecoder.runPureAStar(map); 
        long endA = System.currentTimeMillis();
        
        System.out.println("Optimal Cost (Benchmark): " + optimalCost);
        System.out.println("Time taken: " + (endA - startA) + " ms");
        
        List<Point> optimalPath = DumbDecoder.getPureAStarPath(map);
        if (map.rows <= 30) {
            System.out.println("Optimal Path:");
            printVisualMap(map, optimalPath);
        } else {
            System.out.println("(Map too large to visualize path in console)");
        }

        System.out.println();
        System.out.println("Generating Greedy Seed Path...");
        List<Point> greedyPath = DumbDecoder.getGreedyPath(map);
        if (greedyPath.isEmpty()) {
            System.err.println("Greedy Search failed! (Map might be blocked)");
        } else {
            System.out.println("Greedy Path Found! Steps: " + greedyPath.size());
        }

        // --------------------------------------------------------
        // 3. Setup Genetic Algorithm
        // --------------------------------------------------------
        System.out.println("\n--- Start GA Optimization ---");
        
        int popSize = 100;         
        double mutationRate = 0.1;
        double crossoverRate = 0.9;
        int elitismCount = 5;
        int maxGenerations = 1000;  

        GeneticAlgorithm ga = new GeneticAlgorithm(map, popSize, mutationRate, crossoverRate, elitismCount);

        // --------------------------------------------------------
        // 4. Run Evolution Loop
        // --------------------------------------------------------
        System.out.println("Injecting Greedy Path into Population...");
        ArrayList<Chromosome> population = ga.initPopulation(greedyPath);
        
        double lastBestFitness = Double.MAX_VALUE;
        int stagnationCount = 0;
        double defaultMutationRate = mutationRate;
        boolean useHeuristic = false; // [NEW] เริ่มต้นแบบปิดตา (Blind)

        for (int gen = 1; gen <= maxGenerations; gen++) {
            
            // [NEW] Logic สลับโหมด: ครึ่งหลังเปิดตา A* ช่วยดึงเส้นทาง
            if (gen > maxGenerations / 2) useHeuristic = true;
        

            // ส่ง useHeuristic เข้าไป
            population = ga.evolve(population, useHeuristic);
            
            Collections.sort(population);
            Chromosome best = population.get(0);

            // -----------------------------------------------------------
            // Logic: Adaptive Mutation (ระเบิดพลังเมื่อติดขัด)
            // -----------------------------------------------------------
            if (Math.abs(best.fitness - lastBestFitness) < 0.0001) {
                stagnationCount++; 
            } else {
                stagnationCount = 0; 
                lastBestFitness = best.fitness; 
                ga.setMutationRate(defaultMutationRate); 
            }

            if (stagnationCount > 50) {
                if (stagnationCount == 51) {
                    // System.out.println(" [BOOST MUTATION!]"); // ปิดไว้ก็ได้เดี๋ยวรกจอ
                }
                ga.setMutationRate(0.3); 
            }
            // -----------------------------------------------------------

            
            // Print Status
            if (gen % 1 == 0 || gen == 1 || best.fitness <= optimalCost) {
                System.out.printf("Generation %3d: Best Fitness = %.2f", gen, best.fitness);
                
                if (best.fitness < 10000) {
                    System.out.print(" [Path Found!]");
                } else {
                    System.out.print(" [Searching...]");
                }
                
                if (stagnationCount > 50) System.out.print(" [Stuck... Boosting Mutation]");
                if (useHeuristic) System.out.print(" [Mode: A* Guided]"); // บอกสถานะด้วย
                
                System.out.println();
            }

            // Early Stopping
            if (best.fitness <= optimalCost + 0.001) { // เผื่อ Floating point error นิดนึง
                System.out.println("\n>>> Found Optimal Solution at Gen " + gen + " <<<");
                break;
            }
        }

        // --------------------------------------------------------
        // 5. Final Result
        // --------------------------------------------------------
        Chromosome solution = population.get(0);
        System.out.println("------------------------------");
        System.out.println("Final Solution Found!");
        System.out.println("Best Fitness: " + solution.fitness);
        System.out.println("Gap from Optimal: " + (solution.fitness - optimalCost));

        List<Point> solutionPath = DumbDecoder.getPath(map, solution, true);
        
        if (solutionPath.isEmpty()) {
            System.out.println("WARNING: No valid path found.");
        } else {
            System.out.println("Path Steps: " + solutionPath.size());
            
            if (map.rows <= 90) {
                printVisualMap(map, solutionPath);
            } else {
                System.out.println("Path (Partial): " + solutionPath.get(0) + " ... " + solutionPath.get(solutionPath.size()-1));
            }
        }
        System.out.println("FINISH");
    }

    public static void printVisualMap(MazeMap map, List<Point> path) {
        boolean[][] isPath = new boolean[map.rows][map.cols];
        for (Point p : path) isPath[p.r][p.c] = true;

        for (int r = 0; r < map.rows; r++) {
            for (int c = 0; c < map.cols; c++) {
                if (map.getWeight(r, c) == -1) {
                    System.out.print(" # ");
                } else if (r == map.start.r && c == map.start.c) {
                    System.out.print(" S ");
                } else if (r == map.goal.r && c == map.goal.c) {
                    System.out.print(" G ");
                } else if (isPath[r][c]) {
                    System.out.print(" * ");
                } else {
                    System.out.print(" . ");
                }
            }
            System.out.println();
        }
    }
}