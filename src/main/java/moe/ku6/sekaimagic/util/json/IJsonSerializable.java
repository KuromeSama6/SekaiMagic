package moe.ku6.sekaimagic.util.json;

import com.google.gson.JsonElement;

public interface IJsonSerializable {
    default JsonElement Serialize() {
//        return JsonWrapper.GetBuilder().create().toJsonTree(this);
        JsonWrapper ret = new JsonWrapper();
        SerializeInternal(ret);
        return ret.getJson();
    }

    default void SerializeInternal(JsonWrapper data) {
        JsonWrapper current = new JsonWrapper(JsonWrapper.GetBuilder().create().toJsonTree(this));
        data.Merge("", current);
    }
}
