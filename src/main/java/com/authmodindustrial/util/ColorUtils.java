package com.authmodindustrial.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses color-coded strings into Minecraft {@link Component} objects.
 *
 * <p>Supported formats:
 * <ul>
 *   <li>{@code &#RRGGBB} – 24-bit hex color</li>
 *   <li>{@code &0-9a-f} – legacy color codes</li>
 *   <li>{@code &k l m n o} – formatting (obfuscated, bold, strikethrough, underline, italic)</li>
 *   <li>{@code &r} – reset all formatting</li>
 *   <li>{@code \n} – line break (produces a newline component)</li>
 * </ul>
 */
public final class ColorUtils {

    private static final Pattern CODE_PATTERN =
            Pattern.compile("(&#[A-Fa-f0-9]{6}|&[0-9a-fklmnorA-FKLMNOR])");

    private ColorUtils() {}

    /**
     * Parses a string with color codes and returns a {@link Component}.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // Split lines first so \n becomes actual newlines in chat
        String[] lines = text.split("\\\\n|\\n");
        if (lines.length == 1) {
            return parseSingle(text.replace("\\n", "\n"));
        }

        MutableComponent result = Component.empty();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result = result.append(Component.literal("\n"));
            }
            result = result.append(parseSingle(lines[i]));
        }
        return result;
    }

    private static MutableComponent parseSingle(String text) {
        MutableComponent result = Component.empty();
        Matcher matcher = CODE_PATTERN.matcher(text);

        Style currentStyle = Style.EMPTY;
        int lastEnd = 0;

        List<String> segments = new ArrayList<>();
        List<Style> styles = new ArrayList<>();

        while (matcher.find()) {
            String before = text.substring(lastEnd, matcher.start());
            if (!before.isEmpty()) {
                segments.add(before);
                styles.add(currentStyle);
            }

            String code = matcher.group();
            if (code.startsWith("&#")) {
                int rgb = Integer.parseInt(code.substring(2), 16);
                currentStyle = currentStyle.withColor(TextColor.fromRgb(rgb));
            } else {
                char c = Character.toLowerCase(code.charAt(1));
                currentStyle = applyLegacy(currentStyle, c);
            }

            lastEnd = matcher.end();
        }

        // Remaining text after last code
        String remaining = text.substring(lastEnd);
        if (!remaining.isEmpty()) {
            segments.add(remaining);
            styles.add(currentStyle);
        }

        for (int i = 0; i < segments.size(); i++) {
            result = result.append(Component.literal(segments.get(i)).withStyle(styles.get(i)));
        }

        return result;
    }

    private static Style applyLegacy(Style style, char code) {
        return switch (code) {
            case '0' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.BLACK));
            case '1' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_BLUE));
            case '2' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_GREEN));
            case '3' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA));
            case '4' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_RED));
            case '5' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_PURPLE));
            case '6' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.GOLD));
            case '7' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case '8' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY));
            case '9' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.BLUE));
            case 'a' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case 'b' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.AQUA));
            case 'c' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED));
            case 'd' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
            case 'e' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.YELLOW));
            case 'f' -> style.withColor(TextColor.fromLegacyFormat(ChatFormatting.WHITE));
            case 'k' -> style.withObfuscated(true);
            case 'l' -> style.withBold(true);
            case 'm' -> style.withStrikethrough(true);
            case 'n' -> style.withUnderlined(true);
            case 'o' -> style.withItalic(true);
            case 'r' -> Style.EMPTY;
            default  -> style;
        };
    }
}
