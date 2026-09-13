package it.unicam.hackhub.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "hackhub.cli", name = "enabled", havingValue = "true")
public class CliRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(CliRunner.class);

    private final SpringCliBootstrap springCliBootstrap;

    public CliRunner(SpringCliBootstrap springCliBootstrap) {
        this.springCliBootstrap = springCliBootstrap;
    }

    @Override
    // Avvia la CLI in thread separato solo in ambiente interattivo.
    public void run(ApplicationArguments args) {
        if (System.console() == null) {
            logger.info("CLI disabled: no console");
            return;
        }

        // Thread separato per non bloccare l'avvio REST.
        Thread cliThread = new Thread(() -> {
            try {
                springCliBootstrap.runCli();
            } catch (Exception ex) {
                logger.error("CLI terminated with error", ex);
            }
        }, "hackhub-cli");
        cliThread.setDaemon(true);
        cliThread.start();
    }
}
