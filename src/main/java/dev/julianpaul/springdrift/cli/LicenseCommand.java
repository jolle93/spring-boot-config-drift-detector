package dev.julianpaul.springdrift.cli;

import dev.julianpaul.springdrift.license.GumroadLicenseChecker;
import dev.julianpaul.springdrift.license.LicenseStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.concurrent.Callable;

@Command(
        name = "license",
        description = "Manage your spring-drift license",
        mixinStandardHelpOptions = true,
        subcommands = {
                LicenseCommand.ActivateCommand.class,
                LicenseCommand.StatusCommand.class,
                LicenseCommand.DeactivateCommand.class
        }
)
public class LicenseCommand implements Runnable {

    @Override
    public void run() {
        new picocli.CommandLine(this).usage(System.out);
    }

    @Command(name = "activate", description = "Activate your license key", mixinStandardHelpOptions = true)
    static class ActivateCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Your Gumroad license key")
        private String licenseKey;

        @Override
        public Integer call() {
            LicenseStore store = new LicenseStore();
            GumroadLicenseChecker checker = new GumroadLicenseChecker();

            System.out.println("Verifying license key...");
            try {
                GumroadLicenseChecker.VerificationResult result = checker.verify(licenseKey);
                if (!result.success()) {
                    System.err.println("Activation failed: " + result.message());
                    return 1;
                }
                store.save(licenseKey);
                store.saveVerified();
                System.out.println("License activated successfully. Thank you!");
                return 0;
            } catch (IOException | InterruptedException e) {
                System.err.println("Could not reach license server: " + e.getMessage());
                System.err.println("Please check your internet connection and try again.");
                return 2;
            }
        }
    }

    @Command(name = "status", description = "Show current license status", mixinStandardHelpOptions = true)
    static class StatusCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            LicenseStore store = new LicenseStore();
            if (!store.hasKey()) {
                System.out.println("No license key found.");
                System.out.println("Activate with: spring-drift license activate <KEY>");
                return 1;
            }
            if (store.isVerifiedRecently()) {
                System.out.println("License: active (verified within the last 7 days)");
            } else {
                System.out.println("License: key found, verification pending (will re-verify on next scan)");
            }
            return 0;
        }
    }

    @Command(name = "deactivate", description = "Remove the stored license key", mixinStandardHelpOptions = true)
    static class DeactivateCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            try {
                new LicenseStore().clear();
                System.out.println("License deactivated.");
                return 0;
            } catch (IOException e) {
                System.err.println("Failed to remove license: " + e.getMessage());
                return 1;
            }
        }
    }
}
