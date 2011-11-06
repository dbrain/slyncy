package com.shinetech.android.slyncy;

import org.ektorp.android.http.AndroidHttpClient;

import android.app.Activity;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.couchbase.android.CouchbaseMobile;
import com.couchbase.android.ICouchbaseDelegate;

public class SlyncyActivity extends Activity implements android.view.View.OnClickListener {
	
	protected static ServiceConnection couchConnection;
	protected static AndroidHttpClient httpClient;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button addItemButton = (Button)findViewById(R.id.new_item_button);
        addItemButton.setOnClickListener(this);
        ListView itemsListView = (ListView)findViewById(R.id.item_list);
        registerForContextMenu(itemsListView);
        startCouch();
    }
    
    @Override
    public void onClick(View v) {
    	int viewId = v.getId();
    	switch (viewId) {
    	case R.id.new_item_button:
    		startActivity(new Intent(this, SlyncyAddItemActivity.class));
    		break;
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.start_replication:
    		return true;
    	case R.id.settings:
    		startActivity(new Intent(this, SlyncyPreferenceActivity.class));
    		return true;
		default:
			return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	return super.onContextItemSelected(item);
    }
    
    private void startCouch() {
    	CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), couchDelegate);
    	couchConnection = couch.startCouchbase();
    }
    
    private void startEktorp(String host, int port) {
    	
    }
    
    private ICouchbaseDelegate couchDelegate = new ICouchbaseDelegate() {
		@Override
		public void exit(String error) {
			
		}
		
		@Override
		public void couchbaseStarted(String host, int port) {
			
		}
	};
	
	protected void onDestroy() {
		if (couchConnection != null) unbindService(couchConnection);
		if (httpClient != null) httpClient.shutdown();
	};
}