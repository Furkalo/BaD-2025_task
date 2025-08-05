package org.example;

import java.util.*;

public class DictionaryData {
    private static final Map<String, Integer> dictionary = new HashMap<>();

    static {
        loadDictionaryFromFile("src/main/java/org/example/dictionary.txt");
    }

    private static void loadDictionaryFromFile(String fileName) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.FileReader(fileName, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty()) {
                    if (line.contains(":")) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            try {
                                dictionary.put(parts[0], Integer.parseInt(parts[1]));
                            } catch (NumberFormatException e) {
                                dictionary.put(line, 1);
                            }
                        }
                    } else {
                        dictionary.put(line, 1);
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("Error loading dictionary file: " + e.getMessage());
        }
    }

    public static Map<String, Integer> getDictionary() {
        return Collections.unmodifiableMap(dictionary);
    }
}
