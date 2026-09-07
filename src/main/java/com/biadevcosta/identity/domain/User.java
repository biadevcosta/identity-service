package com.biadevcosta.identity.domain;

import com.biadevcosta.identity.domain.exception.InvalidUserException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * User aggregate. Holds the registration rules:
 * <ul>
 *   <li>{@code email} is required and must have a basic {@code local@domain.tld} shape;</li>
 *   <li>{@code passwordHash} is required (hashing happens in infrastructure, behind a port);</li>
 *   <li>{@code fullName} is required;</li>
 *   <li>{@code crm} and {@code specialty} are required for {@link Role#DOCTOR} and rejected for any other role.</li>
 * </ul>
 * This class has no framework dependency.
 */
public class User {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final String id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final String fullName;
    private final String phone;
    private final String crm;
    private final String specialty;
    private final LocalDateTime createdAt;

    private User(String id, String email, String passwordHash, Role role, String fullName,
                 String phone, String crm, String specialty, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
        this.phone = phone;
        this.crm = crm;
        this.specialty = specialty;
        this.createdAt = createdAt;
    }

    /** Registration factory: enforces the creation invariants. {@code passwordHash} is already hashed. */
    public static User register(String email, String passwordHash, Role role, String fullName,
                                String phone, String crm, String specialty) {
        String normalizedEmail = normalizeEmail(email);
        if (isBlank(normalizedEmail) || !EMAIL.matcher(normalizedEmail).matches()) {
            throw new InvalidUserException("a valid email is required");
        }
        if (isBlank(passwordHash)) {
            throw new InvalidUserException("passwordHash is required");
        }
        if (role == null) {
            throw new InvalidUserException("role is required");
        }
        if (isBlank(fullName)) {
            throw new InvalidUserException("fullName is required");
        }
        if (role == Role.DOCTOR) {
            if (isBlank(crm) || isBlank(specialty)) {
                throw new InvalidUserException("crm and specialty are required for a doctor");
            }
        } else if (!isBlank(crm) || !isBlank(specialty)) {
            throw new InvalidUserException("crm and specialty are only allowed for a doctor");
        }
        return new User(UUID.randomUUID().toString(), normalizedEmail, passwordHash, role, fullName,
                blankToNull(phone), blankToNull(crm), blankToNull(specialty), LocalDateTime.now());
    }

    /** Rebuilds a user from persistence without re-checking creation invariants. */
    public static User rehydrate(String id, String email, String passwordHash, Role role, String fullName,
                                 String phone, String crm, String specialty, LocalDateTime createdAt) {
        return new User(id, email, passwordHash, role, fullName, phone, crm, specialty, createdAt);
    }

    /** Canonical form of an email for storage and lookup: trimmed and lower-cased. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    public String id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public String fullName() {
        return fullName;
    }

    public String phone() {
        return phone;
    }

    public String crm() {
        return crm;
    }

    public String specialty() {
        return specialty;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
