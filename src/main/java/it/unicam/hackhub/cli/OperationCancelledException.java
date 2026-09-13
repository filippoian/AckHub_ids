package it.unicam.hackhub.cli;

public class OperationCancelledException extends RuntimeException {
    public OperationCancelledException() {
        super("Operation cancelled");
    }
}
