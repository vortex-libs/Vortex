package fr.vortex.parser.builders;

import fr.vortex.parser.VortexParser;
import fr.vortex.parser.VortexParserPlaceholder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VortexParserBuilder {

    private char legacyChar = '§';
    private String legacyDefaultColor = "WHITE";
    private boolean stripUnknownLegacy = true;

    private boolean miniMessageStrict = false;
    private boolean stripUnknownTags = true;

    private final List<VortexParserPlaceholder> placeholders = new ArrayList<>();

    public LegacyBuilder legacy() {
        return new LegacyBuilder(this);
    }

    public MiniMessageBuilder miniMessage() {
        return new MiniMessageBuilder(this);
    }

    public VortexParserBuilder addPlaceholder(VortexParserPlaceholder placeholder) {
        if (placeholder != null) placeholders.add(placeholder);
        return this;
    }

    public VortexParser build() {
        VortexParser parser = new VortexParser(
                legacyChar, legacyDefaultColor, stripUnknownLegacy,
                miniMessageStrict, stripUnknownTags, new ArrayList<>(placeholders)
        );

        placeholders.forEach(parser::addPlaceholder);

        return parser;
    }
}
