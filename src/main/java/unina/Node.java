package unina;

import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;


public class Node {

    private Set<OWLAxiom> structure;
    private Node sxPtr = null;
    private Node dxPtr = null;
    private OWLIndividual x;

    public Node(OWLIndividual x){
        this.structure = new TreeSet <OWLAxiom>();
        this.x = x;
    }
    
    public Set<OWLAxiom> getStructure(){
        return structure;
    }

    public void setStructure(Set<OWLAxiom> structure){
        this.structure = structure;
    }

    public void setSxPtr(Node ptr){
        this.sxPtr = ptr;
    }

    public void setDxPtr(Node ptr){
        this.dxPtr = ptr;
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
