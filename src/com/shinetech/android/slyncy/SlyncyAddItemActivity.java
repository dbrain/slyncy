package com.shinetech.android.slyncy;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class SlyncyAddItemActivity extends Activity implements OnClickListener {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_item);
		Button addFieldButton = (Button)findViewById(R.id.add_field_button);
		Button doneButton = (Button)findViewById(R.id.done_button);
		addFieldButton.setOnClickListener(this);
		doneButton.setOnClickListener(this);
	}
	
	public void addNewField() {
		ViewGroup fieldContainer = (ViewGroup)findViewById(R.id.field_container);
		getLayoutInflater().inflate(R.layout.new_field, fieldContainer);
	}
	
	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		switch (viewId) {
		case R.id.add_field_button:
			addNewField();
			break;
		case R.id.done_button:
			setResult(RESULT_OK);
			finish();
			break;
		}
	}
	
}
