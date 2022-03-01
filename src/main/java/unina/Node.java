package unina;

import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;


public class Node {

    private Set<OWLAxiom> structure;
    private Node parent = null;
    private OWLIndividual x;
    private boolean blocked = false;
    private boolean sx = false;

    public Node(OWLIndividual x){
        this.structure = new TreeSet <OWLAxiom>();
        this.x = x;
    }
    
    public void setStructure(Set<OWLAxiom> structure){
        this.structure = structure;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public void setParent(Node ptr) {
        this.parent = ptr;
    }

    public void setSx(){
        this.sx = true;
    }

    public Set<OWLAxiom> getStructure(){
        return structure;
    }

    public boolean isBlocked() {
        return this.blocked;
    }

    public Node getParent() {
        return parent;
    }

    public boolean getSx(){
        return this.sx;
    }

    public OWLIndividual getIndividual(){
        return x;
    }
}
