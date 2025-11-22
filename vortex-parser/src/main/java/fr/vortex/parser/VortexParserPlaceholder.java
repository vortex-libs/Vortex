package fr.vortex.parser;

import lombok.Getter;

import java.util.function.Supplier;

@Getter
public class VortexParserPlaceholder {

    private final String key;
    private final Supplier<String> replacer;

    private String entryReplacer = "<";
    private String finalReplacer = ">";

    private VortexParserPlaceholder(String key, Supplier<String> replacer) {
        this.key = key;
        this.replacer = replacer;
    }

    public static VortexParserPlaceholder create(String key, Supplier<String> replacer) {
        return new VortexParserPlaceholder(key, replacer);
    }

    public VortexParserPlaceholder setEntryReplacer(String entry) {
        if (entry != null && !entry.isEmpty()) this.entryReplacer = entry;
        return this;
    }

    public VortexParserPlaceholder setFinalReplacer(String fin) {
        if (fin != null && !fin.isEmpty()) this.finalReplacer = fin;
        return this;
    }

    public String getPlaceholderString() {
        return entryReplacer + key + finalReplacer;
    }
}
