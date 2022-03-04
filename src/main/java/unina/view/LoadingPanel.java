package unina.view;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

public class LoadingPanel extends JPanel {

    Thread t;

    public LoadingPanel() {
        
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        } catch (Exception e) { e.printStackTrace(); }

       createPanel();
    }

    private void createPanel() {
        Thread t = new Thread(new Runnable() {    
            @Override
            public void run() {    
                JOptionPane.showMessageDialog(null, "Inference Engine is running ...", "Loading", JOptionPane.INFORMATION_MESSAGE); 
            }    
        });         
        t.start();
        this.t = t;
    }
    
    public void closePanel() {
        t.interrupt();
    }
}
