package unina;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
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


    
        System.out.println("Ciao Sasi \n\n\n");
        System.out.println("Stampa concetto C\n");
        System.out.println(intersection + "\n\n");

        OWLIndividual x = df.getOWLAnonymousIndividual();
        OWLIndividual y = df.getOWLAnonymousIndividual();



        OWLClassAssertionAxiom a1 = df.getOWLClassAssertionAxiom(Person, x);
        OWLClassAssertionAxiom a2 = df.getOWLClassAssertionAxiom(Person, x);


        a1.accept(new OWLAxiomVisitor() {
            
        });

        OWLClassExpression c1 = a1.getClassExpression();
        OWLClassExpression c2 = a2.getClassExpression();

        System.out.println("prova equals " + c1.equals(c2.getComplementNNF()));
        System.out.println("prova equals " + a1.equals(a2));


        Set<OWLAxiom> aboxes = new TreeSet<>();

        OWLObjectPropertyAssertionAxiom p = df.getOWLObjectPropertyAssertionAxiom(attends, x, y);

        aboxes.add(p);
        
        System.out.println("Tableux : "+ r.reasoning(intersection));

  /*  

        //OWLEquivalentClassesAxiom student_equiv = df.getOWLEquivalentClassesAxiom(Student, intersections);
    
        //o.add(student_equiv);

        // asserzioni
        OWLIndividual me = df.getOWLNamedIndividual(IOR + "#Sasi");
        OWLClassAssertionAxiom classAss = df.getOWLClassAssertionAxiom(Person, me);
        o.add(classAss);
        classAss = df.getOWLClassAssertionAxiom(attendsC, me);
        o.add(classAss);
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

