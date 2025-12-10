import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {
    // Hyperparameters (ค่าที่ปรับจูนได้)
    private int popSize;         // จำนวนประชากร (เช่น 100)
    private double mutationRate; // โอกาสกลายพันธุ์ (เช่น 0.05)
    private double crossoverRate;// โอกาสผสมพันธุ์ (เช่น 0.9)
    private int elitismCount;    // จำนวนตัวเทพที่จะเก็บไว้เสมอ (กันคำตอบหาย)

    private MazeMap map;
    private Random rand = new Random();

    public GeneticAlgorithm(MazeMap map, int popSize, double mutationRate, double crossoverRate, int elitismCount) {
        this.map = map;
        this.popSize = popSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.elitismCount = elitismCount;
    }

    public ArrayList<Chromosome> initPopulation(List<Point> seedPath) {
        ArrayList<Chromosome> population = new ArrayList<>();
    
        int seedCount = (seedPath != null && !seedPath.isEmpty()) ? 2 : 0; 

        for (int i = 0; i < seedCount; i++) {
            Chromosome seed = new Chromosome(map.rows, map.cols);
            
            for (int r = 0; r < map.rows; r++) {
                for (int c = 0; c < map.cols; c++) {
                    seed.setGene(r, c, rand.nextDouble() * 0.2); 
                }
            }
            
            for (Point p : seedPath) {
                seed.setGene(p.r, p.c, 0.8 + (rand.nextDouble() * 0.2)); 
            }
            
            seed.fitness = DumbDecoder.calculateFitness(map, seed, false);
            population.add(seed);
            System.out.println(">> Injected Seed Chromosome! Fitness: " + seed.fitness);
        }

        for (int i = seedCount; i < popSize; i++) {
            Chromosome c = new Chromosome(map.rows, map.cols);
            c.randomInit(); 
            c.fitness = DumbDecoder.calculateFitness(map, c, false);
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

    public ArrayList<Chromosome> evolve(ArrayList<Chromosome> population, boolean useHeuristic) {
        ArrayList<Chromosome> newPopulation = new ArrayList<>();

        // 1. Elitism: เก็บตัวเทพไว้เหมือนเดิม
        Collections.sort(population);
        for (int i = 0; i < elitismCount; i++) {
            newPopulation.add(population.get(i).clone());
        }

        // 2. Generation Loop: สร้างประชากรให้ครบจำนวน (แต่ยังไม่คำนวณ Fitness!)
        while (newPopulation.size() < popSize) {
            Chromosome parent1 = tournamentSelection(population);
            Chromosome parent2 = tournamentSelection(population);

            Chromosome child;
            if (rand.nextDouble() < crossoverRate) {
                child = uniformCrossover(parent1, parent2);
            } else {
                child = parent1.clone();
            }

            child.mutate(mutationRate);

            // *** สำคัญ: บังคับให้เป็น -1 ไว้ก่อน เพื่อรอเข้าคิวคำนวณพร้อมกัน ***
            // (ถ้า clone มาแล้วไม่ mutate ค่า fitness เก่าจะติดมา เราต้องล้างออกถ้าต้องการคำนวณใหม่
            // แต่ถ้ามั่นใจว่า Logic เดิมดีแล้ว ก็ปล่อยไว้ได้ แต่เพื่อความชัวร์ Reset ดีกว่าถ้ามีการเปลี่ยน Gen)
            if (child.fitness != -1) {
                child.fitness = -1; 
            }

            newPopulation.add(child);
        }

        // -----------------------------------------------------------
        // 3. Parallel Execution: พระเอกขี่ม้าขาวอยู่ตรงนี้! 🚀
        // -----------------------------------------------------------
        // ใช้ parallelStream() สั่งให้ Java แบ่งงานให้ CPU ทุก Core คำนวณพร้อมกัน
        newPopulation.parallelStream().forEach(child -> {
            // คำนวณเฉพาะตัวที่ยังไม่มีคะแนน (Fitness = -1)
            if (child.fitness == -1) {
                // *** ตรงนี้แก้ชื่อ Class ให้ตรงกับที่คุณใช้นะครับ (DumbDecoder หรือ PathDecoder) ***
                child.fitness = DumbDecoder.calculateFitness(map, child, useHeuristic);
            }
        });

        return newPopulation;
    }

    public void setMutationRate(double newRate) {
        this.mutationRate = newRate;
    }

    private Chromosome tournamentSelection(ArrayList<Chromosome> pop) {
        int tournamentSize = 5;
        Chromosome best = null;
        
        for (int i = 0; i < tournamentSize; i++) {
            int randomIndex = rand.nextInt(pop.size());
            Chromosome candidate = pop.get(randomIndex);
            
            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private Chromosome uniformCrossover(Chromosome p1, Chromosome p2) {
        Chromosome child = new Chromosome(map.rows, map.cols);
        
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