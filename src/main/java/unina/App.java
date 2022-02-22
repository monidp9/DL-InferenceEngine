package unina;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.IRI;
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
    public static void main( String[] args ) {
        
        App app = new App();  
        app.createAnOntology();
    }

    public void createAnOntology() {

        IRI IOR = IRI.create("http://owl.api.tutorial");
       
        Reasoner r = new Reasoner();
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology o = null;

        try {
            o = man.createOntology(IOR);
        } catch(OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
    
        OWLClass Student = df.getOWLClass(IOR + "#Student");
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
    
        Stream<OWLClassExpression> operands = Stream.of(Person, enrollU, University);
        OWLObjectIntersectionOf intersections = df.getOWLObjectIntersectionOf(operands);

        Stream<OWLClassExpression> operands2 = Stream.of(intersections, attendsC, University);
        OWLObjectIntersectionOf intersections2 = df.getOWLObjectIntersectionOf(operands2);

    
        OWLEquivalentClassesAxiom student_equiv = df.getOWLEquivalentClassesAxiom(Student, intersections);
    
        o.add(student_equiv);

        // asserzioni
        OWLIndividual me = df.getOWLNamedIndividual(IOR + "#Sasi");
        OWLClassAssertionAxiom classAss = df.getOWLClassAssertionAxiom(Person, me);
        o.add(classAss);
        classAss = df.getOWLClassAssertionAxiom(attendsC, me);
        o.add(classAss);

        System.out.println("\n\n\n");
        r.reasoning(me, intersections);

  /*  

    
        OWLIndividual manchesterUniversity = df.getOWLNamedIndividual(IOR + "#manchersterUniversity");
        OWLObjectPropertyAssertionAxiom objPropAss = df.getOWLObjectPropertyAssertionAxiom(
            isEnrolledIn, me, manchesterUniversity
        );
        o.add(objPropAss);
    
    
        // salva l'ontologia creata
        File fileout = new File("/Users/salvatoreamodio/Desktop/students.owl");
    
        try {
            man.saveOntology(o, new ManchesterSyntaxDocumentFormat(), new FileOutputStream(fileout));
        } catch (Exception e) {
            e.printStackTrace();
        }
*/ 
    }


}

