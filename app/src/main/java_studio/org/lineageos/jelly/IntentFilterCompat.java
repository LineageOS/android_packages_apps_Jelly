package org.lineageos.jelly;

import android.content.IntentFilter;

public class IntentFilterCompat {
    public static boolean filterIsBrowser(IntentFilter filter) {
        return filter.countDataAuthorities() == 0;
    }
}
