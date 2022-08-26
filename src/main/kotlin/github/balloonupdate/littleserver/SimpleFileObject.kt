package github.balloonupdate.littleserver

import com.alibaba.fastjson2.JSONObject

abstract class AbstractSimpleFileObject(
    open var name: String
) {
    abstract fun toJson(): JSONObject
}

class SimpleFileObject(
    name: String,
    var length: Long,
    var hash: String,
    var modified: Long,
) : AbstractSimpleFileObject(name) {
    override fun toJson(): JSONObject {
        val obj = JSONObject()

        obj.put("name", name)
        obj.put("length", length)
        obj.put("hash", hash)
        obj.put("modified", modified)

        return obj
    }
}

class SimpleDirectoryObject(
    name: String,
    var children: ArrayList<AbstractSimpleFileObject>,
) : AbstractSimpleFileObject(name) {
    override fun toJson(): JSONObject
    {
        val obj = JSONObject()
        obj.put("name", name)

        val cs = mutableListOf<JSONObject>()
        for (child in children)
            cs += child.toJson()

        obj.put("children", cs)

        return obj
    }
}