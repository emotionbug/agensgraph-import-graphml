package io.github.emotionbug.agtools

import java.io.Reader
import java.sql.Connection
import java.util.*
import java.util.function.Function
import javax.xml.namespace.QName
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

/**
 * Created by mh on 10.07.13.
 */

class XmlGraphMLReader(db: Connection) {
    private val db: Connection
    private var defaultRelType: String = "UNKNOWN"
    private var labels = false

    init {
        this.db = db
    }

    fun nodeLabels(readLabels: Boolean): XmlGraphMLReader {
        labels = readLabels
        return this
    }

    internal enum class Type {
        BOOLEAN {
            override fun parse(value: String?): Any {
                return java.lang.Boolean.valueOf(value)
            }

            override fun parseList(value: String?): Any {
                return parseList(
                    value,
                    Boolean::class.java
                ) { i: Any -> i as Boolean }
            }
        },
        INT {
            override fun parse(value: String?): Any {
                return value?.toInt() ?: 0
            }

            override fun parseList(value: String?): Any {
                return parseList(
                    value,
                    Int::class.java
                ) { n: Any -> (n as Number).toInt() }
            }
        },
        LONG {
            override fun parse(value: String?): Any {
                return value?.toLong() ?: 0
            }

            override fun parseList(value: String?): Any {
                return parseList(
                    value,
                    Long::class.java
                ) { i: Any -> (i as Number).toLong() }
            }
        },
        FLOAT {
            override fun parse(value: String?): Any {
                return value?.toFloat() ?: 0.0
            }

            override fun parseList(value: String?): Any {
                return parseList(
                    value,
                    Float::class.java
                ) { i: Any -> (i as Number).toFloat() }
            }
        },
        DOUBLE {
            override fun parse(value: String?): Any {
                return value?.toDouble() ?: 0.0
            }

            override fun parseList(value: String?): Any {
                return parseList(
                    value,
                    Double::class.java
                ) { i: Any -> (i as Number).toDouble() }
            }
        },
        STRING {
            override fun parse(value: String?): Any {
                return value!!
            }

            override fun parseList(value: String?): Any {
                return parseList(
                    value,
                    String::class.java
                ) { i: Any -> i as String }
            }
        };

        abstract fun parse(value: String?): Any?
        abstract fun parseList(value: String?): Any?

        companion object {
            fun <T> parseList(value: String?, asClass: Class<T>?, convert: Function<Any, T>): Array<T> {
                val parsed: List<*> = JsonUtils.parse(value, null, MutableList::class.java) as MutableList<T>
                val converted = java.lang.reflect.Array.newInstance(asClass, parsed.size) as Array<T>
                for (i in parsed.indices) converted[i] = convert.apply(parsed[i]!!)
                return converted
            }

            fun forType(type: String?): Type {
                return if (type == null) STRING else valueOf(type.trim { it <= ' ' }.uppercase(Locale.getDefault()))
            }
        }
    }

    internal class Key(var name: String?, type: String?, listType: String?, forNode: String?) {
        var forNode: Boolean
        var listType: Type? = null
        var type: Type
        var defaultValue: Any? = null

        init {
            this.type = Type.forType(type)
            if (listType != null) {
                this.listType = Type.forType(listType)
            }
            this.forNode = forNode == null || forNode.equals("node", ignoreCase = true)
        }

        fun setDefault(data: String?) {
            defaultValue = type.parse(data)
        }

        fun parseValue(input: String?): Any? {
            if (input == null || input.trim { it <= ' ' }.isEmpty()) return defaultValue
            return if (listType != null) listType!!.parseList(input) else type.parse(input)
        }

        companion object {
            fun defaultKey(id: String?, forNode: Boolean): Key {
                return Key(id, "string", null, if (forNode) "node" else "edge")
            }
        }
    }

    @Throws(XMLStreamException::class)
    fun parseXML(input: Reader?): Long {
        val inputFactory = XMLInputFactory.newInstance()
        inputFactory.setProperty("javax.xml.stream.isCoalescing", true)
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)
        val reader = inputFactory.createXMLEventReader(input)
        var last: Entity? = null
        val nodeKeys: MutableMap<String?, Key> = HashMap()
        val relKeys: MutableMap<String?, Key> = HashMap()
        var count = 0
        while (reader.hasNext()) {
            val event = reader.next() as XMLEvent?
            if (event!!.isStartElement) {
                val element = event.asStartElement()
                val name = element.name.localPart
                if (name == "graphml" || name == "graph") continue
                if (name == "key") {
                    val id = getAttribute(element, ID)
                    val key = Key(
                        getAttribute(element, NAME),
                        getAttribute(element, TYPE),
                        getAttribute(element, LIST),
                        getAttribute(element, FOR)
                    )
                    val next = peek(reader)
                    if (next.isStartElement && next.asStartElement().name.localPart == "default") {
                        reader.nextEvent().asStartElement()
                        key.setDefault(reader.nextEvent().asCharacters().data)
                    }
                    if (key.forNode) nodeKeys[id] = key else relKeys[id] = key
                    continue
                }
                if (name == "data") {
                    if (last == null) continue
                    val id = getAttribute(element, KEY)
                    val isNode = last is Node
                    var key =
                        if (isNode) nodeKeys[id] else relKeys[id]
                    if (key == null) key = Key.defaultKey(id, isNode)
                    var value = key.defaultValue
                    val next = peek(reader)
                    if (next.isCharacters) {
                        value = key.parseValue(reader.nextEvent().asCharacters().data)
                    }
                    if (value != null) {
                        if (labels && isNode && id == "labels") {
                            addLabels(last as Node, value.toString())
                        } else if (!labels || isNode || id != "label") {
                            last.setProperty(key.name, value)
                        }
                    } else if (next.eventType == XMLStreamConstants.END_ELEMENT) {
                        last.setProperty(key.name, EMPTY)
                    }
                    continue
                }
                if (name == "node") {
                    last?.let { createRow(it) }
                    val id = getAttribute(element, ID)!!
                    val node: Node = createNode()
                    if (labels) {
                        val labels = getAttribute(element, LABELS)
                        addLabels(node, labels)
                    }
                    node.setProperty("id", id)
                    setDefaults(nodeKeys, node)
                    last = node
                    node.id = id
                    count++
                    continue
                }
                if (name == "edge") {
                    last?.let { createRow(it) }
                    val source = getAttribute(element, SOURCE)!!
                    val target = getAttribute(element, TARGET)!!
                    val relationshipType: String = getAttribute(element, LABEL) ?: getRelationshipType(reader)
                    val relationship: Relationship = createRelationship(source, target, relationshipType)
                    setDefaults(relKeys, relationship)
                    last = relationship
                    count++
                }
            }
        }
        return count.toLong()
    }

    private fun createRow(entity: Entity) {
        if (entity is Node) {
            TODO()
        } else {
            TODO()
        }
    }

    private fun createNode(): Node {
        return Node()
    }

    private fun createRelationship(fromId: String, toId: String, relationshipType: String): Relationship {
        return Relationship(fromId, toId, relationshipType)
    }

    @Throws(XMLStreamException::class)
    private fun getRelationshipType(reader: XMLEventReader): String {
        if (labels) {
            val peek = reader.peek()
            val isChar = peek.isCharacters
            if (isChar && !peek.asCharacters().isWhiteSpace) {
                val value = peek.asCharacters().data
                val el = ":"
                val typeRel = if (value.contains(el)) value.replace(el, EMPTY) else value
                return typeRel.trim { it <= ' ' }
            }
            val notStartElementOrContainsKeyLabel = (isChar
                    || !peek.isStartElement
                    || containsLabelKey(peek))
            if (!peek.isEndDocument && notStartElementOrContainsKeyLabel) {
                reader.nextEvent()
                return getRelationshipType(reader)
            }
        }
        reader.nextEvent() // to prevent eventual wrong reader (f.e. self-closing tag)
        return defaultRelType
    }

    private fun containsLabelKey(peek: XMLEvent): Boolean {
        val keyAttribute = peek.asStartElement().getAttributeByName(QName("key"))
        return keyAttribute != null && keyAttribute.value == "label"
    }

    private fun addLabels(node: Node, labels: String?) {
        var labels = labels ?: return
        labels = labels.trim { it <= ' ' }
        if (labels.isEmpty()) return
        val parts = labels.split(LABEL_SPLIT).toTypedArray()
        for (part in parts) {
            if (part.trim { it <= ' ' }.isEmpty()) continue
            node.addLabel(part.trim { it <= ' ' })
        }
    }

    @Throws(XMLStreamException::class)
    private fun peek(reader: XMLEventReader): XMLEvent {
        val peek = reader.peek()
        if (peek.isCharacters && peek.asCharacters().isWhiteSpace) {
            reader.nextEvent()
            return peek(reader)
        }
        return peek
    }

    private fun setDefaults(keys: Map<String?, Key>, pc: Entity) {
        if (keys.isEmpty()) return
        for (key in keys.values) {
            if (key.defaultValue != null) pc.setProperty(key.name, key.defaultValue)
        }
    }

    private fun getAttribute(element: StartElement, qname: QName): String? {
        val attribute = element.getAttributeByName(qname)
        return attribute?.value
    }

    companion object {
        const val LABEL_SPLIT = " *: *"
        val ID: QName = QName.valueOf("id")!!
        val LABELS: QName = QName.valueOf("labels")!!
        val SOURCE: QName = QName.valueOf("source")!!
        val TARGET: QName = QName.valueOf("target")!!
        val LABEL: QName = QName.valueOf("label")!!
        val FOR: QName = QName.valueOf("for")!!
        val NAME: QName = QName.valueOf("attr.name")!!
        val TYPE: QName = QName.valueOf("attr.type")!!
        val LIST: QName = QName.valueOf("attr.list")!!
        val KEY: QName = QName.valueOf("key")!!

        private const val EMPTY = ""
    }
}