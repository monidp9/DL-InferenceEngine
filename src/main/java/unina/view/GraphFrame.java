package unina.view;

import javax.swing.JFrame;

public class GraphFrame extends JFrame {

    public GraphFrame(){
        this.setVisible(true);
        this.setTitle("Tableau's graph");
        this.setSize(1200, 1200);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        GraphPanel graphPanel = new GraphPanel();
        this.setContentPane(graphPanel);   
    }
    
}
