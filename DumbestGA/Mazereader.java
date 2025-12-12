package DumbestGA;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Mazereader {
    ArrayList<ArrayList<Integer>> maze;
    public Mazereader(){
        maze = new ArrayList<>();
    }
    
    public ArrayList<ArrayList<Integer>> read(String filePath){
        try{
            File file = new File(filePath);
            Scanner sc = new Scanner(file);
            int rowCnt = 0;
            while(sc.hasNextLine()){
                String line = sc.nextLine();
                line = line.replace("\"\"", "\"");
                String [] a = line.split("\"");
                maze.add(new ArrayList<>());    
                for (String str : a) {
                    // System.out.println(str.charAt(0));
                    if(str.charAt(0) != '#' && str.charAt(0) != 'S' && str.charAt(0) != 'G'){
                        int num = Integer.parseInt(str);
                        maze.get(rowCnt).add(num);
                    }
                    else{
                        for(int i=0 ;i <str.length() ;i++){
                            char c = str.charAt(i);
                            if(c == '#'){
                                maze.get(rowCnt).add(-1);
                            }
                            else if(c == 'S' && c=='G'){
                                maze.get(rowCnt).add(0);
                            }else{
                                maze.get(rowCnt).add(0);
                            }
                        }
                    }
                }
                rowCnt++;
            }
            sc.close();
            return maze;
        }catch(FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            return null;
        }
    } 
}
