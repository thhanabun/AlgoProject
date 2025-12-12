package DumbestGA;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Chromosome2 implements Comparable<Chromosome2> {
    public double[] genes;
    public double fitness = -1;
    public int rows, cols;
    private static final Random rand = new Random();
    public List<Point> path = new ArrayList<>();

    public static final int MUTATION_RANDOM = 0;
    public static final int MUTATION_FLIP   = 1;
    public static final int MUTATION_HYBRID = 2;
    
    public Chromosome2(int rows, int cols) {
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

    public void mutate(double mutationRate, List<Point> parentPath) {
        
        // 1. Standard Noise (สุ่มแก้เล็กๆ น้อยๆ ทั่วแมพ)
        // ลด Rate ลงหน่อย เพราะเราจะเน้น Block Mutation แทน
        for (int i = 0; i < genes.length; i++) {
            if (rand.nextDouble() < mutationRate * 0.1) { 
                genes[i] += (rand.nextDouble() - 0.5) * 0.1;
                if(genes[i] < 0) genes[i] = 0.001;
                if(genes[i] > 1) genes[i] = 1.0;
            }
        }

        // 2. *** BLOCK MUTATION (ระเบิดปูพรม) ***
        // เปลี่ยนค่าเป็นหย่อมๆ (Radius 3-5) เพื่อให้เกิดแรงดึงดูดใหม่
        int bombCount = 5; // ระเบิด 5 จุด
        for (int k = 0; k < bombCount; k++) {
            if (rand.nextDouble() < mutationRate) {
                int r = rand.nextInt(rows);
                int c = rand.nextInt(cols);
                applyBlockMutation(r, c, 3); // รัศมี 3
            }
        }

        // 3. *** PATH SABOTAGE (วางระเบิดเส้นทางเดิม) ***
        // บังคับให้เปลี่ยนใจจากเส้นทางที่พ่อแม่เคยเดิน
        if (parentPath != null && !parentPath.isEmpty()) {
            int sabotageCount = 3; // แกล้ง 3 จุดบนเส้นทางเดิม
            for (int k = 0; k < sabotageCount; k++) {
                if (rand.nextDouble() < 0.5) { // โอกาส 50% ที่จะแกล้ง
                    Point target = parentPath.get(rand.nextInt(parentPath.size()));
                    applyBlockMutation(target.r, target.c, 4); // รัศมี 4 (ใหญ่หน่อยจะได้เลี้ยวหนี)
                }
            }
        }

        if (parentPath != null && parentPath.size() > 10) {
            if (rand.nextDouble() < 0.3) { // โอกาส 30%
                // สุ่มจุดเริ่มต้น A และจุดจบ B บนเส้นทาง
                int idx1 = rand.nextInt(parentPath.size() - 5);
                int idx2 = idx1 + 5 + rand.nextInt(parentPath.size() - idx1 - 5); // ต้องห่างกันอย่างน้อย 5 ก้าว
                
                Point p1 = parentPath.get(idx1);
                Point p2 = parentPath.get(idx2);
                
                // ลากเส้น Priority สูง เชื่อมระหว่าง p1 และ p2 (Bresenham's Line Algorithm แบบง่าย)
                applyLineMutation(p1.r, p1.c, p2.r, p2.c);
            }
        }
        
        this.fitness = -1; // Reset Fitness
    }

    // Helper: เปลี่ยนค่าเป็นหย่อม (วงกลม)
    private void applyBlockMutation(int centerR, int centerC, int radius) {
        double newVal = rand.nextDouble(); // ค่า Priority ใหม่ของหย่อมนี้
        
        for (int r = centerR - radius; r <= centerR + radius; r++) {
            for (int c = centerC - radius; c <= centerC + radius; c++) {
                // เช็คขอบเขต
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    // เช็คว่าเป็นวงกลม
                    if (Math.sqrt(Math.pow(r - centerR, 2) + Math.pow(c - centerC, 2)) <= radius) {
                        // เปลี่ยนค่า gene (ผสมของเดิม 20% ของใหม่ 80%)
                        int idx = r * cols + c;
                        genes[idx] = (genes[idx] * 0.2) + (newVal * 0.8);
                    }
                }
            }
        }
    }

    private void applyLineMutation(int r1, int c1, int r2, int c2) {
        int dr = Math.abs(r2 - r1);
        int dc = Math.abs(c2 - c1);
        int sx = (r1 < r2) ? 1 : -1;
        int sy = (c1 < c2) ? 1 : -1;
        int err = dr - dc;

        while (true) {
            if (r1 >= 0 && r1 < rows && c1 >= 0 && c1 < cols) {
                // ใส่ Priority สูงๆ ให้เส้นนี้ เพื่อล่อให้ตัวเดินเดินตัดตรง
                genes[r1 * cols + c1] = 0.95; 
            }

            if (r1 == r2 && c1 == c2) break;
            int e2 = 2 * err;
            if (e2 > -dc) { err -= dc; r1 += sx; }
            if (e2 < dr) { err += dr; c1 += sy; }
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

    public Chromosome2 clone() {
        Chromosome2 c = new Chromosome2(rows, cols);
        System.arraycopy(this.genes, 0, c.genes, 0, genes.length);
        c.fitness = this.fitness;
        return c;
    }

    @Override
    public int compareTo(Chromosome2 other) {
        return Double.compare(this.fitness, other.fitness);
    }
}