package unina;

import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;


public class Reasoner {
    
    private Node node;
    private OWLDataFactory df = OWLManager.getOWLDataFactory();

    public Reasoner(){}
    
    public boolean reasoning(OWLClassExpression C){ //capire se creare l'ontologia qua dentro

        OWLIndividual x0 = df.getOWLAnonymousIndividual();
        this.node = new Node(x0);
        Set<OWLAxiom> structure = node.getStructure();
        structure.add(df.getOWLClassAssertionAxiom(C, x0));
        
        return  dfs(node);
    }

    private boolean dfs(Node node){ //ragionare sull eliminazione dei puntatori e dei nodi 

        boolean isAppliedRule = false;
        Node newNode = null;

        Set<OWLAxiom> structure = node.getStructure();
        Set<OWLAxiom> structureTmp = new TreeSet<>(structure);

        OWLClassExpression classExpression = null;
        OWLIndividual x1;

        // applica AND esaustivamente 
        do{    
            isAppliedRule = false;
            for (OWLAxiom abox : structure){
                if (abox instanceof OWLClassAssertionAxiom){ 
                    classExpression = ((OWLClassAssertionAxiom) abox).getClassExpression();

                    if(classExpression instanceof OWLObjectIntersectionOf){
                        if(handleIntersectionOf(classExpression, node, structureTmp)){
                            isAppliedRule = true;
                        }
                    } 
                }
            }
            node.setStructure(new TreeSet<OWLAxiom>(structureTmp)); 
            structure = node.getStructure();

        }while (isAppliedRule);  

        // applica OR esaustivamente 
        for (OWLAxiom abox : structure){
            if (abox instanceof OWLClassAssertionAxiom){ 
                classExpression = ((OWLClassAssertionAxiom) abox).getClassExpression();

                if (classExpression instanceof OWLObjectUnionOf) {
                    newNode = new Node(node.getIndividual());
                    newNode.setStructure(new TreeSet<OWLAxiom>(structure));
                    isAppliedRule = handleUnionOf(classExpression, node, newNode);
                }
            }

            /* 
                se la regola non viene applicata viene valutata la prossima abox; 
                terminato il ciclo si valutano le applicazioni delle regole esistenziali ed universali
            */
            if (isAppliedRule){
                node.setSxPtr(newNode);
                //se la nuovstruttura non è clash-free o la chiamata a sx ritorna false si analizza il ramo dx
                if (!isClashFree(newNode.getStructure()) || !dfs(newNode)){ 
                    // viene ripresa la struttura non contenente il primo disgiunto
                    // structureTmp = new TreeSet<OWLAxiom>(structure); non serve più structureTmp è la struttura iniziale
                    newNode = new Node(node.getIndividual()); 
                    newNode.setStructure(new TreeSet<OWLAxiom>(structure));

                    //la regola è sempre applicata in quanto si aggiunge alla struttura l'altro disgiunto
                    isAppliedRule = handleUnionOf(classExpression, node, newNode); 

                    if (isClashFree(newNode.getStructure())){ 
                        node.setDxPtr(newNode);
                        return dfs(newNode);
                    } else {
                        return false;
                    }                     
                } else {
                    return true;
                }
            }
        }

        if(isClashFree(structure)){
            // applica ESISTENZIALE
            do{    
                for (OWLAxiom abox : structure){
                    if (abox instanceof OWLClassAssertionAxiom){ 
                        classExpression = ((OWLClassAssertionAxiom) abox).getClassExpression();
        
                        if (classExpression instanceof OWLObjectSomeValuesFrom) {
                            x1 = df.getOWLAnonymousIndividual();
                            newNode = new Node(x1); //non è detto che viene utilizzato, anzi se non va a buon fine l'applicazione della regola è creato invano
                            newNode.setStructure(new TreeSet<OWLAxiom>());
                            structureTmp = new TreeSet<OWLAxiom>(structure); //si lavora con una struttura d'appogio perche il ruolo non puo essere inserito sulla struttura sulla quale si itera
                            // structureTmp non servirebbe in quanto basterebbe indicare a "handleAllValuesFrom" semplicemente qual è la proprietà da tenere in considerazione
                            isAppliedRule = handleSomeValuesFrom(classExpression, node, newNode, structureTmp);
                        }
                    }

                    if (isAppliedRule){ 
                        // applica UNIVERSALE esaustivamente
                        for (OWLAxiom abox2 : structure) {
                            if (abox2 instanceof OWLClassAssertionAxiom){ 
                                classExpression = ((OWLClassAssertionAxiom) abox2).getClassExpression();

                                if (classExpression instanceof OWLObjectAllValuesFrom){
                                    handleAllValuesFrom(classExpression, node, newNode, structureTmp);
                                }
                            }
                        }
                        
                        if (isClashFree(newNode.getStructure())){ 
                            node.setSxPtr(newNode);

                            if(!dfs(newNode)){
                                return false;
                            }
                            isAppliedRule = false;
                        } else {
                            return false;
                        }
                    }
                }
                node.setStructure(new TreeSet<OWLAxiom>(structureTmp)); //capire se structureTmp genera problemi nel complesso funzionamento (?)
                structure = node.getStructure();

            } while (isAppliedRule); 
        } else {
            return false;
        }
        
        return true;
    } 

    private boolean handleAllValuesFrom(OWLClassExpression axiom, Node node, Node newNode, Set<OWLAxiom> structureTmp){

        int strSize = newNode.getStructure().size();

        axiom.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectAllValuesFrom avf) {

                OWLClassExpression filler = avf.getFiller();
                OWLObjectPropertyExpression property = avf.getProperty();
                OWLObjectPropertyAssertionAxiom propertyAxiom = df.getOWLObjectPropertyAssertionAxiom(property, node.getIndividual(), newNode.getIndividual());
                Set<OWLAxiom> newStructure = newNode.getStructure();
                
                if (structureTmp.contains(propertyAxiom)){
                    OWLClassAssertionAxiom abox = df.getOWLClassAssertionAxiom(filler, newNode.getIndividual());
                    if(!newStructure.contains(abox)){ //inutile, essendo un set non verrebbe aggiunto se già ci fosse
                        newStructure.add(abox);
                    }
                }
            }
        });

        return strSize < newNode.getStructure().size();
    }

    private boolean handleSomeValuesFrom(OWLClassExpression classExpression, Node node, Node newNode, Set<OWLAxiom> structureTmp) {
        
        int strSize = newNode.getStructure().size();

        classExpression.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectSomeValuesFrom svf) {

                OWLClassExpression filler = svf.getFiller();
                OWLObjectPropertyExpression property = svf.getProperty();
                OWLObjectPropertyAssertionAxiom propertyAxiom = df.getOWLObjectPropertyAssertionAxiom(property, node.getIndividual(), newNode.getIndividual());
                Set<OWLAxiom> structure = node.getStructure();
                Set<OWLAxiom> newStructure = newNode.getStructure();

                if(!structure.contains(propertyAxiom)){
                    structureTmp.add(propertyAxiom);
                    OWLClassAssertionAxiom abox = df.getOWLClassAssertionAxiom(filler, newNode.getIndividual());
                    newStructure.add(abox);
                }
            }
        });

        return strSize < newNode.getStructure().size();
    }

    private boolean handleIntersectionOf (OWLClassExpression classExpression, Node node, Set<OWLAxiom> structure){
        
        int strSize = structure.size();

        classExpression.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectIntersectionOf oi) {
                for (OWLClassExpression ce : oi.getOperandsAsList()) {
                    OWLClassAssertionAxiom abox = df.getOWLClassAssertionAxiom(ce, node.getIndividual());

                    if (!structure.contains(abox)){
                        structure.add(abox);
                    }
                }
            }
        });

        return strSize < structure.size();
    }

    private boolean handleUnionOf (OWLClassExpression classExpression, Node node, Node newNode){ 

        classExpression.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectUnionOf ou) {
                boolean flag = false;
                OWLClassAssertionAxiom abox;

                OWLClassExpression secondDisj = ou.getOperandsAsList().get(1);
                OWLClassAssertionAxiom secondDisjAx = df.getOWLClassAssertionAxiom(secondDisj, newNode.getIndividual());

                // quando termina la chiamata ricorsiva a sx e si risale, bisogna aggiungere il secondo disgiunto e scendere a dx
                flag = false;

                for (OWLClassExpression disjunct : ou.getOperandsAsList()) {

                    Set<OWLAxiom> newStructure = newNode.getStructure();
                    abox = df.getOWLClassAssertionAxiom(disjunct, newNode.getIndividual());

                    if (!newStructure.contains(abox)){ 
                        /*
                            La seconda condizione serve quando ripassando su una formula (P or B) e provenendo
                            da B già selezionato, P non deve essere messo nella struttura.
                        */
                        if ((node.getSxPtr()==null || flag) && !newStructure.contains(secondDisjAx)){
                            newStructure.add(abox);
                            break;
                        } else {
                            flag = true;
                        }
                        
                    } else {                         
                        break;
                    }
                }
            }
        });

        return node.getStructure().size() < newNode.getStructure().size();
    }

    private boolean isClashFree(Set<OWLAxiom> structure){
        OWLClassExpression complementClassExpression, classExpression;

        for (OWLAxiom abox: structure){
            if (abox instanceof OWLClassAssertionAxiom){ 
                classExpression = ((OWLClassAssertionAxiom) abox).getClassExpression();

                if(classExpression instanceof OWLClass){
                    complementClassExpression = classExpression.getObjectComplementOf();

                    for(OWLAxiom abox2: structure){
                        if (abox2 instanceof OWLClassAssertionAxiom){ 
                            classExpression = ((OWLClassAssertionAxiom) abox2).getClassExpression();
                            if(classExpression.equals(complementClassExpression)){
                                return false;
                            }
                        }
                    }
                }
            } 
        }
        return true;
    }
}