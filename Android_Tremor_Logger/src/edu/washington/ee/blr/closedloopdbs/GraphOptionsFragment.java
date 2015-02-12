package edu.washington.ee.blr.closedloopdbs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/* Class represents dialog box that lets user select graph options.
 * 
 * Any activity creating this fragment MUST implement its inner Callback interface.
 * 
 * */

public class GraphOptionsFragment extends DialogFragment {
	
	public interface Callback {
		public void onOptionsChosen(GraphOptions chosenOptions);
	}
	
	GraphOptions selectedOptions;
	Callback parentActivity;
	
	//UI Elements
	RadioGroup mRadioGroupGraphOptions;
	RadioButton mRadioLiveGraph;
	CheckBox mAccelCheckBox;
	CheckBox mGyroCheckBox;
	CheckBox mCompassCheckBox;
	CheckBox mMagnitudesOnlyCheckBox;
	EditText mPacketsToShowEditText;
	TextView mPacketsToShowTextView;
	
	
	public GraphOptionsFragment(GraphOptions incomingOptions) {
		super();
		selectedOptions = incomingOptions;
	}
	

	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		parentActivity = (Callback) getActivity();
		View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_graph, null);
		
		//SET UP: Checkboxes
		mAccelCheckBox = (CheckBox) v.findViewById(R.id.checkbox_accel);
		mGyroCheckBox = (CheckBox) v.findViewById(R.id.checkbox_gyro);
		mCompassCheckBox = (CheckBox) v.findViewById(R.id.checkbox_compass);
		mMagnitudesOnlyCheckBox = (CheckBox) v.findViewById(R.id.checkbox_magnitude);
		//set Checkboxes according to incoming Graph Options
		if (selectedOptions.isAccelOn)
			mAccelCheckBox.setChecked(true);
		if (selectedOptions.isGyroOn)
			mGyroCheckBox.setChecked(true);
		if (selectedOptions.isCompassOn)
			mCompassCheckBox.setChecked(true);
		if (selectedOptions.isMagnitudeOn)
			mMagnitudesOnlyCheckBox.setChecked(true);
		
		//SET UP: packets to Show Edit Text and set according to incoming Graph Options
		mPacketsToShowEditText = (EditText) v.findViewById(R.id.edittext_timeToShow);
		mPacketsToShowEditText.setText(String.valueOf( selectedOptions.packetsToShow ));
		
		//SET UP: packets to show TextView
		mPacketsToShowTextView = (TextView) v.findViewById(R.id.textview_timeToShow);
		
		//SET UP: Radio Button Group
		mRadioGroupGraphOptions = (RadioGroup) v.findViewById(R.id.radiogroup_graph_options_fragment);
		mRadioGroupGraphOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				
				switch(group.getCheckedRadioButtonId()) {
				case R.id.radiobutton_graph:
					selectedOptions.optionType = GraphOptions.GraphOptionType.LIVE_GRAPH_MODE;
					setSensoryGroupEnabled(true);
					break;
				case R.id.radiobutton_int:
					selectedOptions.optionType = GraphOptions.GraphOptionType.CURRENT_STATE_MODE;
					setSensoryGroupEnabled(false);
					break;
				case R.id.radiobutton_raw:
					selectedOptions.optionType = GraphOptions.GraphOptionType.DEBUG_MODE;
					setSensoryGroupEnabled(false);
					break;
				}
				
			}
		});
		//set Radio Group Buttons according to incoming Graph Options
		switch(selectedOptions.optionType) {
			case DEBUG_MODE:
				mRadioGroupGraphOptions.check(R.id.radiobutton_raw);
				break;
			case CURRENT_STATE_MODE:
				mRadioGroupGraphOptions.check(R.id.radiobutton_int);
				break;
			case LIVE_GRAPH_MODE:
				mRadioGroupGraphOptions.check(R.id.radiobutton_graph);
				setSensoryGroupEnabled(true);
				break;
		}
		

		
		//Build and display AlertDialog, and define OK button to return newly selected Graph Options
		return new AlertDialog.Builder(getActivity()).setTitle("Graph Options").setView(v)
				.setPositiveButton(android.R.string.ok, 
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								
								selectedOptions.packetsToShow = Integer.valueOf(mPacketsToShowEditText.getText().toString());
								
								if (mAccelCheckBox.isChecked())
									selectedOptions.isAccelOn = true;
								else
									selectedOptions.isAccelOn = false;
								
								if (mGyroCheckBox.isChecked())
									selectedOptions.isGyroOn = true;
								else
									selectedOptions.isGyroOn = false;
								
								if (mCompassCheckBox.isChecked())
									selectedOptions.isCompassOn = true;
								else
									selectedOptions.isCompassOn = false;
								
								if (mMagnitudesOnlyCheckBox.isChecked())
									selectedOptions.isMagnitudeOn = true;
								else
									selectedOptions.isMagnitudeOn = false;
								
								parentActivity.onOptionsChosen(selectedOptions);
							}
						}).create();
		
	}
	
	//method disables/enables Live Graph-related UI Elements when 'Show Live Graph' is selected.
	void setSensoryGroupEnabled(boolean areEnabled) {
		if (areEnabled) {
			mAccelCheckBox.setEnabled(true);
			mGyroCheckBox.setEnabled(true);
			mCompassCheckBox.setEnabled(true);
			mMagnitudesOnlyCheckBox.setEnabled(true);
			mPacketsToShowTextView.setEnabled(true);
			mPacketsToShowEditText.setEnabled(true);
			
		} else {

			mAccelCheckBox.setEnabled(false);
			mGyroCheckBox.setEnabled(false);
			mCompassCheckBox.setEnabled(false);
			mMagnitudesOnlyCheckBox.setEnabled(false);
			mPacketsToShowTextView.setEnabled(false);
			mPacketsToShowEditText.setEnabled(false);
		}
	}
	
	
	

	
}
