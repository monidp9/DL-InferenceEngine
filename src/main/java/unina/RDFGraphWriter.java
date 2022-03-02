package unina;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;


public class RDFGraphWriter {
    private Model model;
    private String namespace;

    public RDFGraphWriter() {
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

        x = model.createResource(nodePrefix + node.getIndividual()); // ID DEL NODO

        if(obj instanceof Node) {
            child = (Node) obj;

            y = model.createResource(nodePrefix + child.getIndividual()); // ID DEL NODO
            prop = model.createProperty(namespace, propName);
            x.addProperty(prop, y);

        } else if(obj instanceof String) {
            labels = (String) obj;

            prop = model.createProperty(namespace, propName);
            x.addProperty(prop, labels);
        }

    }

    public void saveRDF(String filePath) {
        try {
            FileOutputStream fileout = new FileOutputStream(filePath);
            RDFDataMgr.write(fileout, model, Lang.TURTLE);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        }   
    }
}
