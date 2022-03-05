# Inference Engine for satisfiability in ALC

For this academic project it has been required to build an inference engine based on the Tableau method in order to implement the satisfiability of a concept in ALC. In details, the engine must check the satisfiability in respect with a not empty TBox T, using some specific methods:

1. Lazy unfolding 
2. Blocking technique 

The first one is required for speeding up the computation. The latter is necessary to preserve the decidability of the reasoning, otherwise lost because of the not empty TBox T. The input is:

| Concept | TBox |
| :---: | :---: |
| C | T |

They are managed with a specific serialization, in particular T loaded from a file, C read and loaded from a GUI through Manchester syntax. The tableau resulting from the query is graphically represented in RDF format (Turtle notation) and visualized with a specific graph visualization program (graphviz). The passed time is returned too.
