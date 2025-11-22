package fr.vortex.parser.builders;

import fr.vortex.parser.VortexParser;

public record MiniMessageBuilder(VortexParserBuilder parent) {

    public MiniMessageBuilder strict(boolean strict) {
        parent.setMiniMessageStrict(strict);
        return this;
    }

    public MiniMessageBuilder stripUnknownTags(boolean strip) {
        parent.setStripUnknownTags(strip);
        return this;
    }

    public LegacyBuilder Legacy() {
        return parent.legacy();
    }

    public VortexParser build() {
        return parent.build();
    }
}
