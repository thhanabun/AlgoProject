package GA_StocasticDecoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import Struct.MazeMap;
import Struct.Point;

public class StocasticDecoder {
    
    public static double calculateFitness(MazeMap map, StocasticChromosome chromo, List<Point> path) {
        path.clear();
        Point start = map.start;
        Point goal = map.goal;

        int curR = start.r;
        int curC = start.c;

        path.add(new Point(curR, curC));
        boolean[][] isVisited = new boolean[map.rows][map.cols];
        isVisited[curR][curC] = true;

        int maxSteps = map.rows * map.cols * 10; 
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

                if (map.isValid(nr, nc) && !isVisited[nr][nc] 
                    && !StocasticGlobalKnowledge.isDeadEnd(nr, nc) 
                    && !chromo.isMyBlock(nr, nc)) {
                        
                    double p = chromo.getPriority(nr, nc);
                    if (p < 0.001) p = 0.001; 
                    
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
                if (path.size() > 1) {
                    int badR = curR;
                    int badC = curC;
                    
                    path.remove(path.size() - 1); 
                    Point prev = path.get(path.size() - 1);
                    curR = prev.r;
                    curC = prev.c;

                    int openExits = 0;
                    for (int[] d : dirs) {
                        int nr = badR + d[0];
                        int nc = badC + d[1];
                        if (map.isValid(nr, nc) && !StocasticGlobalKnowledge.isDeadEnd(nr, nc) && !(badR == map.start.r && badC == map.start.c)) { //fix start
                            openExits++;
                        }
                    }

                    if (openExits <= 1) {
                        StocasticGlobalKnowledge.markDeadEnd(badR, badC);
                    }
                    
                } else {
                    break; 
                }
            }
        }

        double distR = Math.abs(curR - goal.r);
        double distC = Math.abs(curC - goal.c);
        double manhattanDist = distR + distC;

        double basePenalty = map.rows * map.cols * 10;
        double distancePenalty = Math.pow(manhattanDist, 2) * 2.5; 

        if (path.isEmpty()) {
            return basePenalty * 2; 
        }
        return basePenalty + distancePenalty;
    }


    public static List<Point> getPath(MazeMap map, StocasticChromosome c, boolean b) {
        List<Point> p = new ArrayList<>();
        calculateFitness(map, c, p);
        return p;
    }

}