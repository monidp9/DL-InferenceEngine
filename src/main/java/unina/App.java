package unina;

import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
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

    public void createAnOntology() throws Exception {
        // crea una TBox semplice sul cibo

        IRI IOR = IRI.create("http://owl.api.tutorial");
       
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLOntology o = null;

        try {
            o = man.createOntology(IOR);
        } catch(OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
        PrefixManager pm = new DefaultPrefixManager("http://owl.api.tutorial#");
    
        OWLClass Person = df.getOWLClass("Person", pm);
        OWLClass Food = df.getOWLClass("Food", pm);
        OWLClass VegetarianFood = df.getOWLClass("VegetarianFood", pm);
        OWLClass Vegetable = df.getOWLClass("Vegetable", pm);
        OWLClass Meat = df.getOWLClass("Meat", pm);
        OWLClass Vegetarian = df.getOWLClass("Vegetarian", pm);
    
        OWLObjectProperty eats = df.getOWLObjectProperty("eats", pm);
    
        OWLObjectAllValuesFrom eatsFood = df.getOWLObjectAllValuesFrom(eats, Food);
        OWLObjectAllValuesFrom eatsVegFood = df.getOWLObjectAllValuesFrom(eats, VegetarianFood);
        
        OWLObjectPropertyDomainAxiom domAx = df.getOWLObjectPropertyDomainAxiom(eats, Person);
        o.add(domAx);

        OWLObjectPropertyRangeAxiom rangeAx = df.getOWLObjectPropertyRangeAxiom(eats, eatsFood);
        o.add(rangeAx);

        OWLClassExpression notFood = Food.getObjectComplementOf();
        OWLSubClassOfAxiom subClassAx = df.getOWLSubClassOfAxiom(Person, notFood);
        o.add(subClassAx);

        subClassAx = df.getOWLSubClassOfAxiom(VegetarianFood, notFood);
        o.add(subClassAx);

        subClassAx = df.getOWLSubClassOfAxiom(Vegetable, VegetarianFood);
        o.add(subClassAx);

        OWLClassExpression notVegFood = VegetarianFood.getObjectComplementOf();

        Stream<OWLClassExpression> operands = Stream.of(Food, notVegFood);
        OWLObjectIntersectionOf intersections = df.getOWLObjectIntersectionOf(operands);
        subClassAx = df.getOWLSubClassOfAxiom(Meat, intersections);
        o.add(subClassAx);
    
        operands = Stream.of(Person, eatsVegFood);
        intersections = df.getOWLObjectIntersectionOf(operands);
        OWLEquivalentClassesAxiom equivAx = df.getOWLEquivalentClassesAxiom(Vegetarian, intersections);
        o.add(equivAx);

        // File fileout = new File("/Users/monidp/Desktop/IWProject/inference-engine-dl/food.man.owl");
        // man.saveOntology(o, new ManchesterSyntaxDocumentFormat(), new FileOutputStream(fileout));
    }
}

