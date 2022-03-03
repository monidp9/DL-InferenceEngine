package unina.view;

import java.io.File;
import java.io.IOException;

import unina.IOParser;

import java.awt.Desktop;


public class View {

    private ConceptPanel conceptPanel = null; 
    private LoadingPanel loadingPanel = null; 

    public void openConceptReadingView(IOParser io) {
        conceptPanel = new ConceptPanel(io);
        loadingPanel = new LoadingPanel();
    }

    public void openGraphView() {
        loadingPanel.closePanel();

        try {
            Desktop.getDesktop().open(new File("result/tableau_graph.png"));
        } catch(IOException e) {
            e.printStackTrace();
        }
    
        System.exit(1);
    }

    public void showError(String msg) {
        conceptPanel.showMsgError(msg);
        System.exit(1);
    }

}
