package com.daily.lastsys.features.recommendation;

import com.daily.lastsys.features.map.EmotionMapMarkerResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class RecommendationRepository {
    private final JdbcTemplate jdbc;
    public RecommendationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<RecommendationTagDocument> findUntagged() {
        return jdbc.query("select id, emotion_label, title, location_name, description from emotion_map_markers where tags_classified = false order by id limit 100",
                (rs, row) -> new RecommendationTagDocument(rs.getLong("id"), RecommendationTagDocument.textFor(rs.getString("emotion_label"), rs.getString("title"), rs.getString("location_name"), rs.getString("description"))));
    }
    public List<RecommendationSearchDocument> findRecommendationCandidates() {
        return jdbc.query("select id, emotion_label, title, location_name, description from emotion_map_markers order by created_at desc, id desc limit 120",
                (rs, row) -> new RecommendationSearchDocument(rs.getLong("id"), rs.getString("emotion_label"), rs.getString("title"), rs.getString("location_name"), rs.getString("description")));
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
        List<String> searchTerms = intent.searchTerms() == null ? List.of() : intent.searchTerms().stream()
                .filter(term -> term != null && !term.isBlank())
                .map(term -> term.toLowerCase(Locale.ROOT))
                .distinct()
                .limit(10)
                .toList();
        if (includes.isEmpty() && intent.emotions().isEmpty() && searchTerms.isEmpty()) return List.of();
        StringBuilder sql = new StringBuilder("""
                select m.id, m.latitude, m.longitude, u.nickname as author_nickname, m.emotion_label, m.emotion_color,
                       m.title, m.location_name, m.description, m.created_at,
                       case when m.user_id = ? then true else false end as own
                from emotion_map_markers m join users u on u.id = m.user_id where 1=1
                """);
        List<Object> args = new ArrayList<>(); args.add(userId);
        sql.append(" and (");
        boolean hasCandidate = false;
        if (!includes.isEmpty()) {
            sql.append('(');
            for (List<String> group : includes) {
                if (hasCandidate) sql.append(" and ");
                addGroup(sql, args, group, true);
                hasCandidate = true;
            }
            sql.append(')');
        }
        if (!intent.emotions().isEmpty()) {
            if (hasCandidate) sql.append(" or ");
            sql.append("m.emotion_label in (").append(String.join(",", java.util.Collections.nCopies(intent.emotions().size(), "?"))).append(')');
            args.addAll(intent.emotions());
            hasCandidate = true;
        }
        if (!searchTerms.isEmpty()) {
            if (hasCandidate) sql.append(" or ");
            sql.append('(');
            for (int index = 0; index < searchTerms.size(); index++) {
                if (index > 0) sql.append(" or ");
                sql.append("position(? in lower(concat_ws(' ', m.location_name, m.title, coalesce(m.description, '')))) > 0");
                args.add(searchTerms.get(index));
            }
            sql.append(')');
        }
        sql.append(')');
        for (List<String> group : excludes) addGroup(sql, args, group, false);
        sql.append(" order by m.created_at desc, m.id desc limit 5");
        return jdbc.query(sql.toString(), (rs, row) -> new EmotionMapMarkerResponse(rs.getLong("id"), rs.getObject("latitude", BigDecimal.class), rs.getObject("longitude", BigDecimal.class), rs.getString("author_nickname"), rs.getString("emotion_label"), rs.getString("emotion_color"), rs.getString("title"), rs.getString("location_name"), rs.getString("description"), rs.getObject("created_at", LocalDateTime.class), rs.getBoolean("own")), args.toArray());
    }
    public List<EmotionMapMarkerResponse> findByIds(Long userId, List<Long> markerIds) {
        if (markerIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(markerIds.size(), "?"));
        StringBuilder sql = new StringBuilder("""
                select m.id, m.latitude, m.longitude, u.nickname as author_nickname, m.emotion_label, m.emotion_color,
                       m.title, m.location_name, m.description, m.created_at,
                       case when m.user_id = ? then true else false end as own
                from emotion_map_markers m join users u on u.id = m.user_id
                where m.id in (
                """).append(placeholders).append(") order by case m.id");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(markerIds);
        for (int index = 0; index < markerIds.size(); index++) {
            sql.append(" when ? then ").append(index);
            args.add(markerIds.get(index));
        }
        sql.append(" else ").append(markerIds.size()).append(" end");
        return jdbc.query(sql.toString(), (rs, row) -> new EmotionMapMarkerResponse(rs.getLong("id"), rs.getObject("latitude", BigDecimal.class), rs.getObject("longitude", BigDecimal.class), rs.getString("author_nickname"), rs.getString("emotion_label"), rs.getString("emotion_color"), rs.getString("title"), rs.getString("location_name"), rs.getString("description"), rs.getObject("created_at", LocalDateTime.class), rs.getBoolean("own")), args.toArray());
    }
    private void addGroup(StringBuilder sql, List<Object> args, List<String> group, boolean include) {
        sql.append(include ? " and exists (" : " and not exists (").append("select 1 from emotion_map_marker_tags t where t.marker_id=m.id and t.tag_code in (")
                .append(String.join(",", java.util.Collections.nCopies(group.size(), "?"))).append("))"); args.addAll(group);
    }
}
