/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;

import java.util.HashSet;
import java.util.Set;

import de.blinkt.openvpn.core.Preferences;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class ExternalAppDatabase {

	Context mContext;

	public ExternalAppDatabase(Context c) {
		mContext =c;
	}
}
