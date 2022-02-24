package unina;

import java.util.LinkedList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

public class Reasoner {
    
    private Node node;

    public Reasoner(){
    }
    
    public boolean reasoning(OWLClassExpression C){

        this.node = new Node();
        List<OWLClassExpression> structure = node.getStructure();
        structure.add(C);
        
        return  dfs(node);
    }

    private boolean dfs(Node node){

        boolean isAppliedRule = false;
        boolean isAppliedRule2 = false;
        Node newNode = null;
        List<OWLObjectPropertyExpression> propertyContainer = new LinkedList<>();


        List<OWLClassExpression> structure = node.getStructure();
        List<OWLClassExpression> structureTmp = new LinkedList<>(structure);
        
        // applica AND esaustivamente 
        do{    
            for (OWLClassExpression axiom : structure){
                if (axiom instanceof OWLObjectIntersectionOf){
                    isAppliedRule = handleIntersectionOf(axiom, structureTmp);
                } 
            }
            node.setStructure(new LinkedList<>(structureTmp)); 
            structure = node.getStructure();

        }while (isAppliedRule);  

        // applica OR esaustivamente aggiungendo uno soltanto dei disgiunti
        for (OWLClassExpression axiom : structure){

            if ( axiom instanceof OWLObjectUnionOf ) {
                isAppliedRule = handleUnionOf(axiom, true, structureTmp);
            }

            /* 
                se la regola non viene applicata viene valutata la prossima Abox; 
                terminato il ciclo si valutano le applicazioni delle regole esistenziali ed universali.
            */
            if (isAppliedRule){ 
                // se la struttura con l'unione del disgiunto diventa clash-free, la chiamata ricorsiva termina e ritorna false
                if (isClashFree(structureTmp)){ 
                    newNode = new Node();
                    newNode.setStructure(structureTmp);
                    node.setSxPtr(newNode);

                    // se la chiamata ricorsiva a sx non è andata a buon fine si scende a destra
                    if(!dfs(newNode)){ 
                        structureTmp = new LinkedList<>(structure);
                    
                        //la regola è sempre applicata in quanto si aggiunge alla struttura l'altro disgiunto
                        isAppliedRule = handleUnionOf(axiom, false, structureTmp); 

                        if (isClashFree(structureTmp)){ 
                            newNode = new Node(); 
                            newNode.setStructure(structureTmp);
                            node.setDxPtr(newNode);

                            return dfs(newNode);
                        } else {
                            return false;
                        }                     
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
      
        if(isClashFree(structure)){
            // va a svolgere esistenziale e universale (non so se serve il controllo, dovrebbe non volerci, esce da union)
            if (!isAppliedRule){ 

                // applica ESISTENZIALE
                do{    
                    for (OWLClassExpression abox : structure){
                        if (abox instanceof OWLObjectSomeValuesFrom){
                            isAppliedRule = handleSomeValuesFrom(abox, structureTmp, propertyContainer);
                        }

                        if (isAppliedRule){ 
                            // applica UNIVERSALE esaustivamente
                            for (OWLClassExpression abox2 : structure) {
                                if (abox2 instanceof OWLObjectAllValuesFrom){
                                    isAppliedRule2 = handleAllValuesFrom(abox2, structureTmp, propertyContainer);
                                }
                            }
                            if (isAppliedRule2 == true) 
                                propertyContainer.remove(0);
                    
                            if (isClashFree(structureTmp)){ 
                                newNode = new Node(); 
                                newNode.setStructure(structureTmp);
                                node.setSxPtr(newNode);

                                if(!dfs(newNode)){
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        }
                    }
                    node.setStructure(new LinkedList<>(structureTmp)); 
                    structure = node.getStructure();

                } while (isAppliedRule);  
            }
        } else {
            return false;
        }
        return true;
    } 

    private boolean handleAllValuesFrom(OWLClassExpression axiom, List<OWLClassExpression> structure,List<OWLObjectPropertyExpression> propertyContainer){

        
        int strSize = structure.size();

        axiom.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectAllValuesFrom avf) {
                if (avf.getProperty() == propertyContainer.get(0)){
                    OWLClassExpression abox = avf.getFiller();
                    if(!structure.contains(abox)){
                        structure.add(abox);
                    }
                }
            }
        });

        return strSize < structure.size();
    }

    private boolean handleSomeValuesFrom(OWLClassExpression axiom, List<OWLClassExpression> structure, List<OWLObjectPropertyExpression> propertyContainer) {
        
        int strSize = structure.size();

        axiom.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectSomeValuesFrom svf) {
                OWLClassExpression abox = svf.getFiller();
                if(!structure.contains(abox)){
                    structure.add(abox);
                    propertyContainer.add(svf.getProperty());
                }
            }
        });

        return strSize < structure.size();
    }

    private boolean handleIntersectionOf (OWLClassExpression axiom, List<OWLClassExpression> structure){
        
        int strSize = structure.size();

        axiom.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectIntersectionOf oi) {
                for (OWLClassExpression newAxiom : oi.getOperandsAsList()) {
                    if (!structure.contains(newAxiom)){ 
                        structure.add(newAxiom);
                    }
                }
            }
        });

        return strSize < structure.size();
    }

    private boolean handleUnionOf (OWLClassExpression axiom, Boolean isPtrSxEmpty, List<OWLClassExpression> structure){ 

        int strSize = structure.size();
        axiom.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectUnionOf ou) {
                boolean flag = false;
                OWLClassExpression secondDisjunct = null; 

                for (OWLClassExpression disjunct : ou.getOperandsAsList()) {
                    if(!flag){
                        flag = true;
                    } else {
                        secondDisjunct = disjunct;
                    }
                }

                // quando termina la chiamata ricorsiva a sx e si risale, bisogna aggiungere il secondo disgiunto e scendere a destra
                flag = false;

                for (OWLClassExpression disjunct : ou.getOperandsAsList()) {
                    if (!structure.contains(disjunct)){ 

                        if ((isPtrSxEmpty || flag) && !structure.contains(secondDisjunct)){
                            structure.add(disjunct);
                            break;
                        } else {
                            flag = true;
                        }
                        
                    } else if (structure.contains(disjunct)){                         
                            break;
                    }
                }
            }
        });

        return strSize < structure.size();
    }

    private boolean isClashFree(List<OWLClassExpression> structure){
        OWLClassExpression complementAbox;

        for (OWLClassExpression abox: structure){
            if (abox instanceof OWLClass){
                complementAbox = abox.getObjectComplementOf();
                if(structure.contains(complementAbox))
                    return false;
            }
        }
        return true;
    }
}