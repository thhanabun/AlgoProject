package GA_DijksDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import Struct.MazeMap;
import Struct.Point;

public class DijksDecoder {

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

    public static double calculateFitness(MazeMap map, DijksChromosome chromo, boolean useHeuristic) {
        int rows = map.rows; int cols = map.cols;
        double[][] bestVirtual = new double[rows][cols];
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) bestVirtual[i][j] = Double.MAX_VALUE;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = map.start; Point goal = map.goal;

        double startH = useHeuristic ? manhattan(start, goal) : 0;
        pq.add(new Node(start.r, start.c, 0, 0, startH, null));
        bestVirtual[start.r][start.c] = 0;

        int nodesExplored = 0;
        int maxNodes = rows * cols * 20;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            nodesExplored++;
            
            if (nodesExplored > maxNodes) return 10000 + (nodesExplored * 0.1);

            if (current.r == goal.r && current.c == goal.c) return current.realCost;
            
            if (current.gVirtual > bestVirtual[current.r][current.c]) continue;

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            for (int[] dir : dirs) {
                int nr = current.r + dir[0];
                int nc = current.c + dir[1];
                if (!map.isValid(nr, nc)) continue;

                int weight = map.getWeight(nr, nc);
                double priority = chromo.getPriority(nr, nc);
                if (priority < 0.0001) priority = 0.0001;
                
                double factor = Math.pow(priority, ALPHA); 
                double moveCostVirtual = weight / factor; 

                double newGVirtual = current.gVirtual + moveCostVirtual;
                double newRealCost = current.realCost + weight;

                if (newGVirtual < bestVirtual[nr][nc]) {
                    bestVirtual[nr][nc] = newGVirtual;
                    double newH = useHeuristic ? manhattan(new Point(nr, nc), goal) : 0;
                    pq.add(new Node(nr, nc, newGVirtual, newRealCost, newH, null));
                }
            }
        }
        return 10000 + (nodesExplored * 0.1);
    }

    public static List<Point> getPath(MazeMap map, DijksChromosome chromo, boolean useHeuristic) {
        int rows = map.rows; int cols = map.cols;
        double[][] bestVirtual = new double[rows][cols];
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) bestVirtual[i][j] = Double.MAX_VALUE;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = map.start; Point goal = map.goal;

        double startH = useHeuristic ? manhattan(start, goal) : 0;
        pq.add(new Node(start.r, start.c, 0, 0, startH, null));
        bestVirtual[start.r][start.c] = 0;

        int nodesExplored = 0;
        int maxNodes = rows * cols * 20;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            nodesExplored++;
            if (nodesExplored > maxNodes) break;

            if (current.r == goal.r && current.c == goal.c) return backtrack(current);
            if (current.gVirtual > bestVirtual[current.r][current.c]) continue;

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            for (int[] dir : dirs) {
                int nr = current.r + dir[0];
                int nc = current.c + dir[1];
                if (!map.isValid(nr, nc)) continue;

                int weight = map.getWeight(nr, nc);
                double priority = chromo.getPriority(nr, nc);
                if (priority < 0.0001) priority = 0.0001;

                double factor = Math.pow(priority, ALPHA);
                double moveCostVirtual = weight / factor; 

                double newGVirtual = current.gVirtual + moveCostVirtual;
                double newRealCost = current.realCost + weight;

                if (newGVirtual < bestVirtual[nr][nc]) {
                    bestVirtual[nr][nc] = newGVirtual;
                    double newH = useHeuristic ? manhattan(new Point(nr, nc), goal) : 0;
                    pq.add(new Node(nr, nc, newGVirtual, newRealCost, newH, current));
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
}