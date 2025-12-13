package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import Struct.MazeMap;
import Struct.Point;

public class MazeRenderPanel extends JPanel {
        private BufferedImage baseImage;  // Stores just the walls (Static at 1:1)
        private BufferedImage viewImage;  // Stores walls + paths (Dynamic at 1:1)
        private int cols, rows;
        private double zoomFactor = 1.0;  // 1.0 = 1 pixel per cell

        public MazeRenderPanel(int cols, int rows) {
            this.cols = cols;
            this.rows = rows;
            
            // Initial preferred size (1:1 scale)
            setPreferredSize(new Dimension(cols, rows));
            
            // Initialize buffers at native resolution (1 pixel = 1 cell)
            baseImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
            viewImage = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        }

        // --- ZOOM LOGIC ---
        
        public void setZoom(double z) {
            this.zoomFactor = Math.max(1.0, Math.min(50.0, z)); // Clamp zoom
            updateSize();
        }

        public void zoomIn(double factor) {
            setZoom(zoomFactor * factor);
        }

        public void zoomOut(double factor) {
            setZoom(zoomFactor / factor);
        }

        private void updateSize() {
            int newW = (int) Math.ceil(cols * zoomFactor);
            int newH = (int) Math.ceil(rows * zoomFactor);
            setPreferredSize(new Dimension(newW, newH));
            revalidate(); // Tell ScrollPane to update scrollbars
            repaint();
        }

        // --- RENDER LOGIC ---

        // Draw the walls once (1000x1000 pixels max)
        public void renderBaseMap(MazeMap map) {
            // Speed Trick: Iterate pixels directly
             for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int val = map.getWeight(r, c);
                    int color = 0xFFFFFFFF; // White (Path)
                    if (val == -1) color = 0xFF000000; // Black (Wall)
                    else if (val == -2) color = 0xFFFF0000; // Red
                    else if (val == 0) color = 0xFF00FF00; // Green
                    baseImage.setRGB(c, r, color);
                }
            }
            resetToBase(); 
        }

        public void resetToBase() {
            Graphics2D g = viewImage.createGraphics();
            g.drawImage(baseImage, 0, 0, null);
            g.dispose();
        }

        // Draw a path on top of the existing view (Pixel by Pixel)
        public void overlayPath(List<Point> path, Color c) {
            if (path == null) return;
            
            int rgb = c.getRGB();
            for (Point p : path) {
                if (p.c >= 0 && p.c < cols && p.r >= 0 && p.r < rows) {
                    viewImage.setRGB(p.c, p.r, rgb);
                }
            }
        }
        
        public void overlayPoints(List<Point> points, Color c) {
        if (points == null || points.isEmpty()) return;
        
        int rgb = c.getRGB();
        
        for (Point p : points) {
            // Verify bounds to prevent crashes
            if (p.c >= 0 && p.c < cols && p.r >= 0 && p.r < rows) {
                // Set the pixel color directly. 
                // paintComponent will handle scaling this up to the zoom factor.
                viewImage.setRGB(p.c, p.r, rgb);
            }
        }
    }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            
            // OPTIMIZATION: Use Nearest Neighbor to keep pixels sharp when zooming in
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            
            // Draw the 1:1 image SCALED up to the current zoom level
            g2.scale(zoomFactor, zoomFactor);
            g2.drawImage(viewImage, 0, 0, null);
        }
    }