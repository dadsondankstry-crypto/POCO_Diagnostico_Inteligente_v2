package br.com.pocodiagnostico;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.TextUtils;
import java.util.*;
import java.io.*;
import java.text.*;

public class MainActivity extends Activity {
    private static final int PICK_DIR = 7, SAVE_REPORT = 8;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityManager am;
    private TextView ramText, selfText, diskText, statusText, folderText, filesText, historyText;
    private LinearLayout list;
    private Uri treeUri;
    private volatile boolean scanning = false;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final ArrayList<String> samples = new ArrayList<>();
    private String lastSummary = "Nenhuma análise feita";
    private final Runnable ticker = new Runnable() { public void run() { sample(); handler.postDelayed(this, 3000); } };
    private final int NAVY=Color.rgb(19,29,51), BLUE=Color.rgb(40,105,235), PALE=Color.rgb(244,247,252);

    static class Entry { Uri uri; String name, path; long bytes; Entry(Uri u,String n,String p,long b){uri=u;name=n;path=p;bytes=b;} }
    @Override public void onCreate(Bundle b) { super.onCreate(b); am=(ActivityManager)getSystemService(ACTIVITY_SERVICE); build(); sample(); }
    @Override protected void onResume(){ super.onResume(); handler.removeCallbacks(ticker); handler.postDelayed(ticker,3000); }
    @Override protected void onPause(){ handler.removeCallbacks(ticker); super.onPause(); }
    int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(NAVY);t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    void add(LinearLayout p,View v,int top){LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(-1,-2);l.topMargin=dp(top);p.addView(v,l);}
    void card(LinearLayout parent,View child){ LinearLayout frame=new LinearLayout(this);frame.setPadding(dp(17),dp(17),dp(17),dp(17));frame.setOrientation(1);frame.setBackground(bg(Color.WHITE,18));frame.addView(child);add(parent,frame,12); }
    Button button(String name,Runnable run){Button b=new Button(this);b.setText(name);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(BLUE));b.setOnClickListener(v->run.run());return b;}
    void build(){
      ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(PALE);
      LinearLayout main=new LinearLayout(this);main.setOrientation(1);main.setPadding(dp(17),dp(24),dp(17),dp(36));scroll.addView(main);setContentView(scroll);
      add(main,text("POCO Diagnóstico",28,true),0);
      TextView sub=text("Memória, armazenamento e relatórios • v1.0",13,false);sub.setTextColor(Color.DKGRAY);add(main,sub,3);
      LinearLayout r=new LinearLayout(this);r.setOrientation(1);add(r,text("RAM DO SISTEMA • ATUALIZA A CADA 3 SEGUNDOS",13,true),0);
      ramText=text("Carregando...",17,false);add(r,ramText,8);selfText=text("",14,false);add(r,selfText,8);
      TextView info=text("RAM em uso é estimativa (total − disponível). O Android pode recuperar cache quando necessário.",12,false);add(r,info,9);card(main,r);
      LinearLayout d=new LinearLayout(this);d.setOrientation(1);add(d,text("ARMAZENAMENTO INTERNO",13,true),0);diskText=text("",17,false);add(d,diskText,8);card(main,d);
      LinearLayout h=new LinearLayout(this);h.setOrientation(1);add(h,text("MONITORAMENTO",16,true),0);statusText=text("Somente enquanto este app estiver aberto.",13,false);add(h,statusText,8);historyText=text("",13,false);add(h,historyText,8);card(main,h);
      LinearLayout p=new LinearLayout(this);p.setOrientation(1);add(p,text("ARQUIVOS GRANDES",16,true),0);
      add(p,text("Escolha uma pasta para analisar. Acesso limitado à pasta autorizada; nada é excluído automaticamente.",13,false),7);
      add(p,button("Escolher pasta e analisar",()->pick()),10);
      folderText=text("Nenhuma pasta selecionada",13,false);add(p,folderText,7);
      filesText=text("",13,false);add(p,filesText,7);list=new LinearLayout(this);list.setOrientation(1);add(p,list,5);card(main,p);
      LinearLayout a=new LinearLayout(this);a.setOrientation(1);add(a,text("APLICATIVOS E TRAVAMENTOS",16,true),0);
      add(a,text("O Android 16 não permite a apps comuns medir a RAM de todos os outros apps. Para investigar o ChatGPT, abra as configurações de apps e o histórico de falhas do HyperOS.",13,false),8);
      add(a,button("Configurações dos aplicativos",()->openSettings(Settings.ACTION_APPLICATION_SETTINGS)),10);
      add(a,button("Detalhes do ChatGPT (se instalado)",()->openChatGPT()),5);
      card(main,a);
      LinearLayout report=new LinearLayout(this);report.setOrientation(1);add(report,text("RELATÓRIO PARA O CHATGPT",16,true),0);
      add(report,text("Salva um TXT que você pode anexar à nossa conversa. Revise nomes de arquivos antes de compartilhar.",13,false),8);
      add(report,button("Gerar relatório .TXT",()->saveReport()),10);
      card(main,report);
      TextView foot=text("Privacidade: nenhum dado é enviado automaticamente. Sem root, sem limpeza forçada de RAM e sem permissões de acesso total aos arquivos.",12,false);add(main,foot,18);
    }
    String size(long n){return String.format(Locale.forLanguageTag("pt-BR"),"%.2f GB",Math.max(0,n)/1073741824.0);}
    String mb(long n){return String.format(Locale.forLanguageTag("pt-BR"),"%.0f MB",Math.max(0,n)/1048576.0);}
    String timestamp(){return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());}
    void sample(){
      try{
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();am.getMemoryInfo(mi);
        long occupied=Math.max(0,mi.totalMem-mi.availMem);
        ramText.setText("Total: "+size(mi.totalMem)+"\nDisponível: "+size(mi.availMem)+"\nEm uso (estimado): "+size(occupied)+"\nLimite de alerta: "+mb(mi.threshold)+"\nMemória baixa: "+(mi.lowMemory?"SIM ⚠":"Não"));
        Runtime rt=Runtime.getRuntime();long self=rt.totalMemory()-rt.freeMemory();
        selfText.setText("Este app (heap Java): "+mb(self)+" / limite aprox. "+mb(rt.maxMemory()));
        StatFs fs=new StatFs(Environment.getDataDirectory().getAbsolutePath());long total=fs.getTotalBytes(),free=fs.getAvailableBytes();
        diskText.setText("Total: "+size(total)+"\nDisponível: "+size(free)+"\nOcupado: "+size(total-free));
        samples.add(timestamp()+" | RAM_disponivel="+mb(mi.availMem)+" | RAM_total="+mb(mi.totalMem)+" | lowMemory="+mi.lowMemory);
        if(samples.size()>600)samples.remove(0);
        statusText.setText("Registros nesta sessão: "+samples.size()+" (máximo 600)");
        historyText.setText("Última leitura: "+timestamp()+"\nEstado: "+(mi.lowMemory?"memória baixa":"normal"));
      }catch(Exception e){statusText.setText("Falha de leitura: "+e.getClass().getSimpleName());}
    }
    void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_DIR);}
    void saveReport(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/plain");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"poco_diagnostico_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date())+".txt");startActivityForResult(i,SAVE_REPORT);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null||data.getData()==null)return;
      if(req==PICK_DIR){treeUri=data.getData();try{getContentResolver().takePersistableUriPermission(treeUri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}catch(Exception ignored){} scan();}
      if(req==SAVE_REPORT){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException("Saída vazia");out.write(report().getBytes("UTF-8"));toast("Relatório salvo. Anexe o TXT ao ChatGPT.");}catch(Exception e){toast("Erro ao salvar: "+e.getMessage());}}
    }
    void scan(){if(scanning){toast("Análise em andamento");return;}scanning=true;folderText.setText("Analisando pasta selecionada...");filesText.setText("");list.removeAllViews();
      new Thread(()->{ArrayList<Entry> result=new ArrayList<>();String summary;try{String rootDoc=DocumentsContract.getTreeDocumentId(treeUri);Uri doc=DocumentsContract.buildDocumentUriUsingTree(treeUri,rootDoc);int[] count={0};long[] bytes={0};walk(doc,"",result,count,bytes,0);result.sort((a,b)->Long.compare(b.bytes,a.bytes));summary="Arquivos: "+count[0]+" | Total lido: "+size(bytes[0])+" | Pastas/arquivos: limite 5000";}catch(Exception e){summary="Falha ao ler pasta: "+e.getMessage();}
       final String s=summary;runOnUiThread(()->{synchronized(entries){entries.clear();entries.addAll(result);}lastSummary=s;folderText.setText("Pasta selecionada: "+treeUri.getLastPathSegment());filesText.setText(s+"\nMaiores arquivos (até 30):");showFiles();scanning=false;});}).start();
    }
    void walk(Uri folder,String prefix,ArrayList<Entry> result,int[] count,long[] bytes,int depth){if(depth>15||count[0]>=5000)return;
      Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,DocumentsContract.getDocumentId(folder));
      try(Cursor c=getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE},null,null,null)){
       if(c==null)return;while(c.moveToNext()&&count[0]<5000){String id=c.getString(0),name=c.getString(1),mime=c.getString(2);long sz=c.isNull(3)?0:c.getLong(3);Uri uri=DocumentsContract.buildDocumentUriUsingTree(treeUri,id);if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){walk(uri,prefix+name+"/",result,count,bytes,depth+1);}else{count[0]++;bytes[0]+=Math.max(sz,0);result.add(new Entry(uri,name,prefix+name,sz));}}}
      catch(Exception ignored){}
    }
    void showFiles(){list.removeAllViews();int shown=0;ArrayList<Entry> copy; synchronized(entries){copy=new ArrayList<>(entries);}for(Entry e:copy){if(shown++>=30)break;
      LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);TextView t=text(e.path+"  •  "+mb(e.bytes),13,false);t.setMaxLines(3);t.setEllipsize(TextUtils.TruncateAt.END);add(row,t,7);
      Button del=button("Excluir com confirmação",()->confirmDelete(e));add(row,del,2);add(list,row,9);
    }}
    void confirmDelete(Entry e){new AlertDialog.Builder(this).setTitle("Excluir arquivo?").setMessage(e.path+"\n"+mb(e.bytes)+"\n\nA exclusão pode ser permanente. Confirme apenas se tiver certeza.").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(d,w)->{
      try{boolean ok=DocumentsContract.deleteDocument(getContentResolver(),e.uri);toast(ok?"Excluído":"Não foi possível excluir");if(ok)scan();}catch(Exception x){toast("Sem autorização para excluir: "+x.getMessage());}}).show();}
    String report(){StringBuilder b=new StringBuilder();b.append("POCO DIAGNÓSTICO v1.0 — ").append(timestamp()).append("\n");
      b.append("Modelo: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\nAndroid API: ").append(Build.VERSION.SDK_INT).append("\n\n");
      b.append("RAM\n").append(ramText.getText()).append("\n").append(selfText.getText()).append("\n\nARMAZENAMENTO\n").append(diskText.getText()).append("\n\n");
      b.append("PASTA ANALISADA\n").append(lastSummary).append("\n");ArrayList<Entry> copy; synchronized(entries){copy=new ArrayList<>(entries);}for(int i=0;i<Math.min(copy.size(),60);i++){Entry e=copy.get(i);b.append(i+1).append(". ").append(e.path).append(" — ").append(mb(e.bytes)).append("\n");}
      b.append("\nAMOSTRAS DE RAM (somente app aberto)\n");for(String s:samples)b.append(s).append("\n");
      b.append("\nLIMITAÇÕES: não mede RAM por aplicativo de terceiros; não acessa logs privados de falhas; RAM ocupada é estimativa.\n");return b.toString();}
    void openSettings(String action){try{startActivity(new Intent(action));}catch(Exception e){toast("Configuração indisponível");}}
    void openChatGPT(){try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:com.openai.chatgpt"));startActivity(i);}catch(Exception e){openSettings(Settings.ACTION_APPLICATION_SETTINGS);}}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
