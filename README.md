# Inference Engine for satisfability in ALC

For this academic project it has been required to build an inference engine based on the Tableau method in order to implement the satisfability of a concept in ALC. In details, the engine must check the satisfability in respect with a not empty TBox T, using some specific methods:

1. Lazy unfolding 
2. Blocking technique 

The first one is required for speeding up the computation. The latter is necessary to preserve the decidability of the inference engine, otherwise lost because of the non empty TBox T. The input is:

| Concept | TBox |
| :---: | :---: |
| C | T |

They are loaded with a specific serialization, in particular T loaded from a file, C readed and loaded from prompt (written in Manchester syntax). The tableau query result is graphically represented in RDF format and visualized with a specific graph visualization program. The passed time is returned too.
