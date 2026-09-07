package com.biadevcosta.identity.infrastructure.persistence;

import com.biadevcosta.identity.domain.Role;
import com.biadevcosta.identity.domain.User;

/** Translates between the {@link User} domain object and its persistence row. */
final class UserMapper {

    private UserMapper() {
    }

    static UserEntity toNewEntity(User user) {
        UserEntity entity = new UserEntity();
        copyInto(user, entity);
        return entity;
    }

    /** Copies the domain state onto an existing row, keeping its {@code version}. */
    static void copyInto(User user, UserEntity entity) {
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role().name());
        entity.setFullName(user.fullName());
        entity.setPhone(user.phone());
        entity.setCrm(user.crm());
        entity.setSpecialty(user.specialty());
        entity.setCreatedAt(user.createdAt());
    }

    static User toDomain(UserEntity entity) {
        return User.rehydrate(
                entity.getId(), entity.getEmail(), entity.getPasswordHash(),
                Role.valueOf(entity.getRole()), entity.getFullName(),
                entity.getPhone(), entity.getCrm(), entity.getSpecialty(), entity.getCreatedAt());
    }
}
