package fr.vortex.parser;

import fr.vortex.parser.builders.VortexParserBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

/**
 * @param legacyChar
 * @param miniMessageStrict
 */
public record VortexParser(char legacyChar, String legacyDefaultColor, boolean stripUnknownLegacy,
                           boolean miniMessageStrict, boolean stripUnknownTags, List<VortexParserPlaceholder> placeholders) {

    public static VortexParserBuilder builder() {
        return new VortexParserBuilder();
    }

    public VortexParser addPlaceholder(VortexParserPlaceholder placeholder) {
        if (placeholder != null) placeholders.add(placeholder);
        return this;
    }

    public Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.text("");

        String processed = processLegacy(text);
        processed = processMiniMessage(processed);
        processed = applyPlaceholders(processed);

        MiniMessage miniMessage = MiniMessage.builder()
                .strict(miniMessageStrict)
                .build();

        return miniMessage.deserialize(processed);
    }

    public String from(Component component) {
        if (component == null) return "";

        MiniMessage miniMessage = MiniMessage.builder()
                .strict(miniMessageStrict)
                .build();

        String text = miniMessage.serialize(component);

        text = revertLegacy(text);

        return text;
    }

    private String applyPlaceholders(String text) {
        for (VortexParserPlaceholder placeholder : placeholders) {
            String keyString = placeholder.getPlaceholderString();
            String replacement = placeholder.getReplacer().get();
            if (replacement == null) replacement = "";
            text = text.replace(keyString, replacement);
        }
        return text;
    }

    private String processLegacy(String text) {
        if (text == null || text.isEmpty()) return "";

        text = text.replace('&', legacyChar);

        if (stripUnknownLegacy) {
            text = text.replaceAll("(?i)" + legacyChar + "[^0-9A-FK-OR]", "");
        }

        if (!text.isEmpty() && text.charAt(0) != legacyChar) {
            text = legacyChar + legacyDefaultColor.charAt(0) + text;
        }

        return text;
    }

    private String processMiniMessage(String text) {
        return text;
    }

    private String revertLegacy(String text) {
        if (legacyChar != '§') {
            text = text.replace(legacyChar, '&');
        }
        return text;
    }
}