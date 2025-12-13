package GA_StocasticDecoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import Struct.MazeMap;
import Struct.Point;

public class StocasticGA {
    private int popSize;        
    private double mutationRate; 
    private double crossoverRate;
    private int elitismCount;

    private MazeMap map;
    private Random rand = new Random();

    public StocasticGA(MazeMap map, int popSize, double mutationRate, double crossoverRate, int elitismCount) {
        this.map = map;
        this.popSize = popSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
        GlobalKnowledge.init(map.rows, map.cols);
    }

    public ArrayList<StocasticChromosome> initPopulation(List<Point> seedPath) {
        ArrayList<StocasticChromosome> population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            StocasticChromosome c = new StocasticChromosome(map.rows, map.cols);
            c.randomInit(); 
            c.path = new ArrayList<>(); 
            c.fitness = StocasticDecoder.calculateFitness(map, c, c.path);
            population.add(c);
        }
        return population;
    }

    public ArrayList<StocasticChromosome> evolve(ArrayList<StocasticChromosome> population, boolean useHeuristic, int mutationMode) {
        ArrayList<StocasticChromosome> newPopulation = new ArrayList<>();
        Collections.sort(population);
        
        for (int i = 0; i < elitismCount; i++) {
            StocasticChromosome original = population.get(i);
            StocasticChromosome clone = original.clone();
        
            if (clone.path == null || clone.path.isEmpty()) {
                if (original.path != null) {
                    clone.path = new ArrayList<>(original.path);
                }
            }

            newPopulation.add(clone);
        }

        int freshBloodCount = (int)(popSize * 0); 
        int breedCount = popSize - elitismCount - freshBloodCount;

        while (newPopulation.size() < elitismCount + breedCount) {
            StocasticChromosome parent1 = tournamentSelection(population);
            StocasticChromosome parent2 = tournamentSelection(population);

            StocasticChromosome child;
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
            StocasticChromosome immigrant = new StocasticChromosome(map.rows, map.cols);
            immigrant.randomInit();
            immigrant.fitness = -1;
            newPopulation.add(immigrant);
        }

        newPopulation.parallelStream().forEach(child -> {
            if (child.fitness == -1) {
                List<Point> tempPath = new ArrayList<>();
                child.fitness = StocasticDecoder.calculateFitness(map, child, tempPath);
                child.path = tempPath;
            }
        });
        
        return newPopulation;
    }

    public void setMutationRate(double newRate) {
        this.mutationRate = newRate;
    }

    private StocasticChromosome tournamentSelection(ArrayList<StocasticChromosome> pop) {
        int tournamentSize = 5;
        StocasticChromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = rand.nextInt(pop.size());
            StocasticChromosome candidate = pop.get(randomIndex);

            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private StocasticChromosome uniformCrossover(StocasticChromosome p1, StocasticChromosome p2) {
        StocasticChromosome child = new StocasticChromosome(map.rows, map.cols);
        
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