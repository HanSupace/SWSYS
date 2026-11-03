import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** One-off, opt-in JDBC seed. No secrets or startup hooks are stored here. */
public class SeedSeunghak {
    static final Path ROOT = Path.of("").toAbsolutePath();
    static final String PREFIX = "seed_seunghak_v1_";
    record Place(String name, String lat, String lng) {}
    record Entry(String place, String emotion, String title, String body, List<String> tags) {}

    static List<String[]> lines(String name) throws Exception {
        return Files.readAllLines(ROOT.resolve("scripts/seed/" + name), StandardCharsets.UTF_8).stream()
                .filter(s -> !s.isBlank() && !s.startsWith("#"))
                .map(s -> s.split("\\|", -1)).toList();
    }

    public static void main(String[] args) throws Exception {
        var places = new LinkedHashMap<String, Place>();
        for (var p : lines("seunghak-places.psv")) places.put(p[0], new Place(p[1], p[2], p[3]));
        var colors = new LinkedHashMap<String, String>();
        var catalog = Files.readString(ROOT.resolve("src/main/java/com/daily/lastsys/features/emotion/EmotionCatalog.java"));
        var matcher = Pattern.compile("new EmotionMeta\\(\"[^\"]+\", \"[^\"]+\", \"[^\"]+\", \"([^\"]+)\", \"[^\"]+\", \"(#[A-Fa-f0-9]+)\"").matcher(catalog);
        while (matcher.find()) colors.put(matcher.group(1), matcher.group(2));
        var entries = new ArrayList<Entry>();
        var counts = new TreeMap<String, Integer>();
        var titles = new HashSet<String>();
        for (var r : lines("seunghak-records.psv")) {
            if (r.length != 5 || !places.containsKey(r[0]) || !colors.containsKey(r[1])
                    || r[2].isBlank() || r[2].length() > 24 || r[3].isBlank() || r[3].length() > 2000
                    || !titles.add(r[2])) throw new IllegalStateException("Invalid seed row: " + r[2]);
            entries.add(new Entry(r[0], r[1], r[2], r[3], List.of(r[4].split(","))));
            counts.merge(r[1], 1, Integer::sum);
        }
        if (entries.size() != 100 || counts.size() != colors.size() || places.size() != 10)
            throw new IllegalStateException("Expected 100 records, all emotions, 10 places");
        System.out.println("Validated records=" + entries.size() + " emotions=" + counts);
        if (args.length == 0 || args[0].equals("validate")) return;

        var env = new Properties();
        try (var reader = Files.newBufferedReader(ROOT.resolve(".env"), StandardCharsets.UTF_8)) { env.load(reader); }
        for (var key : List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD"))
            if (env.getProperty(key, "").isBlank()) throw new IllegalStateException("Missing " + key);
        var props = new Properties();
        props.setProperty("user", env.getProperty("DB_USER"));
        props.setProperty("password", env.getProperty("DB_PASSWORD"));
        props.setProperty("sslmode", "require");
        props.setProperty("connectTimeout", "15");
        props.setProperty("socketTimeout", "30");
        String url = "jdbc:postgresql://" + env.getProperty("DB_HOST") + ":" + env.getProperty("DB_PORT") + "/" + env.getProperty("DB_NAME");
        try (var db = DriverManager.getConnection(url, props)) {
            if (args[0].equals("inspect")) {
                try (var s = db.createStatement(); var rs = s.executeQuery("select table_name from information_schema.tables where table_schema=current_schema() order by table_name")) {
                    while (rs.next()) System.out.println("Table: " + rs.getString(1));
                }
                try (var s = db.createStatement(); var rs = s.executeQuery("select to_regclass('users'),to_regclass('emotion_map_markers')")) {
                    rs.next(); if (rs.getString(1) == null || rs.getString(2) == null) {
                        System.out.println("Project schema is not initialized."); return;
                    }
                }
                try (var s = db.createStatement(); var rs = s.executeQuery("select (select count(*) from users), (select count(*) from emotion_map_markers)")) {
                    rs.next(); System.out.println("Connected. users=" + rs.getLong(1) + " existing_records=" + rs.getLong(2));
                }
                return;
            }
            if (!args[0].equals("apply")) throw new IllegalArgumentException("Use validate, inspect or apply");
            db.setAutoCommit(false);
            try {
                try (var s = db.createStatement()) { s.execute("select pg_advisory_xact_lock(65261003100)"); }
                try (var s = db.createStatement(); var rs = s.executeQuery("select to_regclass('users'),to_regclass('emotion_map_markers')")) {
                    rs.next(); if (rs.getString(1) == null && rs.getString(2) == null) {
                        try (var init = db.createStatement()) {
                            init.execute(Files.readString(ROOT.resolve("src/main/resources/schema.sql")));
                        }
                        System.out.println("Initialized existing project schema in the same transaction.");
                    }
                }
                long oldCount;
                try (var q = db.prepareStatement("select count(*) from emotion_map_markers m join users u on u.id=m.user_id where left(u.username, ?) = ?")) {
                    q.setInt(1, PREFIX.length()); q.setString(2, PREFIX);
                    try (var rs = q.executeQuery()) { rs.next(); oldCount = rs.getLong(1); }
                }
                if (oldCount == 100) { verify(db, entries, colors, places); db.rollback(); System.out.println("Already seeded: verified 100 records, no changes."); return; }
                if (oldCount != 0) throw new IllegalStateException("Partial seed exists; refusing duplicate insertion");
                Set<String> allowed = new HashSet<>();
                try (var s = db.createStatement(); var rs = s.executeQuery("select tag_code from recommendation_tag_catalog")) {
                    while (rs.next()) allowed.add(rs.getString(1));
                }
                for (var entry : entries) if (!allowed.containsAll(entry.tags())) throw new IllegalStateException("Unknown recommendation tag");
                String[] names = {"책갈피산책", "느린점심", "초록공책", "오후한페이지", "언덕의바람", "작은쉼표", "구름발걸음", "커피와여백", "나무사이빛", "오늘의창가"};
                long[] users = new long[10];
                // Unusable credential: these accounts are for authored seed records only.
                for (int i = 0; i < users.length; i++) {
                    try (var q = db.prepareStatement("insert into users(username,password_hash,nickname) values (?,?,?) on conflict(username) do nothing")) {
                        q.setString(1, PREFIX + String.format("%02d", i + 1));
                        q.setString(2, "!disabled-" + UUID.randomUUID()); q.setString(3, names[i]); q.executeUpdate();
                    }
                    try (var q = db.prepareStatement("select id from users where username=?")) {
                        q.setString(1, PREFIX + String.format("%02d", i + 1));
                        try (var rs = q.executeQuery()) { rs.next(); users[i] = rs.getLong(1); }
                    }
                }
                var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
                for (int i = 0; i < entries.size(); i++) {
                    var e = entries.get(i); var p = places.get(e.place()); long id;
                    try (var q = db.prepareStatement("insert into emotion_map_markers(user_id,latitude,longitude,emotion_label,emotion_color,title,location_name,description,tags_classified,created_at,updated_at) values (?,?,?,?,?,?,?,?,true,?,?) returning id")) {
                        q.setLong(1, users[(i + i / 10) % 10]);
                        q.setBigDecimal(2, new java.math.BigDecimal(p.lat())); q.setBigDecimal(3, new java.math.BigDecimal(p.lng()));
                        q.setString(4, e.emotion()); q.setString(5, colors.get(e.emotion())); q.setString(6, e.title());
                        q.setString(7, p.name()); q.setString(8, e.body());
                        var created = Timestamp.valueOf(now.minusMinutes((long) (entries.size() - i) * 71));
                        q.setTimestamp(9, created); q.setTimestamp(10, created);
                        try (var rs = q.executeQuery()) { rs.next(); id = rs.getLong(1); }
                    }
                    for (var tag : e.tags()) try (var q = db.prepareStatement("insert into emotion_map_marker_tags(marker_id,tag_code) values (?,?)")) {
                        q.setLong(1, id); q.setString(2, tag); q.executeUpdate();
                    }
                }
                verify(db, entries, colors, places);
                db.commit();
                System.out.println("Committed: 100 records, 10 places, 10 dedicated authors, all 8 emotions, recommendation tags saved.");
            } catch (Exception ex) { db.rollback(); throw ex; }
        } catch (SQLException ex) {
            // Avoid printing connection strings, credentials or server detail in logs.
            System.err.println("Database operation failed. SQLState=" + ex.getSQLState());
            System.exit(1);
        }
    }

    static void verify(Connection db, List<Entry> entries, Map<String,String> colors, Map<String,Place> places) throws Exception {
        var expected = new HashMap<String,Entry>();
        for (var e : entries) expected.put(e.title(), e);
        int count = 0;
        try (var q = db.prepareStatement("select m.* from emotion_map_markers m join users u on u.id=m.user_id where left(u.username,?)=?")) {
            q.setInt(1, PREFIX.length()); q.setString(2, PREFIX);
            try (var rs = q.executeQuery()) {
                while (rs.next()) {
                    var e = expected.remove(rs.getString("title"));
                    if (e == null) throw new IllegalStateException("Unexpected seed record");
                    var p = places.get(e.place());
                    if (!e.emotion().equals(rs.getString("emotion_label")) || !e.body().equals(rs.getString("description"))
                            || !colors.get(e.emotion()).equals(rs.getString("emotion_color")) || !p.name().equals(rs.getString("location_name"))
                            || new java.math.BigDecimal(p.lat()).compareTo(rs.getBigDecimal("latitude")) != 0
                            || new java.math.BigDecimal(p.lng()).compareTo(rs.getBigDecimal("longitude")) != 0 || !rs.getBoolean("tags_classified"))
                        throw new IllegalStateException("Seed content mismatch");
                    var tags = new HashSet<String>();
                    try (var tq = db.prepareStatement("select tag_code from emotion_map_marker_tags where marker_id=?")) {
                        tq.setLong(1, rs.getLong("id"));
                        try (var tr = tq.executeQuery()) { while (tr.next()) tags.add(tr.getString(1)); }
                    }
                    if (!tags.equals(new HashSet<>(e.tags()))) throw new IllegalStateException("Seed tags mismatch");
                    count++;
                }
            }
        }
        if (count != 100 || !expected.isEmpty()) throw new IllegalStateException("Seed count mismatch");
        System.out.println("Verified database contents: 100 records, matching emotions, colors, locations, bodies and tags.");
    }
}
