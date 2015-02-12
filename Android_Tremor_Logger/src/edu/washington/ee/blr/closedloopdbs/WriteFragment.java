package edu.washington.ee.blr.closedloopdbs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

/* This class runs the dialog window that appears when the user chooses to write to a text file.
 * 
 * Any activity using this dialog MUST implement its Callback interface
 * 
 * */

public class WriteFragment extends DialogFragment {
	

	//Parent handles
	Activity parent;
	Callback parentInterface;
	
	//UI handles
	EditText mFilenameEditText;
	EditText mBytesEditText;
	EditText mHoursEditText;
	EditText mSecondsEditText;
	RadioGroup mWriteFileRadioGroup;
	TextView mIllegalCharsTextView;
	Button mOKButton;
	
	//handle to the dialog box itself
	AlertDialog dialog;
	
	//list of reserved characters that cannot be part of a filename in the Android file system
	private static final char[] reservedChars = {'|','\\','?','*','<','"',':','>','+','[',']','/','\''};
	
	
	public interface Callback {
		public void onWriteDialogOK(WriteOptions options);
		public void onWriteDialogCANCEL();
	}

	@SuppressLint("InflateParams")
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		//get calling Activity and inflate view
		parent = getActivity();
		parentInterface = (Callback) parent;
		
		View v = parent.getLayoutInflater().inflate(R.layout.dialog_write_text, null);
		
		//SET UP: mFilenameEditText
		mFilenameEditText = (EditText) v.findViewById(R.id.edittext_filename);
		mIllegalCharsTextView = (TextView) v.findViewById(R.id.textview_filename_warning);
		mFilenameEditText.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	if (mOKButton == null) {
	        		mOKButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
	        	}
	        	String enteredString = s.toString();
	        	for (char each: reservedChars) {
	        		if (enteredString.indexOf(each) != -1) {
	        			mOKButton.setEnabled(false);
	        			mIllegalCharsTextView.setVisibility(View.VISIBLE);
	        			return;
	        		}
	        	}

    			mOKButton.setEnabled(true);
    			mIllegalCharsTextView.setVisibility(View.INVISIBLE);
	        	
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    }); 
		
		//SET UP: mBytesEditText
		mBytesEditText = (EditText) v.findViewById(R.id.edittext_bytes);
		mBytesEditText.setText("21000");
		
		//SET UP: hours and min EditTexts
		mHoursEditText = (EditText) v.findViewById(R.id.edittext_write_min);
		mHoursEditText.setText("0");
		mSecondsEditText = (EditText) v.findViewById(R.id.edittext_write_sec);
		mSecondsEditText.setText("10");
		mSecondsEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					processExcessSeconds();
				}
				
			}
		});
		
		//SET UP: RadioButton Group
		mWriteFileRadioGroup = (RadioGroup) v.findViewById(R.id.radiogroup_write_mode);
		mWriteFileRadioGroup.check(R.id.radiobutton_byte_write_mode);
		setWriteFileRadioGroupElementsActive();
		mWriteFileRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				setWriteFileRadioGroupElementsActive();
				
			}
		});
		
		//we create the dialog box and save a handle to it so we can disabled the OK button if user
		//chooses a filename that is invalid.
		dialog = new AlertDialog.Builder(getActivity()).setTitle("Write to file...").setView(v)
				.setPositiveButton(android.R.string.ok, 
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								
								WriteOptions options = new WriteOptions();
								options.filename = mFilenameEditText.getText().toString();
								switch(mWriteFileRadioGroup.getCheckedRadioButtonId()  ) {
									case R.id.radiobutton_byte_write_mode:
										options.mode = WriteOptions.WriteMode.BYTES;
										options.quantity = getSelectedEditTextNumber(mBytesEditText);
									break;
									case R.id.radiobutton_time_write_mode:
										options.mode = WriteOptions.WriteMode.TIME;
										options.quantity = getSelectedEditTextNumber(mHoursEditText)*60 + getSelectedEditTextNumber(mSecondsEditText);
									break;
									case R.id.radiobutton_continuous_write_mode:
										options.mode = WriteOptions.WriteMode.CONTINUOUS;
									break;
								}
								
								parentInterface.onWriteDialogOK(options);

							}
						})
				.setNegativeButton(android.R.string.cancel, 
						new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								parentInterface.onWriteDialogCANCEL();
								
							}
						}) .create();
		
		
		return dialog;
		
	}
	
	//when the seconds box loses focus, we want to turns its excess seconds into hours, for ease of user interaction
	void processExcessSeconds() {
		int seconds = getSelectedEditTextNumber(mSecondsEditText);
		if (seconds > 59) {
			int excessMinutes = seconds / 60;
			int newSeconds = seconds % 60;
			int existingMinutes = getSelectedEditTextNumber(mHoursEditText);
			mHoursEditText.setText(String.valueOf(existingMinutes + excessMinutes));
			mSecondsEditText.setText(String.valueOf(newSeconds));
		}
	}
	
	//this method is used to parse the number currently entered in any of the three textboxes in the page, since
	//the code must alaways check to make sure we get the number '0' from an empty string.
	int getSelectedEditTextNumber(EditText edText) {
		String text = edText.getText().toString();
		if (text.length() > 0) {
			return Integer.valueOf(text);
		}
		else return 0;	
	}
	
	//this method enables/disables the appropriate UI elements depending on what options a user has selected.
	void setWriteFileRadioGroupElementsActive() {
		
		mBytesEditText.setEnabled(false);
		mHoursEditText.setEnabled(false);
		mSecondsEditText.setEnabled(false);
		
		switch(mWriteFileRadioGroup.getCheckedRadioButtonId()) {
			case R.id.radiobutton_byte_write_mode:
				mBytesEditText.setEnabled(true);
				break;
			case R.id.radiobutton_time_write_mode:
				mHoursEditText.setEnabled(true);
				mSecondsEditText.setEnabled(true);
				break;
			case R.id.radiobutton_continuous_write_mode:
				break;
		
		}
	}
	
}
