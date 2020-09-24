package com.adgad.dopamine;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.AttributeSet;

import androidx.preference.MultiSelectListPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContactListPreference extends MultiSelectListPreference {

    ContentResolver cr;
    Cursor cursor;

    public ContactListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        List<CharSequence> entries = new ArrayList<CharSequence>();
        List<CharSequence> entriesValues = new ArrayList<CharSequence>();

        cr = context.getContentResolver();
        cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,null, null, null, null);

        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

            entries.add(name);
            entriesValues.add(name);
        }

        setEntries(entries.toArray(new CharSequence[]{}));
        setEntryValues(entriesValues.toArray(new CharSequence[]{}));
    }

    @Override
    public CharSequence getSummary() {
            int count = 0;
            CharSequence[] entries = getEntries();
            if(entries.length > 0) {
                CharSequence[] entryValues = getEntryValues();
                Set<String> values = getValues();
                int pos = 0;

                for (String value : values) {
                    pos++;
                    int index = -1;
                    for (int i = 0; i < entryValues.length; i++) {
                        if (entryValues[i].equals(value)) {
                            index = i;
                            break;
                        }
                    }
                    count++;
                }
            }
        return count + " contacts blocked";
    }
}