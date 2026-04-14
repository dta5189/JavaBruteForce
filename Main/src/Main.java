import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Main {

    // Arabic letter frequencies (right-to-left order as given)
    private static final double[] ARABIC_FREQUENCIES = {
            11.6, // ا (Alif)
            4.8, // ب
            3.7, // ت
            1.1, // ث
            2.8, // ج
            2.6, // ح
            1.1, // خ
            3.5, // د
            1.0, // ذ
            4.7, // ر
            0.9, // ز
            6.5, // س
            3.0, // ش
            2.9, // ص
            1.5, // ض
            1.7, // ط
            0.7, // ظ
            3.9, // ع
            1.0, // غ
            3.0, // ف
            2.7, // ق
            3.6, // ك
            5.3, // ل
            3.1, // م
            7.2, // ن
            2.5, // ه
            6.0, // و
            6.7  // ي
    };

    // Arabic alphabet in right-to-left order
    private static final char[] ARABIC_ALPHABET = {
            'ا','ب','ت','ث','ج','ح','خ','د','ذ','ر','ز','س',
            'ش','ص','ض','ط','ظ','ع','غ','ف','ق','ك','ل','م','ن','ه','و','ي'
    };

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // 1. Get plaintext and API key from user
        System.out.print("Enter plaintext message: ");
        String plaintext = scanner.nextLine();

        System.out.print("Enter your Google API Key: ");
        String apiKey = scanner.nextLine();

        System.out.print("Enter Caesar Cipher shift key (integer): ");
        int shift = Integer.parseInt(scanner.nextLine().trim());

        // 2. Translate to Arabic using Google Translate API
        System.out.println("\n--- Google Translate ---");
        String translated = translateToArabic(plaintext, apiKey);
        System.out.println("Translated (Arabic): " + translated);

        // 3. Caesar Cipher encrypt the translated text
        System.out.println("\n--- Caesar Cipher Encryption ---");
        String encrypted = caesarEncrypt(translated, shift);
        System.out.println("Encrypted text: " + encrypted);

        // 4. Frequency Analysis on the encrypted Arabic text
        System.out.println("\n--- Frequency Analysis ---");
        frequencyAnalysis(encrypted);

        scanner.close();
    }

    // Google Translate REST call
    public static String translateToArabic(String text, String apiKey) throws Exception {
        String encodedText = URLEncoder.encode(text, "UTF-8");
        String urlStr = "https://translation.googleapis.com/language/translate/v2"
                + "?q=" + encodedText
                + "&target=ar"
                + "&format=text"
                + "&key=" + apiKey;

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept-Charset", "UTF-8");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errResponse = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) errResponse.append(line);
            throw new RuntimeException("Google API error " + responseCode + ": " + errResponse);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) response.append(line);
        in.close();

        // Print raw JSON so we can see exactly what came back
        System.out.println("Raw JSON: " + response.toString());

        String json = response.toString();
        String marker = "\"translatedText\":\"";
        int start = json.indexOf(marker) + marker.length();

        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    // Caesar Cipher for Arabic
    public static String caesarEncrypt(String text, int shift) {
        int alphabetSize = ARABIC_ALPHABET.length;
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            int index = findInAlphabet(c);
            if (index != -1) {
                int newIndex = (index + shift) % alphabetSize;
                if (newIndex < 0) newIndex += alphabetSize;
                result.append(ARABIC_ALPHABET[newIndex]);
            } else {
                result.append(c); // keep spaces, punctuation as-is
            }
        }
        return result.toString();
    }

    private static int findInAlphabet(char c) {
        for (int i = 0; i < ARABIC_ALPHABET.length; i++) {
            if (ARABIC_ALPHABET[i] == c) return i;
        }
        return -1;
    }

    //  Frequency Analysis
    public static void frequencyAnalysis(String text) {
        int[] counts = new int[ARABIC_ALPHABET.length];
        int total = 0;

        for (char c : text.toCharArray()) {
            int index = findInAlphabet(c);
            if (index != -1) {
                counts[index]++;
                total++;
            }
        }

        if (total == 0) {
            System.out.println("No Arabic characters found for analysis.");
            return;
        }

        System.out.println("Character | Observed% | Expected%");
        System.out.println("----------|-----------|----------");
        for (int i = 0; i < ARABIC_ALPHABET.length; i++) {
            double observed = (counts[i] * 100.0) / total;
            System.out.printf("    %c     |  %5.2f%%  |  %5.2f%%\n",
                    ARABIC_ALPHABET[i], observed, ARABIC_FREQUENCIES[i]);
        }

        // Best shift guess using chi-squared
        System.out.println("\nBrute-force shift guesses (lowest chi-squared = best fit):");
        for (int guessShift = 0; guessShift < ARABIC_ALPHABET.length; guessShift++) {
            double chi = chiSquared(counts, total, guessShift);
            System.out.printf("Shift %2d: chi² = %.2f\n", guessShift, chi);
        }
    }

    private static double chiSquared(int[] counts, int total, int shift) {
        double chi = 0;
        int n = ARABIC_ALPHABET.length;
        for (int i = 0; i < n; i++) {
            int shiftedIndex = (i - shift + n) % n;
            double expected = (ARABIC_FREQUENCIES[shiftedIndex] / 100.0) * total;
            double diff = counts[i] - expected;
            chi += (diff * diff) / expected;
        }
        return chi;
    }
}