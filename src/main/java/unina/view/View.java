package unina.view;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import unina.IOParser;

import java.awt.Desktop;


public class View extends JFrame{

    private LoadingPanel loadingPanel = null;

    public View(){
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void openConceptReadingView(IOParser io) {
        setContentPane(new ConceptPanel(io));
        this.loadingPanel = new LoadingPanel();

    }

    public void openGraphView() {
        loadingPanel.closePanel();
        try {
            Desktop.getDesktop().open(new File("result/tableau_graph.png"));
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
