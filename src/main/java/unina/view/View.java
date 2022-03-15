package unina.view;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import unina.IOParser;

import java.awt.Desktop;


public class View extends JFrame{

    private ConceptPanel conceptPanel = null; 
    private LoadingPanel loadingPanel = null; 

    public View(){
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public void openConceptReadingView(IOParser io) {
        conceptPanel = new ConceptPanel(io);
        setContentPane(this.conceptPanel);
        conceptPanel.createPanel();
        
        loadingPanel = new LoadingPanel();
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

    public void showError(String msg) {
        conceptPanel.showMsgError(msg);
    }

}
