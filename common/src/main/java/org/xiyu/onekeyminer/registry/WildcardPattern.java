package org.xiyu.onekeyminer.registry;

import java.util.Objects;
import java.util.regex.Pattern;

/** Converts the two supported glob tokens without exposing regex syntax. */
final class WildcardPattern {
    private WildcardPattern() {
    }

    static boolean matches(String candidate, String glob) {
        return compile(glob).matcher(Objects.requireNonNull(candidate, "candidate")).matches();
    }

    static Pattern compile(String glob) {
        Objects.requireNonNull(glob, "glob");
        StringBuilder regex = new StringBuilder(glob.length() + 8);
        StringBuilder literal = new StringBuilder();
        for (int index = 0; index < glob.length(); index++) {
            char character = glob.charAt(index);
            if (character == '*' || character == '?') {
                appendLiteral(regex, literal);
                regex.append(character == '*' ? ".*" : ".");
            } else {
                literal.append(character);
            }
        }
        appendLiteral(regex, literal);
        return Pattern.compile(regex.toString());
    }

    private static void appendLiteral(StringBuilder regex, StringBuilder literal) {
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }
}
