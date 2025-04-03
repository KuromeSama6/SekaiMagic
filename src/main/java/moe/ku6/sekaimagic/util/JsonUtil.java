package moe.ku6.sekaimagic.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import moe.ku6.sekaimagic.util.json.JsonWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Slf4j
@UtilityClass
public class JsonUtil {
    public static JsonWrapper Read(File file) {
        if (!file.exists() || file.length() == 0) return new JsonWrapper(new JsonObject());

        try (FileReader reader = new FileReader(file)) {
            return new JsonWrapper(JsonWrapper.GetBuilder().create().fromJson(reader, JsonObject.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonWrapper UpdateAndWrite(File file, InputStream res) {
        return UpdateAndWrite(file, new JsonWrapper(JsonWrapper.GetBuilder().create().fromJson(Util.ReadToStringAutoClose(res), JsonObject.class)));
    }

    public static JsonWrapper UpdateAndWrite(File file, JsonWrapper def) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                Files.createFile(file.toPath());
            }

            JsonWrapper current = Read(file);
            UpdateJson(current, def);
            current.Save(file);
            return current;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void UpdateJson(JsonWrapper current, JsonWrapper def) {
        for (String key : def.GetKeys()) {
            JsonElement ele = current.Get(key);
            JsonElement defEle = def.Get(key);

            if (ele == null) current.Set(key, defEle);
            else if (defEle.isJsonObject() && ele.isJsonObject()) UpdateJson(new JsonWrapper(ele.getAsJsonObject()), new JsonWrapper(defEle.getAsJsonObject()));
        }
    }

}
