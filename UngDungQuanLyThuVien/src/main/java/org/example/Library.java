package org.example;

import java.util.*;
import java.sql.*;
import java.util.Objects;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class Library {
    private static Library instance;
    private final ArrayList<LibraryItem> items = new ArrayList<>();
    private final ArrayList<LibraryUser> users = new ArrayList<>();

    // ===== Indexes for fast search =====
    private final Map<String, List<Book>> idxTitle    = new HashMap<>();
    private final Map<String, List<Book>> idxAuthor   = new HashMap<>();
    private final Map<String, List<Book>> idxCategory = new HashMap<>();
    // Prefix index for title (1..3 chars)
    private final Map<String, List<Book>> idxTitlePfx = new HashMap<>();
    // Normalized ISBN -> Book
    private final Map<String, Book> isbnIndex = new HashMap<>();

    // Basic stopwords (áp dụng CHỈ cho token dài > 1)
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the","a","an","and","or","of","in","on","for","to",
            "la","le","les","de","du","des",
            "va","và","của","là","những","các","một"
    ));

    private Library() {}

    public static Library getInstance() {
        if (instance == null) instance = new Library();
        return instance;
    }

    public List<LibraryItem> getItems() { return items; }
    public List<LibraryUser> getUsers() { return users; }

    public void loadBooksFromDatabase() {
        try {
            BookDAO dao = new BookDAO();
            var list = dao.getAllBooks();
            items.clear();
            items.addAll(list);
            rebuildIndexes(); // build cache + index ngay sau khi load
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadUsersFromDatabase() {
        try {
            UserDAO udao = new UserDAO();
            users.clear();
            users.addAll(udao.getAllUsers());
            for (LibraryUser lu : users) {
                if (lu instanceof User u) {
                    u.getBorrowRecord().clear();
                    u.getBorrowRecord().addAll(udao.getBorrowRecords(u.getUserId()));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addItem(LibraryItem item) {
        items.add(item);
        if (item instanceof Book b) {
            ensureBookCache(b);
            indexBook(b);
        }
    }

    public void removeItem(LibraryItem item) {
        items.remove(item);
        if (item instanceof Book b) deindexBook(b);
    }

    public LibraryItem findItemByTitle(String title) {
        for (LibraryItem item : items) {
            if (item.getTitle().equalsIgnoreCase(title)) return item;
        }
        return null;
    }

    public LibraryItem findItemByIsbn(String isbn) {
        String key = normalize(isbn);
        if (key == null) return null;
        Book hit = isbnIndex.get(key);
        if (hit != null) return hit;

        // fallback giữ tương thích
        for (LibraryItem it : items) {
            String cur = normalize(it != null ? it.getIsbn() : null);
            if (Objects.equals(cur, key)) return it;
        }
        return null;
    }

    public void addUser(LibraryUser user) { users.add(user); }
    public void removeUser(LibraryUser user) { users.remove(user); }

    public LibraryUser findUserById(String userId) {
        for (LibraryUser user : users) {
            if (user.getUserId().equals(userId)) return user;
        }
        return null;
    }

    public LibraryItem findItemById(String id) { return null; }

    // =================== SEARCH (optimized) ===================

    public List<Book> search(String q) {
        if (q == null) q = "";
        final String nq = normalize(q.trim());
        if (nq == null || nq.isBlank()) return Collections.emptyList();

        // Trường hợp nhập đúng ISBN -> trả nhanh
        Book isbnHit = isbnIndex.get(nq);
        if (isbnHit != null) {
            List<Book> one = new ArrayList<>(1);
            one.add(isbnHit);
            return one;
        }

        // Tokenize: KHÔNG loại token 1 ký tự; chỉ bỏ stopwords khi token length > 1
        List<String> rawTokens = tokens(nq);
        if (rawTokens.isEmpty()) return Collections.emptyList();

        List<String> qTokens = new ArrayList<>(rawTokens.size());
        for (String t : rawTokens) {
            if (t.length() > 1 && STOPWORDS.contains(t)) continue;
            qTokens.add(t);
        }
        // Nếu vì stopwords mà rỗng, dùng lại rawTokens (vd: "a")
        if (qTokens.isEmpty()) qTokens = rawTokens;

        // 1) Candidate generation từ index (exact + prefix 1..3)
        LinkedHashSet<Book> candidates = new LinkedHashSet<>();
        for (String qt : qTokens) {
            addPosting(idxTitle.get(qt), candidates);
            addPosting(idxAuthor.get(qt), candidates);
            addPosting(idxCategory.get(qt), candidates);

            int maxLen = Math.min(3, qt.length());
            for (int len = 1; len <= maxLen; len++) {
                String pfx = qt.substring(0, len);
                addPosting(idxTitlePfx.get(pfx), candidates);
            }
        }

        // 2) Fallback: nếu vẫn trống, quét nhẹ theo substring (đã normalize)
        if (candidates.isEmpty()) {
            for (LibraryItem li : items) {
                if (!(li instanceof Book b)) continue;
                ensureBookCache(b);
                String nt = b.getNormTitle();
                String na = b.getNormAuthors();
                String nc = b.getNormCategory();
                if ((nt != null && nt.contains(nq)) ||
                        (na != null && na.contains(nq)) ||
                        (nc != null && nc.contains(nq))) {
                    candidates.add(b);
                }
            }
            // Nếu vẫn trống và query 1 ký tự, match theo ký tự đầu của token title
            if (candidates.isEmpty() && nq.length() == 1) {
                char c0 = nq.charAt(0);
                for (LibraryItem li : items) {
                    if (!(li instanceof Book b)) continue;
                    ensureBookCache(b);
                    List<String> tts = b.getTitleTokens();
                    if (tts == null) continue;
                    for (String tt : tts) {
                        if (!tt.isEmpty() && tt.charAt(0) == c0) {
                            candidates.add(b);
                            break;
                        }
                    }
                }
            }
        }

        if (candidates.isEmpty()) return Collections.emptyList();

        // 3) Rerank ứng viên + Top-K (K=50)
        final int K = 50;
        PriorityQueue<Scored<Book>> topK =
                new PriorityQueue<>(K, Comparator.comparingDouble(s -> s.score)); // min-heap

        for (Book b : candidates) {
            double score = scoreBookForQuery(b, nq, qTokens);
            if (score <= 0) continue;
            if (topK.size() < K) topK.add(new Scored<>(b, score));
            else if (score > topK.peek().score) {
                topK.poll();
                topK.add(new Scored<>(b, score));
            }
        }

        if (topK.isEmpty()) return Collections.emptyList();

        // 4) Xuất list theo điểm giảm dần; tie-break bằng độ dài tiêu đề
        ArrayList<Scored<Book>> buf = new ArrayList<>(topK);
        buf.sort((a, b) -> {
            int cmp = Double.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            String ta = b.value.getTitle() == null ? "" : b.value.getTitle();
            String tb = a.value.getTitle() == null ? "" : a.value.getTitle();
            return Integer.compare(ta.length(), tb.length());
        });

        ArrayList<Book> out = new ArrayList<>(buf.size());
        for (Scored<Book> s : buf) out.add(s.value);
        return out;
    }

    // =============== Scoring ===============
    private double scoreBookForQuery(Book b, String normQuery, List<String> qTokens) {
        ensureBookCache(b);
        String nt = b.getNormTitle();
        String na = b.getNormAuthors();

        List<String> titleTokens = b.getTitleTokens();
        List<String> authorTokens = b.getAuthorTokens();
        List<String> categoryTokens = b.getCategoryTokens();

        double score = 0;

        // Bonus cụm con khi query đủ dài
        if (normQuery.length() >= 3) {
            if (nt != null && nt.contains(normQuery)) score += 60; // title phrase
            if (na != null && na.contains(normQuery)) score += 30; // author phrase
        }

        for (String qt : qTokens) {
            boolean hitExact = false;
            boolean hitPrefix = false;

            // exact token
            if (containsToken(titleTokens, qt)) { score += 30; hitExact = true; }
            if (containsToken(authorTokens, qt)) { score += 20; hitExact = true; }
            if (containsToken(categoryTokens, qt)) { score += 16; hitExact = true; }

            // prefix (đếm hit, giới hạn 3)
            if (!hitExact) {
                int tHits = prefixHits(titleTokens, qt);
                if (tHits > 0) { score += 12 * Math.min(tHits, 3); hitPrefix = true; }
                int aHits = prefixHits(authorTokens, qt);
                if (aHits > 0) { score += 8 * Math.min(aHits, 3); hitPrefix = true; }
                int cHits = prefixHits(categoryTokens, qt);
                if (cHits > 0) { score += 6 * Math.min(cHits, 3); hitPrefix = true; }
            }

            // fuzzy nhẹ (ED<=1) chỉ khi không có exact/prefix
            if (!hitExact && !hitPrefix) {
                int bestTitleEd = bestEditDistance(titleTokens, qt, 1);
                int bestAuthorEd = bestEditDistance(authorTokens, qt, 1);
                if (bestTitleEd == 1) score += 4;
                if (bestAuthorEd == 1) score += 3;
            }
        }

        // sách còn số lượng
        if (b.getQuantity() > 0) score += 1.5;

        return score;
    }

    // ===================== Index build/update =====================

    private void rebuildIndexes() {
        idxTitle.clear(); idxAuthor.clear(); idxCategory.clear();
        idxTitlePfx.clear(); isbnIndex.clear();

        for (LibraryItem li : items) {
            if (!(li instanceof Book b)) continue;
            ensureBookCache(b);
            indexBook(b);
        }
    }

    private void ensureBookCache(Book b) {
        if (b.getNormTitle() != null) return; // đã cache
        String nt = normalize(nullToEmpty(b.getTitle()));
        String na = normalize(nullToEmpty(b.getAuthors()));
        String nc = normalize(nullToEmpty(b.getCategory()));
        String ni = normalize(nullToEmpty(b.getIsbn()));

        b.setNormTitle(nt);
        b.setNormAuthors(na);
        b.setNormCategory(nc);
        b.setNormIsbn(ni);
        b.setTitleTokens(tokens(nt));
        b.setAuthorTokens(tokens(na));
        b.setCategoryTokens(tokens(nc));
    }

    private void indexBook(Book b) {
        // isbn
        if (b.getNormIsbn() != null && !b.getNormIsbn().isBlank()) {
            isbnIndex.put(b.getNormIsbn(), b);
        }
        // title/author/category tokens
        for (String t : b.getTitleTokens())    putPosting(idxTitle, t, b);
        for (String t : b.getAuthorTokens())   putPosting(idxAuthor, t, b);
        for (String t : b.getCategoryTokens()) putPosting(idxCategory, t, b);

        // prefix 1..3 kí tự cho title
        for (String t : b.getTitleTokens()) {
            int max = Math.min(3, t.length());
            for (int len = 1; len <= max; len++) {
                String pfx = t.substring(0, len);
                putPosting(idxTitlePfx, pfx, b);
            }
        }
    }

    private void deindexBook(Book b) {
        // ISBN
        if (b.getNormIsbn() != null) {
            Book cur = isbnIndex.get(b.getNormIsbn());
            if (cur == b) isbnIndex.remove(b.getNormIsbn());
        }
        // Posting lists
        removePosting(idxTitle, b.getTitleTokens(), b);
        removePosting(idxAuthor, b.getAuthorTokens(), b);
        removePosting(idxCategory, b.getCategoryTokens(), b);

        ArrayList<String> pfxs = new ArrayList<>();
        for (String t : b.getTitleTokens()) {
            int max = Math.min(3, t.length());
            for (int len = 1; len <= max; len++) pfxs.add(t.substring(0, len));
        }
        removePosting(idxTitlePfx, pfxs, b);
    }

    private static void putPosting(Map<String, List<Book>> idx, String key, Book b) {
        if (key == null || key.isBlank()) return;
        idx.computeIfAbsent(key, k -> new ArrayList<>(4)).add(b);
    }

    private static void removePosting(Map<String, List<Book>> idx, List<String> keys, Book b) {
        if (keys == null) return;
        for (String k : keys) {
            List<Book> lst = idx.get(k);
            if (lst != null) {
                lst.remove(b);
                if (lst.isEmpty()) idx.remove(k);
            }
        }
    }

    private static void addPosting(List<Book> posting, LinkedHashSet<Book> acc) {
        if (posting == null || posting.isEmpty()) return;
        acc.addAll(posting);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    /* ===================== Helpers (public static để Book tái dùng) ===================== */

    public static final Pattern NON_ALNUM = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}]+");

    public static String normalize(String s) {
        if (s == null) return null;
        String x = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        x = x.toLowerCase(Locale.ROOT);
        x = x.replace('đ', 'd').replace('Đ', 'd');
        x = x.trim().replaceAll("\\s+", " ");
        return x;
    }

    public static List<String> tokens(String s) {
        if (s == null || s.isBlank()) return Collections.emptyList();
        String t = NON_ALNUM.matcher(s).replaceAll(" ").trim();
        if (t.isBlank()) return Collections.emptyList();
        String[] parts = t.split("\\s+");
        List<String> list = new ArrayList<>(parts.length);
        for (String p : parts) if (!p.isBlank()) list.add(p);
        return list;
    }

    private static boolean containsToken(List<String> fieldTokens, String q) {
        if (fieldTokens == null || fieldTokens.isEmpty()) return false;
        for (String t : fieldTokens) if (t.equals(q)) return true;
        return false;
    }

    private static int prefixHits(List<String> fieldTokens, String qPrefix) {
        if (fieldTokens == null || fieldTokens.isEmpty()) return 0;
        int c = 0;
        for (String t : fieldTokens) if (t.startsWith(qPrefix)) c++;
        return c;
    }

    /** Best edit distance giữa q và bất kỳ token nào trong fieldTokens, cắt ngưỡng maxEd để tối ưu. */
    private static int bestEditDistance(List<String> fieldTokens, String q, int maxEd) {
        if (fieldTokens == null || fieldTokens.isEmpty()) return maxEd + 1;
        int best = Integer.MAX_VALUE;
        for (String t : fieldTokens) {
            int d = boundedLevenshtein(q, t, maxEd);
            if (d < best) {
                best = d;
                if (best == 0) break;
            }
        }
        return best == Integer.MAX_VALUE ? maxEd + 1 : best;
    }

    /** Levenshtein có ngưỡng dừng sớm (nếu vượt maxEd thì trả về >maxEd). */
    private static int boundedLevenshtein(String a, String b, int maxEd) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > maxEd) return maxEd + 1;

        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];

        for (int j = 0; j <= lb; j++) prev[j] = j;

        for (int i = 1; i <= la; i++) {
            curr[0] = i;

            int from = Math.max(1, i - maxEd);
            int to = Math.min(lb, i + maxEd);

            if (from > to) return maxEd + 1;
            if (from > 1) curr[from - 1] = maxEd + 1;

            for (int j = from; j <= to; j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                int del = prev[j] + 1;
                int ins = curr[j - 1] + 1;
                int sub = prev[j - 1] + cost;
                curr[j] = Math.min(sub, Math.min(del, ins));
            }

            if (to < lb) curr[Math.min(to + 1, lb)] = maxEd + 1;

            boolean allBig = true;
            for (int j = from; j <= to; j++) {
                if (curr[j] <= maxEd) { allBig = false; break; }
            }
            if (allBig) return maxEd + 1;

            int[] tmp = prev; prev = curr; curr = tmp;
        }
        int ed = prev[lb];
        return ed > maxEd ? maxEd + 1 : ed;
    }

    private static class Scored<T> {
        final T value;
        final double score;
        Scored(T v, double s) { value = v; score = s; }
    }
}