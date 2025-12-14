package GA_DepthFirstSearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import Struct.MazeMap;
import Struct.Point;

public class DFSGA {
    private int popSize;        
    private double mutationRate; 
    private double crossoverRate;
    private int elitismCount;

    private MazeMap map;
    private Random rand = new Random();

    public DFSGA(MazeMap map, int popSize, double mutationRate, double crossoverRate, int elitismCount) {
        this.map = map;
        this.popSize = popSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
        DFSGlobalKnowledge.init(map.rows, map.cols);
    }

    public ArrayList<DFSChromosome> initPopulation(List<Point> seedPath) {
        ArrayList<DFSChromosome> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            DFSChromosome c = new DFSChromosome(map.rows, map.cols);
            c.randomInit(); 
            c.path = new ArrayList<>(); 
            c.fitness = DFSPriorityDecoder.calculateFitness(map, c, c.path);
            population.add(c);
        }
        return population;
    }

    public ArrayList<DFSChromosome> evolve(ArrayList<DFSChromosome> population, boolean useHeuristic, int mutationMode) {
        ArrayList<DFSChromosome> newPopulation = new ArrayList<>();
        Collections.sort(population);
        
        for (int i = 0; i < elitismCount; i++) {
            DFSChromosome original = population.get(i);
            DFSChromosome clone = original.clone();
        
            if (clone.path == null || clone.path.isEmpty()) {
                if (original.path != null) {
                    clone.path = new ArrayList<>(original.path);
                }
            }

            newPopulation.add(clone);
        }

        int freshBloodCount = (int)(popSize * 0.15); 
        int breedCount = popSize - elitismCount - freshBloodCount;

        while (newPopulation.size() < elitismCount + breedCount) {
            DFSChromosome parent1 = tournamentSelection(population);
            DFSChromosome parent2 = tournamentSelection(population);

            DFSChromosome child;
            if (rand.nextDouble() < crossoverRate) {
                child = uniformCrossover(parent1, parent2);
            } else {
                child = parent1.clone();
            }

            child.mutate(mutationRate, mutationMode, parent1.path,map);

            if (child.fitness != -1) child.fitness = -1; 
            newPopulation.add(child);
        }

        for (int i = 0; i < freshBloodCount; i++) {
            DFSChromosome immigrant = new DFSChromosome(map.rows, map.cols);
            immigrant.randomInit();
            immigrant.fitness = -1;
            newPopulation.add(immigrant);
        }

        newPopulation.parallelStream().forEach(child -> {
            if (child.fitness == -1) {
                List<Point> tempPath = new ArrayList<>();
                //child.fitness = DFSDecoder.calculateFitness(map, child, tempPath);
                child.fitness = DFSPriorityDecoder.calculateFitness(map, child, tempPath);
                child.path = tempPath;
            }
        });
        
        return newPopulation;
    }

    public void setMutationRate(double newRate) {
        this.mutationRate = newRate;
    }

    private DFSChromosome tournamentSelection(ArrayList<DFSChromosome> pop) {
        int tournamentSize = 5;
        DFSChromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = rand.nextInt(pop.size());
            DFSChromosome candidate = pop.get(randomIndex);

            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private DFSChromosome uniformCrossover(DFSChromosome p1, DFSChromosome p2) {
        DFSChromosome child = new DFSChromosome(map.rows, map.cols);
        
        for (int i = 0; i < child.genes.length; i++) {
            if (rand.nextBoolean()) {
                child.genes[i] = p1.genes[i];
            } else {
                child.genes[i] = p2.genes[i];
            }
        }

        child.inheritWalls(p1, p2);
    
        child.fitness = -1;
        return child;
    }
}