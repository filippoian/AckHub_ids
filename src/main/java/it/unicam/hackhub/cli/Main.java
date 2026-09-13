package it.unicam.hackhub.cli;

public class Main {
    public static void main(String[] args) {
        String[] options = java.util.Arrays.copyOf(args, args.length + 1);
        options[args.length] = "--hackhub.cli.enabled=true";
        it.unicam.hackhub.HackhubApplication.main(options);
    }
}
