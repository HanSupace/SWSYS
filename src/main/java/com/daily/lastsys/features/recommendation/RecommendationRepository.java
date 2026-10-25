package com.daily.lastsys.features.recommendation;

import com.daily.lastsys.features.map.EmotionMapMarkerResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RecommendationRepository {
    private final JdbcTemplate jdbc;
    public RecommendationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RecommendationTagDocument> findUntagged() {
        return jdbc.query("select id, emotion_label, title, location_name, description from emotion_map_markers where tags_classified = false order by id limit 100",
                (rs, row) -> new RecommendationTagDocument(rs.getLong("id"), RecommendationTagDocument.textFor(rs.getString("emotion_label"), rs.getString("title"), rs.getString("location_name"), rs.getString("description"))));
    }
    public void replaceTags(Long markerId, List<String> tags, boolean classified) {
        jdbc.update("delete from emotion_map_marker_tags where marker_id = ?", markerId);
        for (String tag : tags) if (RecommendationTagCatalog.allCodes().contains(tag))
            jdbc.update("insert into emotion_map_marker_tags(marker_id, tag_code) values (?, ?) on conflict do nothing", markerId, tag);
        jdbc.update("update emotion_map_markers set tags_classified = ? where id = ?", classified, markerId);
    }
    public List<EmotionMapMarkerResponse> search(Long userId, RecommendationIntent intent) {
        List<List<String>> includes = RecommendationTagCatalog.relatedGroups(intent.includeTags());
        List<List<String>> excludes = RecommendationTagCatalog.relatedGroups(intent.excludeTags());
        if (includes.isEmpty() && intent.emotions().isEmpty()) return List.of();
        StringBuilder sql = new StringBuilder("""
                select m.id, m.latitude, m.longitude, u.nickname as author_nickname, m.emotion_label, m.emotion_color,
                       m.title, m.location_name, m.description, m.created_at,
                       case when m.user_id = ? then true else false end as own
                from emotion_map_markers m join users u on u.id = m.user_id where 1=1
                """);
        List<Object> args = new ArrayList<>(); args.add(userId);
        for (List<String> group : includes) addGroup(sql, args, group, true);
        for (List<String> group : excludes) addGroup(sql, args, group, false);
        if (!intent.emotions().isEmpty()) {
            sql.append(" and m.emotion_label in (").append(String.join(",", java.util.Collections.nCopies(intent.emotions().size(), "?"))).append(")");
            args.addAll(intent.emotions());
        }
        sql.append(" order by m.created_at desc, m.id desc limit 5");
        return jdbc.query(sql.toString(), (rs, row) -> new EmotionMapMarkerResponse(rs.getLong("id"), rs.getObject("latitude", BigDecimal.class), rs.getObject("longitude", BigDecimal.class), rs.getString("author_nickname"), rs.getString("emotion_label"), rs.getString("emotion_color"), rs.getString("title"), rs.getString("location_name"), rs.getString("description"), rs.getObject("created_at", LocalDateTime.class), rs.getBoolean("own")), args.toArray());
    }
    private void addGroup(StringBuilder sql, List<Object> args, List<String> group, boolean include) {
        sql.append(include ? " and exists (" : " and not exists (").append("select 1 from emotion_map_marker_tags t where t.marker_id=m.id and t.tag_code in (")
                .append(String.join(",", java.util.Collections.nCopies(group.size(), "?"))).append("))"); args.addAll(group);
    }
}
