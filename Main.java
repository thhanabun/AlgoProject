import javax.swing.SwingUtilities;

import gui.CombinedGUI;

public class Main {
    public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> new CombinedGUI());
    }
}
