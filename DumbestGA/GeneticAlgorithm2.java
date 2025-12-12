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
    }

    public ArrayList<Chromosome2> initPopulation(List<Point> seedPath) {
        ArrayList<Chromosome2> population = new ArrayList<>();
    
        int seedCount = (seedPath != null && !seedPath.isEmpty()) ? 2 : 0; 

        for (int i = 0; i < seedCount; i++) {
            Chromosome2 seed = new Chromosome2(map.rows, map.cols);
            
            for (int r = 0; r < map.rows; r++) {
                for (int c = 0; c < map.cols; c++) {
                    seed.setGene(r, c, rand.nextDouble() * 0.2); 
                }
            }
            
            for (Point p : seedPath) {
                seed.setGene(p.r, p.c, 0.8 + (rand.nextDouble() * 0.2)); 
            }
            List<Point> path = new ArrayList<>();
            seed.fitness = DumbestDecoder.calculateFitness(map, seed, path);
            population.add(seed);
            System.out.println(">> Injected Seed Chromosome! Fitness: " + seed.fitness);
        }

        List<Point> path = new ArrayList<>();
        for (int i = seedCount; i < popSize; i++) {
            Chromosome2 c = new Chromosome2(map.rows, map.cols);
            c.randomInit(); 
            c.fitness = DumbestDecoder.calculateFitness(map, c, path);
            population.add(c);
        }
        
        return population;
    }

    // public ArrayList<Chromosome> evolve(ArrayList<Chromosome> population) {
    //     ArrayList<Chromosome> newPopulation = new ArrayList<>();

    //     Collections.sort(population);

    //     for (int i = 0; i < elitismCount; i++) {
    //         newPopulation.add(population.get(i).clone());
    //     }

    //     while (newPopulation.size() < popSize) {

    //         Chromosome parent1 = tournamentSelection(population);
    //         Chromosome parent2 = tournamentSelection(population);


    //         Chromosome child;
    //         if (rand.nextDouble() < crossoverRate) {
    //             child = uniformCrossover(parent1, parent2);
    //         } else {
    //             child = parent1.clone(); 
    //         }

    //         child.mutate(mutationRate);

    //         if (child.fitness == -1) {
    //             child.fitness = DumbDecoder.calculateFitness(map, child);
    //         }
        
    //         newPopulation.add(child);
    //     }

    //     return newPopulation;
    // }

    public ArrayList<Chromosome2> evolve(ArrayList<Chromosome2> population, boolean useHeuristic, int mutationMode) {
        ArrayList<Chromosome2> newPopulation = new ArrayList<>();
        Collections.sort(population);
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.add(population.get(i).clone());
        }

        int oldsz = population.size();
        int newcnt = (oldsz*20)/100;

        while (newPopulation.size() < popSize-newcnt) {
            Chromosome2 parent1 = tournamentSelection(population);
            Chromosome2 parent2 = tournamentSelection(population);

            Chromosome2 child;
            if (rand.nextDouble() < crossoverRate) {
                child = uniformCrossover(parent1, parent2);
            } else {
                child = parent1.clone();
            }

            child.mutate(mutationRate,parent1.path);

            if (child.fitness != -1) {
                child.fitness = -1; 
            }

            newPopulation.add(child);
        }

        int freshBloodCount = population.size() - newPopulation.size();
        for (int i = 0; i < freshBloodCount; i++) {
            Chromosome2 immigrant = new Chromosome2(map.rows, map.cols); 
            newPopulation.add(immigrant);
        }

        newPopulation.parallelStream().forEach(child -> {
            child.path = new ArrayList<>();
            if (child.fitness == -1) {
                child.fitness = DumbestDecoder.calculateFitness(map, child,child.path);
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
            int randomIndex = rand.nextInt(pop.size());
            Chromosome2 candidate = pop.get(randomIndex);

            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private Chromosome2 uniformCrossover(Chromosome2 p1, Chromosome2 p2) {
        Chromosome2 child = new Chromosome2(map.rows, map.cols);
        
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