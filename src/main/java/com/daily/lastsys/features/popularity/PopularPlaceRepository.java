package com.daily.lastsys.features.popularity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class PopularPlaceRepository {
    private final JdbcTemplate jdbc;

    public PopularPlaceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int recordSearch(Long userId, PlaceSearchEventRequest request, String placeKey) {
        return jdbc.update("""
                insert into place_search_events (
                    user_id, provider_place_id, place_key, place_name, category_name,
                    latitude, longitude, search_bucket
                )
                values (
                    ?, ?, ?, ?, ?, ?, ?,
                    to_timestamp(floor(extract(epoch from current_timestamp) / 600) * 600)
                )
                on conflict (user_id, place_key, search_bucket) do nothing
                """,
                userId,
                blankToNull(request.placeId()),
                placeKey,
                request.placeName().trim(),
                blankToNull(request.categoryName()),
                request.latitude(),
                request.longitude()
        );
    }

    public List<PopularPlaceResponse> findPopularPlaces() {
        return jdbc.query("""
                with marker_activity as (
                    select lower(regexp_replace(m.location_name, '[[:space:]]+', '', 'g')) as place_key,
                           min(m.location_name) as place_name,
                           avg(m.latitude) as latitude,
                           avg(m.longitude) as longitude,
                           count(distinct l.id)::int as like_count,
                           count(distinct c.id)::int as comment_count,
                           greatest(
                               coalesce(max(l.created_at), timestamp 'epoch'),
                               coalesce(max(c.created_at), timestamp 'epoch')
                           ) as last_engagement_at
                    from emotion_map_markers m
                    left join likes l on l.record_id = m.id and l.created_at >= current_timestamp - interval '24 hours'
                    left join comments c on c.record_id = m.id and c.created_at >= current_timestamp - interval '24 hours'
                    group by lower(regexp_replace(m.location_name, '[[:space:]]+', '', 'g'))
                ), search_activity as (
                    select place_key,
                           (array_agg(place_name order by created_at desc))[1] as place_name,
                           (array_agg(category_name order by created_at desc) filter (where category_name is not null))[1] as category_name,
                           avg(latitude) as latitude,
                           avg(longitude) as longitude,
                           count(*)::int as search_count,
                           max(created_at) as last_search_at
                    from place_search_events
                    where created_at >= current_timestamp - interval '24 hours'
                    group by place_key
                )
                select coalesce(m.place_name, s.place_name) as place_name,
                       coalesce(s.category_name, '') as category_name,
                       coalesce(m.latitude, s.latitude) as latitude,
                       coalesce(m.longitude, s.longitude) as longitude,
                       coalesce(s.search_count, 0) as search_count,
                       coalesce(m.like_count, 0) as like_count,
                       coalesce(m.comment_count, 0) as comment_count,
                       coalesce(s.search_count, 0) + coalesce(m.like_count, 0) * 3 + coalesce(m.comment_count, 0) * 2 as popularity_score,
                       greatest(coalesce(s.last_search_at, timestamp 'epoch'), coalesce(m.last_engagement_at, timestamp 'epoch')) as last_activity_at
                from marker_activity m
                full outer join search_activity s on s.place_key = m.place_key
                where coalesce(s.search_count, 0) + coalesce(m.like_count, 0) * 3 + coalesce(m.comment_count, 0) * 2 > 0
                order by popularity_score desc, last_activity_at desc, place_name asc
                limit 10
                """,
                (rs, row) -> new PopularPlaceResponse(
                        row + 1,
                        rs.getString("place_name"),
                        rs.getString("category_name"),
                        rs.getObject("latitude", BigDecimal.class),
                        rs.getObject("longitude", BigDecimal.class),
                        rs.getInt("search_count"),
                        rs.getInt("like_count"),
                        rs.getInt("comment_count"),
                        rs.getInt("popularity_score")
                )
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
