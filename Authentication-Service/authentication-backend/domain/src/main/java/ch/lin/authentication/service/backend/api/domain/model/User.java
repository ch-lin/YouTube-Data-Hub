/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.authentication.service.backend.api.domain.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static ch.lin.authentication.service.backend.api.domain.model.User.TABLE_NAME;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a user in the system.
 * <p>
 * This class is a JPA entity that maps to the "user" table and also implements
 * Spring Security's {@link UserDetails} interface to integrate with the
 * authentication framework.
 */
@Table(name = TABLE_NAME)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id", "firstname", "lastname", "email", "password", "role"}, callSuper = false)
public class User implements UserDetails {

    /**
     * The serialization version UID for this class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the database table for users.
     */
    public static final String TABLE_NAME = "user";

    /**
     * The unique identifier for the user (primary key).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * The user's first name.
     */
    @NotNull
    @Column(nullable = false)
    private String firstname;

    /**
     * The user's last name.
     */
    @NotNull
    @Column(nullable = false)
    private String lastname;

    /**
     * The user's email address, which also serves as their username for
     * authentication.
     */
    @NotNull
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The user's hashed password.
     */
    @NotNull
    @Column(nullable = false)
    private String password;

    /**
     * The role assigned to the user, determining their permissions.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * Returns the authorities granted to the user. In this implementation, it's
     * a single role.
     *
     * @return A collection of granted authorities.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Returns the username used to authenticate the user. In this case, it's
     * the email address.
     *
     * @return The user's email address.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account has expired. An expired account
     * cannot be authenticated.
     *
     * @return {@code true} because this implementation does not support account
     * expiration.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked. A locked user cannot be
     * authenticated.
     *
     * @return {@code true} because this implementation does not support account
     * locking.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) has expired. Expired
     * credentials prevent authentication.
     *
     * @return {@code true} because this implementation does not support
     * credential expiration.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled. A disabled user cannot
     * be authenticated.
     *
     * @return {@code true} because this implementation does not support
     * disabling accounts.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
