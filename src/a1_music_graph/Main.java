/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package a1_music_graph;

/**
 *
 * @author kenschiller
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Panel p = new Panel();
        p.writeImage("output1.png");
        p.setLeftMargin(1);
        p.writeImage("output2.png");
        p.setLeftMargin(2);
        p.writeImage("output3.png");
        p.setLeftMargin(3);
        p.writeImage("output4.png");
    }

}
