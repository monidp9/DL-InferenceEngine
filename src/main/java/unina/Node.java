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
    private Node parentOnGraph = null;
    private Integer id = 0;
    private static Integer counterId = 0;

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

    public void setParentOnGraph(Node ptr) {
        this.parentOnGraph = ptr;
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

    public Node getParentOnGraph() {
        return parentOnGraph;
    }

    public boolean getSx(){
        return this.sx;
    }

    public OWLIndividual getIndividual(){
        return x;
    }

    public Integer getId() {
        if(id == 0) {
            counterId++;
            id = counterId;
        }
        return id;
    }
}
