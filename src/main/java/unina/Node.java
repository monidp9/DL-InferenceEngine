package unina;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLClassExpression;

public class Node {

    private Set<OWLClassExpression> structure;

    private Node sxPtr;
    private Node dxPtr;

    public Node(){}
    
    public Node(Set<OWLClassExpression> structure){
        this.structure = structure;
    }

    public Set<OWLClassExpression> getStructure(){
        return structure;
    }

    public void setStructure(Set<OWLClassExpression> structure){
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
}
