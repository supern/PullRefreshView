package com.votbar.view;

import java.util.Arrays;
import java.util.LinkedList;

import com.votbar.view.PullRefreshView.OnRefreshListener;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Main extends Activity {
    /** Called when the activity is first created. */
    private LinkedList<String> mListItems;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListItems = new LinkedList<String>();
        mListItems.addAll(Arrays.asList(mStrings));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mListItems);

        ListView listView = (ListView) findViewById(R.id.ListView);
        listView.setAdapter(adapter);
        
        final PullRefreshView pullRefreshView = (PullRefreshView) findViewById(R.id.pull);
        pullRefreshView.setRefreshListener(new OnRefreshListener() {
			
			@Override
			public void onRefresh() {
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Main.this.runOnUiThread(new Runnable() {
							
							@Override
							public void run() {
								pullRefreshView.refreshFinishd();
							}
						});
					}
				}).start();
			}
		});
    }
    

    private String[] mStrings = {
            "Abbaye de Belloc", "Abbaye du Mont des Cats", "Abertam",
            "Abondance", "Ackawi", "Acorn", "Adelost", "Affidelice au Chablis",
            "Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre",
            "Allgauer Emmentaler"
            };
}