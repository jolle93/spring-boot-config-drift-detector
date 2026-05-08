package dev.julianpaul.springdrift.cli;

import picocli.CommandLine.Command;

@Command(
        name = "spring-drift",
        description = "Detects configuration drift across Spring Boot stages",
        version = "1.0.0",
        mixinStandardHelpOptions = true,
        subcommands = {ScanCommand.class}
)
public class SpringDriftCommand implements Runnable {

    @Override
    public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }
}
