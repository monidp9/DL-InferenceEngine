package unina;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class App 
{
    public static void main( String[] args ) throws Exception{
        
        App app = new App();  
        app.createAnOntology();
    }

    public void createAnOntology() throws Exception{

        IRI IOR = IRI.create("http://owl.api.tutorial");
       
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
        OWLObjectUnionOf union3 = df.getOWLObjectUnionOf(operands3);

        Stream<OWLClassExpression> operands = Stream.of(Person, enrollU);
        OWLObjectUnionOf union = df.getOWLObjectUnionOf(operands);


        Stream<OWLClassExpression> operands5 = Stream.of(union3, union);
        OWLObjectIntersectionOf intersection = df.getOWLObjectIntersectionOf(operands5);


        intersection = df.getOWLObjectIntersectionOf(Person, University);
        OWLEquivalentClassesAxiom classAssertion = df.getOWLEquivalentClassesAxiom(Course, intersection);
        OWLSubClassOfAxiom ax = df.getOWLSubClassOfAxiom(Course, intersection);
        o.add(classAssertion);

        File fileout = new File("prova.man.owl");
        man.saveOntology(o, new ManchesterSyntaxDocumentFormat(), new FileOutputStream(fileout));
    }

}

