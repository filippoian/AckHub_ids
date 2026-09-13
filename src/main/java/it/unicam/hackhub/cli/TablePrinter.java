package it.unicam.hackhub.cli;

import java.util.ArrayList;
import java.util.List;

public final class TablePrinter {
    private TablePrinter() {
    }

    public static void print(List<String> headers, List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("Nessun elemento.");
            return;
        }
        if (headers == null || headers.isEmpty()) {
            throw new IllegalArgumentException("Headers are required");
        }

        int columnCount = headers.size();
        int[] widths = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            widths[i] = cell(headers.get(i)).length();
        }

        List<List<String>> normalizedRows = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> normalized = new ArrayList<>(columnCount);
            for (int i = 0; i < columnCount; i++) {
                String value = "";
                if (row != null && i < row.size()) {
                    value = cell(row.get(i));
                }
                normalized.add(value);
                widths[i] = Math.max(widths[i], value.length());
            }
            normalizedRows.add(normalized);
        }

        printRow(toCells(headers), widths);
        printSeparator(widths);
        for (List<String> row : normalizedRows) {
            printRow(row, widths);
        }
    }

    public static String truncate(String value, int max) {
        if (value == null) {
            return "-";
        }
        if (max <= 0) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        if (max <= 3) {
            return normalized.substring(0, max);
        }
        return normalized.substring(0, max - 3) + "...";
    }

    private static List<String> toCells(List<String> values) {
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            result.add(cell(value));
        }
        return result;
    }

    private static void printRow(List<String> values, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) {
                line.append(" | ");
            }
            line.append(padRight(values.get(i), widths[i]));
        }
        System.out.println(line);
    }

    private static void printSeparator(int[] widths) {
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            if (i > 0) {
                separator.append("-+-");
            }
            separator.append("-".repeat(widths[i]));
        }
        System.out.println(separator);
    }

    private static String padRight(String value, int width) {
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    private static String cell(String value) {
        return value == null ? "-" : value;
    }
}
