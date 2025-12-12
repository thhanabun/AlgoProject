package DumbestGA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm2 {
    private int popSize;        
    private double mutationRate; 
    private double crossoverRate;
    private int elitismCount;
    private MazeMap map;
    private Random rand = new Random();

    public GeneticAlgorithm2(MazeMap map, int popSize, double mutationRate, double crossoverRate, int elitismCount) {
        this.map = map;
        this.popSize = popSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
        GlobalKnowledge.init(map.rows, map.cols);
    }

    public ArrayList<Chromosome2> initPopulation(List<Point> seedPath) {
        ArrayList<Chromosome2> population = new ArrayList<>();
        
        // Loop สร้างประชากร
        for (int i = 0; i < popSize; i++) {
            Chromosome2 c = new Chromosome2(map.rows, map.cols);
            
            // ใช้ Random Init แบบธรรมดา (ไม่ต้องส่ง goal แล้วเพราะเรากลับมาใช้แบบ Simple)
            c.randomInit(); 
            
            // *** สร้าง Path เก็บไว้ในตัว และคำนวณ Fitness ***
            c.path = new ArrayList<>(); 
            c.fitness = DumbestDecoder.calculateFitness(map, c, c.path);
            
            population.add(c);
        }
        return population;
    }

    public ArrayList<Chromosome2> evolve(ArrayList<Chromosome2> population, boolean useHeuristic, int mutationMode) {
        ArrayList<Chromosome2> newPopulation = new ArrayList<>();
        Collections.sort(population);
        
        // 1. Elitism
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.add(population.get(i).clone());
        }

        int freshBloodCount = (int)(popSize * 0.1); 
        int breedCount = popSize - elitismCount - freshBloodCount;

        // 2. Breeding
        while (newPopulation.size() < elitismCount + breedCount) {
            Chromosome2 parent1 = tournamentSelection(population);
            Chromosome2 parent2 = tournamentSelection(population);

            Chromosome2 child;
            if (rand.nextDouble() < crossoverRate) {
                child = uniformCrossover(parent1, parent2);
            } else {
                child = parent1.clone();
            }

            // *** [UPDATE] ส่ง map เข้าไปเพื่อให้เช็ค Junction ได้ ***
            child.mutate(mutationRate, mutationMode, parent1.path,map);

            if (child.fitness != -1) child.fitness = -1; 
            newPopulation.add(child);
        }

        // 3. Fresh Blood (พวกนี้ไม่มี Block ติดตัวมา ช่วยรีเช็คทาง)
        for (int i = 0; i < freshBloodCount; i++) {
            Chromosome2 immigrant = new Chromosome2(map.rows, map.cols);
            immigrant.randomInit();
            immigrant.fitness = -1;
            newPopulation.add(immigrant);
        }

        // 4. Calculate Fitness
        newPopulation.parallelStream().forEach(child -> {
            if (child.fitness == -1) {
                List<Point> tempPath = new ArrayList<>();
                child.fitness = DumbestDecoder.calculateFitness(map, child, tempPath);
                child.path = tempPath;
            }
        });
        
        return newPopulation;
    }

    public void setMutationRate(double newRate) {
        this.mutationRate = newRate;
    }

    private Chromosome2 tournamentSelection(ArrayList<Chromosome2> pop) {
        int tournamentSize = 5;
        Chromosome2 best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Chromosome2 candidate = pop.get(rand.nextInt(pop.size()));
            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private Chromosome2 uniformCrossover(Chromosome2 p1, Chromosome2 p2) {
        Chromosome2 child = new Chromosome2(map.rows, map.cols);
        
        for (int i = 0; i < child.genes.length; i++) {
            child.genes[i] = rand.nextBoolean() ? p1.genes[i] : p2.genes[i];
        }

        child.inheritWalls(p1, p2);
        
        child.fitness = -1;
        return child;
    }
}