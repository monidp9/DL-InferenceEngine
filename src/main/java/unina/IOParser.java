package unina;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;

public class IOParser {
    /*
        Effettua il parsing di una stringa in Manchester Syntax presa in
        input convertendola in un concetto complesso (OWLClassExpression). 
        Inoltre carica da file la TBox.
    */
    OWLOntology o;
    OWLOntologyManager man;

    public IOParser() throws OWLOntologyCreationException {
        this.man = OWLManager.createOWLOntologyManager();
        File file = new File("/Users/monidp/Desktop/IWProject/inference-engine-dl/students.owl");
        o = man.loadOntologyFromOntologyDocument(file);
    }

    public OWLClassExpression fromStringToConcept(String concept) {
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();

        // stringa da convertire
        parser.setStringToParse(concept);

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
        return parser.parseClassExpression();
    }

    public static void main(String[] args) throws Exception{    
        IOParser io = new IOParser();
        OWLClassExpression concept = io.fromStringToConcept("(not <http://owl.api.tutorial#Student> or <http://owl.api.tutorial#Person>) and <http://owl.api.tutorial#isEnrolledIn> some  <http://owl.api.tutorial#University>");
        System.out.println("\n\n" + concept);
    }
}
