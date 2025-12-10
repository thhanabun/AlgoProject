import java.util.ArrayList;

public class MazeMain {
    public static void main(String[] args) {
       Mazereader mr = new Mazereader();
       ArrayList<ArrayList<Integer>> maze = mr.read("MAZE/m15_15.txt");
       for (ArrayList<Integer> arrayList : maze) {
            for (Integer i : arrayList) {
                System.out.print(i + " ");
            }        
            System.out.println();
       } 
    }
}
