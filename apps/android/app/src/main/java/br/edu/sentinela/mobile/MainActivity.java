package br.edu.sentinela.mobile;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import org.json.*;

/** Interface Android nativa em Java. Nenhum WebView e nenhum acesso a URLs analisadas. */
public final class MainActivity extends Activity {
  private final int ink = Color.rgb(28, 48, 57),
      green = Color.rgb(22, 122, 99),
      muted = Color.rgb(81, 105, 96);
  private LinearLayout root, body;
  private TextView status;
  private SecureTokens tokens;
  private ApiClient api;
  private JSONObject user, selected;
  private int generation = 0, historyPage = 0;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  interface Work {
    JSONObject run() throws Exception;
  }

  interface Done {
    void accept(JSONObject result) throws Exception;
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    getWindow().setStatusBarColor(Color.rgb(16, 31, 44));
    getWindow().setNavigationBarColor(Color.rgb(16, 31, 44));
    tokens = new SecureTokens(this);
    api = new ApiClient(tokens);
    if (state != null)
      try {
        String value = state.getString("selected");
        if (value != null) selected = new JSONObject(value);
      } catch (Exception ignored) {
      }
    if (tokens.get("access").isEmpty()) auth(false);
    else {
      shell("Preparando seu espaço", false);
      button("Tentar conexão novamente", this::recreate);
      run(
          null,
          () -> api.call("/me", "GET", null),
          r -> {
            user = r;
            home();
          });
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    if (selected != null) out.putString("selected", selected.toString());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    executor.shutdownNow();
  }

  private int dp(int x) {
    return Math.round(x * getResources().getDisplayMetrics().density);
  }

  private GradientDrawable bg(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private TextView text(String s, int size, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextColor(color);
    t.setTextSize(size);
    t.setLineSpacing(dp(3), 1);
    if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    t.setTextIsSelectable(true);
    return t;
  }

  private void gap(int n) {
    Space v = new Space(this);
    body.addView(v, new LinearLayout.LayoutParams(1, dp(n)));
  }

  private void paragraph(String s) {
    body.addView(text(s, 14, muted, false));
    gap(14);
  }

  private void heading(String s) {
    body.addView(text(s, 25, ink, true));
    gap(12);
  }

  private Button button(String title, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTextSize(14);
    b.setTextColor(Color.WHITE);
    b.setBackground(bg(green, 10));
    b.setMinHeight(dp(50));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
    lp.setMargins(0, dp(7), 0, dp(7));
    body.addView(b, lp);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private EditText field(String label, String hint, int type) {
    TextView l = text(label, 12, ink, true);
    body.addView(l);
    EditText f = new EditText(this);
    f.setHint(hint);
    f.setInputType(type);
    f.setTextSize(14);
    f.setSingleLine(true);
    f.setPadding(dp(12), 0, dp(12), 0);
    f.setBackground(bg(Color.WHITE, 8));
    f.setFilters(
        new android.text.InputFilter[] {
          new android.text.InputFilter.LengthFilter(
              type == InputType.TYPE_TEXT_VARIATION_URI ? 2048 : 1500)
        });
    l.setLabelFor(View.generateViewId());
    f.setId(l.getLabelFor());
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
    lp.setMargins(0, dp(7), 0, dp(18));
    body.addView(f, lp);
    return f;
  }

  private void shell(String title, boolean navigation) {
    generation++;
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.rgb(245, 248, 247));
    root.setPadding(0, dp(24), 0, dp(22));
    setContentView(root);
    LinearLayout brandRow = new LinearLayout(this);
    brandRow.setOrientation(LinearLayout.HORIZONTAL);
    brandRow.setGravity(Gravity.CENTER_VERTICAL);
    brandRow.setPadding(dp(22), dp(12), dp(22), dp(12));
    brandRow.setBackgroundColor(Color.rgb(16, 31, 44));
    ImageView logo = new ImageView(this);
    android.graphics.BitmapFactory.Options logoOptions = new android.graphics.BitmapFactory.Options();
    logoOptions.inSampleSize = 8;
    logo.setImageBitmap(android.graphics.BitmapFactory.decodeResource(getResources(), R.mipmap.sentinela_logo, logoOptions));
    logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    brandRow.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));
    TextView brand = text("sentinela.", 25, Color.WHITE, true);
    brand.setPadding(dp(10), 0, 0, 0);
    brand.setTextIsSelectable(false);
    brandRow.addView(brand);
    root.addView(brandRow);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(dp(23), dp(25), dp(23), dp(25));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    heading(title);
    status = text("", 13, Color.rgb(166, 76, 79), false);
    status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    body.addView(status);
    gap(8);
    if (navigation) {
      LinearLayout nav = new LinearLayout(this);
      nav.setPadding(dp(8), dp(7), dp(8), dp(5));
      nav.setBackgroundColor(Color.WHITE);
      String[] names = {"Início", "Histórico", "Assistente", "Conta"};
      Runnable[] actions = {
        this::home,
        () -> {
          historyPage = 0;
          history();
        },
        this::chat,
        this::account
      };
      for (int i = 0; i < names.length; i++) {
        Button b = new Button(this);
        b.setText(names[i]);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setTextColor(green);
        b.setMinHeight(dp(48));
        b.setBackgroundColor(Color.WHITE);
        final Runnable action = actions[i];
        b.setOnClickListener(v -> action.run());
        nav.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1));
      }
      root.addView(nav);
    }
  }

  private void run(Button source, Work work, Done done) {
    final int ticket = generation;
    if (source != null) source.setEnabled(false);
    status.setText("Aguarde…");
    executor.execute(
        () -> {
          try {
            JSONObject r = work.run();
            runOnUiThread(
                () -> {
                  if (isDestroyed() || generation != ticket) return;
                  if (source != null) source.setEnabled(true);
                  status.setText("");
                  try {
                    done.accept(r);
                  } catch (Exception x) {
                    status.setText("Não foi possível apresentar o resultado.");
                  }
                });
          } catch (Exception x) {
            runOnUiThread(
                () -> {
                  if (isDestroyed() || generation != ticket) return;
                  if (source != null) source.setEnabled(true);
                  status.setText(
                      x instanceof java.net.UnknownHostException
                          ? "Sem conexão. Verifique a internet e tente novamente."
                          : x.getMessage() == null ? "Não foi possível conectar." : x.getMessage());
                  if (x instanceof ApiClient.Failure
                      && ((ApiClient.Failure) x).status == 401
                      && tokens.get("access").isEmpty()) {
                    user = null;
                    auth(false);
                  }
                });
          }
        });
  }

  private JSONObject obj(Object... pairs) throws JSONException {
    JSONObject o = new JSONObject();
    for (int i = 0; i < pairs.length; i += 2) o.put(pairs[i].toString(), pairs[i + 1]);
    return o;
  }

  private void auth(boolean register) {
    user = null;
    selected = null;
    shell(register ? "Sua proteção começa aqui." : "Bom ter você aqui.", false);
    paragraph("Analise links suspeitos e entenda os sinais antes de clicar.");
    EditText name =
        register
            ? field(
                "Seu nome",
                "Como podemos chamar você?",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS)
            : null;
    EditText email =
        field(
            "Email",
            "voce@exemplo.com",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    EditText password =
        field(
            "Senha",
            register ? "Pelo menos 12 caracteres" : "Sua senha",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    EditText code =
        register
            ? null
            : field(
                "Segundo fator (se ativo)",
                "Autenticador ou recuperação",
                InputType.TYPE_CLASS_TEXT);
    Button submit = button(register ? "Criar minha conta" : "Entrar", () -> {});
    submit.setOnClickListener(
        v -> {
          String mail = email.getText().toString().trim(), pass = password.getText().toString();
          if (!android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches()
              || register && pass.length() < 12
              || pass.isEmpty()) {
            status.setText("Confira o email e a senha. No cadastro, use pelo menos 12 caracteres.");
            return;
          }
          run(
              submit,
              () -> {
                JSONObject b = obj("email", mail, "password", pass);
                if (register) b.put("name", name.getText().toString().trim());
                else if (code.length() > 0) b.put("code", code.getText().toString().trim());
                JSONObject r = api.call("/auth/" + (register ? "register" : "login"), "POST", b);
                api.save(r);
                return r;
              },
              r -> {
                user = r.getJSONObject("user");
                home();
              });
        });
    button(register ? "Já tenho conta" : "Criar uma conta", () -> auth(!register));
    gap(15);
    paragraph(
        "Projeto acadêmico. O score é uma avaliação por critérios, não uma garantia de segurança.");
  }

  private void home() {
    if(user.optBoolean("mfa_required") && !user.optBoolean("mfa_enabled")) { account(); return; }
    shell("Seu espaço de proteção.", true);
    button("Meus sites · monitoramento", this::sites);
    gap(12);
    paragraph(
        "Olá, "
            + user.optString("name", "pessoa")
            + ". Cole um link para conferir os sinais de risco.");
    EditText url =
        field(
            "Link para análise",
            "https://exemplo.com",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    paragraph(
        "O endereço não será aberto. Query e fragmento são removidos antes de armazenar e consultar"
            + " reputação.");
    Button analyze = button("Analisar link", () -> {});
    analyze.setOnClickListener(
        v -> {
          String value = url.getText().toString().trim();
          if (value.isEmpty()) {
            url.setError("Cole uma URL.");
            return;
          }
          run(
              analyze,
              () -> api.call("/analyses", "POST", obj("url", value)),
              r -> {
                selected = r;
                result();
              });
        });
    gap(20);
    body.addView(text("EXEMPLOS CONTROLADOS", 11, green, true));
    button("Experimentar: poucos indicadores", () -> url.setText("https://example.org"));
    button(
        "Experimentar: imitação de marca",
        () -> url.setText("http://banc0-seguranca-login.test/confirmar"));
    paragraph(
        "O segundo exemplo só recebe correspondência sintética com DEMO_MODE ativo. Nenhum"
            + " resultado é pré-fixado no aplicativo.");
  }

  private void sites() {
    if(user.optBoolean("mfa_required") && !user.optBoolean("mfa_enabled")) { account(); return; }
    shell("Meus sites", true);
    paragraph("Verificações externas e eventos do conector. Cada domínio precisa de confirmação DNS. Não há garantia de segurança 100%.");
    button("Adicionar site", () -> siteForm(null));
    run(null, () -> api.call("/sites", "GET", null), result -> {
      JSONArray items=result.optJSONArray("items");
      if(items==null || items.length()==0) { paragraph("Nenhum site cadastrado. Adicione uma URL para abrir seu painel."); return; }
      for(int i=0;i<items.length();i++) {
        final JSONObject site=items.getJSONObject(i);
        button(site.optString("name") + " · " + site.optString("domain"), () -> siteDashboard(site.optString("id")));
        paragraph(site.optBoolean("enabled") ? site.optBoolean("verified") ? "Domínio confirmado" : "Aguardando confirmação DNS" : "Monitoramento pausado");
      }
    });
  }

  private void siteForm(JSONObject site) {
    shell(site==null ? "Cadastrar site" : "Editar site", true);
    EditText name=field("Nome do site", "Minha loja", InputType.TYPE_CLASS_TEXT);
    EditText url=field("URL pública", "https://meusite.com.br", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    if(site!=null) { name.setText(site.optString("name")); url.setText(site.optString("origin")); }
    paragraph("Apenas domínios sob seu controle e portas padrão. A raiz é monitorada. Trocar a origem exige nova confirmação e revoga a chave anterior; o histórico fica separado por revisão.");
    Button save=button("Salvar site", () -> {});
    save.setOnClickListener(v -> run(save, () -> api.call(site==null ? "/sites" : "/sites/"+site.getString("id"), site==null ? "POST" : "PATCH", obj("name", name.getText().toString(), "url", url.getText().toString())), result -> siteDashboard(result.getString("id"))));
  }

  private void siteDashboard(String id) {
    shell("Painel do site", true);
    button("Todos os sites", this::sites);
    run(null, () -> api.call("/sites/"+id+"/dashboard?days=7", "GET", null), result -> {
      JSONObject site=result.getJSONObject("site"),latest=result.optJSONObject("latest");
      heading(site.optString("name"));paragraph(site.optString("origin")+" · revisão "+site.optInt("revision"));
      button("Editar domínio ou nome", () -> siteForm(site));
      button(site.optBoolean("enabled") ? "Pausar monitoramento" : "Retomar monitoramento", () -> run(null, () -> api.call("/sites/"+id+"/status", "PATCH", obj("enabled",!site.optBoolean("enabled"))), value -> siteDashboard(id)));
      if(!site.optBoolean("verified")) {
        heading("Confirmar domínio");
        paragraph("No painel DNS do provedor, crie TXT. Nome: "+site.optString("dnsName")+". Valor: "+site.optString("dnsValue")+". Se o provedor acrescenta o domínio, use _sentinela no Nome. Mantenha o TXT para a coleta continuar.");
        button("Já adicionei: confirmar DNS", () -> run(null, () -> api.call("/sites/"+id+"/verify", "POST", null), value -> siteDashboard(id)));
      }
      gap(15);heading("Últimos sete dias");
      paragraph("Disponibilidade nas amostras: "+(result.isNull("availability") ? "sem dados" : result.optDouble("availability")+"%")+" · "+result.optInt("sampleCount")+" verificações.");
      if(latest!=null) {
        paragraph("Último status: "+latest.optString("status")+" · HTTP "+latest.optInt("httpCode")+" · "+latest.optLong("responseMs")+" ms. "+latest.optString("message"));
        if(!latest.isNull("tlsExpires")) paragraph("Certificado válido na amostra até "+new java.text.SimpleDateFormat("dd/MM/yyyy",java.util.Locale.getDefault()).format(new java.util.Date(latest.getLong("tlsExpires"))));
        JSONArray findings=latest.optJSONArray("findings");if(findings!=null)for(int i=0;i<findings.length();i++)paragraph(findings.getJSONObject(i).optString("title")+". "+findings.getJSONObject(i).optString("detail"));
      } else paragraph("Aguardando primeira verificação. Sem gráficos ou indicadores inventados.");
      JSONArray checks=result.optJSONArray("checks");
      if(checks!=null && checks.length()>0) {
        final JSONArray samples=checks;
        View chart=new View(this) { protected void onDraw(android.graphics.Canvas canvas) {
          super.onDraw(canvas); android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);paint.setColor(green);paint.setStrokeWidth(dp(2));paint.setStyle(android.graphics.Paint.Style.STROKE);
          android.graphics.Path path=new android.graphics.Path(); int count=Math.min(60,samples.length()),start=samples.length()-count;double max=1;
          for(int i=start;i<samples.length();i++)max=Math.max(max,samples.optJSONObject(i).optDouble("response_ms"));
          for(int n=0;n<count;n++){float x=dp(10)+(getWidth()-dp(20))*n/(float)Math.max(1,count-1);float y=getHeight()-dp(10)-(float)(samples.optJSONObject(start+n).optDouble("response_ms")/max*(getHeight()-dp(20)));if(n==0)path.moveTo(x,y);else path.lineTo(x,y);}canvas.drawPath(path,paint);
        }};
        chart.setContentDescription("Gráfico dos tempos das últimas verificações, em milissegundos");body.addView(chart,new LinearLayout.LayoutParams(-1,dp(140)));
      }
      paragraph("Eventos reportados: "+result.optInt("reportedEvents")+". Simulados: "+result.optInt("simulatedEvents")+". Relatos não comprovam invasão. A chave do conector é configurada pela área web.");
      button("Atualizar painel", () -> siteDashboard(id));
      if(site.optBoolean("verified")&&site.optBoolean("enabled"))button("Enfileirar verificação", () -> run(null, () -> api.call("/sites/"+id+"/check", "POST", null), value -> {status.setText("Verificação enfileirada. Atualize o painel após a coleta.");}));
      EditText question=field("Pergunta sobre este site", "Como está meu site?",InputType.TYPE_CLASS_TEXT);
      button("Conversar sobre este site", () -> run(null, () -> api.call("/sites/"+id+"/assistant", "POST", obj("question",question.getText().toString())), value -> paragraph(value.optString("answer"))));
    });
  }

  private String risk(String s) {
    return s.equals("HIGH") ? "Alto risco" : s.equals("MEDIUM") ? "Risco médio" : "Baixo risco";
  }

  private String source(String s) {
    switch (s) {
      case "FOUND":
        return "Correspondência encontrada";
      case "NOT_FOUND":
        return "Sem correspondência";
      case "UNAVAILABLE":
        return "Indisponível";
      default:
        return "Não configurada";
    }
  }

  private void result() {
    if (selected == null) {
      home();
      return;
    }
    shell("Entenda este resultado.", true);
    body.addView(
        text(
            risk(selected.optString("risk")),
            20,
            selected.optString("risk").equals("HIGH")
                ? Color.rgb(155, 63, 70)
                : selected.optString("risk").equals("MEDIUM") ? Color.rgb(125, 91, 25) : green,
            true));
    body.addView(text(selected.optInt("score") + " / 100", 48, ink, true));
    paragraph("Pontuação de risco, não probabilidade de fraude.");
    body.addView(text(selected.optString("domain"), 19, ink, true));
    paragraph(selected.optString("url"));
    if (selected.optString("coverage").equals("PARTIAL"))
      paragraph(
          "COBERTURA PARCIAL: algumas fontes não foram consultadas. Isso não significa que o link é"
              + " seguro.");
    gap(10);
    body.addView(text("Indicadores encontrados", 18, ink, true));
    JSONArray indicators = selected.optJSONArray("indicators");
    if (indicators == null || indicators.length() == 0)
      paragraph("Nenhum indicador nas regras locais. Baixo risco não garante segurança.");
    else
      for (int i = 0; i < indicators.length(); i++) {
        JSONObject item = indicators.optJSONObject(i);
        gap(12);
        body.addView(text(item.optString("title") + " · +" + item.optInt("weight"), 15, ink, true));
        paragraph(item.optString("explanation"));
      }
    body.addView(text("Fontes e cobertura", 18, ink, true));
    JSONArray sources = selected.optJSONArray("sources");
    if (sources != null)
      for (int i = 0; i < sources.length(); i++) {
        JSONObject item = sources.optJSONObject(i);
        gap(12);
        body.addView(text(item.optString("name"), 15, ink, true));
        paragraph(source(item.optString("status")) + "\n" + item.optString("detail"));
      }
    button("Entender com o assistente", this::chat);
    button(
        "Excluir esta análise",
        () ->
            new AlertDialog.Builder(this)
                .setTitle("Excluir análise?")
                .setMessage("O resultado e as conversas vinculadas serão removidos da sua conta.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(
                    "Excluir",
                    (d, w) ->
                        run(
                            null,
                            () -> api.call("/analyses/" + selected.getString("id"), "DELETE", null),
                            r -> {
                              selected = null;
                              history();
                            }))
                .show());
  }

  private void history() {
    if(user.optBoolean("mfa_required") && !user.optBoolean("mfa_enabled")) { account(); return; }
    shell("Seu histórico de análises.", true);
    paragraph("Página " + (historyPage + 1) + " · Último ano");
    run(
        null,
        () -> api.call("/analyses?days=365&size=15&page=" + historyPage, "GET", null),
        r -> {
          JSONArray rows = r.getJSONArray("items");
          if (rows.length() == 0)
            paragraph("Nenhuma análise por aqui ainda. Faça sua primeira verificação no início.");
          for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            button(
                row.getString("domain")
                    + "\n"
                    + risk(row.getString("risk"))
                    + " · "
                    + row.getInt("score")
                    + "/100",
                () ->
                    run(
                        null,
                        () -> api.call("/analyses/" + row.getString("id"), "GET", null),
                        a -> {
                          selected = a;
                          result();
                        }));
          }
          if (historyPage > 0)
            button(
                "Página anterior",
                () -> {
                  historyPage--;
                  history();
                });
          if ((historyPage + 1) * 15 < r.getInt("total"))
            button(
                "Próxima página",
                () -> {
                  historyPage++;
                  history();
                });
        });
  }

  private void chat() {
    if(user.optBoolean("mfa_required") && !user.optBoolean("mfa_enabled")) { account(); return; }
    shell("Vamos entender os sinais?", true);
    if (selected != null) {
      body.addView(text("Análise: " + selected.optString("domain"), 14, green, true));
      button(
          "Conversar sem esta análise",
          () -> {
            selected = null;
            chat();
          });
    }
    paragraph(
        "Explicações locais ou pelo Colab, quando configurado. Não envie senhas, códigos ou dados"
            + " bancários.");
    LinearLayout conversation = new LinearLayout(this);
    conversation.setOrientation(LinearLayout.VERTICAL);
    body.addView(conversation);
    EditText question =
        field("Sua pergunta", "Por que esse link é suspeito?", InputType.TYPE_CLASS_TEXT);
    Button send = button("Enviar pergunta", () -> {});
    send.setOnClickListener(
        v -> {
          String q = question.getText().toString().trim();
          if (q.isEmpty()) {
            question.setError("Escreva uma pergunta.");
            return;
          }
          run(
              send,
              () -> {
                JSONObject b = obj("message", q);
                if (selected != null) b.put("analysisId", selected.getString("id"));
                return api.call("/chat", "POST", b);
              },
              r -> {
                conversation.addView(text("Você: " + q, 14, ink, true));
                conversation.addView(text("Sentinela · " + r.optString("mode"), 11, green, true));
                conversation.addView(text(r.getString("answer"), 14, muted, false));
                question.setText("");
              });
        });
    button(
        "Consultar conversas anteriores",
        () ->
            run(
                null,
                () -> api.call("/chat", "GET", null),
                r -> {
                  conversation.removeAllViews();
                  JSONArray a = r.getJSONArray("items");
                  if (a.length() == 0)
                    conversation.addView(text("Você ainda não tem conversas.", 14, muted, false));
                  for (int i = a.length() - 1; i >= 0; i--) {
                    JSONObject m = a.getJSONObject(i);
                    conversation.addView(text("Você: " + m.getString("question"), 14, ink, true));
                    conversation.addView(text(m.getString("answer"), 14, muted, false));
                  }
                }));
    button(
        "Excluir conversas",
        () ->
            new AlertDialog.Builder(this)
                .setMessage("Excluir seu histórico de conversas?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton(
                    "Excluir",
                    (d, w) ->
                        run(
                            null,
                            () -> api.call("/chat", "DELETE", null),
                            r -> conversation.removeAllViews()))
                .show());
  }

  private void account() {
    shell("Sua segurança, em camadas.", true);
    if(user.optBoolean("mfa_required")) paragraph("O segundo fator é obrigatório. Guarde os códigos de recuperação fora deste aparelho.");
    body.addView(text(user.optString("name"), 20, ink, true));
    paragraph(user.optString("email"));
    paragraph("Segundo fator: " + (user.optBoolean("mfa_enabled") ? "ativo" : "ainda não ativado"));
    EditText password =
        field(
            "Confirme sua senha",
            "Senha atual",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    EditText code =
        field("Código do segundo fator", "Autenticador ou recuperação", InputType.TYPE_CLASS_TEXT);
    if (!user.optBoolean("mfa_enabled")) {
      button(
          "1. Configurar autenticador",
          () ->
              run(
                  null,
                  () ->
                      api.call(
                          "/me/mfa/setup", "POST", obj("password", password.getText().toString())),
                  r -> {
                    body.addView(text("Chave TOTP: " + r.getString("secret"), 16, ink, true));
                    paragraph(
                        "Adicione a chave ao autenticador: 6 dígitos e 30 segundos. Depois informe"
                            + " o código acima e ative.");
                  }));
      button(
          "2. Ativar segundo fator",
          () ->
              run(
                  null,
                  () ->
                      api.call(
                          "/me/mfa/enable",
                          "POST",
                          obj(
                              "password",
                              password.getText().toString(),
                              "code",
                              code.getText().toString())),
                  r -> {
                    user.put("mfa_enabled", true);
                    body.addView(text("GUARDE OS CÓDIGOS DE RECUPERAÇÃO", 15, green, true));
                    paragraph(
                        r.getJSONArray("recoveryCodes").join("\n")
                            + "\n"
                            + "Cada código funciona apenas uma vez. Eles não serão mostrados"
                            + " novamente.");
                  }));
    } else if(!user.optBoolean("mfa_required"))
      button(
          "Desativar segundo fator",
          () ->
              run(
                  null,
                  () ->
                      api.call(
                          "/me/mfa/disable",
                          "POST",
                          obj(
                              "password",
                              password.getText().toString(),
                              "code",
                              code.getText().toString())),
                  r -> {
                    user.put("mfa_enabled", false);
                    account();
                  }));
    gap(20);
    EditText next =
        field(
            "Nova senha",
            "Pelo menos 12 caracteres",
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    button(
        "Alterar senha e encerrar sessões",
        () ->
            run(
                null,
                () ->
                    api.call(
                        "/me/password",
                        "POST",
                        obj(
                            "password",
                            password.getText().toString(),
                            "newPassword",
                            next.getText().toString(),
                            "code",
                            code.getText().toString())),
                r -> {
                  tokens.clear();
                  auth(false);
                }));
    button(
        "Encerrar todas as sessoes",
        () ->
            run(
                null,
                () -> api.call("/me/sessions/revoke-all", "POST", null),
                r -> {
                  tokens.clear();
                  auth(false);
                }));
    button(
        "Sair da conta",
        () ->
            run(
                null,
                () -> api.call("/auth/logout", "POST", null),
                r -> {
                  tokens.clear();
                  selected = null;
                  auth(false);
                }));
  }
}
