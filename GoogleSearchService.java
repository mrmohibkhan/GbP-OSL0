import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class GoogleSearchService {

    // Replace with your actual API key and Search Engine ID
    private static final String API_KEY = "insert your API";
    private static final String SEARCH_ENGINE_ID = "insert your cxd";

    // Method to perform a search and return the list of URLs and time taken
    public static SearchResult performSearch(String query, int numResults) {
        List<String> searchResults = new ArrayList<>();
        long elapsedTime = 0;
        
        try {
            // URL encode the query to handle special characters
            String encodedQuery = URLEncoder.encode(query, "UTF-8");

            // Construct the search URL
            String searchURL = "https://www.googleapis.com/customsearch/v1?key=" + API_KEY +
                               "&cx=" + SEARCH_ENGINE_ID + 
                               "&q=" + encodedQuery + "&num=" + numResults;

            // Start time: Before sending the HTTP request
            long startTime = System.currentTimeMillis();  // You can also use System.nanoTime()

            // Send the HTTP request and get the response
            String jsonResponse = sendHttpRequest(searchURL);

            // End time: After receiving the response
            long endTime = System.currentTimeMillis();  // Or System.nanoTime()

            // Calculate the time difference
            elapsedTime = endTime - startTime;  // In milliseconds

            // Parse the search results and get the URLs
            searchResults = parseResults(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return the search results along with the time taken
        return new SearchResult(searchResults, elapsedTime);
    }

    // Helper method to send HTTP GET request and return the response
    private static String sendHttpRequest(String searchURL) throws Exception {
        URL url = new URL(searchURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) { // Success
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            return content.toString();
        } else {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            String inputLine;
            StringBuilder errorContent = new StringBuilder();
            while ((inputLine = errorReader.readLine()) != null) {
                errorContent.append(inputLine);
            }
            errorReader.close();
            throw new Exception("Failed : HTTP Error code : " + responseCode + " Response: " + errorContent.toString());
        }
    }

    // Helper method to parse search results from JSON response and return URLs
    private static List<String> parseResults(String jsonResponse) {
        List<String> urls = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(jsonResponse);
        
        if (jsonObject.has("items")) {
            JSONArray items = jsonObject.getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String link = item.getString("link");
                urls.add(link);  // Add the URL to the list
            }
        }

        return urls;
    }
}
