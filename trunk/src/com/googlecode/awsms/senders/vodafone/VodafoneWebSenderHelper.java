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

package com.googlecode.awsms.senders.vodafone;

import android.content.Context;

import com.googlecode.awsms.senders.WebSenderHelper;

public class VodafoneWebSenderHelper extends WebSenderHelper {

    final static String DATABASE_ID = "Vodafone";
    
    public VodafoneWebSenderHelper(Context context) {
	super(context);
    }
    
    public String getUsername() {
        return preferences.getString("VodafoneUsername", "");
    }

    public String getPassword() {
        return preferences.getString("VodafonePassword", "");
    }

    public int calcRemaining(int length) {
	if (length == 0) {
	    return 0;
	} else if (length <= 160) {
	    return 160 - length;
	} else if (length <= 307) {
	    return 307 - length;
	} else if (length <= 360) {
	    return 360 - length;
	} else {
	    return 360 - length;
	}
    }

    public int calcFragments(int length) {
	if (length == 0) {
	    return 0;
	} else if (length <= 160) {
	    return 1;
	} else if (length <= 307) {
	    return 2;
	} else if (length <= 360) {
	    return 3;
	} else {
	    return 0;
	}
    }
    
    public void addCount(int length) {
	database.insertNew(DATABASE_ID, calcFragments(length));
    }
    
    public int getCount() {
	return database.queryToday(DATABASE_ID);
    }
    
    public int getLimit() {
	return 10;
    }

}
