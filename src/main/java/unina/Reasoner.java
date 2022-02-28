package unina;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.util.Pair;
import java.util.*;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;


public class Reasoner {
    
    private Node node;
    private OWLDataFactory df = OWLManager.getOWLDataFactory();
    private List<OWLAxiom> tbox = null;
    private OWLClassExpression tboxInConcept = null;
    private List<OWLAxiom> Tu;
    private boolean usingLazyUnfolding = false;

    // ----------------------------------------------------------- reasoning ----------------------------------------------------------- //

    public boolean reasoning(OWLClassExpression C){
        OWLClassExpression translatedTbox = null;
        OWLIndividual x0 = df.getOWLAnonymousIndividual();
        this.node = new Node(x0);

        Set<OWLAxiom> structure = node.getStructure();
        structure.add(df.getOWLClassAssertionAxiom(C, x0));

        if(usingLazyUnfolding){
            Pair<List<OWLAxiom>, List<OWLAxiom>> tboxLazyUnfolding = lazyUnfoldingPartitioning(tbox); 
            List<OWLAxiom> Tg = tboxLazyUnfolding.getValue();
            List<OWLAxiom> Tu = tboxLazyUnfolding.getKey();
            this.Tu = Tu;

            if(Tg != null && !Tg.isEmpty()){
                OWLClassExpression translatedTg = fromTBoxToConcept(Tg);
                System.out.println("\nTg: \n" + translatedTg + "\n");      
                structure.add(df.getOWLClassAssertionAxiom(translatedTg, x0));
                
            } 

        } else {
            if(tbox != null && !tbox.isEmpty()) {
                translatedTbox = fromTBoxToConcept(tbox);
                System.out.println("\nTBOX: \n" + translatedTbox + "\n");      
                this.tboxInConcept = translatedTbox;
                structure.add(df.getOWLClassAssertionAxiom(translatedTbox, x0));
            }
        }
        
        return dfs(node);
    }

    private boolean dfs(Node node){ //ragionare sull eliminazione dei puntatori e dei nodi 
        if(node.isBlocked()) {
            return true;
        }

        boolean isAppliedRule = false;
        Node newNode = null;

        Set<OWLAxiom> structure = node.getStructure();
        Set<OWLAxiom> structureTmp;

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

        // applica OR esaustivamente 
        for (OWLAxiom axiom : structure) {
            if (axiom instanceof OWLClassAssertionAxiom){ 
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();

                if (classExpression instanceof OWLObjectUnionOf) {
                    newNode = new Node(node.getIndividual());
                    newNode.setStructure(new TreeSet<OWLAxiom>(structure));

                    individual = ((OWLClassAssertionAxiom) axiom).getIndividual();
                    isAppliedRule = handleUnionOf(classExpression, individual, node, newNode);
                }
            }

            /* 
                se la regola non viene applicata viene valutata il prossimo assioma; 
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
                    isAppliedRule = handleUnionOf(classExpression, individual, node, newNode); 

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

        // applica ESISTENZIALE
        if(isClashFree(structure)) { 
            structureTmp = new TreeSet<OWLAxiom>(structure);

            for (OWLAxiom firstAxiom : structureTmp) {
                if (firstAxiom instanceof OWLClassAssertionAxiom) { 
                    classExpression = ((OWLClassAssertionAxiom) firstAxiom).getClassExpression();
    
                    if (classExpression instanceof OWLObjectSomeValuesFrom) {
                        newNode = new Node(df.getOWLAnonymousIndividual()); //non è detto che viene utilizzato, anzi se non va a buon fine l'applicazione della regola è creato invano
                        newNode.setStructure(new TreeSet<OWLAxiom>(structure)); // il nuovo nodo porta con se i concetti del padre

                        individual = ((OWLClassAssertionAxiom) firstAxiom).getIndividual();
                        // structureTmp non servirebbe in quanto basterebbe indicare a "handleAllValuesFrom" semplicemente qual è la proprietà da tenere in considerazione
                        isAppliedRule = handleSomeValuesFrom(classExpression, individual, node, newNode);
                    }
                }

                if (isAppliedRule){
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
                        node.setSxPtr(newNode);
                        setIfBlocked(newNode);
                        if(!dfs(newNode)){
                            return false;
                        }
                        isAppliedRule = false;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
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
                        newStructure.add(classAssertion); // essendo un set non viene aggiunto se già presente
                    }
                }
            });
            return oldSize < newNode.getStructure().size();
        }
        return false;
    }

    private boolean handleSomeValuesFrom(OWLClassExpression classExpression, OWLIndividual individual, Node node, Node newNode) {
        if(individual.equals(node.getIndividual())) {
            int strSize = newNode.getStructure().size();
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
                        // la regola dell'esistenziale è stata applicata e la tbox è vuota: la si aggiunge nel nuovo individuo
                        OWLClassAssertionAxiom tboxAss = df.getOWLClassAssertionAxiom(tboxInConcept, newNode.getIndividual());
                        newStructure.add(tboxAss);
                    }
                    
                }
            });
            return strSize < newNode.getStructure().size();
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

                    // quando termina la chiamata ricorsiva a sx e si risale, bisogna aggiungere il secondo disgiunto e scendere a dx
                    flag = false;

                    for (OWLClassExpression disjunct : ou.getOperandsAsList()) {
                        Set<OWLAxiom> newStructure = newNode.getStructure();
                        classAssertion = df.getOWLClassAssertionAxiom(disjunct, newNode.getIndividual());

                        if (!newStructure.contains(classAssertion)){ 
                            /*
                                La seconda condizione serve quando ripassando su una formula (P or B) e provenendo
                                da B già selezionato, P non deve essere messo nella struttura.
                            */
                            if ((node.getSxPtr() == null || flag) && !newStructure.contains(secondDisjAx)){
                                newStructure.add(classAssertion);
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
        return false;
    }

    private boolean isClashFree(Set<OWLAxiom> structure) {
        OWLClassExpression complementClassExpression, classExpression;
        OWLClassAssertionAxiom classAssertion;
        OWLIndividual x, y;

        for (OWLAxiom axiom: structure){
            if (axiom instanceof OWLClassAssertionAxiom) { 
                classAssertion = (OWLClassAssertionAxiom) axiom;
                classExpression = classAssertion.getClassExpression();

                if(classExpression instanceof OWLClass) {
                    if(classExpression.isOWLNothing()) {
                        return false;
                    }

                    x = classAssertion.getIndividual();
                    complementClassExpression = classExpression.getObjectComplementOf();

                    for(OWLAxiom axiom2: structure){
                        if (axiom2 instanceof OWLClassAssertionAxiom) { 
                            y = ((OWLClassAssertionAxiom) axiom2).getIndividual();
                            if(y.equals(x)) {
                                classExpression = ((OWLClassAssertionAxiom) axiom2).getClassExpression();
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
        if(tboxInConcept != null) {
            Node parentNode = node.getParent();
            Set<OWLAxiom> parentStructure = parentNode.getStructure();
            Set<OWLAxiom> structure = node.getStructure();

            OWLClassAssertionAxiom classAssertion, parentClassAssertion;
            OWLIndividual individual;
            OWLClassExpression ce;

            boolean blocked = true;

            for(OWLAxiom axiom: structure) {
                if(axiom instanceof OWLClassAssertionAxiom) {
                    classAssertion = (OWLClassAssertionAxiom) axiom;
                    ce = classAssertion.getClassExpression();
                    individual = classAssertion.getIndividual();

                    if(individual.equals(node.getIndividual())) {
                        parentClassAssertion = df.getOWLClassAssertionAxiom(ce, parentNode.getIndividual());
                        if(!parentStructure.contains(parentClassAssertion)) {
                            blocked = false;
                            break;
                        }
                    }
                }
            }
            node.setBlocked(blocked);
        }
    }

    // ----------------------------------------------------------- lazy unfolding ----------------------------------------------------------- //

    public void activeLazyUnfolding() {
        this.usingLazyUnfolding = true;
    }

    private Pair<List<OWLAxiom>, List<OWLAxiom>> lazyUnfoldingPartitioning(List<OWLAxiom> tbox){

        List<OWLAxiom> Tu = new LinkedList<>();
        List<OWLAxiom> Tg = new LinkedList<>();

        OWLEquivalentClassesAxiom equivClassAx;
        OWLSubClassOfAxiom subClassAx, modifiedSubClassAx;

        // presupponiamo che la tbox sia formata solo da OWLEquivalentClassesAxiom e OWLSubClassOfAxiom
        
        for(OWLAxiom axiom : tbox){ // pesca a caso o un OWLEquivalentClassesAxiom o un OWLSubClassOfAxiom
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
            }
        }

        return new Pair<List<OWLAxiom>, List<OWLAxiom>>(Tu, Tg);
    }

    private OWLSubClassOfAxiom transformSubClassAx(OWLSubClassOfAxiom subClassAx){

        /*  
         *  il metodo trasforma l'assioma di inclusione in modo tale che
         *  nel LHS sia presente solo un concetto atomico. La verifica
         *  che ci siano le condizioni per farlo viene fatta nel metodo
         *  'isUnfoldableAddingSubClass'
         */
        
        OWLClassExpression A = subClassAx.getSubClass();
        OWLClassExpression C = subClassAx.getSuperClass();

        List<OWLSubClassOfAxiom> ret = new LinkedList<>();

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
                if(newA != null){ //lo sarà sempre
                    conjunctSet.remove(newA);
                    operand = df.getOWLObjectIntersectionOf(conjunctSet);  
                    operand = operand.getComplementNNF();
                    superClass = df.getOWLObjectUnionOf(Stream.of(operand, C));
                    newSubClassAx = df.getOWLSubClassOfAxiom(newA, superClass);

                    ret.add(newSubClassAx);
                }
            }
        });
        return ret.get(0);
    }

    private boolean isUnfoldableAddingSubClass(List<OWLAxiom> Tu, OWLSubClassOfAxiom subClassAx) {
        
        OWLClassExpression A = null;
        List<Boolean> ret = new LinkedList<>();
        ret.add(false);
        A = subClassAx.getSubClass();


        /* 
         * verifica che A sia un concetto atomico oppure del tipo A ∩ C (con C concetto complesso) 
         * trasformando l'inclusione in modo da avere il LHS formato da un solo concetto atomico A
         * e spostando a destra i restanti congiunti C
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
                            ret.set(0, true);
                        }
                    }
                }  
            });
            if(ret.get(0)){
                return true;
            }
        }
        return false;
    }

    private boolean isUnfoldableAddingEquivalentClass(List<OWLAxiom> Tu, OWLEquivalentClassesAxiom equivClassAx) {

        /* 
         * il metodo si aspetta che il LHS di un EquoivalentClassesAxiom sia un concetto atomico 
         * il caso contrario, l'assioma non viene considerato
         */


        List<OWLClassExpression> equivParts = equivClassAx.classExpressions().collect(Collectors.toList());
        OWLClassExpression A = equivParts.get(0); // si assume che A sia di tipo OWLClass
        OWLClassExpression C = equivParts.get(1); // C può essere un ER.C ? 
        
        List<Boolean> ret = new LinkedList<>();
        ret.add(true);


        if (A instanceof OWLClass){
            // controlla che l'assioma non sia ciclico
            isCyclicalConcept(Tu, (OWLClass) A, C, ret);

            if(!ret.get(0)){
                return false;
            }
            return checkCompatibilityWithGCI(Tu, (OWLClass) A);  

        } else {
            return false;
        }      
    }

    private boolean checkCompatibilityWithGCI(List<OWLAxiom> Tu, OWLClass A){

        OWLSubClassOfAxiom subClassAx;
        OWLEquivalentClassesAxiom equivClassAx;
        OWLClassExpression P;
        
        // controlla che A non compaia a sinistra di nessun’altra GCI di Tu
        for(OWLAxiom axiom : Tu){
            // controllo inclusioni  (Tu viene forzato ad avere una sola inclusione possibile per non avere ambiguità nell'applicazione delle regole)
            if (axiom instanceof OWLSubClassOfAxiom){
                subClassAx = (OWLSubClassOfAxiom) axiom;
                P = subClassAx.getSubClass();
                if(A.equals(P)){  // bisogna valutare anche il complemento ??
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

    private void isCyclicalConcept(List<OWLAxiom> Tu, OWLClass A, OWLClassExpression C, List<Boolean> ret) {

        /* 
         * verifica che nessun concetto sia definito direttamente 
         * o indirettamente in termini di se stesso.
         * 
         * PS. non tiene conto dei fake cicle, cioè casi in cui il concetto 
         *     è sintatticamente ciclico, ma non semanticamente.
         */

        if(C instanceof OWLClass){
            if (C.equals(A)){ //vale anche se c'è il complemento ???
                ret.set(0, false);
            } 
        }
        
        if(C instanceof OWLObjectIntersectionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    for (OWLClassExpression ce : oi.getOperandsAsList()) {
                        isCyclicalConcept(Tu, A, ce, ret);
                        if(!ret.get(0)){
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
                        isCyclicalConcept(Tu, A, ce, ret);
                        if(!ret.get(0)){
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
                    isCyclicalConcept(Tu, A, svf.getFiller(), ret);
                }
            });
        }
        if(C instanceof OWLObjectAllValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectAllValuesFrom avf) {
                    isCyclicalConcept(Tu, A, avf.getFiller(), ret);
                }
            });
        }
    }

    private boolean applyLazyUnfoldingRule(OWLClassAssertionAxiom classAssertion, Node node){
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
                        if(axiom instanceof OWLSubClassOfAxiom) {
                            subClassAx = (OWLSubClassOfAxiom) axiom;
                            firstClass = subClassAx.getSubClass();
                            secondClass = subClassAx.getSuperClass();

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