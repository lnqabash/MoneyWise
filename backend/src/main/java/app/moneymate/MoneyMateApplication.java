package app.moneymate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MoneyMate backend — modular monolith (thin vertical slice).
 *
 * Modules (packages) with strict boundaries per ADR-0002:
 *   - profile : users, identity, OTP auth
 *   - budget  : income, expenses, dynamic envelopes, safe-to-spend
 *   - savings : goals, micro-save contributions
 *   - scam    : explainable scam detection (rule-based engine)
 *
 * Runs on embedded H2 by default (no setup). Activate the "docker"
 * profile to use PostgreSQL via docker-compose.
 */
@SpringBootApplication
@EnableScheduling
public class MoneyMateApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyMateApplication.class, args);
    }
}
