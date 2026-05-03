package com.livemore.api.service;

import com.livemore.api.domain.RefreshTokenEntity;
import com.livemore.api.domain.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Component
public class MySqlAuthStore implements AuthStore {

    private final MySqlConnectionProvider connectionProvider;

    public MySqlAuthStore(MySqlConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<UserAccount> findUserById(String userId) {
        String sql = "SELECT id, phone_digits, created_at, updated_at FROM users WHERE id = ?";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                UserAccount user = new UserAccount();
                user.setId(rs.getString("id"));
                user.setPhoneDigits(rs.getString("phone_digits"));
                user.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                user.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                return Optional.of(user);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public Optional<UserAccount> findUserByIdentity(String provider, String providerUserId) {
        String sql = """
                SELECT u.id, u.phone_digits, u.created_at, u.updated_at
                FROM user_identities i
                JOIN users u ON u.id = i.user_id
                WHERE i.provider = ? AND i.provider_user_id = ?
                LIMIT 1
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, provider);
            ps.setString(2, providerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                UserAccount user = new UserAccount();
                user.setId(rs.getString("id"));
                user.setPhoneDigits(rs.getString("phone_digits"));
                user.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                user.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
                return Optional.of(user);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public UserAccount upsertUser(UserAccount user) {
        String sql = """
                INSERT INTO users (id, phone_digits, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    phone_digits = VALUES(phone_digits),
                    updated_at = VALUES(updated_at)
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            if (user.getPhoneDigits() == null || user.getPhoneDigits().isBlank()) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, user.getPhoneDigits());
            }
            ps.setTimestamp(3, Timestamp.from(user.getCreatedAt()));
            ps.setTimestamp(4, Timestamp.from(user.getUpdatedAt()));
            ps.executeUpdate();
            return user;
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public void upsertUserIdentity(String userId, String provider, String providerUserId, String providerRawId, String displayName) {
        String sql = """
                INSERT INTO user_identities (
                    user_id, provider, provider_user_id, provider_raw_id, display_name, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    user_id = VALUES(user_id),
                    provider_raw_id = VALUES(provider_raw_id),
                    display_name = VALUES(display_name),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, provider);
            ps.setString(3, providerUserId);
            ps.setString(4, providerRawId);
            ps.setString(5, displayName);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public void revokeAllRefreshTokens(String userId) {
        String sql = "UPDATE refresh_tokens SET revoked = 1, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }

    @Override
    public Optional<RefreshTokenEntity> findRefreshTokenByHashAndUserId(String tokenHash, String userId) {
        String sql = """
                SELECT id, token_hash, user_id, expires_at, revoked, created_at
                FROM refresh_tokens
                WHERE token_hash = ? AND user_id = ?
                LIMIT 1
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                RefreshTokenEntity token = new RefreshTokenEntity();
                token.setId(rs.getString("id"));
                token.setTokenHash(rs.getString("token_hash"));
                token.setUserId(rs.getString("user_id"));
                token.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
                token.setRevoked(rs.getBoolean("revoked"));
                token.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                return Optional.of(token);
            }
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_read_failed");
        }
    }

    @Override
    public void upsertRefreshToken(RefreshTokenEntity token) {
        String sql = """
                INSERT INTO refresh_tokens (id, token_hash, user_id, expires_at, revoked, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    expires_at = VALUES(expires_at),
                    revoked = VALUES(revoked),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (var conn = connectionProvider.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token.getId());
            ps.setString(2, token.getTokenHash());
            ps.setString(3, token.getUserId());
            ps.setTimestamp(4, Timestamp.from(token.getExpiresAt()));
            ps.setBoolean(5, token.isRevoked());
            ps.setTimestamp(6, Timestamp.from(token.getCreatedAt()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "mysql_write_failed");
        }
    }
}
