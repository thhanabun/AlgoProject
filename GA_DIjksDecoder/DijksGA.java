package GA_DijksDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import Struct.MazeMap;
import Struct.Point;

public class DijksGA {
    private int popSize;         
    private double mutationRate; 
    private double crossoverRate;
    private int elitismCount;

    private MazeMap map;
    private Random rand = new Random();

    public DijksGA(MazeMap map, int popSize, double mutationRate, double crossoverRate, int elitismCount) {
        this.map = map;
        this.popSize = popSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
    }

    public ArrayList<DijksChromosome> initPopulation(List<Point> seedPath) {
        ArrayList<DijksChromosome> population = new ArrayList<>();
        int seedCount = (seedPath != null && !seedPath.isEmpty()) ? 2 : 0; 
        for (int i = 0; i < seedCount; i++) {
            DijksChromosome seed = new DijksChromosome(map.rows, map.cols);
            for (int r = 0; r < map.rows; r++) {
                for (int c = 0; c < map.cols; c++) {
                    seed.setGene(r, c, rand.nextDouble() * 0.2); 
                }
            }
            for (Point p : seedPath) seed.setGene(p.r, p.c, 0.8 + (rand.nextDouble() * 0.2)); 
            seed.fitness = DijksDecoder.calculateFitness(map, seed, false);
            population.add(seed);
            System.out.println(">> Injected Seed Chromosome! Fitness: " + seed.fitness);
        }

        for (int i = seedCount; i < popSize; i++) {
            DijksChromosome c = new DijksChromosome(map.rows, map.cols);
            c.randomInit(); 
            c.fitness = DijksDecoder.calculateFitness(map, c, false);
            population.add(c);
        }
        
        return population;
    }

    public ArrayList<DijksChromosome> evolve(ArrayList<DijksChromosome> population, boolean useHeuristic, int mutationMode) {
        ArrayList<DijksChromosome> newPopulation = new ArrayList<>();
        Collections.sort(population);

        for (int i = 0; i < elitismCount; i++) {
            newPopulation.add(population.get(i).clone());
        }

        while (newPopulation.size() < popSize) {
            DijksChromosome parent1 = tournamentSelection(population);
            DijksChromosome parent2 = tournamentSelection(population);

            DijksChromosome child;
            if (rand.nextDouble() < crossoverRate) {
                child = uniformCrossover(parent1, parent2);
            } else {
                child = parent1.clone();
            }

            child.mutate(mutationRate, mutationMode);

            if (child.fitness != -1) child.fitness = -1; 
            newPopulation.add(child);
        }

        newPopulation.parallelStream().forEach(child -> {
            if (child.fitness == -1) {
                child.fitness = DijksDecoder.calculateFitness(map, child, useHeuristic);
            }
        });

        return newPopulation;
    }

    public void setMutationRate(double newRate) {
        this.mutationRate = newRate;
    }

    private DijksChromosome tournamentSelection(ArrayList<DijksChromosome> pop) {
        int tournamentSize = 5;
        DijksChromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = rand.nextInt(pop.size());
            DijksChromosome candidate = pop.get(randomIndex);

            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private DijksChromosome uniformCrossover(DijksChromosome p1, DijksChromosome p2) {
        DijksChromosome child = new DijksChromosome(map.rows, map.cols);
        
        for (int i = 0; i < child.genes.length; i++) {
            if (rand.nextBoolean()) {
                child.genes[i] = p1.genes[i];
            } else {
                child.genes[i] = p2.genes[i];
            }
        }
        child.fitness = -1;
        return child;
    }
}