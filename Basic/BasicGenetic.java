package Basic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;


public class BasicGenetic {
    public ArrayList<BasicChromosome> population;
    public int populationSize;
    public double mutationRate;
    public int geneLength;
    public MazeMap maze;

    private Random rand = new Random();

    public BasicGenetic(int populationSize, int geneLength, double mutationRate, MazeMap maze) {
        this.populationSize = populationSize;
        this.geneLength = geneLength;
        this.mutationRate = mutationRate;
        this.maze = maze;

        population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            population.add(new BasicChromosome(geneLength));
        }
    }
    
    public void evaluatePopulation() {
        for (BasicChromosome ch : population) {
            ch.evaluate(maze);
        }
    }

    public BasicChromosome selectParent() {
        // Tournament
        BasicChromosome a = population.get(rand.nextInt(populationSize));
        BasicChromosome b = population.get(rand.nextInt(populationSize));
        return (a.fitness > b.fitness) ? a : b;
    }

    public BasicChromosome crossover(BasicChromosome p1, BasicChromosome p2) {
        int point = rand.nextInt(geneLength);
        int[] childGenes = new int[geneLength];
        for (int i = 0; i < geneLength; i++) {
            childGenes[i] = (i < point) ? p1.genes[i] : p2.genes[i];
        }
        return new BasicChromosome(childGenes);
    }

    public void evolve() {
        ArrayList<BasicChromosome> newPop = new ArrayList<>();

        // keep the best
        BasicChromosome best = getBest();
        newPop.add(best.copy());

        while (newPop.size() < populationSize) {
            BasicChromosome p1 = selectParent();
            BasicChromosome p2 = selectParent();
            BasicChromosome child = crossover(p1, p2);
            child.mutate(mutationRate);
            newPop.add(child);
        }

        population = newPop;
    }

    public BasicChromosome getBest() {
        return Collections.max(population, Comparator.comparingInt(ch -> ch.fitness));
    }

}