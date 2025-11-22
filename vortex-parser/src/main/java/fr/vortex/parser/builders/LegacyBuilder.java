package fr.vortex.parser.builders;

import fr.vortex.parser.VortexParser;

public record LegacyBuilder(VortexParserBuilder parent) {

    public LegacyBuilder legacyChar(String c) {
        if (c != null && !c.isEmpty()) {
            parent.setLegacyChar(c.charAt(0));
        }
        return this;
    }

    public LegacyBuilder defaultColor(String color) {
        parent.setLegacyDefaultColor(color);
        return this;
    }

    public LegacyBuilder stripUnknownCodes(boolean strip) {
        parent.setStripUnknownLegacy(strip);
        return this;
    }

    public MiniMessageBuilder MiniMessage() {
        return parent.miniMessage();
    }

    public VortexParser build() {
        return parent.build();
    }
}
