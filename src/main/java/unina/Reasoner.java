package unina;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.util.Pair;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;


public class Reasoner {
    
    private OWLDataFactory df = OWLManager.getOWLDataFactory();

    private List<OWLAxiom> Tu = null;
    private List<OWLAxiom> tbox = null; 
    private OWLClassExpression tboxInConcept = null; // o Tg in caso di lazy unfolding
    private boolean useLazyUnfolding = false;
    private RDFGraphWriter rdfGraphWriter = new RDFGraphWriter();

    // ----------------------------------------------------------- reasoning ----------------------------------------------------------- //

    public boolean reasoning(OWLClassExpression C) {
        OWLClassExpression translatedTbox = null;
        OWLIndividual x0 = df.getOWLAnonymousIndividual();
        Node node = new Node(x0);

        Set<OWLAxiom> structure = node.getStructure();
        structure.add(df.getOWLClassAssertionAxiom(C, x0));

        if(useLazyUnfolding && tbox != null && !tbox.isEmpty()){
            Pair<List<OWLAxiom>, List<OWLAxiom>> tboxLazyUnfolding = lazyUnfoldingPartitioning(tbox); 
            List<OWLAxiom> Tu = tboxLazyUnfolding.getKey();
            List<OWLAxiom> Tg = tboxLazyUnfolding.getValue();
            this.Tu = Tu;

            if(Tg != null && !Tg.isEmpty()) {
                OWLClassExpression translatedTg = fromTBoxToConcept(Tg);
                this.tboxInConcept = translatedTg;
                structure.add(df.getOWLClassAssertionAxiom(translatedTg, x0));
            }
        } else {
            translatedTbox = fromTBoxToConcept(tbox);
            this.tboxInConcept = translatedTbox;
            structure.add(df.getOWLClassAssertionAxiom(translatedTbox, x0));
        }

        rdfGraphWriter.initRDF();
        rdfGraphWriter.initGraph(node);

        boolean sat = dfs(node);

        rdfGraphWriter.renderRDF("result/tableau_rdf");
        rdfGraphWriter.renderGraph("result/tableau_graph");

        return sat;
    }

    private boolean dfs(Node node){ 
        if(node.isBlocked()) {
            System.out.println("BLOCKED");
            return true;
        }

        boolean isAppliedRule = false;
        Node newNode = null;

        Set<OWLAxiom> structure = node.getStructure();
        Set<OWLAxiom> structureTmp = null;

        OWLClassExpression classExpression = null;
        OWLIndividual individual = null;

        // applica AND esaustivamente 
        do{    
            isAppliedRule = false;
            structureTmp = new TreeSet<OWLAxiom>(structure);

            for (OWLAxiom axiom : structureTmp){
                if (axiom instanceof OWLClassAssertionAxiom){ 
                    classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                    if(classExpression instanceof OWLObjectIntersectionOf){
                        individual = ((OWLClassAssertionAxiom) axiom).getIndividual();
                        if(handleIntersectionOf(classExpression, individual, node)){
                            isAppliedRule = true;
                        }
                    } 
                }
            }
        } while (isAppliedRule);  

        // scrittura della label del nodo
        rdfGraphWriter.setNodeLabel(node.getParentOnGraph(), node, false);


        // applica OR esaustivamente 
        for (OWLAxiom axiom : structure) {
            if (axiom instanceof OWLClassAssertionAxiom){ 
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if (classExpression instanceof OWLObjectUnionOf) {
                    newNode = new Node(node.getIndividual());
                    newNode.setParentOnGraph(node);
                    newNode.setStructure(new TreeSet<OWLAxiom>(structure));

                    individual = ((OWLClassAssertionAxiom) axiom).getIndividual();
                    isAppliedRule = handleUnionOf(classExpression, individual, node, newNode);
                }
            }

            // se la regola non viene applicata viene valutato il prossimo assioma.
            if (isAppliedRule){
                node.setSx();

                rdfGraphWriter.addRDFTriple(node, "orEdge", newNode);
                rdfGraphWriter.writeOnGraph(node, newNode, "⊔");       


                if(!isClashFree(newNode.getStructure())){
                    rdfGraphWriter.setNodeColor(newNode, "red");
                    rdfGraphWriter.setNodeLabel(node, newNode, false);         
                }

                if (!isClashFree(newNode.getStructure()) || !dfs(newNode)) { 
                    // viene ripresa la struttura priva del primo disgiunto

                    
                    newNode = new Node(node.getIndividual()); 
                    newNode.setStructure(new TreeSet<OWLAxiom>(structure));
                    newNode.setParentOnGraph(node);

                    isAppliedRule = handleUnionOf(classExpression, individual, node, newNode); 
                  
                    rdfGraphWriter.addRDFTriple(node, "orEdge", newNode);
                    rdfGraphWriter.writeOnGraph(node, newNode, "⊔");  

                    if (isClashFree(newNode.getStructure())){ 
                        return dfs(newNode);
                    } else {
                        rdfGraphWriter.setNodeLabel(node, newNode, false);
                        rdfGraphWriter.setNodeColor(newNode, "red");
                        return false;
                    }                     
                } else {
                    return true;
                }
            }
        }

        // applica regole di LAZY UNFOLDING
        do {
            isAppliedRule = false;
            structureTmp = new TreeSet<OWLAxiom>(structure);

            for(OWLAxiom axiom: structureTmp) {
                if(axiom instanceof OWLClassAssertionAxiom) {
                    if(applyLazyUnfoldingRule((OWLClassAssertionAxiom) axiom, node)) {
                        isAppliedRule = true;
                    }
                }
            }
        } while(isAppliedRule);


        rdfGraphWriter.setNodeLabel(node.getParentOnGraph(), node, true);
        
        // applica ESISTENZIALE
        if(isClashFree(structure)) { 
            structureTmp = new TreeSet<OWLAxiom>(structure);

            for (OWLAxiom firstAxiom : structureTmp) {
                if (firstAxiom instanceof OWLClassAssertionAxiom) { 
                    classExpression = ((OWLClassAssertionAxiom) firstAxiom).getClassExpression();
    
                    if (classExpression instanceof OWLObjectSomeValuesFrom) {

                        // non è detto che venga sempre utilizzato; il nuovo nodo porta cin sé i concetti del padre
                        newNode = new Node(df.getOWLAnonymousIndividual());
                        newNode.setStructure(new TreeSet<OWLAxiom>(structure)); 
                        newNode.setParentOnGraph(node);

                        individual = ((OWLClassAssertionAxiom) firstAxiom).getIndividual();
                        isAppliedRule = handleSomeValuesFrom(classExpression, individual, node, newNode);
                    }
                }

                if (isAppliedRule){
                    rdfGraphWriter.addRDFTriple(node, "exEdge", newNode);
                    rdfGraphWriter.writeOnGraph(node, newNode, "∃");                

                    // applica UNIVERSALE esaustivamente
                    for (OWLAxiom secondAxiom : structureTmp) {
                        if (secondAxiom instanceof OWLClassAssertionAxiom){ 
                            classExpression = ((OWLClassAssertionAxiom) secondAxiom).getClassExpression();
                            if (classExpression instanceof OWLObjectAllValuesFrom){
                                handleAllValuesFrom(classExpression, individual, node, newNode);
                            }
                        }
                    }

                    if (isClashFree(newNode.getStructure())){ 
                        node.setSx();
                        setIfBlocked(newNode);
                        if(!dfs(newNode)){
                            return false;
                        }
                        isAppliedRule = false;
                    } else {
                        rdfGraphWriter.setNodeLabel(node, newNode, false);
                        rdfGraphWriter.setNodeColor(newNode,"red");
                        return false;
                    }
                }
            }
        } else {
            rdfGraphWriter.setNodeColor(node, "red");
            return false;        
        }

        if(!node.getSx()){
            rdfGraphWriter.setNodeColor(node, "green");
        }

        return true;
    } 

    private boolean handleAllValuesFrom(OWLClassExpression classExpression, OWLIndividual individual, Node node, Node newNode) {
        if(individual.equals(node.getIndividual())) {
            int oldSize = newNode.getStructure().size();

            classExpression.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectAllValuesFrom avf) {
                    OWLClassExpression filler = avf.getFiller();
                    OWLObjectPropertyExpression property = avf.getProperty();
                    OWLObjectPropertyAssertionAxiom propAssertion = df.getOWLObjectPropertyAssertionAxiom(property, node.getIndividual(), newNode.getIndividual());
                    
                    Set<OWLAxiom> structure = node.getStructure();
                    Set<OWLAxiom> newStructure = newNode.getStructure();
                    
                    if (structure.contains(propAssertion)) {
                        OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(filler, newNode.getIndividual());
                        newStructure.add(classAssertion); 
                    }
                }
            });
            return oldSize < newNode.getStructure().size();
        }
        return false;
    }

    private boolean handleSomeValuesFrom(OWLClassExpression classExpression, OWLIndividual individual, Node node, Node newNode) {

        if(individual.equals(node.getIndividual())) {
            int oldSize = newNode.getStructure().size();

            classExpression.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectSomeValuesFrom svf) {
                    OWLClassExpression filler = svf.getFiller();
                    OWLObjectPropertyExpression property = svf.getProperty();

                    Set<OWLAxiom> structure = node.getStructure();
                    Set<OWLAxiom> newStructure = newNode.getStructure();

                    OWLObjectPropertyAssertionAxiom propAssertion = df.getOWLObjectPropertyAssertionAxiom(property, node.getIndividual(), newNode.getIndividual());
                    OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(filler, newNode.getIndividual());

                    structure.add(propAssertion);
                    newStructure.add(propAssertion);
                    newStructure.add(classAssertion);

                    newNode.setParent(node);

                    if(tboxInConcept != null) {
                        // la regola dell'esistenziale è stata applicata e la tbox (o Tg in caso di lazy unfolding) non è vuota: la si aggiunge nel nuovo individuo
                        OWLClassAssertionAxiom tboxAss = df.getOWLClassAssertionAxiom(tboxInConcept, newNode.getIndividual());
                        newStructure.add(tboxAss);
                    }
                    
                }
            });
            return oldSize < newNode.getStructure().size();
        }
        return false;
    }

    private boolean handleIntersectionOf(OWLClassExpression classExpression, OWLIndividual individual, Node node) {
        OWLIndividual currIndividual = node.getIndividual();
        Set<OWLAxiom> structure = node.getStructure();

        if(individual.equals(currIndividual)) {
            int strSize = structure.size();
            classExpression.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    for (OWLClassExpression ce : oi.getOperandsAsList()) {
                        OWLClassAssertionAxiom classAssertion = df.getOWLClassAssertionAxiom(ce, currIndividual);

                        if (!structure.contains(classAssertion)) {  
                            structure.add(classAssertion);
                        }
                    }
                }
            });
            return strSize < structure.size();
        }
        return false;      
    }

    private boolean handleUnionOf(OWLClassExpression classExpression, OWLIndividual individual, Node node, Node newNode) { 
        if(individual.equals(node.getIndividual())) {
            classExpression.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectUnionOf ou) {
                    boolean flag = false;
                    OWLClassAssertionAxiom classAssertion;

                    OWLClassExpression secondDisj = ou.getOperandsAsList().get(1);
                    OWLClassAssertionAxiom secondDisjAx = df.getOWLClassAssertionAxiom(secondDisj, newNode.getIndividual());

                    // utile per forzare il controllo del secondo disgiunto dopo terminazione della chiamata ricorsiva a sx
                    flag = false;

                    for (OWLClassExpression disjunct : ou.getOperandsAsList()) {
                        Set<OWLAxiom> newStructure = newNode.getStructure();
                        classAssertion = df.getOWLClassAssertionAxiom(disjunct, newNode.getIndividual());

                        if (!newStructure.contains(classAssertion)){ 
                            /*
                                La seconda condizione serve quando ripassando su una formula (P or B) e provenendo
                                da B già selezionato, P non deve essere messo nella struttura.
                            */
                            if ((!node.getSx() || flag) && !newStructure.contains(secondDisjAx)){
                                newStructure.add(classAssertion);
                                break;
                            } else {
                
                                flag = true;
                            }
                        } else {      
                            /* 
                             * Non si applica la regola UnionOf quando:
                             *   - unione già risolta 
                             *   - disgiunto già presente nella struttura, quindi non sono rispettate le condizioni 
                             *     per l'applicazione della regola 
                             */                   
                            break;
                        }
                    }
                }
            });
            return node.getStructure().size() < newNode.getStructure().size();
        }
        return false;
    }

    private boolean isClashFree(Set<OWLAxiom> structure) {

        /*
         * Verifica la presenza di clash su concetti atomici. 
         */

        OWLClassExpression complementClassExpression, classExpression;
        OWLClassAssertionAxiom classAssertion;
        OWLIndividual x, y;

        for (OWLAxiom firstAxiom: structure){
            if (firstAxiom instanceof OWLClassAssertionAxiom) { 
                classAssertion = (OWLClassAssertionAxiom) firstAxiom;
                classExpression = classAssertion.getClassExpression();

                if(classExpression instanceof OWLClass) {
                    if(classExpression.isOWLNothing()) {
                        return false;
                    }

                    x = classAssertion.getIndividual();
                    complementClassExpression = classExpression.getObjectComplementOf();

                    for(OWLAxiom secondAxiom: structure){
                        if (secondAxiom instanceof OWLClassAssertionAxiom) { 
                            y = ((OWLClassAssertionAxiom) secondAxiom).getIndividual();
                            if(y.equals(x)) {
                                classExpression = ((OWLClassAssertionAxiom) secondAxiom).getClassExpression();
                                if(classExpression.equals(complementClassExpression)){
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private void setIfBlocked(Node node) {

        /*
         * Imposta il blocking di un nodo qualora la sua struttura dovesse 
         * essere contenuta in quella del padre. 
         */

        if(tboxInConcept != null) {
            Node parentNode = node.getParent();
            Set<OWLAxiom> parentStructure = parentNode.getStructure();
            Set<OWLAxiom> structure = node.getStructure();

            OWLClassAssertionAxiom classAssertion, parentClassAssertion;
            OWLIndividual individual;
            OWLClassExpression ce, parentCe;

            Set<OWLClassExpression> ceFlat = null, parentCeFlat = null;

            boolean blocked = true;

            for(OWLAxiom firstAxiom: structure) {
                if(firstAxiom instanceof OWLClassAssertionAxiom) {
                    classAssertion = (OWLClassAssertionAxiom) firstAxiom;

                    ce = classAssertion.getClassExpression();
                    individual = classAssertion.getIndividual();

                    if(individual.equals(node.getIndividual())) {
                        
                        if(ce instanceof OWLObjectIntersectionOf) {
                            ceFlat = ce.asConjunctSet();
                        } else if(ce instanceof OWLObjectUnionOf) {
                            ceFlat = ce.asDisjunctSet();
                        } else {
                            ceFlat = null;
                        }

                        if(ceFlat != null) {
                            blocked = false;
                            for(OWLAxiom secondAxiom: parentStructure) {
                                if(secondAxiom instanceof OWLClassAssertionAxiom) {
                                    parentClassAssertion = (OWLClassAssertionAxiom) secondAxiom;
                                    parentCe = parentClassAssertion.getClassExpression();
                                    
                                    if(parentCe instanceof OWLObjectIntersectionOf) {
                                        parentCeFlat = parentCe.asConjunctSet();
                                    } else if(parentCe instanceof OWLObjectUnionOf) {
                                        parentCeFlat = parentCe.asDisjunctSet();
                                    } else {
                                        parentCeFlat = null;
                                    }

                                    if(parentCeFlat != null && parentCeFlat.equals(ceFlat)) {
                                        blocked = true;
                                        break;
                                    }
                                }
                            }
                            if(!blocked) {
                                break;
                            }
                        } else {
                            parentClassAssertion = df.getOWLClassAssertionAxiom(ce, parentNode.getIndividual());
                            if(!parentStructure.contains(parentClassAssertion)) {
                                blocked = false;
                                break;
                            }
                        }

                    }
                }
            }
            node.setBlocked(blocked);
        }
    }

    // ----------------------------------------------------------- lazy unfolding ----------------------------------------------------------- //

    public void activeLazyUnfolding() {
        this.useLazyUnfolding = true;
    }

    private Pair<List<OWLAxiom>, List<OWLAxiom>> lazyUnfoldingPartitioning(List<OWLAxiom> tbox){
        /* 
         * Tale metodo partiziona la tbox andando a creare due liste di assiomi:
         *  - Tu contenente solo assiomi unfoldable
         *  - Tg contenente la restante parte di assiomi
         * 
         * Il tipo di ritorno pair contiene nell'ordine Tu e Tg.
         * 
         * La tbox presenta solo 4 tipologie di assiomi: OWLEquivalentClassesAxiom,
         * OWLSubClassOfAxiom, OWLObjectPropertyDomainAxiom, OWLObjectPropertyDomainAxiom.
         * Vengono presi in considerazione soltanto i primi due mentre gli altri saranno esclusi
         * in quanto rispettano sicuramente la precondizione di avere un concetto atomico in LHS 
         * (dopo avrli trasformati in assiomi di inclusione)
         */

        List<OWLAxiom> Tu = new LinkedList<>();
        List<OWLAxiom> Tg = new LinkedList<>();

        OWLEquivalentClassesAxiom equivClassAx;
        OWLSubClassOfAxiom subClassAx, modifiedSubClassAx;
        
        for(OWLAxiom axiom : tbox){ 
            if (axiom instanceof OWLEquivalentClassesAxiom){
                equivClassAx = (OWLEquivalentClassesAxiom) axiom;

                if(isUnfoldableAddingEquivalentClass(Tu, equivClassAx)){
                    Tu.add(equivClassAx);
                } else {
                    Tg.add(equivClassAx);
                } 
            } else if (axiom instanceof OWLSubClassOfAxiom){
                subClassAx = (OWLSubClassOfAxiom) axiom;

                if(isUnfoldableAddingSubClass(Tu, subClassAx)){

                    if(subClassAx instanceof OWLObjectIntersectionOf){
                        modifiedSubClassAx = transformSubClassAx(subClassAx);
                        Tu.add(modifiedSubClassAx);
                    } else {
                        Tu.add(subClassAx);
                    }
                } else {
                    Tg.add(subClassAx);
                } 
            } else {
                Tg.add(axiom);
            }
        }

        return new Pair<List<OWLAxiom>, List<OWLAxiom>>(Tu, Tg);
    }

    private OWLSubClassOfAxiom transformSubClassAx(OWLSubClassOfAxiom subClassAx){

        /*  
         *  Il metodo trasforma l'assioma di inclusione in modo tale che
         *  nel LHS sia presente solo un concetto atomico. La verifica
         *  che ci siano le condizioni per farlo viene fatta nel metodo
         *  'isUnfoldableAddingSubClass'
         */
        
        OWLClassExpression A = subClassAx.getSubClass();
        OWLClassExpression C = subClassAx.getSuperClass();

        Container<OWLSubClassOfAxiom> ret = new Container<>();

        A.accept(new OWLClassExpressionVisitor() {
            @Override
            public void visit(OWLObjectIntersectionOf oi) {
                Set<OWLClassExpression> conjunctSet =  oi.asConjunctSet();
                OWLClassExpression superClass, operand = null, newA = null;;
                OWLSubClassOfAxiom newSubClassAx = null;

                for (OWLClassExpression ce : conjunctSet){
                    if (ce instanceof OWLClass){
                        newA = ce; 
                        break;
                    }
                }
                //lo sarà sempre perché il controllo viene fatto in 'isUnfoldableAddingSubClass'
                if(newA != null){ 
                    conjunctSet.remove(newA);
                    operand = df.getOWLObjectIntersectionOf(conjunctSet);  
                    operand = operand.getComplementNNF();
                    superClass = df.getOWLObjectUnionOf(Stream.of(operand, C));
                    newSubClassAx = df.getOWLSubClassOfAxiom(newA, superClass);

                    ret.setValue(newSubClassAx);
                }
            }
        });
        return ret.getValue();
    }

    private boolean isUnfoldableAddingSubClass(List<OWLAxiom> Tu, OWLSubClassOfAxiom subClassAx) {
        
        OWLClassExpression A = null;
        Container<Boolean> ret = new Container<>(false);
        A = subClassAx.getSubClass();

        /* 
         * Verifica che A o sia un concetto atomico oppure del tipo A ∩ C (con C che può essere 
         * un concetto complesso) trasformando l'inclusione in modo da avere il LHS formato da un 
         * solo concetto atomico A e spostando a destra i restanti congiunti C
        */
        if(A instanceof OWLClass){
            return checkCompatibilityWithGCI(Tu, (OWLClass) A);    

        } else if(A instanceof OWLObjectIntersectionOf){
            A.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    Set<OWLClassExpression> conjunctSet =  oi.asConjunctSet(); 
                    for (OWLClassExpression ce : conjunctSet){
                        if (ce instanceof OWLClass){
                            ret.setValue(true);
                        }
                    }
                }  
            });
            if(ret.getValue()){
                return true;
            }
        }
        return false;
    }

    private boolean isUnfoldableAddingEquivalentClass(List<OWLAxiom> Tu, OWLEquivalentClassesAxiom equivClassAx) {

        /* 
         * Il metodo si aspetta che il LHS di un EquivalentClassesAxiom sia un concetto atomico (OWLClass)
         * In caso contrario, l'assioma non viene considerato.
         */

        List<OWLClassExpression> equivParts = equivClassAx.classExpressions().collect(Collectors.toList());
        OWLClassExpression A = equivParts.get(0); 
        OWLClassExpression C = equivParts.get(1); 
        
        Container<Boolean> ret = new Container<>(true);

        if (A instanceof OWLClass){

            isAcyclicalConcept(Tu, (OWLClass) A, C, ret);

            if(!ret.getValue()){
                return false;
            }
            return checkCompatibilityWithGCI(Tu, (OWLClass) A);  

        } else {
            return false;
        }      
    }

    private boolean checkCompatibilityWithGCI(List<OWLAxiom> Tu, OWLClass A){

        /*
         * Il metodo controlla che il concetto atomico A non compaia a
         * sinistra di nessuna altra GCI in Tu.
         * In Tu ci può essere solo un assioma di inclusione che nel LHS ha
         * un certo concetto atomico P. 
         */
        
        OWLSubClassOfAxiom subClassAx;
        OWLEquivalentClassesAxiom equivClassAx;
        OWLClassExpression P;
        
        for(OWLAxiom axiom : Tu){
            // controllo inclusioni 
            if (axiom instanceof OWLSubClassOfAxiom){
                subClassAx = (OWLSubClassOfAxiom) axiom;
                P = subClassAx.getSubClass();
                if(A.equals(P)){  
                    return false;
                }
            }
            // controllo equivalenze
            if (axiom instanceof OWLEquivalentClassesAxiom){
                equivClassAx = (OWLEquivalentClassesAxiom) axiom;
                List<OWLClassExpression> equivParts = equivClassAx.classExpressions().collect(Collectors.toList());
                P = equivParts.get(0); 

                if (A.equals(P)){
                    return false;
                }
            }
        }
        return true;
    }

    private void isAcyclicalConcept(List<OWLAxiom> Tu, OWLClass A, OWLClassExpression C, Container<Boolean> ret) {

        /* 
         * Verifica che nessun concetto sia definito direttamente o indirettamente in termini di se stesso.
         * 
         * PS. non tiene conto dei fake cicle, cioè casi in cui il concetto è sintatticamente ciclico, 
         * ma non semanticamente.
         */

        if(C instanceof OWLClass){
            if (C.equals(A)){ 
                ret.setValue(false);
            } else {
                for (OWLAxiom axiom: Tu){
                    if(axiom instanceof OWLEquivalentClassesAxiom){
                        OWLEquivalentClassesAxiom equivClassAx = (OWLEquivalentClassesAxiom) axiom; 
                        List<OWLClassExpression> equivParts = equivClassAx.classExpressions().collect(Collectors.toList());

                        OWLClassExpression lhsEquiv = equivParts.get(0);

                        if(C.equals(lhsEquiv)){
                            OWLClassExpression rhsEquiv = equivParts.get(1);
                            isAcyclicalConcept(Tu, A, rhsEquiv, ret);
                        }
                    }
                }
            }
        }
        
        if(C instanceof OWLObjectIntersectionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    for (OWLClassExpression ce : oi.getOperandsAsList()) {
                        isAcyclicalConcept(Tu, A, ce, ret);
                        if(!ret.getValue()){
                            break;
                        }
                    }
                }
            });
        }
        if(C instanceof OWLObjectUnionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectUnionOf ou) {
                    for (OWLClassExpression ce : ou.getOperandsAsList()) {
                        isAcyclicalConcept(Tu, A, ce, ret);
                        if(!ret.getValue()){
                            break;
                        }
                    }
                }
            });
        }
        if(C instanceof OWLObjectSomeValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectSomeValuesFrom svf) {
                    isAcyclicalConcept(Tu, A, svf.getFiller(), ret);
                }
            });
        }
        if(C instanceof OWLObjectAllValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectAllValuesFrom avf) {
                    isAcyclicalConcept(Tu, A, avf.getFiller(), ret);
                }
            });
        }
    }

    private boolean applyLazyUnfoldingRule(OWLClassAssertionAxiom classAssertion, Node node){

        /*
         * Applica le regole di Lazy Unfolding a partire da un'asserzione. 
         */

        if(Tu != null && !Tu.isEmpty()) {
            OWLClassExpression firstClass = null, secondClass = null;
            OWLClassExpression ce = classAssertion.getClassExpression();
            
            OWLObjectComplementOf complement = null;

            OWLIndividual x, y;
            y = classAssertion.getIndividual();
            x = node.getIndividual();

            OWLEquivalentClassesAxiom equivAx;
            OWLSubClassOfAxiom subClassAx;
            OWLClassAssertionAxiom newClassAssertion;

            Set<OWLAxiom> structure = node.getStructure();
            int oldSize = structure.size();

            if(x.equals(y)) {
                if(ce instanceof OWLClass) {

                    for(OWLAxiom axiom: Tu) {
                        if(axiom instanceof OWLEquivalentClassesAxiom) {
                            equivAx = (OWLEquivalentClassesAxiom) axiom;

                            List<OWLClassExpression> l = equivAx.classExpressions().collect(Collectors.toList());
                            firstClass = l.get(0);
                            secondClass = l.get(1);

                            if(firstClass.equals(ce)) {
                                secondClass = secondClass.getNNF();
                                newClassAssertion = df.getOWLClassAssertionAxiom(secondClass, x);
                                structure.add(newClassAssertion);
                                break;
                            }

                        } else if(axiom instanceof OWLSubClassOfAxiom) {
                            subClassAx = (OWLSubClassOfAxiom) axiom;

                            firstClass = subClassAx.getSubClass();
                            secondClass = subClassAx.getSuperClass();

                            if(firstClass.equals(ce)) {
                                secondClass = secondClass.getNNF();
                                newClassAssertion = df.getOWLClassAssertionAxiom(secondClass, x);
                                structure.add(newClassAssertion);
                                break;
                            }
                        }
                    }

                } else if(ce instanceof OWLObjectComplementOf) {
                    
                    complement = (OWLObjectComplementOf) ce;
                    ce = complement.getOperand();

                    for(OWLAxiom axiom: Tu) {
                        if(axiom instanceof OWLEquivalentClassesAxiom) {
                            equivAx = (OWLEquivalentClassesAxiom) axiom;

                            List<OWLClassExpression> l = equivAx.classExpressions().collect(Collectors.toList());
                            firstClass = l.get(0);
                            secondClass = l.get(1);

                            if(firstClass.equals(ce)) {
                                secondClass = secondClass.getComplementNNF();
                                newClassAssertion = df.getOWLClassAssertionAxiom(secondClass, x);
                                structure.add(newClassAssertion);
                                break;
                            }
                        }
                    }
                    
                }
            }
            return oldSize < structure.size();
        }
        return false;
    }

    // ----------------------------------------------------------- tbox management ----------------------------------------------------------- //

    public void setTbox(List<OWLAxiom> tbox){
        this.tbox = tbox;
    }

    private OWLClassExpression fromTBoxToConcept(List<OWLAxiom> tbox) {

        /*
        * Trasforma Tbox in ingresso in un concetto. 
        */

        OWLSubClassOfAxiom subClassAx;
        OWLEquivalentClassesAxiom equivClassAx;

        OWLClassExpression subClass, superClass, concept = null, operand = null;
        OWLClassExpression gciConj = null, equivConj = null, domRangeConj = null;


        for(OWLAxiom ax: tbox) {
            // gestione delle GCI
            if(ax instanceof OWLSubClassOfAxiom) {
                subClassAx = (OWLSubClassOfAxiom) ax;

                subClass = subClassAx.getSubClass();
                superClass = subClassAx.getSuperClass();
                
                subClass = subClass.getComplementNNF();
                operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                if(gciConj != null) {
                    gciConj = df.getOWLObjectIntersectionOf(Stream.of(gciConj, operand));
                } else {
                    gciConj = operand;
                }
            }

            // gestione delle equivalenze
            if(ax instanceof OWLEquivalentClassesAxiom) {
                equivClassAx = (OWLEquivalentClassesAxiom) ax;

                for(OWLSubClassOfAxiom sca: equivClassAx.asOWLSubClassOfAxioms()) {
                    
                    subClass = sca.getSubClass();
                    superClass = sca.getSuperClass();

                    subClass = subClass.getComplementNNF();
                    operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                    if(equivConj != null) {
                        equivConj = df.getOWLObjectIntersectionOf(Stream.of(equivConj, operand));
                    } else {
                        equivConj = operand;
                    }
                }
            }

            // gestione dominio di un ruolo
            if(ax instanceof OWLObjectPropertyDomainAxiom) {
                OWLObjectPropertyDomainAxiom domainAx = (OWLObjectPropertyDomainAxiom) ax;
                subClassAx = domainAx.asOWLSubClassOfAxiom();

                subClass = subClassAx.getSubClass();
                superClass = subClassAx.getSuperClass();

                subClass = subClass.getComplementNNF();
                operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                if(domRangeConj != null) {
                    domRangeConj = df.getOWLObjectIntersectionOf(Stream.of(domRangeConj, operand));
                } else {
                    domRangeConj = operand;
                }
            }

            // gestione codominio di un ruolo
            if(ax instanceof OWLObjectPropertyRangeAxiom) {
                OWLObjectPropertyRangeAxiom rangeAx = (OWLObjectPropertyRangeAxiom) ax;
                OWLObjectPropertyExpression prop = rangeAx.getProperty();
    
                subClass = df.getOWLObjectSomeValuesFrom(prop, df.getOWLThing());
                subClass = subClass.getComplementNNF();

                superClass = df.getOWLObjectAllValuesFrom(prop, rangeAx.getRange());

                operand = df.getOWLObjectUnionOf(Stream.of(subClass, superClass));

                if(domRangeConj != null) {
                    domRangeConj = df.getOWLObjectIntersectionOf(Stream.of(domRangeConj, operand));
                } else {
                    domRangeConj = operand;
                }
            }
        }
        Stream<OWLClassExpression> operands = Stream.of(gciConj, equivConj, domRangeConj);
        concept = df.getOWLObjectIntersectionOf(operands.filter(Objects::nonNull));

        return concept;
    }
}