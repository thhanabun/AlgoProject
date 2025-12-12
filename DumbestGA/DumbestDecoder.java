package DumbestGA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class DumbestDecoder {

    public static double ALPHA = 4; 
    private static class Node implements Comparable<Node> {
        int r, c;
        double gVirtual;
        double realCost; 
        double h;        
        Node parent;

        public Node(int r, int c, double gVirtual, double realCost, double h, Node parent) {
            this.r = r; this.c = c;
            this.gVirtual = gVirtual;
            this.realCost = realCost;
            this.h = h;
            this.parent = parent;
        }

        public double getF() { return gVirtual + h; }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.getF(), other.getF());
        }
    }
    
    public static double calculateFitness(MazeMap map, Chromosome2 chromo, List<Point> path) {
        path.clear();
        Point start = map.start;
        Point goal = map.goal;

        int curR = start.r;
        int curC = start.c;
        
        path.add(new Point(curR, curC));
        boolean[][] isVisited = new boolean[map.rows][map.cols];
        isVisited[curR][curC] = true;

        int maxSteps = map.rows * map.cols * 2; 
        Random deterministicRand = new Random(chromo.hashCode());

        for (int step = 0; step < maxSteps; step++) {
            
            if (curR == goal.r && curC == goal.c) {
                double totalCost = 0;
                for (Point p : path) totalCost += map.getWeight(p.r, p.c);
                return totalCost;
            }

            List<Point> validMoves = new ArrayList<>();
            List<Double> probs = new ArrayList<>();
            double sumPriority = 0;

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}}; 

            for (int[] d : dirs) {
                int nr = curR + d[0];
                int nc = curC + d[1];

                // เช็ค: เดินได้ + ไม่เคยมา + ไม่ใช่ Global Dead End + ไม่ใช่ Junction Block ส่วนตัว
                if (map.isValid(nr, nc) && !isVisited[nr][nc] 
                    && !GlobalKnowledge.isDeadEnd(nr, nc) 
                    && !chromo.isMyBlock(nr, nc)) {
                        
                    double p = chromo.getPriority(nr, nc);
                    if (p < 0.001) p = 0.001; 
                    p = Math.pow(p, 3); 
                    
                    validMoves.add(new Point(nr, nc));
                    probs.add(p);
                    sumPriority += p;
                }
            }

            if (!validMoves.isEmpty()) {
                double randVal = deterministicRand.nextDouble() * sumPriority;
                double runningSum = 0;
                int selectedIdx = validMoves.size() - 1;
                for (int i = 0; i < validMoves.size(); i++) {
                    runningSum += probs.get(i);
                    if (randVal <= runningSum) { selectedIdx = i; break; }
                }
                Point nextMove = validMoves.get(selectedIdx);
                curR = nextMove.r;
                curC = nextMove.c;
                isVisited[curR][curC] = true;
                path.add(new Point(curR, curC));
            } else {
                // --- BACKTRACKING ---
                if (path.size() > 1) {
                    int badR = curR;
                    int badC = curC;
                    
                    path.remove(path.size() - 1); 
                    Point prev = path.get(path.size() - 1);
                    curR = prev.r;
                    curC = prev.c;
                    
                    // --- GLOBAL LEARNING ---
                    int openExits = 0;
                    for (int[] d : dirs) {
                        int nr = badR + d[0];
                        int nc = badC + d[1];
                        // นับทางออกที่ไม่ใช่กำแพง และไม่ใช่ Global Dead End
                        if (map.isValid(nr, nc) && !GlobalKnowledge.isDeadEnd(nr, nc)) {
                            openExits++;
                        }
                    }

                    // ถ้ามีทางออก <= 1 (คือทางที่ถอยมา) = ตันจริง -> บอกโลกรู้เลย!
                    if (openExits <= 1) {
                        GlobalKnowledge.markDeadEnd(badR, badC);
                    }
                    
                } else {
                    break; 
                }
            }
        }

        double distToGoal = Math.sqrt(Math.pow(curR - goal.r, 2) + Math.pow(curC - goal.c, 2));
        return 5000000.0 + (distToGoal * 1000.0); 
    }


    public static List<Point> getPath(MazeMap map, Chromosome2 c, boolean b) {
        List<Point> p = new ArrayList<>();
        calculateFitness(map, c, p);
        return p;
    }

    public static List<Point> getGreedyPath(MazeMap map) {
        int rows = map.rows; int cols = map.cols;
        boolean[][] visited = new boolean[rows][cols];
        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = map.start; Point goal = map.goal;

        pq.add(new Node(start.r, start.c, 0, 0, manhattan(start, goal), null));
        visited[start.r][start.c] = true;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.r == goal.r && current.c == goal.c) return backtrack(current);

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            for (int[] dir : dirs) {
                int nr = current.r + dir[0];
                int nc = current.c + dir[1];
                if (!map.isValid(nr, nc) || visited[nr][nc]) continue;

                int weight = map.getWeight(nr, nc);
                visited[nr][nc] = true;
                pq.add(new Node(nr, nc, 0, current.realCost + weight, manhattan(new Point(nr, nc), goal), current));
            }
        }
        return new ArrayList<>();
    }

    public static double runPureAStar(MazeMap map) {
        int rows = map.rows; int cols = map.cols;
        double[][] bestDist = new double[rows][cols];
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) bestDist[i][j] = Double.MAX_VALUE;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = map.start; Point goal = map.goal;

        pq.add(new Node(start.r, start.c, 0, 0, manhattan(start, goal), null));
        bestDist[start.r][start.c] = 0;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.r == goal.r && current.c == goal.c) return current.realCost;
            if (current.realCost > bestDist[current.r][current.c]) continue;

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            for (int[] dir : dirs) {
                int nr = current.r + dir[0];
                int nc = current.c + dir[1];
                if (!map.isValid(nr, nc)) continue;
                
                double newCost = current.realCost + map.getWeight(nr, nc);
                if (newCost < bestDist[nr][nc]) {
                    bestDist[nr][nc] = newCost;
                    pq.add(new Node(nr, nc, newCost, newCost, manhattan(new Point(nr, nc), goal), null));
                }
            }
        }
        return -1;
    }
    
    public static List<Point> getPureAStarPath(MazeMap map) {
        int rows = map.rows; int cols = map.cols;
        double[][] bestDist = new double[rows][cols];
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) bestDist[i][j] = Double.MAX_VALUE;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = map.start; Point goal = map.goal;

        pq.add(new Node(start.r, start.c, 0, 0, manhattan(start, goal), null));
        bestDist[start.r][start.c] = 0;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (current.r == goal.r && current.c == goal.c) return backtrack(current);
            if (current.realCost > bestDist[current.r][current.c]) continue;

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            for (int[] dir : dirs) {
                int nr = current.r + dir[0];
                int nc = current.c + dir[1];

                if (!map.isValid(nr, nc)) continue;
                double newCost = current.realCost + map.getWeight(nr, nc);
                if (newCost < bestDist[nr][nc]) {
                    bestDist[nr][nc] = newCost;
                    pq.add(new Node(nr, nc, newCost, newCost, manhattan(new Point(nr, nc), goal), current));
                }
            }
        }
        return new ArrayList<>();
    }

    private static List<Point> backtrack(Node endNode) {
        List<Point> path = new ArrayList<>();
        Node curr = endNode;
        while (curr != null) { path.add(new Point(curr.r, curr.c)); curr = curr.parent; }
        Collections.reverse(path);
        return path;
    }

    private static double manhattan(Point a, Point b) {
        return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
    }

    public static void smoothPath(List<Point> path) {
        if (path.size() < 3) return;

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < path.size() - 2; i++) {
                Point p1 = path.get(i);
                Point p3 = path.get(i + 2);

                // เช็คว่า p1 กับ p3 อยู่ติดกันไหม (บน-ล่าง-ซ้าย-ขวา)
                // ถ้าติดกัน แสดงว่า p2 (ตัวกลาง) คือส่วนเกินที่เดินอ้อม -> ลบทิ้ง
                if (Math.abs(p1.r - p3.r) + Math.abs(p1.c - p3.c) == 1) {
                    path.remove(i + 1);
                    changed = true;
                    i--; // ถอย index กลับมาเช็คใหม่
                }
            }
        }
    }
}