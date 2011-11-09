package com.shinetech.android.slyncy;

import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.ReplicationCommand;
import org.ektorp.ViewQuery;
import org.ektorp.ViewResult.Row;
import org.ektorp.android.http.AndroidHttpClient;
import org.ektorp.android.util.CouchbaseViewListAdapter;
import org.ektorp.android.util.EktorpAsyncTask;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.DesignDocument;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.android.CouchbaseMobile;
import com.couchbase.android.ICouchbaseDelegate;

public class SlyncyActivity extends Activity implements OnClickListener {
	// request codes
	private static final int ADD_ITEM = 1;
	private static final int EDIT_ITEM = 2;
	
	// db constants
	protected static final String DATABASE_NAME = "slyncy";
	protected static final String designDocId = "_design/slyncy";
	protected static final String byNameViewName = "byName";
	// TODO Ektorp list adapter changes feed currently doesn't includeDocs, should be an option.
	protected static final String byNameViewFn = "function(doc) {if (doc.name) emit(doc.name, doc);}";
	
	protected static ServiceConnection couchConnection;
	protected static AndroidHttpClient httpClient;
	protected static CouchDbInstance dbInstance;
	protected ReplicationCommand pushReplicationCommand;
	protected ReplicationCommand pullReplicationCommand;
	protected CouchDbConnector dbConnector;
	protected CouchbaseViewListAdapter adapter;
	
	private Dialog loadingDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button addItemButton = (Button)findViewById(R.id.new_item_button);
        addItemButton.setOnClickListener(this);
        ListView itemsListView = (ListView)findViewById(R.id.item_list);
        registerForContextMenu(itemsListView);
        showLoading();
        startCouch();
    }
    
    @Override
    public void onClick(View v) {
    	int viewId = v.getId();
    	switch (viewId) {
    	case R.id.new_item_button:
    		startActivityForResult(new Intent(this, SlyncyAddItemActivity.class), ADD_ITEM);
    		break;
    	}
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK) {
    		switch (requestCode) {
        	case ADD_ITEM:
        		Map<String, String> addValues = (Map<String, String>)data.getSerializableExtra(SlyncyAddItemActivity.FORM_VALUES);
        		saveDocument(addValues, "CREATE");
        		break;
        	case EDIT_ITEM:
        		Map<String, String> editValues = (Map<String, String>)data.getSerializableExtra(SlyncyAddItemActivity.FORM_VALUES);
        		saveDocument(editValues, "EDIT");
        		break;
    		}
    	} else {
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }
    
    public void saveDocument(final Map<String, String> values, final String mode) {
    	new EktorpAsyncTask() {	
			@Override
			protected void doInBackground() {
				CouchDbConnector connector = dbInstance.createConnector(DATABASE_NAME, true);
				if (mode.equals("CREATE")) {
					connector.create(values);
				} else if (mode.equals("EDIT")) {
					connector.update(values);
				}
			}
		}.execute();
    }
    
    public void deleteDocument(final String id, final String rev) {
    	new EktorpAsyncTask() {
			@Override
			protected void doInBackground() {
				Log.d("CouchDb", String.format("Deleting %s rev: %s", id, rev));
				CouchDbConnector connector = dbInstance.createConnector(DATABASE_NAME, true);
				connector.delete(id, rev);
			}
		}.execute();
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
    		startReplication();
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
    	getMenuInflater().inflate(R.menu.item_menu, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	switch (item.getItemId()) {
    		case R.id.edit_item:
    			Intent editIntent = new Intent(getApplicationContext(), SlyncyAddItemActivity.class);
    			editIntent.putExtra(SlyncyAddItemActivity.EDIT_DOC, adapter.getRow(menuInfo.position).getValue());
    			startActivityForResult(editIntent, EDIT_ITEM);
    			return true;
    		case R.id.delete_item:
    			Row row = adapter.getRow(menuInfo.position);
    			deleteDocument(row.getId(), row.getValueAsNode().get("_rev").getTextValue());
    			return true;
			default:
				return super.onContextItemSelected(item);
    	}
    }
    
    private void startCouch() {
    	CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), couchDelegate);
    	couchConnection = couch.startCouchbase();
    }
    
    private void startEktorp(String host, int port) {
    	if (httpClient != null) httpClient.shutdown();
    	AndroidHttpClient.Builder clientBuilder = new AndroidHttpClient.Builder();
    	httpClient = (AndroidHttpClient)clientBuilder.host(host).port(port).build();
    	dbInstance = new StdCouchDbInstance(httpClient);
    	
    	new EktorpAsyncTask() {
			@Override
			protected void doInBackground() {
				dbConnector = dbInstance.createConnector(DATABASE_NAME, true);

				try {
					DesignDocument dDoc = dbConnector.get(DesignDocument.class, designDocId);
					dDoc.addView(byNameViewName, new DesignDocument.View(byNameViewFn));
					dbConnector.update(dDoc);
				} catch (DocumentNotFoundException ndfe) {
					DesignDocument dDoc = new DesignDocument(designDocId);
					dDoc.addView(byNameViewName, new DesignDocument.View(byNameViewFn));
					dbConnector.create(dDoc);
				}
			}
			
			@Override
			protected void onSuccess() {
				ViewQuery query = new ViewQuery().designDocId(designDocId).viewName(byNameViewName).descending(true);
				adapter = new CouchbaseViewListAdapter(dbConnector, query, true) {
					@Override
					public View getView(int position, View currentView, ViewGroup parent) {
						View view = currentView;
				        if (view == null) {
				            view = getLayoutInflater().inflate(R.layout.list_item, null);
				        }
				        
				        TextView nameText = (TextView)view.findViewById(R.id.nameText);
				        Row row = getRow(position);
				        JsonNode item = row.getValueAsNode();
				        JsonNode itemText = item.get("name");
				        if (itemText != null) {
				        	nameText.setText(itemText.getTextValue());
				        } else {
				        	nameText.setText("");
				        }
				        view.setId(position);
				        view.setTag(row.getId());
				        return view;
					}
				};
				ListView itemList = (ListView)findViewById(R.id.item_list);
				itemList.setAdapter(adapter);
				stopLoading();
			}
		}.execute();
    }
    
    private ICouchbaseDelegate couchDelegate = new ICouchbaseDelegate() {
		@Override
		public void exit(String error) {
			error = String.format("Failed to connect to couch: %s", error);
			Log.e("CouchDb", error);
			Toast.makeText(getBaseContext(), error, Toast.LENGTH_LONG).show();
		}
		
		@Override
		public void couchbaseStarted(String host, int port) {
			startEktorp(host, port);
		}
	};
	
	protected void onDestroy() {
		if (couchConnection != null) unbindService(couchConnection);
		if (adapter != null) adapter.cancelContinuous();
		if (httpClient != null) httpClient.shutdown();
		super.onDestroy();
	};
	
	private void startReplication() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String syncUrl = prefs.getString("couchdb_url_pref", null);

		pushReplicationCommand = new ReplicationCommand.Builder()
			.source(DATABASE_NAME)
			.target(syncUrl)
			.continuous(true)
			.build();

		EktorpAsyncTask pushReplication = new EktorpAsyncTask() {
			@Override
			protected void doInBackground() {
				dbInstance.replicate(pushReplicationCommand);
			}
		};
		pushReplication.execute();

		pullReplicationCommand = new ReplicationCommand.Builder()
			.source(syncUrl)
			.target(DATABASE_NAME)
			.continuous(true)
			.build();

		EktorpAsyncTask pullReplication = new EktorpAsyncTask() {
			@Override
			protected void doInBackground() {
				dbInstance.replicate(pullReplicationCommand);
			}
		};
		pullReplication.execute();
	}
	
	public void showLoading() {
		loadingDialog = new Dialog(this);
		loadingDialog.setTitle("Starting Couch");
		loadingDialog.show();
	}
	
	public void stopLoading() {
		loadingDialog.dismiss();
		loadingDialog = null;
	}
}