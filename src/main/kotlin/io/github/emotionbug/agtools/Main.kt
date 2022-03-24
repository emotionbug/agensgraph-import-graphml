package io.github.emotionbug.agtools

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.sql.DriverManager

// -- 16615 ag_graph
// select * from pg_depend;

// select * from pg_depend where objid = 16615;
// DELETE FROM pg_depend where objid = 16615;

// select * from pg_class;

fun main(args: Array<String>) {
    Class.forName("org.postgresql.Driver")
    if (args.size != 5) {
        println("Help\n")
        println("java -jar ./importgraphml.jar <GRAPH NAME> <FILE NAME of GRAPHML> <URL OF AGENSGRAPH> <USERNAME OF AGENSGRAPH> <PASSWORD OF AGENSGRAPH>\n")
    }
    val graphname = args[0]
    val file = File(args[1])
    if (!file.exists()) {
        println("Input file is not exists")
        return
    }

    val connurl = args[2]
    val user = args[3]
    val password = args[4]

    val pgjdbc = DriverManager.getConnection(connurl, user, password)
    pgjdbc.autoCommit = false

    val graphMLReader = XmlGraphMLReader(pgjdbc, graphname)
    graphMLReader.parseXML(BufferedReader(FileReader(file)))
    pgjdbc.close()
}