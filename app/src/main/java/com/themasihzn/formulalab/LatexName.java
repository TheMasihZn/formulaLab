package com.themasihzn.formulalab;

import java.util.regex.Pattern;

/**
 * Turns a file name into a LaTeX line.
 *
 * The whole file name is the formula. Only these are removed or replaced:
 *  - a trailing full-word suffix (.latex, .tex, .formula, .reading, .txt),
 *    plus any version numbers after it (fridge.latex.3 -> fridge)
 *  - stand-in characters for the two that file systems may refuse:
 *      ⧵ (U+29F5)  ->  \   backslash
 *      ∕ (U+2215)  ->  /   slash (a real "/" can never be in a file name)
 */
public final class LatexName {

    private static final Pattern SUFFIX =
            Pattern.compile("\\.(latex|tex|formula|reading|txt)(\\.\\d+)*$", Pattern.CASE_INSENSITIVE);

    private LatexName() {}

    public static String fromFileName(String name) {
        if (name == null) return "";
        String n = name;
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);
        n = SUFFIX.matcher(n).replaceFirst("");
        n = n.replace('\u29F5', '\\').replace('\u2215', '/');
        return n;
    }
}
