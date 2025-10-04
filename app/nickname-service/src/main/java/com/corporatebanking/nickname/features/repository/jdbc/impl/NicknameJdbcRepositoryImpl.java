import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.corporatebanking.nickname.features.models.NicknameData;
import com.corporatebanking.nickname.features.repository.jdbc.NicknameJdbcRepository;

@Repository
public class NicknameJdbcRepositoryImpl implements NicknameJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public NicknameJdbcRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<NicknameData> nicknameRowMapper = (rs, rowNum) -> new NicknameData(
            rs.getLong("id"),
            rs.getLong("from_account_id"),
            rs.getLong("to_account_id"),
            rs.getString("nickname"),
            rs.getDate("created_at") != null ? rs.getDate("created_at").toLocalDate() : null,
            rs.getDate("updated_at") != null ? rs.getDate("updated_at").toLocalDate() : null,
            rs.getObject("created_by", Long.class),
            rs.getObject("updated_by", Long.class)
    );

    @Override
    public NicknameData save(NicknameData nickname) {
        String sql = "INSERT INTO nicknames (\"from_account\"_id, \"to_account\"_id, nickname, created_at, created_by) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, nickname.fromAccount());
            ps.setLong(2, nickname.toAccount());
            ps.setString(3, nickname.nickname());
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            if (nickname.createdBy() != null) {
                ps.setLong(5, nickname.createdBy());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId != null) {
            return findById(generatedId.longValue()).orElse(null);
        }
        return null;
    }

    @Override
    public Optional<NicknameData> findById(Long id) {
    	String sql = "SELECT * FROM nicknames n WHERE n.\"to_account\"_id = ?";
        return jdbcTemplate.query(sql, nicknameRowMapper, id).stream().findFirst();
    }

    @Override
    public List<NicknameData> findAll() {
        String sql = "SELECT * FROM nicknames";
        return jdbcTemplate.query(sql, nicknameRowMapper);
    }

    @Override
    public Optional<NicknameData> update(NicknameData nickname) {
        String sql = "UPDATE nicknames SET \"from_account\"_id = ?, \"to_account\"_id = ?, nickname = ?, updated_at = ?, updated_by = ? WHERE id = ?";
        int updatedRows = jdbcTemplate.update(sql,
                nickname.fromAccount(),
                nickname.toAccount(),
                nickname.nickname(),
                Date.valueOf(LocalDate.now()),
                nickname.updatedBy(),
                nickname.id());

        if (updatedRows > 0) {
            return findById(nickname.id());
        }
        return Optional.empty();
    }

    @Override
    public void deleteById(Long id) {
    	String sql = "SELECT * FROM nicknames n WHERE n.\"to_account\"_id = ?";
        jdbcTemplate.update(sql, id);
    }
}
