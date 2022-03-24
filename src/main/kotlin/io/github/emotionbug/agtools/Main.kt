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
    val file = File("/home/emotionbug/IdeaProjects/agensgraph-import-graphml/largeFile.graphml")

    val connurl = "jdbc:postgresql://127.0.0.1:5432/postgres"
    val user = "postgres"
    val password = "agensgraph"
    val pgjdbc = DriverManager.getConnection(connurl, user, password)
    pgjdbc.autoCommit = false

    val graphMLReader = XmlGraphMLReader(pgjdbc, "test")
    graphMLReader.parseXML(BufferedReader(FileReader(file)))
    pgjdbc.close()
}