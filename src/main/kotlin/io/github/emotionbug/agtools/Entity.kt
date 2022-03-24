package io.github.emotionbug.agtools

import net.minidev.json.JSONObject

const val DEFAULT_VERTEX_LABEL = "default_label_v"
const val DEFAULT_EDGE_LABEL = "default_edge_v"

abstract class Entity {
    val property: JSONObject = JSONObject()
    fun setProperty(name: String?, value: Any?) {
        property[name] = value
    }
}

class Node : Entity() {
    var id: String? = null
    var label: String? = null

    fun addLabel(label: String) {
        if (label == "vertex") {
            this.label = DEFAULT_VERTEX_LABEL
        } else
            this.label = label
    }
}

class Relationship(val fromId: String, val toId: String, var relationshipType: String) : Entity()