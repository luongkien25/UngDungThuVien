package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ImageCache {

    // Kích thước thumbnail (khớp JLabel 136x200)
    private static final int W = 136, H = 200, TIMEOUT_MS = 8000;

    // === Cấu hình: DÙNG RAM NHIỀU HƠN, GIỮ CACHE ĐẾN KHI JVM TẮT ===
    // 1.5 GB mặc định (bạn có thể set từ Main)
    private volatile long maxBytes = 1_500L * 1024 * 1024;
    private long currentBytes = 0;

    // Thread pool tải ảnh
    private static final int CORE_THREADS = 6, MAX_THREADS = 12, QUEUE_CAP = 2000;
    private final ThreadPoolExecutor executor;

    // Placeholder / Error icon
    private static final ImageIcon PLACEHOLDER = createPlaceholder("IMG");
    private static final ImageIcon ERROR_ICON  = createPlaceholder("ERR");

    // Singleton
    private static final ImageCache INSTANCE = new ImageCache();
    public static ImageCache getInstance() { return INSTANCE; }

    // LRU theo dung lượng: url -> ImageIcon (strong reference)
    // accessOrder=true để LRU theo truy cập
    private final LinkedHashMap<String, ImageIcon> lru =
            new LinkedHashMap<>(256, 0.75f, true);

    // Gộp request cùng URL
    private final ConcurrentHashMap<String, CompletableFuture<ImageIcon>> inflight = new ConcurrentHashMap<>();

    private ImageCache() {
        // Tắt ImageIO disk cache → đọc thẳng vào RAM (mượt hơn)
        javax.imageio.ImageIO.setUseCache(false);

        this.executor = new ThreadPoolExecutor(
                CORE_THREADS, MAX_THREADS,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAP),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );
        // Giữ core threads (không timeout) để preload ổn định hơn
        this.executor.allowCoreThreadTimeOut(false);
    }

    // ===================== API CHÍNH =====================

    /** Trả placeholder ngay; khi ảnh xong sẽ gọi onReady(icon) trên EDT. */
    public ImageIcon getIconAsync(String url, java.util.function.Consumer<ImageIcon> onReady) {
        if (url == null || url.isBlank() || "N/A".equalsIgnoreCase(url)) return ERROR_ICON;

        // 1) Cache hit
        ImageIcon hit;
        synchronized (lru) { hit = lru.get(url); }
        if (hit != null) return hit;

        // 2) Coalesce
        CompletableFuture<ImageIcon> fut = inflight.computeIfAbsent(url, u ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        ImageIcon icon = downloadScaled(u); // may throw
                        putInternal(u, icon);
                        return icon;
                    } catch (Exception e) {
                        putInternal(u, ERROR_ICON);
                        return ERROR_ICON;
                    } finally {
                        inflight.remove(u);
                    }
                }, executor)
        );

        // 3) Callback an toàn trên EDT
        fut.thenAccept(icon -> {
            if (onReady != null) SwingUtilities.invokeLater(() -> onReady.accept(icon));
        });

        // 4) Trả placeholder ngay để UI hiển thị tức thì
        return PLACEHOLDER;
    }

    /** Overload: tự setIcon cho JLabel, tránh race khi list tái dùng label. */
    public ImageIcon getIconAsync(String url, JLabel targetLabel) {
        if (targetLabel != null) targetLabel.putClientProperty("img.url", url);
        return getIconAsync(url, icon -> {
            if (targetLabel != null) {
                Object u = targetLabel.getClientProperty("img.url");
                if (u != null && u.equals(url)) {
                    targetLabel.setIcon(icon);
                    targetLabel.revalidate();
                    targetLabel.repaint();
                }
            }
        });
    }

    /** Overload tương thích chữ ký cũ. */
    public ImageIcon getIconAsync(String isbn, String url, int hintHeight, JLabel targetLabel) {
        return getIconAsync(url, targetLabel);
    }

    // ===================== QUẢN LÝ CACHE =====================

    /** Cho phép đổi ngân sách RAM khi chạy (bytes). */
    public void setMaxBytes(long bytes) {
        synchronized (lru) {
            this.maxBytes = Math.max(bytes, 64L * 1024 * 1024); // tối thiểu 64MB
            trimToBudget();
        }
    }

    /** Xoá cache thủ công (ít dùng). */
    public void clear() {
        synchronized (lru) {
            lru.clear();
            currentBytes = 0;
        }
        inflight.clear();
    }

    /** Ngừng tải thêm ảnh; giữ cache trong RAM đến khi JVM tắt. */
    public void shutdown() {
        try { executor.shutdownNow(); } catch (Throwable ignore) {}
        // KHÔNG clear LRU —> giữ ảnh đến lúc JVM thoát hẳn
    }

    public boolean isShutdown() {
        return executor.isShutdown() || executor.isTerminated();
    }

    // ===================== NỘI BỘ =====================

    private static ImageIcon createPlaceholder(String text) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, W, H);
            g.setColor(new Color(210, 210, 210));
            g.drawRect(0, 0, W - 1, H - 1);
            g.setFont(new Font("Segoe UI", Font.BOLD, 18));
            FontMetrics fm = g.getFontMetrics();
            int x = (W - fm.stringWidth(text)) / 2;
            int y = (H - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(new Color(150, 150, 150));
            g.drawString(text, x, y);
        } finally {
            g.dispose();
        }
        return new ImageIcon(img);
    }

    /** Tải ảnh, scale về W×H, vẽ lên TYPE_INT_RGB nền trắng để không bị đen. */
    private static ImageIcon downloadScaled(String urlStr) throws Exception {
        if (urlStr.startsWith("http://")) {
            urlStr = "https://" + urlStr.substring(7);
        }

        Image src = fetchImage(urlStr);
        if (src == null) throw new RuntimeException("download failed");

        BufferedImage dst = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = dst.createGraphics();
        try {
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, W, H);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(src, 0, 0, W, H, null);
        } finally {
            g2.dispose();
        }
        return new ImageIcon(dst);
    }

    private static Image fetchImage(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122 Safari/537.36");
            // Loại WEBP cứng: chỉ nhận jpeg/png/avif
            conn.setRequestProperty("Accept", "image/avif,image/jpeg,image/png,*/*;q=0.5");

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;

            String ctype = conn.getContentType();
            if (ctype != null && ctype.toLowerCase().contains("image/webp")) return null;

            // 1) Thử ImageIO
            try (InputStream in = conn.getInputStream()) {
                Image img = javax.imageio.ImageIO.read(in);
                if (img != null) return img;
            }

            // 2) Fallback: Toolkit
            try (InputStream in2 = ((HttpURLConnection) url.openConnection()).getInputStream()) {
                byte[] data = in2.readAllBytes();
                Image img2 = Toolkit.getDefaultToolkit().createImage(data);
                MediaTracker mt = new MediaTracker(new Canvas());
                mt.addImage(img2, 0);
                mt.waitForID(0, TIMEOUT_MS);
                if (!mt.isErrorAny() && img2.getWidth(null) > 0 && img2.getHeight(null) > 0) {
                    return img2;
                }
            }

            return null;
        } catch (Exception ignore) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static long estimateBytes(ImageIcon icon) {
        // Ưu tiên lấy từ BufferedImage
        Image img = icon.getImage();
        if (img instanceof BufferedImage bi) {
            return (long) bi.getWidth() * bi.getHeight() * 4; // ARGB/RGB ~ 4 bytes/pixel (an toàn)
        }
        int w = Math.max(1, icon.getIconWidth());
        int h = Math.max(1, icon.getIconHeight());
        return (long) w * h * 4;
    }

    private void putInternal(String url, ImageIcon icon) {
        long add = estimateBytes(icon);
        synchronized (lru) {
            ImageIcon old = lru.put(url, icon);
            if (old != null) currentBytes -= estimateBytes(old);
            currentBytes += add;
            trimToBudget();
        }
    }

    /** Loại bỏ từ mục LRU cũ nhất cho tới khi <= maxBytes. */
    private void trimToBudget() {
        var it = lru.entrySet().iterator();
        while (currentBytes > maxBytes && it.hasNext()) {
            Map.Entry<String, ImageIcon> e = it.next();
            ImageIcon removed = e.getValue();
            currentBytes -= estimateBytes(removed);
            it.remove();
        }
    }
}
