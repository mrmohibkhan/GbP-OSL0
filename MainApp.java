import java.util.List;

public class MainApp {
public static void abc(String x, int y)
{
      // Example query
        String query = x;
        int numResults = y;

        // Perform the search using GoogleSearchService
        SearchResult result = GoogleSearchService.performSearch(query, numResults);

        // Display the time taken for the search
        System.out.println("Time taken: " + result.getTimeTaken() + " ms");

        // Display the search results (URLs)
        List<String> urls = result.getUrls();
        if (!urls.isEmpty()) {
            System.out.println("Search Results:");
            for (String url : urls) {
                System.out.println(url);
            }
        } else {
            System.out.println("No results found.");
        }

}
    public static void main(String[] args) {
  
String x="Pakistan zindabad";
int y=10;

abc(x,y);

    }
}
