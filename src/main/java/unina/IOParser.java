package unina;

import java.io.File;
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
    OWLOntology o = null;
    OWLOntologyManager man;

    public IOParser() throws OWLOntologyCreationException {
        this.man = OWLManager.createOWLOntologyManager();
        File file = new File("/Users/monidp/Desktop/IWProject/inference-engine-dl/students.owl");
        o = man.loadOntologyFromOntologyDocument(file);
    }

    public OWLClassExpression parseInputConcept(String concept) throws OWLOntologyCreationException{
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        parser.setStringToParse(concept);
        parser.setDefaultOntology(this.o);

        Set<OWLOntology> ontologies = man.getOntologies();

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
        //io.parseInputConcept(args[0]);
        OWLClassExpression concept = io.parseInputConcept("(not <http://owl.api.tutorial#Student> or <http://owl.api.tutorial#Person>) and <http://owl.api.tutorial#isEnrolledIn> some  <http://owl.api.tutorial#University>");
        System.out.println("\n\n" + concept);
    }
}
