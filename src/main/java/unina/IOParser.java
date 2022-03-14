package unina;

import java.io.File;
import java.util.stream.Collectors;

import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

import unina.view.View;


public class IOParser {

    /*
     * Effettua il parsing di una stringa in Manchester Syntax presa in input,
     * convertendola in un concetto complesso (OWLClassExpression). Permette
     * di caricare da file la knowledge base di interesse (TBox).
     */

    private OWLOntology o;
    private OWLOntologyManager man;

    private OWLClassExpression concept = null;
    private View view;

    private String basePath = "ontologies/";

    public IOParser() {
        man = OWLManager.createOWLOntologyManager();  
    }

    public  List<OWLAxiom> getTbox() {
        List<OWLAxiom> axioms = o.logicalAxioms().collect(Collectors.toList());
        return axioms;
    }

    public void loadOntology(String ontologyFile) {

        /*
         * Carica da file l'ontologia specificata tramite path.
         */

        String filePath = basePath + ontologyFile;
        File file = new File(filePath);
        try {
            o = man.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    public void fromExprToConcept(String expr) {

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
                view.showError("Logical operators (and, or) are expected to be binary");
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
                view.showError("Logical operators (and, or) are expected to be binary");
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

    public void setView(View view){
        this.view = view;
    }

    public OWLClassExpression getConcept() {
        return concept;
    }

    public static void main(String[] args) {    
        IOParser io = new IOParser();
        Reasoner reasoner = new Reasoner();

        View view = new View();
        io.setView(view);

        // caricamento TBox
        io.loadOntology("food.man.owl");
        reasoner.setTbox(io.getTbox());

        //lettura e traduzione in concetto
        view.openConceptReadingView(io);

        OWLClassExpression concept = io.getConcept();
        
        System.out.println("\n------ TABLEAUX METHOD FOR SAT IN ALC ------");
        System.out.println("\nINPUT CONCEPT: \n" + concept + "\n");

        reasoner.activeLazyUnfolding();
        
        boolean sat = reasoner.reasoning(concept);
        System.out.print("Concept satisfiability: ");
        if(sat) {
            System.out.println("Yes");
        } else {
            System.out.println("No");
        }
        System.out.println("\n");

        view.openGraphView();
    }

}