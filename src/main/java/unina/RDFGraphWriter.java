package unina;

import java.io.*;
import java.util.*;

import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.semanticweb.owlapi.model.*;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.*;
import static guru.nidi.graphviz.model.Factory.*;


public class RDFGraphWriter {
    private Model model;
    private String namespace;
    private HashMap<Node, MutableNode> graphNodes = new HashMap<>();
    private MutableGraph graph; 

    // ----------------------------------------------------------- rdf ----------------------------------------------------------- //

    public void initRDF() {
        model = ModelFactory.createDefaultModel();
        namespace = "http://example.org/";
        model.setNsPrefix("ex", namespace);
    }

    public void addRDFTriple(Node node, String propName, Object obj) {
        Resource x, y;
        Property prop;

        Node child;

        String labels;
        String nodePrefix = namespace + "node#";

        x = model.createResource(nodePrefix + node.getId());

        if(obj instanceof Node) {
            child = (Node) obj;

            y = model.createResource(nodePrefix + child.getId());
            prop = model.createProperty(namespace, propName);
            x.addProperty(prop, y);

        } else if(obj instanceof String) {
            labels = (String) obj;

            prop = model.createProperty(namespace, propName);
            x.addProperty(prop, labels);
        }

    }

    public void renderRDF(String filePath) {
        try {
            FileOutputStream fileout = new FileOutputStream(filePath);
            RDFDataMgr.write(fileout, model, Lang.TURTLE);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }   
    }

    // ----------------------------------------------------------- graph ----------------------------------------------------------- //

    public void initGraph(Node node){
        MutableNode n = mutNode(node.getId().toString()); 
        graphNodes.put(node, n);
        
        graph = mutGraph().setDirected(true);
        graph.add(n);  
    }

    public void writeOnGraph(Node node, Node child, String rule) {
        MutableNode mutableNode = graphNodes.get(node);
        child.setId(node.getId()+1);

        MutableNode mutChild = mutNode(child.getId().toString());
        
        mutableNode.addLink(to(mutChild).with(Label.of(" " + rule)));
        graphNodes.put(child, mutChild);
    }

    public String getLabel(Set<OWLAxiom> set){
        OWLClassExpression classExpression;
        String label = null;

        for(OWLAxiom axiom : set){
            if (axiom instanceof OWLClassAssertionAxiom){ 
                classExpression = ((OWLClassAssertionAxiom) axiom).getClassExpression();
                Container<String> labelContainer = new Container<>("");
                createLabel(classExpression, labelContainer);

                if(label == null){
                    label = labelContainer.getValue();
                } else {
                    label = label + "\n" + labelContainer.getValue();

                }
            }
        }
        return label;
     }

    private void createLabel(OWLClassExpression C, Container<String> labelContainer) {
        /* 
         *
         */

        String label = null, conceptName = null; 

        if(C instanceof OWLClass){
            label = labelContainer.getValue();
            conceptName = getConceptName((OWLClass) C);
            labelContainer.setValue(label + conceptName);
        } else if (C instanceof OWLObjectComplementOf){
            label = labelContainer.getValue();
            OWLClassExpression ce = ((OWLObjectComplementOf) C).getOperand();

            if(ce instanceof OWLClass){
                conceptName = getConceptName((OWLClass) ce);                
                labelContainer.setValue(label + "¬" + conceptName);
            } else {
                Container<String> manageComplementOf =  new Container<>("");
                createLabel(C,manageComplementOf);
                labelContainer.setValue(label + "¬(" + manageComplementOf.getValue() + ")");   
            }
        }

        
        if(C instanceof OWLObjectIntersectionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectIntersectionOf oi) {
                    String label = labelContainer.getValue() + "(";
                    labelContainer.setValue(label);

                    OWLClassExpression ceSx =  oi.getOperandsAsList().get(0);
                    createLabel(ceSx, labelContainer);
                    label = labelContainer.getValue() + " ⊓ "; 
                    labelContainer.setValue(label);

                    OWLClassExpression ceDx =  oi.getOperandsAsList().get(1);
                    createLabel(ceDx, labelContainer);
                    label = labelContainer.getValue() + ")"; 
                    labelContainer.setValue(label);
                }
            });
        }
        if(C instanceof OWLObjectUnionOf){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectUnionOf ou) {
                    String label = labelContainer.getValue() + "(";
                    labelContainer.setValue(label);

                    OWLClassExpression ceSx =  ou.getOperandsAsList().get(0);
                    createLabel(ceSx, labelContainer);
                    label = labelContainer.getValue() + " ⊔ "; 
                    labelContainer.setValue(label);

                    OWLClassExpression ceDx =  ou.getOperandsAsList().get(1);
                    createLabel(ceDx, labelContainer);
                    label = labelContainer.getValue() + ")";
                    labelContainer.setValue(label);
                }
            });
        }
        if(C instanceof OWLObjectSomeValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectSomeValuesFrom svf) {
                    String propertyName = getPropertyName(svf.getProperty());

                    String label = labelContainer.getValue() + " ∃" + propertyName + ".";
                    labelContainer.setValue(label);

                    createLabel(svf.getFiller(), labelContainer);
                    label = labelContainer.getValue() + " "; 
                    labelContainer.setValue(label);

                }
            });
        }
        if(C instanceof OWLObjectAllValuesFrom){
            C.accept(new OWLClassExpressionVisitor() {
                @Override
                public void visit(OWLObjectAllValuesFrom avf) {
                    String propertyName = getPropertyName(avf.getProperty());

                    String label = labelContainer.getValue() + " ∀" + propertyName + ".";
                    labelContainer.setValue(label);

                    createLabel(avf.getFiller(), labelContainer);
                    label = labelContainer.getValue() + " "; 
                    labelContainer.setValue(label);
                }
            });
        }
    }
     
    private String getConceptName(OWLClass C){
        String concept = C.toStringID();
        int hashMarkIndex = concept.indexOf("#");
        String conceptName = concept.substring(hashMarkIndex+1, hashMarkIndex+4); 

        if(conceptName.equals("Nothing")){
            conceptName = "⊥";
        } else if(conceptName.equals("Thing")){
            conceptName = "⊤";
        }
        return conceptName;
    }

    private String getPropertyName(OWLObjectPropertyExpression R){
        String property = R.toString();
        int hashMarkIndex = property.indexOf("#");
        String propertyName = property.substring(hashMarkIndex+1, hashMarkIndex+4); //property.length()-1
        return propertyName;
    }

    public void setNodeLabel(Node parent, Node node){
        MutableNode n = graphNodes.get(node);
        Set<OWLAxiom> nodeStructure = node.getStructure();
        Set<OWLAxiom> axiomDifference = new TreeSet<>(nodeStructure);

        if(parent != null){
            Set<OWLAxiom> parentStructure = parent.getStructure();
            axiomDifference.removeAll(parentStructure);
        }

        String nodeLabel = getLabel(axiomDifference);
        System.out.println("id : " + node.getId());
        System.out.println(nodeLabel + "\n\n");

        MutableNode labelNode = mutNode(nodeLabel).add(Shape.RECTANGLE); 
        n.addLink(to(labelNode).with(Style.DASHED));
    }

    public void renderGraph(String filePath) {
        try {
            Graphviz.fromGraph(graph).width(10000).render(Format.PNG).toFile(new File(filePath));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void setNodeColor(Node newNode, String color) {
        if(color.equals("red")){
            graphNodes.get(newNode).add(Color.RED);
        } else if(color.equals("green")){
            graphNodes.get(newNode).add(Color.GREEN);
        }
    }
}
