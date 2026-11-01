package compiler.semantic.type;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecordType extends SemanticType {

    private final String name;
    private final Map<String, SemanticType> fields = new LinkedHashMap<>();

    public RecordType(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void addField(String fieldName, SemanticType type) {
        fields.putIfAbsent(fieldName, type);
    }

    public SemanticType fieldType(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, SemanticType> fields() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String toString() {
        return name != null ? name : "record";
    }
}
