package unina;

import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;


public class Reasoner {
    
    private Node tree;

    public Reasoner(){
        this.tree = new Node();
    }
    
    public boolean reasoning(OWLIndividual x, OWLClassExpression C){

        Set<OWLClassExpression> aboxes = new TreeSet<> ();
        aboxes.add(C);
        tree.setStructure(aboxes);

        if ( C instanceof OWLObjectIntersectionOf ) {
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf ce) {
                    for (OWLClassExpression axiom : ce.getOperands()) {
                        if (!aboxes.contains(axiom)){  // rivedere, capire anche se il metodo di ClassExeption riesce a capire se siano differenti due abox
                            aboxes.add(axiom);
                        }
                    }
                }
            });
        }

        System.out.println(aboxes);

        return false;
    }

}