package ir.berah.nerkhnameh1405;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.AsyncTask;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.ScaleGestureDetector;
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
    HashMap<Integer,Page> pageByNumber = new HashMap<>();
    boolean loaded = false;

    static class Page {
        int number; String text;
        Page(int n, String t){ number=n; text=t == null ? "" : t; }
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
        root.setPadding(16,16,16,10);
        root.setBackgroundColor(Color.rgb(248,248,248));

        TextView title = new TextView(this);
        title.setText("نرخنامه کرایه ۱۴۰۵");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(25,25,25));
        root.addView(title,new LinearLayout.LayoutParams(-1,64));

        TextView origin = new TextView(this);
        origin.setText("مبدا: قزوین");
        origin.setTextSize(18);
        origin.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        origin.setGravity(Gravity.CENTER);
        origin.setTextColor(Color.rgb(25,25,25));
        origin.setPadding(0,0,0,10);
        root.addView(origin);

        search = new EditText(this);
        search.setHint("مقصد یا نام شهر را جستجو کنید");
        search.setSingleLine(true);
        search.setTextSize(17);
        search.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        search.setPadding(20,0,20,0);
        search.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        search.setBackgroundColor(Color.WHITE);
        root.addView(search,new LinearLayout.LayoutParams(-1,58));

        status = new TextView(this);
        status.setText("در حال آماده‌سازی...");
        status.setTextSize(14);
        status.setGravity(Gravity.RIGHT);
        status.setPadding(4,8,4,8);
        root.addView(status);

        ScrollView sv = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0,4,0,30);
        sv.addView(results);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){runSearch();}
            public void afterTextChanged(android.text.Editable e){}
        });
        setContentView(root);
    }

    void runSearch() {
        if(!loaded) return;
        String q=norm(search.getText().toString().trim());
        results.removeAllViews();

        if(q.isEmpty()){
            status.setText("۵۳ صفحه آماده است — هر صفحه را باز کنید");
            for(Page p:pages) addPageRow(p);
            return;
        }

        int count=0;
        for(Page p:pages){
            if(matchesQuery(norm(p.text), q)){
                addPageRow(p);
                count++;
            }
        }
        status.setText(count==0 ? "نتیجه‌ای پیدا نشد" : count+" صفحه مرتبط پیدا شد");
        if(count==0){
            TextView hint=label("عبارت را کوتاه‌تر کنید؛ مثلاً فقط نام شهر را وارد کنید.");
            results.addView(hint);
        }
    }

    boolean matchesQuery(String haystack, String query){
        if(query.isEmpty() || haystack.isEmpty()) return query.isEmpty();
        String[] terms=query.split(" ");
        for(String term:terms) if(!term.isEmpty() && !haystack.contains(term)) return false;
        return true;
    }

    void addPageRow(Page p) {
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14,12,14,12);
        card.setBackgroundColor(Color.WHITE);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView h=label("صفحه "+p.number+"  |  مبدا: قزوین");
        h.setTextSize(17);
        h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        card.addView(h);

        Button open=new Button(this);
        open.setText("باز کردن صفحه");
        open.setOnClickListener(v->showPage(p.number));
        card.addView(open,new LinearLayout.LayoutParams(-1,52));

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,0,0,10);
        results.addView(card,lp);
    }

    TextView label(String s){
        TextView v=new TextView(this);
        v.setText(s);
        v.setTextSize(16);
        v.setTextColor(Color.DKGRAY);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        v.setPadding(10,10,10,10);
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return v;
    }

    String norm(String s){
        return s.replace('ي','ی').replace('ى','ی').replace('ك','ک').replace('ؤ','و').replace('إ','ا').replace('أ','ا')
                .replace('ۀ','ه').replace('ة','ه')
                .replace('٠','۰').replace('١','۱').replace('٢','۲').replace('٣','۳')
                .replace('٤','۴').replace('٥','۵').replace('٦','۶').replace('٧','۷')
                .replace('٨','۸').replace('٩','۹').replace("\u200c"," ").replaceAll("[\\u064B-\\u065F\\u0670]","").replaceAll("\\s+"," ")
                .toLowerCase(Locale.ROOT);
    }

    void showPage(int n){
        final Page page=pageByNumber.get(n);
        if(page==null) return;
        final Dialog dialog=new Dialog(this);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.WHITE);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(12,8,12,8);

        TextView title=label("صفحه "+n+" از "+pages.size());
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        Button prev=new Button(this); prev.setText("‹ قبلی"); prev.setOnClickListener(v->{ dialog.dismiss(); if(n>1) showPage(n-1); });
        Button next=new Button(this); next.setText("بعدی ›"); next.setOnClickListener(v->{ dialog.dismiss(); if(n<pages.size()) showPage(n+1); });
        top.addView(next,new LinearLayout.LayoutParams(90,52));
        top.addView(title,new LinearLayout.LayoutParams(0,52,1));
        top.addView(prev,new LinearLayout.LayoutParams(90,52));

        EditText localSearch=new EditText(this);
        localSearch.setHint("جستجو فقط در همین جدول");
        localSearch.setSingleLine(true);
        localSearch.setTextSize(16);
        localSearch.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        localSearch.setPadding(18,0,18,0);
        localSearch.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        localSearch.setBackgroundColor(Color.rgb(245,245,245));
        top.addView(localSearch,new LinearLayout.LayoutParams(-1,54));
        box.addView(top);

        FrameLayout frame=new FrameLayout(this);
        ScrollView vs=new ScrollView(this);
        vs.setFillViewport(true);
        ImageView im=new ImageView(this);
        im.setAdjustViewBounds(true);
        im.setScaleType(ImageView.ScaleType.FIT_CENTER);
        im.setBackgroundColor(Color.WHITE);
        File f=new File(dataDir,"pages/page-"+String.format(Locale.US,"%02d",n)+".jpg");
        if(!f.exists()) f=new File(dataDir,"pages/page-"+String.format(Locale.US,"%03d",n)+".jpg");
        Bitmap bm=BitmapFactory.decodeFile(f.getAbsolutePath());
        if(bm!=null) im.setImageBitmap(bm);
        im.setContentDescription("تصویر کامل صفحه "+n);
        vs.addView(im,new ScrollView.LayoutParams(-1,-2));
        frame.addView(vs,new FrameLayout.LayoutParams(-1,-1));

        TextView localStatus=label("");
        localStatus.setTextSize(14);
        localStatus.setGravity(Gravity.CENTER);
        localStatus.setBackgroundColor(Color.WHITE);
        localStatus.setVisibility(View.GONE);
        frame.addView(localStatus,new FrameLayout.LayoutParams(-1,48,Gravity.TOP));

        box.addView(frame,new LinearLayout.LayoutParams(-1,0,1));

        Button close=new Button(this);
        close.setText("بستن");
        close.setOnClickListener(v->dialog.dismiss());
        box.addView(close,new LinearLayout.LayoutParams(-1,52));

        localSearch.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){
                String q=norm(s.toString().trim());
                if(q.isEmpty()){ localStatus.setVisibility(View.GONE); return; }
                Page p=pageByNumber.get(n);
                boolean found=p!=null && matchesQuery(norm(p.text),q);
                localStatus.setText(found ? "در متن این جدول پیدا شد" : "در متن استخراج‌شده این جدول پیدا نشد");
                localStatus.setTextColor(found ? Color.rgb(0,120,60) : Color.rgb(170,0,0));
                localStatus.setVisibility(View.VISIBLE);
            }
            public void afterTextChanged(android.text.Editable e){}
        });

        dialog.setContentView(box);
        dialog.show();
        if(dialog.getWindow()!=null) dialog.getWindow().setLayout(-1,-1);
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
                        File out=new File(dataDir,e.getName());
                        if(e.isDirectory()){out.mkdirs(); zin.closeEntry(); continue;}
                        File parent=out.getParentFile();
                        if(parent!=null) parent.mkdirs();
                        FileOutputStream fos=new FileOutputStream(out);
                        int k; while((k=zin.read(buf))>0) fos.write(buf,0,k);
                        fos.close(); zin.closeEntry();
                    }
                    zin.close();
                }
                String json="";
                try{
                    InputStream jin=getAssets().open("ocr_all.json");
                    ByteArrayOutputStream jb=new ByteArrayOutputStream();
                    byte[] bx=new byte[8192]; int jn;
                    while((jn=jin.read(bx))>0) jb.write(bx,0,jn);
                    jin.close();
                    json=new String(jb.toByteArray(),StandardCharsets.UTF_8);
                }catch(Exception ignored){}
                if(!json.isEmpty()){
                    JSONObject jo=new JSONObject(json);
                    for(int i=1;i<=53;i++){ Page p=new Page(i,jo.optString(String.valueOf(i),"")); pages.add(p); pageByNumber.put(i,p); }
                }else{
                    for(int i=1;i<=53;i++){
                        File f=new File(dataDir,"ocr/page-"+String.format(Locale.US,"%02d",i)+".txt");
                        String t="";
                        if(f.exists()) t=new String(read(f),StandardCharsets.UTF_8);
                        Page p=new Page(i,t); pages.add(p); pageByNumber.put(i,p);
                    }
                }
                return "ok";
            }catch(Exception e){return e.toString();}
        }
        protected void onPostExecute(String r){
            if("ok".equals(r)){ loaded=true; runSearch(); }
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