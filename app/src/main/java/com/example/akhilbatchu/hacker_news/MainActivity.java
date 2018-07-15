package com.example.akhilbatchu.hacker_news;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayAdapter adapter;
    SQLiteDatabase db;
    ListView lv;
    ArrayList<String> title,content;
    public void updateListView()
    {
        Cursor c= db.rawQuery("SELECT * FROM articles",null);
        int contentIndex = c.getColumnIndex("CONTENT");
        int titleIndex = c.getColumnIndex("title");
        if(c.moveToFirst())
        {
            title.clear();
            content.clear();
            do {
                title.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            }while(c.moveToNext());
            adapter.notifyDataSetChanged();

        }

    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lv = (ListView)findViewById(R.id.listView);
        title = new ArrayList<>();
        content = new ArrayList<>();

        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,title);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this,SecondActivity.class);
                intent.putExtra("content",content.get(i));
                startActivity(intent);
            }
        });


        db = this.openOrCreateDatabase("Articiles", Context.MODE_PRIVATE,null);
       // db.execSQL("CREATE TABLE IF NOT EXISTS  articles(id INTEGER PRIMARY KEY, articleid INTEGER, title VARCHAR,CONTENT VARCHAR)");
        updateListView();

        DownloadTask task = new DownloadTask();
        try {
       //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e)
        {
            e.printStackTrace();
        }


    }
    public class DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... strings) {
            String result = "";
            String Articletitle,Articleurl;

            URL url;

            HttpURLConnection urlConnection = null;

            try {

                url = new URL(strings[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;

                    result += current;

                    data = reader.read();
                }

                Log.i("URLContent", result);
                JSONArray array = new JSONArray(result);
                int n = 20;
                if(array.length()<20)
                {
                    n = array.length();
                }
                db.execSQL("DELETE FROM articles ");
                for(int i =0;i<n;i++)
                {
                    String articleid = array.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleid+".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();

                    in = urlConnection.getInputStream();

                     reader = new InputStreamReader(in);

                     data = reader.read();
                     String articleInfo = "";
                    while (data != -1) {

                        char current = (char) data;

                        articleInfo += current;

                        data = reader.read();
                    }
                    //Log.i("ArticleInfo",articleInfo);
                    JSONObject object = new JSONObject(articleInfo);
                    if(!object.isNull("title") && !object.isNull("url"))
                    {
                        Articletitle = object.getString("title");
                        Articleurl = object.getString("url");

                        Log.i("ArticileTitle", Articletitle + Articleurl);
                        url = new URL(Articleurl);

                        urlConnection = (HttpURLConnection) url.openConnection();

                        in = urlConnection.getInputStream();

                        reader = new InputStreamReader(in);

                        data = reader.read();
                        String articleContent = "";
                        while (data!=-1)
                        {
                            char c = (char)data;
                            articleContent+=c;
                            data = reader.read();
                        }
                        Log.i("articileContent",articleContent);
                        String sql = "INSERT INTO articles (articleid,title,CONTENT) VALUES (?,?,?)";
                        SQLiteStatement statement = db.compileStatement(sql);
                        statement.bindString(1,articleid);
                        statement.bindString(2,Articletitle);
                        statement.bindString(3,articleContent);
                        statement.execute();

                    }


                }
            }catch (Exception e)
            {
                e.printStackTrace();
            }




            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}
