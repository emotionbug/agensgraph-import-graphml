```shell
./bin/gremlin.sh
graph = OrientGraph.open("remote:localhost/test", "root", "root");
graph.io(IoCore.graphml()).writeGraph("test.xml");
java -jar ./importgraphml.jar <GRAPH NAME> <FILE NAME of GRAPHML> <URL OF AGENSGRAPH> <USERNAME OF AGENSGRAPH> <PASSWORD OF AGENSGRAPH>
# java -jar ./importgraphml.jar new_graph ~/test.xml jdbc:postgresql://127.0.0.1:5432/postgres postgres postgres
```