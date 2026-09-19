package ir.berah.nerkhnameh;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.AsyncTask;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
    LinearLayout root, results;
    EditText search;
    TextView status;
    File dataDir;
    ArrayList<Page> pages = new ArrayList<>();

    static class Page {
        int number; String text;
        Page(int n, String t){ number=n; text=t; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        buildUi();
        new LoadTask().execute();
    }

    void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,16);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("نرخنامه کرایه ۱۴۰۵");
        title.setTextSize(24); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setGravity(Gravity.CENTER); title.setTextColor(Color.rgb(21,101,192));
        root.addView(title,new LinearLayout.LayoutParams(-1,70));

        search = new EditText(this);
        search.setHint("جستجو در نرخنامه...");
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setPadding(24,0,24,0);
        root.addView(search,new LinearLayout.LayoutParams(-1,60));

        status = new TextView(this);
        status.setText("در حال آماده‌سازی نرخنامه...");
        status.setTextSize(14); status.setPadding(8,10,8,10);
        root.addView(status);

        ScrollView sv = new ScrollView(this);
        results = new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL);
        sv.addView(results);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        search.setOnEditorActionListener((v,a,e)->{runSearch();return true;});
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){runSearch();}
            public void afterTextChanged(android.text.Editable e){}
        });
        setContentView(root);
    }

    void runSearch() {
        if(pages.isEmpty()) return;
        String q=norm(search.getText().toString().trim());
        results.removeAllViews();
        if(q.isEmpty()){
            status.setText("۵۳ صفحه نرخنامه آماده است");
            addAllPages();
            return;
        }
        int count=0;
        for(Page p:pages){
            String t=norm(p.text);
            if(t.contains(q)){
                addResult(p,q); count++;
                if(count>=50) break;
            }
        }
        status.setText(count+" نتیجه");
        if(count==0){
            TextView x=label("نتیجه‌ای پیدا نشد");
            results.addView(x);
        }
    }

    void addAllPages(){
        for(Page p:pages) addResult(p,"");
    }

    void addResult(Page p,String q){
        TextView v=label("صفحه "+p.number+"\n"+snippet(p.text,q));
        v.setOnClickListener(x->showPage(p.number));
        results.addView(v);
    }

    TextView label(String s){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(16); v.setTextColor(Color.DKGRAY);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(20,18,20,18);
        v.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,10); v.setLayoutParams(lp);
        return v;
    }

    String snippet(String t,String q){
        if(t==null||t.trim().isEmpty()) return "مشاهده جدول این صفحه";
        t=t.replace("\n"," ").replaceAll("\\s+"," ").trim();
        if(q.isEmpty()) return t.length()>140?t.substring(0,140)+"…":t;
        String n=norm(t), nq=norm(q); int i=n.indexOf(nq);
        if(i<0) return t.length()>140?t.substring(0,140)+"…":t;
        int a=Math.max(0,i-55), z=Math.min(t.length(),i+nq.length()+85);
        return (a>0?"…":"")+t.substring(a,z)+(z<t.length()?"…":"");
    }

    String norm(String s){
        return s.replace('ي','ی').replace('ى','ی').replace('ك','ک')
                .replace('ۀ','ه').replace('ة','ه')
                .replace('٠','۰').replace('١','۱').replace('٢','۲').replace('٣','۳')
                .replace('٤','۴').replace('٥','۵').replace('٦','۶').replace('٧','۷')
                .replace('٨','۸').replace('٩','۹').toLowerCase(Locale.ROOT);
    }

    void showPage(int n){
        final DialogLike d=new DialogLike(this);
        d.show(n);
    }

    class DialogLike {
        Activity a; Dialog dialog;
        DialogLike(Activity x){a=x;}
        void show(int n){
            dialog=new Dialog(a);
            dialog.setTitle("صفحه "+n);
            LinearLayout box=new LinearLayout(a); box.setOrientation(LinearLayout.VERTICAL);
            ScrollView sv=new ScrollView(a);
            ImageView im=new ImageView(a); im.setAdjustViewBounds(true);
            File f=new File(dataDir,"pages/page-"+String.format(Locale.US,"%02d",n)+".jpg");
            if(!f.exists()) f=new File(dataDir,"pages/page-"+String.format(Locale.US,"%03d",n)+".jpg");
            im.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
            sv.addView(im); box.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
            Button close=new Button(a); close.setText("بستن"); close.setOnClickListener(v->dialog.dismiss());
            box.addView(close);
            dialog.setContentView(box);
            Window w=dialog.getWindow(); if(w!=null) w.setLayout(-1,-1);
            dialog.show();
            if(dialog.getWindow()!=null) dialog.getWindow().setLayout(-1,-1);
        }
    }

    class LoadTask extends AsyncTask<Void,Void,String>{
        protected String doInBackground(Void... v){
            try{
                dataDir=new File(getFilesDir(),"nrate");
                File marker=new File(dataDir,"pages/page-01.jpg");
                if(!marker.exists()){
                    if(dataDir.exists()) delete(dataDir);
                    dataDir.mkdirs();
                    InputStream in=getAssets().open("nrate_assets.zip");
                    ZipInputStream zin=new ZipInputStream(new BufferedInputStream(in));
                    ZipEntry e; byte[] buf=new byte[8192];
                    while((e=zin.getNextEntry())!=null){
                        File out=new File(dataDir,e.getName()); out.getParentFile().mkdirs();
                        FileOutputStream fos=new FileOutputStream(out);
                        int k; while((k=zin.read(buf))>0) fos.write(buf,0,k);
                        fos.close(); zin.closeEntry();
                    }
                    zin.close();
                }
                String json="";
                try {
                    InputStream jin=getAssets().open("ocr_all.json");
                    ByteArrayOutputStream jb=new ByteArrayOutputStream();
                    byte[] bx=new byte[8192]; int jn;
                    while((jn=jin.read(bx))>0) jb.write(bx,0,jn);
                    jin.close();
                    json=new String(jb.toByteArray(),StandardCharsets.UTF_8);
                } catch(Exception ignored) {}
                if(!json.isEmpty()){
                    JSONObject jo=new JSONObject(json);
                    for(int i=1;i<=53;i++) pages.add(new Page(i,jo.optString(String.valueOf(i),"")));
                } else {
                    for(int i=1;i<=53;i++){
                        File f=new File(dataDir,"ocr/page-"+String.format(Locale.US,"%02d",i)+".txt");
                        if(!f.exists()) f=new File(dataDir,"ocr/page-"+String.format(Locale.US,"%03d",i)+".txt");
                        String t="";
                        if(f.exists()) t=new String(read(f),StandardCharsets.UTF_8);
                        pages.add(new Page(i,t));
                    }
                }
                return "ok";
            }catch(Exception e){ return e.toString(); }
        }
        protected void onPostExecute(String r){
            if("ok".equals(r)){ status.setText("۵۳ صفحه نرخنامه آماده است"); runSearch(); }
            else status.setText("خطا در آماده‌سازی: "+r);
        }
    }

    byte[] read(File f)throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();
        FileInputStream in=new FileInputStream(f); byte[] x=new byte[8192]; int n;
        while((n=in.read(x))>0)b.write(x,0,n); in.close(); return b.toByteArray();
    }
    void delete(File f){
        if(f.isDirectory()){File[] fs=f.listFiles(); if(fs!=null) for(File x:fs) delete(x);}
        f.delete();
    }
}
