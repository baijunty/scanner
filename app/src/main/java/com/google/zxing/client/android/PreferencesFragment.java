/*
 * Copyright (C) 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.baijunty.scanner.R;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implements support for barcode scanning preferences.
 *
 * @see PreferencesFragment
 */
public final class PreferencesFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_DECODE_1D_PRODUCT = "preferences_decode_1D_product";
    public static final String KEY_DECODE_1D_INDUSTRIAL = "preferences_decode_1D_industrial";
    public static final String KEY_DECODE_QR = "preferences_decode_QR";
    public static final String KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix";
    public static final String KEY_DECODE_AZTEC = "preferences_decode_Aztec";
    public static final String KEY_DECODE_PDF417 = "preferences_decode_PDF417";


    public static final String KEY_PLAY_BEEP = "preferences_play_beep";
    public static final String KEY_VIBRATE = "preferences_vibrate";
    public static final String KEY_FRONT_LIGHT_MODE = "preferences_front_light_mode";
    public static final String KEY_AUTO_FOCUS = "preferences_auto_focus";
    public static final String KEY_INVERT_SCAN = "preferences_invert_scan";
    public static final String KEY_SEARCH_COUNTRY = "preferences_search_country";

    public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "preferences_disable_continuous_focus";
    public static final String KEY_DISABLE_EXPOSURE = "preferences_disable_exposure";
    public static final String KEY_DISABLE_METERING = "preferences_disable_metering";
    public static final String KEY_DISABLE_BARCODE_SCENE_MODE = "preferences_disable_barcode_scene_mode";
    private CheckBoxPreference[] checkBoxPrefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences,null);
    }

    private static CheckBoxPreference[] findDecodePrefs(PreferenceScreen preferences, String... keys) {
        CheckBoxPreference[] prefs = new CheckBoxPreference[keys.length];
        for (int i = 0; i < keys.length; i++) {
            prefs[i] = preferences.findPreference(keys[i]);
        }
        return prefs;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.preferences);

        PreferenceScreen preferences = getPreferenceScreen();
        preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        checkBoxPrefs = findDecodePrefs(preferences,
                PreferencesFragment.KEY_DECODE_1D_PRODUCT,
                PreferencesFragment.KEY_DECODE_1D_INDUSTRIAL,
                PreferencesFragment.KEY_DECODE_QR,
                PreferencesFragment.KEY_DECODE_DATA_MATRIX,
                PreferencesFragment.KEY_DECODE_AZTEC,
                PreferencesFragment.KEY_DECODE_PDF417);
        disableLastCheckedPref();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        disableLastCheckedPref();
    }

    private void disableLastCheckedPref() {
        Collection<CheckBoxPreference> checked = new ArrayList<>(checkBoxPrefs.length);
        for (CheckBoxPreference pref : checkBoxPrefs) {
            if (pref.isChecked()) {
                checked.add(pref);
            }
        }
        boolean disable = checked.size() <= 1;
        for (CheckBoxPreference pref : checkBoxPrefs) {
            pref.setEnabled(!(disable && checked.contains(pref)));
        }
    }

}
