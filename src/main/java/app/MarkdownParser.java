package app;

public class MarkdownParser {

    public static String parse(String input) {
        if (input == null) return "";

        String output = input;

        // Headings
        output = output.replaceAll("^## (.*)", "SUB: $1");
        output = output.replaceAll("^# (.*)", "TITLE: $1");

        // Bold
        output = output.replaceAll("\\*\\*(.*?)\\*\\*", "[BOLD]$1[/BOLD]");

        // Italic
        output = output.replaceAll("\\*(.*?)\\*", "[ITALIC]$1[/ITALIC]");

        return output;
    }
}
