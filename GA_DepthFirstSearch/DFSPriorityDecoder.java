package GA_DepthFirstSearch;

import java.util.ArrayList;
import java.util.List;

import Struct.MazeMap;
import Struct.Point;

public class DFSPriorityDecoder {
    public static double calculateFitness(MazeMap map, DFSChromosome chromo, List<Point> path) {
        path.clear();
        Point start = map.start;
        Point goal = map.goal;

        int curR = start.r;
        int curC = start.c;
        
        path.add(new Point(curR, curC));
        boolean[][] isVisited = new boolean[map.rows][map.cols];
        isVisited[curR][curC] = true;

        int maxSteps = map.rows * map.cols * 10; 

        for (int step = 0; step < maxSteps; step++) {
            
            if (curR == goal.r && curC == goal.c) {
                double totalCost = 0;
                for (Point p : path) totalCost += map.getWeight(p.r, p.c);
                return totalCost;
            }

            List<Point> validMoves = new ArrayList<>();
            List<Double> priorities = new ArrayList<>();
            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}}; 

            for (int[] d : dirs) {
                int nr = curR + d[0];
                int nc = curC + d[1];

                if (map.isValid(nr, nc) && !isVisited[nr][nc] 
                    && !DFSGlobalKnowledge.isDeadEnd(nr, nc) 
                    && !chromo.isMyBlock(nr, nc)) {
                        
                    double p = chromo.getPriority(nr, nc); 
                    
                    validMoves.add(new Point(nr, nc));
                    priorities.add(p);
                }
            }

            if (!validMoves.isEmpty()) {
                double maxPriority = -Double.MAX_VALUE;
                int selectedIdx = -1;
                for (int i = 0; i < validMoves.size(); i++) {
                    if (priorities.get(i) > maxPriority) {
                        maxPriority = priorities.get(i);
                        selectedIdx = i;
                    }
                }
                Point nextMove = validMoves.get(selectedIdx);
                curR = nextMove.r;
                curC = nextMove.c;
                isVisited[curR][curC] = true;
                path.add(new Point(curR, curC));
                
            } else {
            // Backtracking Logic
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

                    if (map.isValid(nr, nc) && !DFSGlobalKnowledge.isDeadEnd(nr, nc)) { 
                        openExits++;
                    }
                }

                boolean isStart = (badR == map.start.r && badC == map.start.c);

                if (openExits <= 1 && !isStart) {
                    DFSGlobalKnowledge.markDeadEnd(badR, badC);
                }
                
            } else {
                break; // Path empty, cannot backtrack
            }
        }
        }

        double distR = Math.abs(curR - goal.r);
        double distC = Math.abs(curC - goal.c);
        double manhattanDist = distR + distC;

        double basePenalty = 100000.0;
        double distancePenalty = Math.pow(manhattanDist, 2) * 2.5; 

        if (path.isEmpty()) {
            return basePenalty * 2; 
        }
        return basePenalty + distancePenalty;

    }


    public static List<Point> getPath(MazeMap map, DFSChromosome c, boolean b) {
        List<Point> p = new ArrayList<>();
        calculateFitness(map, c, p);
        return p;
    }

}