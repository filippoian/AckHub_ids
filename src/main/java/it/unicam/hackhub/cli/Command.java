package it.unicam.hackhub.cli;

public interface Command {
    String name();
    void execute();
}
