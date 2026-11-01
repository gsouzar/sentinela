import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Copie para o backend Java do cliente, adaptando o pacote. Nunca embuta a chave no front-end. */
public final class SentinelaReporter {
 private final HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(3)).build();
 private final URI endpoint;
 private final String key;
 public SentinelaReporter(String endpoint,String key){
  this.endpoint=URI.create(endpoint);this.key=key;String host=this.endpoint.getHost();
  if(host==null||this.endpoint.getUserInfo()!=null||this.endpoint.getQuery()!=null||this.endpoint.getFragment()!=null||!"/api/v1/ingest/events".equals(this.endpoint.getPath())||!("https".equals(this.endpoint.getScheme())||"http".equals(this.endpoint.getScheme())&&List.of("localhost","127.0.0.1").contains(host))||key==null||key.length()<32)throw new IllegalArgumentException("Endpoint/chave inválidos");
 }
 public CompletableFuture<Boolean> report(String type,boolean simulated){
  if(!Set.of("AUTH_FAILURE","RATE_LIMIT","WAF_BLOCK","INTEGRITY_ALERT","APPLICATION_ERROR").contains(type))throw new IllegalArgumentException("Tipo inválido");
  String body="{\"eventId\":\""+UUID.randomUUID()+"\",\"type\":\""+type+"\",\"occurredAt\":"+System.currentTimeMillis()+",\"simulated\":"+simulated+"}";
  var request=HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8)).header("Content-Type","application/json").header("X-Sentinela-Site-Key",key).POST(HttpRequest.BodyPublishers.ofString(body)).build();
  return client.sendAsync(request,HttpResponse.BodyHandlers.discarding()).thenApply(response->response.statusCode()==200).exceptionally(error->false);
 }
}
