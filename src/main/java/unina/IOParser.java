package unina;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import java.awt.event.*;

import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import javafx.application.Platform;


public class IOParser extends JDialog implements ActionListener{

    /*
     * Effettua il parsing di una stringa in Manchester Syntax presa in input,
     * convertendola in un concetto complesso (OWLClassExpression). Permette
     * di caricare da file la knowledge base di interesse (TBox).
     */

    private OWLOntology o;
    private OWLOntologyManager man;

    private String expr;
    private OWLClassExpression concept;

    private JTextArea t;
    private JDialog d;

    public IOParser() {
        man = OWLManager.createOWLOntologyManager();  
    }

    public  List<OWLAxiom> getTbox() {
        List<OWLAxiom> axioms = o.logicalAxioms().collect(Collectors.toList());
        return axioms;
    }

    public OWLClassExpression readAndTraslateExpr() {

        /*
         * Gestisce la GUI per l'inserimento dell'espressione in Manchester
         * Syntax e attende che l'utente completi l'operazione prima di 
         * restituire il concetto.
         */

        t = new JTextArea();
        d = new JDialog();

        d.setTitle("Enter your concept (Manchester Syntax)");

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            MetalLookAndFeel.setCurrentTheme(new OceanTheme());
        }
        catch (Exception e) { e.printStackTrace(); }

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

        m1.add(mi1);
        m1.add(mi4);
        
        mb.add(m1);
        mb.add(mi2);
        mb.add(mi3);

        d.add(t);
        d.setJMenuBar(mb);

        d.setSize(600, 300);
        d.setLocationRelativeTo(null);
        d.setModal(true);
        d.setVisible(true);

        return concept;
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
            expr = t.getText();

            try {
                fromExprToConcept();
            } catch (ParserException ex) {
                JOptionPane.showMessageDialog(d, "Manchester syntax error.", "ERROR", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            } finally {
                JOptionPane.showMessageDialog(d, "Concept traslated.", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
            }
            
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

    public void loadOntology(String filePath) {

        /*
         * Carica da file l'ontologia specificata tramite path.
         */

        File file = new File(filePath);
        try {
            o = man.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    private void fromExprToConcept() {

        /*
         * Converte l'espressione attributo della classe in un concetto
         * composto secondo il tipo di dato OWLClassExpression.
         */

        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();

        // stringa da convertire
        parser.setStringToParse(expr);

        // ontologia per risolvere entità e classi durante il parsing
        parser.setDefaultOntology(o);

        Set<OWLOntology> ontologies = Collections.singleton(o);

        // checker necessario per mappare le stringhe entità con le entità effettive
        ShortFormProvider sfp = new ManchesterOWLSyntaxPrefixNameShortFormProvider(man.getOntologyFormat(o));
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(
            ontologies, sfp
        );
        ShortFormEntityChecker checker = new ShortFormEntityChecker(shortFormProvider);

        parser.setOWLEntityChecker(checker);

        concept = parser.parseClassExpression();
        concept = concept.getNNF();
        concept = checkBinary(concept);
    }

    private OWLClassExpression checkBinary(OWLClassExpression concept) {

        /*
         * Visita un concetto scomponendolo ricorsivamente. Verifica se gli operatori 
         * sono stati applicati solo su due operandi. In tal caso restituisce il 
         * concetto passato, altrimenti termina il programma con messaggio di errore.
         */

        concept.accept(new OWLClassExpressionVisitor() {
           @Override
           public void visit(OWLObjectIntersectionOf objIn) {
               List<OWLClassExpression> l = objIn.getOperandsAsList();
               if(l.size() > 2) {
                   JOptionPane.showMessageDialog(d, "Logical operators (and, or) are expected to be binary",
                                                 "ERROR", JOptionPane.ERROR_MESSAGE);
                   System.exit(1);
                }
               for(OWLClassExpression ce: l) {
                   if(ce instanceof OWLObjectIntersectionOf || 
                      ce instanceof OWLObjectUnionOf ||
                      ce instanceof OWLObjectSomeValuesFrom ||
                      ce instanceof OWLObjectAllValuesFrom) {
                        checkBinary(ce);
                    }
                }
            }

           @Override
           public void visit(OWLObjectUnionOf objUn) {
              List<OWLClassExpression> l = objUn.getOperandsAsList();
               if(l.size() > 2) {
                  JOptionPane.showMessageDialog(d, "Logical operators (and, or) are expected to be binary",
                                                "ERROR", JOptionPane.ERROR_MESSAGE);
                  System.exit(1);
                }
              for(OWLClassExpression ce: l) {
                  if(ce instanceof OWLObjectIntersectionOf || 
                     ce instanceof OWLObjectUnionOf || 
                     ce instanceof OWLObjectSomeValuesFrom ||
                     ce instanceof OWLObjectAllValuesFrom) {
                        checkBinary(ce);
                    }
                }
            }

           @Override
           public void visit(OWLObjectSomeValuesFrom objSVF) {
                OWLClassExpression filler = objSVF.getFiller();
                checkBinary(filler);
           }

           @Override 
           public void visit(OWLObjectAllValuesFrom objAVF) {
                OWLClassExpression filler = objAVF.getFiller();
                checkBinary(filler);
           }
        });

        return concept;
    }


    public static void main(String[] args) {    

        IOParser io = new IOParser();
        Reasoner reasoner = new Reasoner();

        // caricamento TBox
        String filePath = "ontologie/food.man.owl";
        io.loadOntology(filePath);
        List<OWLAxiom> tbox = io.getTbox();

        //lettura e traduzione in concetto
        OWLClassExpression concept = io.readAndTraslateExpr();

        System.out.println("\n ------ TRANSLATED ------");
        System.out.println("\nCONCEPT: \n" + concept + "\n");

        reasoner.activeLazyUnfolding();
        reasoner.setTbox(tbox);
        
        System.out.println("\nTABLEAUX : "+reasoner.reasoning(concept));
    
        System.exit(0);
    }
}