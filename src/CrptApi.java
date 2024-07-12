import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

  private final HttpClient httpClient;
  private final Semaphore semaphore;
  private final ScheduledExecutorService scheduler;
  private final ObjectMapper objectMapper;

  public CrptApi(TimeUnit timeUnit, int requestLimit) {
    this.httpClient = HttpClient.newHttpClient();
    this.semaphore = new Semaphore(requestLimit);
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.objectMapper = new ObjectMapper();

    long delay = timeUnit.toMillis(1);
    scheduler.scheduleAtFixedRate(
        () -> semaphore.release(requestLimit - semaphore.availablePermits()), delay, delay,
        TimeUnit.MILLISECONDS);
  }

  public void createDocument(Document document, String signature) throws Exception {
    semaphore.acquire();

    String json = objectMapper.writeValueAsString(document);
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))
        .header("Content-Type", "application/json")
        .header("Signature", signature)
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException("Failed to create document: " + response.body());
    }
  }

  public static class Document {

    // Поля документа
    public Description description;
    public String doc_id;
    public String doc_status;
    public String doc_type;
    public boolean importRequest;
    public String owner_inn;
    public String participant_inn;
    public String producer_inn;
    public String production_date;
    public String production_type;
    public Product[] products;
    public String reg_date;
    public String reg_number;

    // Вложенные классы для полей документа
    public static class Description {

      public String participantInn;
    }

    public static class Product {

      public String certificate_document;
      public String certificate_document_date;
      public String certificate_document_number;
      public String owner_inn;
      public String producer_inn;
      public String production_date;
      public String tnved_code;
      public String uit_code;
      public String uitu_code;
    }
  }

  public void shutdown() {
    scheduler.shutdown();
  }

  public static void main(String[] args) throws Exception {
    CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

    Document document = new Document();
    // Заполните поля документа

    api.createDocument(document, "ваша_подпись");

    api.shutdown();
  }
}
