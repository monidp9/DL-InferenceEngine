package unina.view;

import javax.swing.JFrame;

import unina.IOParser;


public class View{

    private ConceptPanel conceptPanel = null; 
    private LoadingPanel loadingPanel = null; 

    public View(){}


    public void openConceptReadingView(IOParser io) {
        conceptPanel = new ConceptPanel(io);
        loadingPanel = new LoadingPanel();
    }

    public void openGraphView(){
        loadingPanel.closePanel();
        GraphFrame graphFrame = new GraphFrame();
    }

    public void showError(String msg){
        conceptPanel.showMsgError(msg);
        System.exit(1);
    }

}
