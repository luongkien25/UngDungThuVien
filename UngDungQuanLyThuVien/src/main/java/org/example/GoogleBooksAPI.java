package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class GoogleBooksAPI {
    public static List<Book> searchBooks(String keyword) throws IOException {
        List<Book> books = new ArrayList<>();
        String apiUrl = "https://www.googleapis.com/books/v1/volumes?q=" + URLEncoder.encode(keyword, "UTF-8");

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();

        JSONObject json = new JSONObject(response.toString());
        JSONArray items = json.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject volumeInfo = items.getJSONObject(i).getJSONObject("volumeInfo");
                String title = volumeInfo.optString("title", "No Title");
                String authors = volumeInfo.has("authors") ? String.join(", ", volumeInfo.getJSONArray("authors").toList().toArray(new String[0])) : "Unknown Author";
                String category = volumeInfo.has("categories") ? volumeInfo.getJSONArray("categories").optString(0, "Unknown") : "Unknown";
                String isbn = "N/A";
                JSONArray identifiers = volumeInfo.optJSONArray("industryIdentifiers");
                if (identifiers != null) {
                    for (int j = 0; j < identifiers.length(); j++) {
                        JSONObject id = identifiers.getJSONObject(j);
                        if (id.getString("type").equals("ISBN_13")) {
                            isbn = id.getString("identifier");
                            break;
                        }
                    }
                }

                String thumbnail = "";
                if (volumeInfo.has("imageLinks")) {
                    JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
                    thumbnail = imageLinks.optString("thumbnail", "");
                }

                Book book = new Book(title, authors, category, isbn, 1, thumbnail);
                books.add(book);
            }
        }

        return books;
    }
}
