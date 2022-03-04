package unina.view;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import javafx.application.Platform;
import unina.IOParser;


public class ConceptPanel extends JPanel implements ActionListener{

    private JTextArea t;
    private JDialog d;
    private IOParser ioParser;

    public ConceptPanel(IOParser ioParser){
        this.ioParser = ioParser;
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        } catch (Exception e) { e.printStackTrace(); }

        createPanel();
    }
    

    private void createPanel() {
        t = new JTextArea();
        d = new JDialog();
    
        t.setLineWrap(true);
        d.setTitle("Enter your concept (Manchester Syntax)"); 
        d.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
   
        JMenuBar mb = new JMenuBar();

        JMenu m1 = new JMenu("File");

        JMenuItem mi1 = new JMenuItem("Save");
        JMenuItem mi2 = new JMenuItem("Done");
        JMenuItem mi3 = new JMenuItem("Close");
        JMenuItem mi4 = new JMenuItem("Open");

        mi1.addActionListener(this);
        mi2.addActionListener(this);
        mi3.addActionListener(this);
        mi4.addActionListener(this);

        d.addWindowListener(new WindowListener(){

            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(1);
            }

            @Override
            public void windowClosed(WindowEvent e) {}

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {}
            } 
        );

        m1.add(mi1);
        m1.add(mi4);
        
        mb.add(m1);
        mb.add(new JSeparator());
        mb.add(mi2);
        mb.add(mi3);

        d.add(t);
        d.setJMenuBar(mb);

        d.setSize(600, 300);
        d.setLocationRelativeTo(null);
        d.setModal(true);
        d.setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        
        /*
         * Implementa le azioni da fare una volta che l'utente interagisce
         * con la GUI.
         */

        String s = e.getActionCommand();



        if(s.equals("Save")) {
            JFileChooser j = new JFileChooser("f:");
 
            // invoca uno dialog per il salvataggio
            int r = j.showSaveDialog(null);
 
            if (r == JFileChooser.APPROVE_OPTION) {
                File fi = new File(j.getSelectedFile().getAbsolutePath());
                try {
                    FileWriter wr = new FileWriter(fi, false);
                    BufferedWriter w = new BufferedWriter(wr);
 
                    w.write(t.getText()); 
                    w.flush();
                    w.close();
                }
                catch (Exception evt) {
                    JOptionPane.showMessageDialog(d, evt.getMessage());
                }
            }
            // se l'utente cancella l'operazione
            else {
                JOptionPane.showMessageDialog(d, "the user cancelled the operation");
            }
        } else if(s.equals("Done")) {
            String expr = t.getText();

            try {
                ioParser.fromExprToConcept(expr);
            } catch (ParserException ex) {
                JOptionPane.showMessageDialog(d, "Manchester syntax error.", "ERROR", JOptionPane.ERROR_MESSAGE);
                t.setText(null);
                return;
            } 
            JOptionPane.showMessageDialog(d, "Concept traslated.", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
            
            d.setModal(false);
            d.setVisible(false);
            Platform.exit();

        } else if(s.equals("Close")) {
            d.setModal(false);
            d.setVisible(false);
            System.exit(0);
        } else if(s.equals("Open")) {
            JFileChooser j = new JFileChooser("f:");
            int r = j.showOpenDialog(null);
 
            if (r == JFileChooser.APPROVE_OPTION) {
                File fi = new File(j.getSelectedFile().getAbsolutePath());
 
                try {
                    String s1 = "", sl = "";

                    FileReader fr = new FileReader(fi);
                    BufferedReader br = new BufferedReader(fr);
 
                    sl = br.readLine();
 
                    while ((s1 = br.readLine()) != null) {
                        sl = sl + "\n" + s1;
                    }
 
                    t.setText(sl);
                    br.close();
                }
                catch (Exception evt) {
                    JOptionPane.showMessageDialog(d, evt.getMessage());
                }
            }
            else
                JOptionPane.showMessageDialog(d, "the user cancelled the operation");
        }
    }

    public void showMsgError(String msg) {
        JOptionPane.showMessageDialog(d, msg, "ERROR", JOptionPane.ERROR_MESSAGE, null);
    }

}
