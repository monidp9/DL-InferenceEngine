package unina;


import java.io.FileOutputStream;
import java.util.Objects;
import java.util.stream.Stream;


import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;


import org.semanticweb.owlapi.model.IRI;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;


public class App 
{
    public static void main( String[] args ) throws Exception {

        App app = new App();
        app.createAnOntology();

    }

    public void createAnOntology() throws Exception{

        IRI IOR = IRI.create("http://owl.api.tutorial");
       
        // Reasoner r = new Reasoner();
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology o = null;

        try {
            o = man.createOntology(IOR);
        } catch(OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
    
        OWLClass Person = df.getOWLClass(IOR + "#Person");
        OWLClass University = df.getOWLClass(IOR + "#University");
        OWLClass Course = df.getOWLClass(IOR + "#Course");

   
        OWLObjectProperty isEnrolledIn = df.getOWLObjectProperty(IOR + "#isEnrolledIn");
        OWLObjectProperty attends = df.getOWLObjectProperty(IOR + "#attends");
    
        OWLObjectSomeValuesFrom enrollU = df.getOWLObjectSomeValuesFrom(
            isEnrolledIn, University
        );
        OWLObjectSomeValuesFrom attendsC = df.getOWLObjectSomeValuesFrom(
            attends, Course
        );
    

        Stream<OWLClassExpression> operands3 = Stream.of(Person, attendsC);

        Stream<OWLClassExpression> operands = Stream.of(Person, enrollU);
        OWLObjectUnionOf union = df.getOWLObjectUnionOf(operands);

        
        // IRI iri = IRI.create("https://protege.stanford.edu/ontologies/pizza/pizza.owl");
        // o = man.loadOntology(iri);

        // File fileout = new File("pizza.man.owl");
        // man.saveOntology(o, new ManchesterSyntaxDocumentFormat(), new FileOutputStream(fileout));
        Stream<OWLClassExpression> s = Stream.of(Person, null);

    }


}

