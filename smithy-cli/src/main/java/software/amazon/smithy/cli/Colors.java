/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.cli;

/**
 * Provides an abstraction for printing with ANSI colors if it is supported.
 */
public enum Colors {
    BLACK(30),
    RED(31),
    BOLD_RED(31, true),
    GREEN(32),
    BOLD_GREEN(32, true),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),
    BRIGHT_BLACK(90),
    BRIGHT_RED(91),
    BRIGHT_BOLD_RED(91, true),
    BRIGHT_GREEN(92),
    BRIGHT_BOLD_GREEN(92, true),
    BRIGHT_YELLOW(93),
    BRIGHT_BOLD_YELLOW(93, true),
    BRIGHT_BLUE(94),
    BRIGHT_MAGENTA(95),
    BRIGHT_CYAN(96),
    BRIGHT_WHITE(97);

    /** Configures whether or not to use ANSI colors. */
    private static boolean useAnsiColors = useAnsi();

    private int escape;
    private boolean bold;

    Colors(int escape) {
        this(escape, false);
    }

    Colors(int escape, boolean bold) {
        this.escape = escape;
        this.bold = bold;
    }

    /**
     * Explicitly configures whether or not to use ANSI colors.
     *
     * @param useAnsiColors Set to true or false to enable/disable.
     */
    public static void setUseAnsiColors(boolean useAnsiColors) {
        Colors.useAnsiColors = useAnsiColors;
    }

    /**
     * Does a really simple check to see if ANSI colors are supported.
     *
     * @return Returns true if ANSI probably works.
     */
    private static boolean useAnsi() {
        return System.console() != null && System.getenv().get("TERM") != null;
    }

    /**
     * Prints to stdout using the provided color if ANSI colors are enabled.
     *
     * @param color ANSI color to print with.
     * @param message Message to print.
     */
    public static void out(Colors color, String message) {
        if (useAnsiColors) {
            System.out.println(format(color, message));
        } else {
            System.out.println(message);
        }
    }

    /**
     * Prints to stderr using the provided color if ANSI colors are enabled.
     *
     * @param color ANSI color to print with.
     * @param message Message to print.
     */
    public static void err(Colors color, String message) {
        if (useAnsiColors) {
            System.err.println(format(color, message));
        } else {
            System.err.println(message);
        }
    }

    private static String format(Colors color, String message) {
        String colored = String.format("\u001b[%dm%s\u001b[0m", color.escape, message);
        return color.bold ? String.format("\033[1m%s\033[0m", colored) : colored;
    }
}
