package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SmartTextRestorer {

    // Константи
    private static final int MIN_TEXT_LENGTH = 2; // тестовий поріг
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int MAX_WORD_LENGTH = 15;
    private static final int LENGTH_BONUS_MULTIPLIER = 5;
    private static final String INPUT_REGEX = "[a-zA-Z\\s.,;:'\"!?\\-\\*]*";
    private static final String BIGRAM_FILE_PATH = "src/main/java/org/example/bigrams.csv";

    static Map<String, Integer> dictionary = DictionaryData.getDictionary();
    static Map<String, Map<String, Integer>> bigramMap = new HashMap<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Завантажуємо біграми один раз
        loadBigramModelFromFile(BIGRAM_FILE_PATH);

        System.out.println("SmartTextRestorer is ready! Type 'exit' to quit.");

        while (true) {
            System.out.println("\nEnter the damaged text (" + MIN_TEXT_LENGTH + "-" + MAX_TEXT_LENGTH + " characters):");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting SmartTextRestorer. Goodbye!");
                break;
            }

            if (!isValidInput(input)) {
                System.out.println("Invalid input! Text must contain only English letters, spaces, punctuation (* allowed), length "
                        + MIN_TEXT_LENGTH + "-" + MAX_TEXT_LENGTH + " characters.");
                continue;
            }

            String restored = restore(input.toLowerCase());
            if (restored == null) {
                System.out.println("Error: Not enough words in the dictionary to restore the text. We are working on expanding the dictionary.");
            } else {
                System.out.println("\nRestored text:\n" + capitalizeFirst(restored));
            }
        }

        scanner.close();
    }

    private static boolean isValidInput(String text) {
        int length = text.length();
        if (length < MIN_TEXT_LENGTH || length > MAX_TEXT_LENGTH) return false;
        return text.matches(INPUT_REGEX);
    }

    private static String restore(String text) {
        Map<Pair<Integer, String>, Result> memo = new HashMap<>();
        Result result = dfsWithScore(text, 0, "", memo);
        return (result == null) ? null : result.sentence.trim();
    }

    private static class Result {
        String sentence;
        int score;

        Result(String sentence, int score) {
            this.sentence = sentence;
            this.score = score;
        }
    }

    private static Result dfsWithScore(String text, int index, String prevWord, Map<Pair<Integer, String>, Result> memo) {
        if (index == text.length()) return new Result("", 0);

        Pair<Integer, String> key = new Pair<>(index, prevWord);
        if (memo.containsKey(key)) return memo.get(key);

        Result bestResult = null;

        for (int len = Math.min(MAX_WORD_LENGTH, text.length() - index); len >= 1; len--) {
            String fragment = text.substring(index, index + len);
            List<String> candidates = getMatchingWords(fragment);

            for (String candidate : candidates) {
                Result next = dfsWithScore(text, index + len, candidate, memo);
                if (next != null) {
                    int baseScore = dictionary.getOrDefault(candidate, 1);
                    int lengthBonus = candidate.length() * LENGTH_BONUS_MULTIPLIER;
                    int bigramBonus = getBigramScore(prevWord, candidate);
                    int totalScore = baseScore + lengthBonus + bigramBonus + next.score;

                    if (bestResult == null || totalScore > bestResult.score) {
                        String sentence = candidate + (next.sentence.isEmpty() ? "" : " " + next.sentence);
                        bestResult = new Result(sentence, totalScore);
                    }
                }
            }
        }

        memo.put(key, bestResult);
        return bestResult;
    }

    private static List<String> getMatchingWords(String fragment) {
        List<String> result = new ArrayList<>();
        for (String word : dictionary.keySet()) {
            if (isWordMatch(word, fragment)) {
                result.add(word);
            }
        }

        result.sort((a, b) -> {
            int lenDiff = b.length() - a.length();
            if (lenDiff != 0) return lenDiff;
            return Integer.compare(dictionary.getOrDefault(b, 0), dictionary.getOrDefault(a, 0));
        });

        return result;
    }

    private static boolean isWordMatch(String word, String fragment) {
        if (word.length() != fragment.length()) return false;
        if (isExactMatch(word, fragment)) return true;
        return isAnagramMatch(word, fragment);
    }

    private static boolean isExactMatch(String word, String pattern) {
        for (int i = 0; i < word.length(); i++) {
            char pc = pattern.charAt(i);
            if (pc != '*' && pc != word.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAnagramMatch(String word, String pattern) {
        Map<Character, Integer> wordCount = new HashMap<>();
        Map<Character, Integer> patternCount = new HashMap<>();
        int stars = 0;

        for (char c : word.toCharArray()) {
            wordCount.put(c, wordCount.getOrDefault(c, 0) + 1);
        }

        for (char c : pattern.toCharArray()) {
            if (c == '*') {
                stars++;
            } else {
                patternCount.put(c, patternCount.getOrDefault(c, 0) + 1);
            }
        }

        int covered = 0;
        for (Map.Entry<Character, Integer> entry : patternCount.entrySet()) {
            char c = entry.getKey();
            int needed = entry.getValue();
            int available = wordCount.getOrDefault(c, 0);

            if (available < needed) return false;
            covered += needed;
        }

        return (word.length() - covered) == stars;
    }

    private static int getBigramScore(String prev, String current) {
        if (prev.isEmpty()) return 0;
        return bigramMap.getOrDefault(prev, Collections.emptyMap()).getOrDefault(current, 0);
    }

    private static void addBigram(String w1, String w2, int score) {
        bigramMap.computeIfAbsent(w1, k -> new HashMap<>()).put(w2, score);
    }

    private static void loadBigramModelFromFile(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String w1 = parts[0].trim();
                    String w2 = parts[1].trim();
                    int score = Integer.parseInt(parts[2].trim());
                    addBigram(w1, w2, score);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading bigram model: " + e.getMessage());
        }
    }

    private static String capitalizeFirst(String sentence) {
        if (sentence.isEmpty()) return sentence;
        return Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
    }

    private record Pair<K, V>(K first, V second) {

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pair)) return false;
            Pair<?, ?> p = (Pair<?, ?>) o;
            return Objects.equals(first, p.first) && Objects.equals(second, p.second);
        }
    }
}