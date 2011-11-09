package com.shinetech.android.slyncy;

import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SlyncyAddItemActivity extends Activity implements OnClickListener {
	public static final int ADD_ITEM = 1;
	public static final int EDIT_ITEM = 2;
	public static final String EDIT_DOC = "EDIT_DOC";
	public static final String FORM_VALUES = "FORM_VALUES";
	private String currentId;
	private String currentRev;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_item);
		Button addFieldButton = (Button)findViewById(R.id.add_field_button);
		Button doneButton = (Button)findViewById(R.id.done_button);
		addFieldButton.setOnClickListener(this);
		doneButton.setOnClickListener(this);
		onNewIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		currentId = null;
		currentRev = null;
		String editItem = intent.getStringExtra(EDIT_DOC);
		if (editItem != null) {
			try {
				JSONObject item = new JSONObject(editItem);
				JSONArray itemNames = item.names();
				int itemNamesLength = itemNames.length();
				for (int i = 0; i < itemNamesLength; i++) {
					String itemName = itemNames.getString(i);
					String value = item.getString(itemName);
					if (itemName.equals("name")) {
						TextView nameView = (TextView)findViewById(R.id.name_field);
						nameView.setText(value);
					} else if (itemName.equals("_id")) {
						currentId = value;
					} else if (itemName.equals("_rev")) {
						currentRev = value;
					} else {
						addNewField(itemName, value);
					}
				}
			} catch (Exception e) {
				// We don't care, its an example app
				Log.e("Json", "Error parsing object JSON", e);				
			}
		}
		super.onNewIntent(intent);
	}
	
	public void addNewField(String name, String value) {
		ViewGroup fieldContainer = (ViewGroup)findViewById(R.id.field_container);
		ViewGroup addedView = (ViewGroup)getLayoutInflater().inflate(R.layout.new_field, null);
		if (name != null) {
			TextView nameView = (TextView)addedView.getChildAt(0);
			nameView.setText(name);
		}
		if (value != null) {
			TextView valueView = (TextView)addedView.getChildAt(1);
			valueView.setText(value);
		}
		fieldContainer.addView(addedView);
	}
	
	public LinkedHashMap<String, String> getValues() {
		ViewGroup fieldContainer = (ViewGroup)findViewById(R.id.field_container);
		int numberOfFields = fieldContainer.getChildCount();
		LinkedHashMap<String, String> values = new LinkedHashMap<String, String>(numberOfFields);
		for (int i = 0; i < numberOfFields; i++) {
			LinearLayout childView = (LinearLayout)fieldContainer.getChildAt(i);
			// No sanity checking at all, like a boss
			TextView nameText = (TextView)childView.getChildAt(0);
			EditText valueText = (EditText)childView.getChildAt(1);
			values.put(nameText.getText().toString(), valueText.getText().toString());
		}
		if (currentId != null) values.put("_id", currentId);
		if (currentRev != null) values.put("_rev", currentRev);
		return values;
	}
	
	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.add_field_button:
			addNewField(null, null);
			break;
		case R.id.done_button:
			Intent data = new Intent();
			data.putExtra(FORM_VALUES, getValues());
			setResult(RESULT_OK, data);
			finish();
			break;
		}
	}
	
}
