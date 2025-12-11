import java.util.Random;

public class Chromosome implements Comparable<Chromosome> {
    public double[] genes;
    public double fitness = -1;
    public int rows, cols;
    private static final Random rand = new Random();

    public static final int MUTATION_RANDOM = 0;
    public static final int MUTATION_FLIP   = 1;
    public static final int MUTATION_HYBRID = 2;
    
    public Chromosome(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.genes = new double[rows * cols];
    }

    public void randomInit() {
        for (int i = 0; i < genes.length; i++) {
            genes[i] = rand.nextDouble();
        }
    }

    public double getPriority(int r, int c) {
        return genes[r * cols + c];
    }

    public void mutate(double mutationRate, int mode) {
        for (int i = 0; i < genes.length; i++) {
            if (rand.nextDouble() < mutationRate) {   
                switch (mode) {
                    case MUTATION_RANDOM:
                        genes[i] = rand.nextDouble();
                        break;
                    case MUTATION_FLIP:
                        if (genes[i] > 0.5) {
                            genes[i] = rand.nextDouble() * 0.2;
                        } else {
                            genes[i] = 0.8 + (rand.nextDouble() * 0.2);
                        }
                        break;
                    case MUTATION_HYBRID:
                        if (rand.nextDouble() < 0.3) { 
                            if (genes[i] > 0.5) {
                                genes[i] = rand.nextDouble() * 0.2;
                            } else {
                                genes[i] = 0.8 + (rand.nextDouble() * 0.2);
                            }
                        } else {
                            double change = (rand.nextDouble() - 0.5) * 0.2;
                            genes[i] += change;
                            if (genes[i] < 0.0001) genes[i] = 0.0001;
                            if (genes[i] > 1.0) genes[i] = 1.0;
                        }
                        break;
                }
                
                this.fitness = -1;
            }
        }
    }

    public void setGene(int r, int c, double value) {
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            genes[r * cols + c] = value;
        }
    }
    
    public void setPriority(int r, int c, double val) {
        if (r >= 0 && r < rows && c >= 0 && c < cols) {
            genes[r * cols + c] = val;
        }
    }

    public Chromosome clone() {
        Chromosome c = new Chromosome(rows, cols);
        System.arraycopy(this.genes, 0, c.genes, 0, genes.length);
        c.fitness = this.fitness;
        return c;
    }

    @Override
    public int compareTo(Chromosome other) {
        return Double.compare(this.fitness, other.fitness);
    }
}