package MazeSolverAlgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import Struct.MazeMap;
import Struct.Point;

public class MyAlgorithm {

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

    public static List<Point> getDijkstraPath(MazeMap map) {
        int rows = map.rows; int cols = map.cols;
        double[][] bestDist = new double[rows][cols];
        for(int i=0; i<rows; i++) for(int j=0; j<cols; j++) bestDist[i][j] = Double.MAX_VALUE;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        Point start = map.start; Point goal = map.goal;

        pq.add(new Node(start.r, start.c, 0, 0,0, null));
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
                    pq.add(new Node(nr, nc, newCost, newCost, 0, current));
                }
            }
        }
        return new ArrayList<>();
    }
    
    private static double manhattan(Point a, Point b) {
        return Math.abs(a.r - b.r) + Math.abs(a.c - b.c);
    }

}