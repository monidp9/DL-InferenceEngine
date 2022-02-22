package unina;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;


public class IOParser {

    /*
     * Effettua il parsing di una stringa in Manchester Syntax presa in input,
     * convertendola in un concetto complesso (OWLClassExpression). Inoltre carica
     * da file la knowledge base di interesse (TBox).
     */

    OWLOntology o;
    OWLOntologyManager man;

    public IOParser() {
        man = OWLManager.createOWLOntologyManager();                
    }

    public String readExpr() {

        /*
         * Legge da riga di comando un'espressione scritta secondo 
         * Manchester Syntax.
         */

        Console console = System.console();
        String input = "";
        
        System.out.println("\n\nEnter your concept in Manchester Syntax:\n");
        input = console.readLine();

        return input;
    }

    public void loadOntologyTBox(String filePath) throws OWLOntologyCreationException{

        /*
         * Carica da file l'ontologia specificata tramite path.
         */

        File file = new File(filePath);
        o = man.loadOntologyFromOntologyDocument(file);
    }

    public OWLClassExpression fromExprToConcept(String expr) {

        /*
         * Converte l'espressione in Manchester Syntax presa da input in un
         * concetto composto secondo il tipo di dato OWLClassExpression.
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

        OWLClassExpression concept = parser.parseClassExpression();
        concept = concept.getNNF();
        concept = visitConcept(concept);

        return concept;
    }

    private OWLClassExpression visitConcept(OWLClassExpression concept) {

        /*
         * Verifica se gli operatori sono stati applicati su solo due operandi.
         * In tal caso restituisce il concetto passato, altrimenti termina il
         * programma con messaggio di errore.
         */

        concept.accept(new OWLClassExpressionVisitor() {
           @Override
           public void visit(OWLObjectIntersectionOf objIn) {
               List<OWLClassExpression> l = objIn.getOperandsAsList();
               if(l.size() > 2) {
                   System.out.println("<!> Parsing Error: logical operators (and, or) are expected to be binary");
                   System.exit(1);
                }
               for(OWLClassExpression ce: l) {
                   if(ce instanceof OWLObjectIntersectionOf || 
                      ce instanceof OWLObjectUnionOf ||
                      ce instanceof OWLObjectSomeValuesFrom ||
                      ce instanceof OWLObjectAllValuesFrom) {
                        visitConcept(ce);
                    }
                }
            }

           @Override
           public void visit(OWLObjectUnionOf objUn) {
              List<OWLClassExpression> l = objUn.getOperandsAsList();
               if(l.size() > 2) {
                  System.out.println("<!> Parsing Error: logical operators (and, or) are expected to be binary");
                  System.exit(1);
                }
              for(OWLClassExpression ce: l) {
                  if(ce instanceof OWLObjectIntersectionOf || 
                     ce instanceof OWLObjectUnionOf || 
                     ce instanceof OWLObjectSomeValuesFrom ||
                     ce instanceof OWLObjectAllValuesFrom) {
                        visitConcept(ce);
                    }
                }
            }

           @Override
           public void visit(OWLObjectSomeValuesFrom objSVF) {
                OWLClassExpression filler = objSVF.getFiller();
                visitConcept(filler);
           }

           @Override 
           public void visit(OWLObjectAllValuesFrom objAVF) {
                OWLClassExpression filler = objAVF.getFiller();
                visitConcept(filler);
           }
        });

        return concept;
    }

    public static void main(String[] args) throws Exception{    
        IOParser io = new IOParser();

        // caricamento TBox
        String filePath = "/Users/monidp/Desktop/IWProject/inference-engine-dl/food.man.owl";
        io.loadOntologyTBox(filePath);

        // lettura concetto
        String expr = io.readExpr();
        OWLClassExpression concept = io.fromExprToConcept(expr);
        
        System.out.println("\n ------ TRASLATED ------");
        System.out.println("\n" + concept);
    }
}
