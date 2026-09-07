package com.biadevcosta.identity.domain;

import com.biadevcosta.identity.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static final String HASH = "{argon2id}$argon2id$v=19$m=19456,t=2,p=1$c2FsdA$aGFzaA";

    @Test
    void register_doctor_keepsCrmAndSpecialty() {
        User u = User.register("doc@hospital.com", HASH, Role.DOCTOR, "Dr. House", "11999998888", "CRM-1", "Diagnostics");

        assertThat(u.id()).isNotBlank();
        assertThat(u.email()).isEqualTo("doc@hospital.com");
        assertThat(u.role()).isEqualTo(Role.DOCTOR);
        assertThat(u.crm()).isEqualTo("CRM-1");
        assertThat(u.specialty()).isEqualTo("Diagnostics");
        assertThat(u.createdAt()).isNotNull();
    }

    @Test
    void register_nonDoctor_hasNoCrmOrSpecialty() {
        User u = User.register("pat@hospital.com", HASH, Role.PATIENT, "John Doe", null, null, null);

        assertThat(u.role()).isEqualTo(Role.PATIENT);
        assertThat(u.crm()).isNull();
        assertThat(u.specialty()).isNull();
    }

    @Test
    void register_rejectsInvalidOrBlankEmail() {
        assertThatThrownBy(() -> User.register("not-an-email", HASH, Role.NURSE, "Nurse Joy", null, null, null))
                .isInstanceOf(InvalidUserException.class);
        assertThatThrownBy(() -> User.register(" ", HASH, Role.NURSE, "Nurse Joy", null, null, null))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void register_rejectsMissingHashOrName() {
        assertThatThrownBy(() -> User.register("a@b.co", " ", Role.NURSE, "Nurse", null, null, null))
                .isInstanceOf(InvalidUserException.class);
        assertThatThrownBy(() -> User.register("a@b.co", HASH, Role.NURSE, " ", null, null, null))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void register_rejectsNullRole() {
        assertThatThrownBy(() -> User.register("a@b.co", HASH, null, "Name", null, null, null))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void register_doctorWithoutCrmOrSpecialty_isRejected() {
        assertThatThrownBy(() -> User.register("doc@b.co", HASH, Role.DOCTOR, "Doc", null, null, "Cardio"))
                .isInstanceOf(InvalidUserException.class)
                .hasMessageContaining("doctor");
        assertThatThrownBy(() -> User.register("doc@b.co", HASH, Role.DOCTOR, "Doc", null, "CRM-9", null))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void register_nonDoctorWithCrmOrSpecialty_isRejected() {
        assertThatThrownBy(() -> User.register("n@b.co", HASH, Role.NURSE, "Nurse", null, "CRM-9", null))
                .isInstanceOf(InvalidUserException.class)
                .hasMessageContaining("only allowed for a doctor");
        assertThatThrownBy(() -> User.register("adm@b.co", HASH, Role.ADMIN, "Admin", null, null, "Cardio"))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void register_normalizesEmail() {
        User u = User.register("  Doc@Hospital.COM  ", HASH, Role.DOCTOR, "Doc", null, "CRM-1", "Cardio");

        assertThat(u.email()).isEqualTo("doc@hospital.com");
    }

    @Test
    void register_blankOptionalFieldsBecomeNull() {
        User u = User.register("p@b.co", HASH, Role.PATIENT, "Pat", "  ", "", "  ");

        assertThat(u.phone()).isNull();
        assertThat(u.crm()).isNull();
        assertThat(u.specialty()).isNull();
    }

    @Test
    void rehydrate_doesNotRevalidate() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(30);

        User u = User.rehydrate("id-1", "bad-email", HASH, Role.PATIENT, "Old User",
                null, null, null, createdAt);

        assertThat(u.id()).isEqualTo("id-1");
        assertThat(u.email()).isEqualTo("bad-email");
        assertThat(u.createdAt()).isEqualTo(createdAt);
    }
}
