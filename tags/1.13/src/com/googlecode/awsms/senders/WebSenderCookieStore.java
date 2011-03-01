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

package com.googlecode.awsms.senders;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

// TODO implement CookieStore without using BasicCookieStore
public class WebSenderCookieStore implements CookieStore, Serializable {
    static final long serialVersionUID = 1L;

    transient CookieStore cookieStore;
    
    public WebSenderCookieStore() {
	cookieStore = new BasicCookieStore();
    }
    
    @Override
    public void addCookie(Cookie cookie) {
	cookieStore.addCookie(cookie);
    }

    @Override
    public void clear() {
	cookieStore.clear();
    }

    @Override
    public boolean clearExpired(Date date) {
	return cookieStore.clearExpired(date);
    }

    @Override
    public List<Cookie> getCookies() {
	return cookieStore.getCookies();
    }

    private void readObject(ObjectInputStream input) 
    		throws IOException, ClassNotFoundException {
	cookieStore = new BasicCookieStore();
	
	input.defaultReadObject();
	int size = input.readInt();
	while (size-- > 0) {
	    WebSenderCookie wsCookie = (WebSenderCookie) input.readObject();
	    cookieStore.addCookie(wsCookie.getCookie());
	}
    }
    
    private void writeObject(ObjectOutputStream output) throws IOException {
	output.defaultWriteObject();
	List<Cookie> cookies = cookieStore.getCookies();
	output.writeInt(cookies.size());
	for (Cookie cookie : cookies) {
	    output.writeObject(new WebSenderCookie(cookie));
	}
    }
    
    // TODO implement Cookie interface entirely
    class WebSenderCookie implements Serializable {
	static final long serialVersionUID = 1L;
	
	int version;
	String name;
	String value;
	String domain;
	String path;
	long expiry;
	
	public WebSenderCookie(Cookie cookie) {
	    version = cookie.getVersion();
	    name = cookie.getName();
	    value = cookie.getValue();
	    domain = cookie.getDomain();
	    path = cookie.getPath();
	    
	    expiry = 0;
	    Date expiryDate = cookie.getExpiryDate();
	    if (expiryDate != null) expiry = expiryDate.getTime();
	}
	
	public Cookie getCookie() {
	    BasicClientCookie cookie = new BasicClientCookie(name, value);
	    cookie.setVersion(version);
	    cookie.setDomain(domain);
	    cookie.setPath(path);
	    if (expiry != 0) cookie.setExpiryDate(new Date(expiry));
	    return cookie;
	}
    }
}
