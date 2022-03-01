package unina;


import java.io.File;
import java.io.FileOutputStream;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import static guru.nidi.graphviz.model.Factory.*;

import org.semanticweb.owlapi.model.IRI;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;


public class App 
{
    public static void main( String[] args ) throws Exception {
        
        App app = new App();  

        // MutableGraph g = mutGraph("example1").setDirected(true).add(
        //                  mutNode("a").add(Color.RED).addLink(mutNode("b")));
        MutableGraph g = mutGraph("example").setDirected(true);

        MutableNode a = mutNode("a");
        MutableNode b = mutNode("b");
        g.add(a);

        a.addLink(to(b).with(Label.of(" OR")));

        Graphviz.fromGraph(g).width(200).render(Format.PNG).toFile(new File("result/ex1m.png"));
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
        OWLObjectUnionOf union3 = df.getOWLObjectUnionOf(operands3);

        Stream<OWLClassExpression> operands = Stream.of(Person, enrollU);
        OWLObjectUnionOf union = df.getOWLObjectUnionOf(operands);

        
        IRI iri = IRI.create("https://protege.stanford.edu/ontologies/pizza/pizza.owl");
        o = man.loadOntology(iri);

        File fileout = new File("pizza.man.owl");
        man.saveOntology(o, new ManchesterSyntaxDocumentFormat(), new FileOutputStream(fileout));
    }


}

