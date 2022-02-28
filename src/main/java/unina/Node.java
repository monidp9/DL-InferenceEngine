package unina;

import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;


public class Node {

    private Set<OWLAxiom> structure;
    private Node sxPtr = null;
    private Node dxPtr = null;
    private Node parent = null;
    private OWLIndividual x;
    private boolean blocked = false;

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

    public void setSxPtr(Node ptr){
        this.sxPtr = ptr;
    }

    public void setDxPtr(Node ptr){
        this.dxPtr = ptr;
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

    public Node getSxPtr(){
        return sxPtr;
    }

    public Node getDxPtr(){
        return dxPtr;
    }

    public OWLIndividual getIndividual(){
        return x;
    }
}
