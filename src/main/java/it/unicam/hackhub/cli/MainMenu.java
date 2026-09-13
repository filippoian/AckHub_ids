package it.unicam.hackhub.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MainMenu {
    private final CommandRegistry commandRegistry;
    private final Scanner scanner;
    private final SessionContext sessionContext;

    public MainMenu(CommandRegistry commandRegistry, Scanner scanner, SessionContext sessionContext) {
        this.commandRegistry = commandRegistry;
        this.scanner = scanner;
        this.sessionContext = sessionContext;
    }

    // Loop principale della CLI: help, dispatch comandi e gestione errori utente.
    public void run() {
        System.out.println("HackHub CLI avviata. Digita 'help' per i comandi, 'exit' per uscire.");
        while (true) {
            System.out.print(prompt());
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            if ("exit".equalsIgnoreCase(input) || "quit".equalsIgnoreCase(input)) {
                System.out.println("Chiusura applicazione.");
                return;
            }
            if ("help".equalsIgnoreCase(input)) {
                List<String> visibleCommands = new ArrayList<>();
                visibleCommands.add("help");
                visibleCommands.add("exit");
                visibleCommands.addAll(commandRegistry.getVisibleCommands(sessionContext));
                System.out.println("Comandi disponibili: " + String.join(", ", visibleCommands));
                continue;
            }

            if (commandRegistry.find(input).isEmpty()) {
                System.out.println("Comando non riconosciuto. Digita 'help'.");
                continue;
            }
            commandRegistry.findVisible(input, sessionContext).ifPresentOrElse(
                    command -> {
                        try {
                            command.execute();
                        } catch (RuntimeException ex) {
                            System.out.println("Errore comando: " + ex.getMessage());
                        }
                    },
                    () -> System.out.println("Comando non disponibile per il profilo corrente.")
            );
        }
    }

    // Cambia prompt in base al profilo attualmente autenticato.
    private String prompt() {
        if (sessionContext.isUserLoggedIn()) {
            return "hackhub(user)> ";
        }
        if (sessionContext.isStaffLoggedIn()) {
            return "hackhub(staff)> ";
        }
        return "hackhub(guest)> ";
    }
}
