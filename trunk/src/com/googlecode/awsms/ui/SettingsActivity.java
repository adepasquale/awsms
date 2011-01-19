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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.googlecode.awsms.R;

/**
 * Activity used to setup web senders parameters, e.g. username and password.
 * 
 * @author Andrea De Pasquale
 */
// TODO create account configuration activity
// TODO create application information activity
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	addPreferencesFromResource(R.layout.settings);

	Preference aboutPreference = (Preference) findPreference("About");
	aboutPreference
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference preference) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(getString(R.string.AboutURL)));
			startActivity(intent);
			return true;
		    }
		});

	Preference feedbackPreference = (Preference) findPreference("Feedback");
	feedbackPreference
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference preference) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri
				.parse(getString(R.string.FeedbackURL)));
			startActivity(intent);
			return true;
		    }
		});

	Preference donatePreference = (Preference) findPreference("Donate");
	donatePreference
		.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		    public boolean onPreferenceClick(Preference preference) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(getString(R.string.DonateURL)));
			startActivity(intent);
			return true;
		    }
		});
    }

}
