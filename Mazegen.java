
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class Mazegen {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("--- โปรแกรมสร้างเขาวงกต (Start/Goal = 0) ---");
        
        // 1. รับค่าขนาดจากผู้ใช้
        System.out.print("กรุณาระบุจำนวนแถว (Rows/Height): ");
        int rows = scanner.nextInt();
        
        System.out.print("กรุณาระบุจำนวนหลัก (Cols/Width): ");
        int cols = scanner.nextInt();
        
        // ตั้งชื่อไฟล์ตามขนาด
        String filename = "m" + rows + "_" + cols + ".txt";
        
        double wallDensity = 0.3; // กำแพง 30%

        try {
            System.out.println("กำลังสร้างข้อมูล... (ขนาด " + rows + "x" + cols + ")");
            generateSolvableMaze(filename, rows, cols, wallDensity);
            System.out.println("เสร็จสิ้น! สร้างไฟล์: " + filename);
        } catch (IOException e) {
            System.err.println("เกิดข้อผิดพลาด: " + e.getMessage());
        }
        
        scanner.close();
    }

    public static void generateSolvableMaze(String filename, int rows, int cols, double wallDensity) throws IOException {
        Random random = new Random();
        
        // ใช้ int[][] เก็บข้อมูล
        // -1 = Wall (#)
        // 0 = Path (สุ่มเลข 1-10)
        // -2 = Start (0)
        // -3 = Goal (0)
        int[][] mazeMap = new int[rows][cols];

        // 1. สุ่มข้อมูลลงตาราง
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 && c == 0) {
                    mazeMap[r][c] = -2; // ระบุว่าเป็นจุด Start
                } else if (r == rows - 1 && c == cols - 1) {
                    mazeMap[r][c] = -3; // ระบุว่าเป็นจุด Goal
                } else {
                    if (random.nextDouble() < wallDensity) {
                        mazeMap[r][c] = -1; // กำแพง
                    } else {
                        mazeMap[r][c] = 0; // ทางเดิน
                    }
                }
            }
        }
        //AlgoProject\Mazegen.java
        // 2. เจาะทางเดินบังคับ (Guaranteed Path)
        int currR = 0;
        int currC = 0;
        
        while (currR != rows - 1 || currC != cols - 1) {
            // ถ้าเจอจุดที่เป็นกำแพง ให้เปลี่ยนเป็นทางเดิน
            if (mazeMap[currR][currC] == -1) {
                mazeMap[currR][currC] = 0;
            }

            // เลือกเดิน ขวา หรือ ล่าง
            boolean moveDown;
            if (currR == rows - 1) moveDown = false;
            else if (currC == cols - 1) moveDown = true;
            else moveDown = random.nextBoolean();

            if (moveDown) currR++;
            else currC++;
        }
        // เคลียร์จุดก่อนถึง Goal ไม่ให้ตัน
        if (mazeMap[currR][currC] == -1) mazeMap[currR][currC] = -3;

        // 3. เขียนลงไฟล์
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            // ขอบบน
            for (int i = 0; i < cols * 4; i++) writer.write("#");
            writer.newLine();

            for (int r = 0; r < rows; r++) {
                writer.write("#"); // ขอบซ้าย
                
                for (int c = 0; c < cols; c++) {
                    int val = mazeMap[r][c];
                    
                    if (val == -2) {
                        // จุด Start ให้เป็น "0"
                        writer.write("\"0\"");
                    } else if (val == -3) {
                        // จุด Goal ให้เป็น "0"
                        writer.write("\"0\"");
                    } else if (val == -1) {
                        writer.write("#");
                    } else {
                        // ทางเดินปกติ สุ่มเลข 1-10
                        int weight = random.nextInt(10) + 1;
                        writer.write("\"" + weight + "\"");
                    }
                }
                
                writer.write("#"); // ขอบขวา
                writer.newLine();
            }

            // ขอบล่าง
            for (int i = 0; i < cols * 4; i++) writer.write("#");
            writer.newLine();
        }
    }
}

