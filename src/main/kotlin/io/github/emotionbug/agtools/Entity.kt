package io.github.emotionbug.agtools

import net.minidev.json.JSONObject

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
        this.label = label
    }
}

class Relationship(val fromId: String, val toId: String, val relationshipType: String) : Entity()