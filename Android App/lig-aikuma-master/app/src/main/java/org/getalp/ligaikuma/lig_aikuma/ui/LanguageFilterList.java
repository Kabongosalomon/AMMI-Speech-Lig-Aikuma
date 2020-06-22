package org.getalp.ligaikuma.lig_aikuma.ui;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import org.getalp.ligaikuma.lig_aikuma.Aikuma;
import org.getalp.ligaikuma.lig_aikuma.lig_aikuma.R;
import org.getalp.ligaikuma.lig_aikuma.model.Language;

public class LanguageFilterList extends ListActivity {
	
	private int textview_id;
	private final String textview_str = "textview_id";
	private ArrayAdapter<Language> adapter;
	private EditText filterText = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter_list);

		filterText = (EditText) findViewById(R.id.search_box);
		filterText.addTextChangedListener(filterTextWatcher);
		adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Aikuma.getLanguages(this.getApplicationContext()));
		setListAdapter(adapter);
	    textview_id = getIntent().getIntExtra(textview_str, -1);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent();
		intent.putExtra(textview_str, textview_id);
		intent.putExtra("language", (Language)l.getItemAtPosition(position));
		setResult(RESULT_OK, intent);
		this.finish();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		filterText.removeTextChangedListener(filterTextWatcher);
	}

	private TextWatcher filterTextWatcher = new TextWatcher() {
		public void afterTextChanged(Editable s) {}
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{adapter.getFilter().filter(s);}
	};
}
