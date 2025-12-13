package gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import Struct.MazeMap;
import Struct.Point;

public class AlgorithmStatusPanel extends JPanel {
        private JLabel dataLabel;
        
        public AlgorithmStatusPanel(String title) {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)
            ));
            dataLabel = new JLabel("<html>Waiting...</html>");
            dataLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            dataLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(dataLabel, BorderLayout.CENTER);
        }

        public void updateStats(List<Point> path, MazeMap map) {
        if (path == null || path.isEmpty()) {
            dataLabel.setText("No Path Found");
            return;
        }
        int totalCost = 0;
        for (Point p : path) {
            if (p.r >= 0 && p.r < map.rows && p.c >= 0 && p.c < map.cols) {
                int w = map.getWeight(p.r, p.c); 
                if (w > 0) totalCost += w;
            }
        
        }
        dataLabel.setText(String.format("<html><b>Steps:</b> %d<br><b>Cost:</b> %d</html>", path.size(), totalCost));
    }

        public void updateStatsLive(int gen, int maxGen, List<Point> path, MazeMap map, double fitness, String status) {
        int totalCost = 0;
        if (path != null) {
            for (Point p : path) {
                if (p.r >= 0 && p.r < map.rows && p.c >= 0 && p.c < map.cols) {
                    int w = map.getWeight(p.r, p.c);
                    if (w > 0) totalCost += w;
                }
            }
        }
        
        String html = String.format(
            "<html>Gen: %d / %d<br>" +
            "Fitness: %.2f<br>" +
            "Cost: %d<br>" +
            "<font color='blue'>%s</font></html>", 
            gen, maxGen, fitness, totalCost, status
        );
        dataLabel.setText(html);
    }
        
        public void setLoading(String msg) {
            dataLabel.setText("<html>" + msg + "</html>");
        }
    }