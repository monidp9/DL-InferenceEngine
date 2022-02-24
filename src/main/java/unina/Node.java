package unina;

import java.util.LinkedList;
import java.util.List;
import org.semanticweb.owlapi.model.OWLClassExpression;

public class Node {

    private List<OWLClassExpression> structure;
    private Node sxPtr = null;
    private Node dxPtr = null;

    public Node(){
        this.structure = new LinkedList<> ();
    }
    
    public List<OWLClassExpression> getStructure(){
        return structure;
    }

    public void setStructure(List<OWLClassExpression> structure){
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
