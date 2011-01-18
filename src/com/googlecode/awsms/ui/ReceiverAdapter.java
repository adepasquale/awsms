/*
 * Copyright 2010-2011 Andrea De Pasquale
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.awsms.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.text.Annotation;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.Filterable;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.googlecode.awsms.R;

public class ReceiverAdapter extends ResourceCursorAdapter 
	implements Filterable {

    final static String TAG = "ReceiverAdapter";
    
    Context context;
    SharedPreferences preferences;

    public ReceiverAdapter(Context context) {
	super(context, R.layout.receiver, null, false);
	this.context = context;
	preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
	TextView name = (TextView) view.findViewById(R.id.ReceiverName);
	name.setText(cursor.getString(2));
	
	TextView number = (TextView) view.findViewById(R.id.ReceiverNumber);
	number.setText(cursor.getString(3));
	
	TextView type = (TextView) view.findViewById(R.id.ReceiverType);
	type.setText(Phone.getTypeLabel(context.getResources(),
		cursor.getInt(4), cursor.getString(5)));
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
	String constraintPath = null;
	if (constraint != null) {
	    constraintPath = constraint.toString();
	}

	Uri queryURI = Uri.withAppendedPath(
		Phone.CONTENT_FILTER_URI, Uri.encode(constraintPath));

	// TODO is it possible to filter out SIM contacts? 
	String[] projection = { 
		Phone._ID,
		Phone.CONTACT_ID,
		Phone.DISPLAY_NAME,
		Phone.NUMBER,
		Phone.TYPE,
		Phone.LABEL,
	};

	String selection = null;
	if (preferences.getBoolean("FilterMobile", true)) {
	    selection = String.format("%s=%s OR %s=%s",
		Phone.TYPE, Phone.TYPE_MOBILE, 
		Phone.TYPE, Phone.TYPE_WORK_MOBILE);
	}

	String sorting = Contacts.TIMES_CONTACTED + " DESC";

	return context.getContentResolver().query(
		queryURI, projection, selection, null, sorting);
    }

    @Override
    public CharSequence convertToString(Cursor cursor) {
	SpannableString receiver = new SpannableString(
		cursor.getString(2) + " <" + cursor.getString(3) + ">");

	receiver.setSpan(new Annotation("number", cursor.getString(3)), 0,
		receiver.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	receiver.setSpan(new Annotation("name", cursor.getString(2)), 0,
		receiver.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

	return receiver;
    }

}
