package it.unicam.hackhub.cli;

import java.util.Scanner;

public class InputHelper {
    private static final String CANCEL_TOKEN = "\\";
    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(withCancelHint(prompt));
        String input = scanner.nextLine();
        if (input.trim().equals(CANCEL_TOKEN)) {
            throw new OperationCancelledException();
        }
        return input;
    }

    public String readNonBlank(String prompt) {
        while (true) {
            String input = readLine(prompt);
            if (!input.trim().isEmpty()) {
                return input;
            }
            System.out.println("Valore non valido.");
        }
    }

    public long readLong(String prompt) {
        while (true) {
            String raw = readLine(prompt).trim();
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ex) {
                System.out.println("Valore numerico non valido.");
            }
        }
    }

    private String withCancelHint(String prompt) {
        return prompt + " (\\): ";
    }
}
