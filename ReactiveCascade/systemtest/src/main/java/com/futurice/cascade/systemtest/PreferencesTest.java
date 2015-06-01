package com.futurice.cascade.systemtest;

import android.content.Context;

import com.futurice.cascade.Async;
import com.futurice.cascade.functional.*;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.reactive.*;
import com.futurice.cascade.rest.PreferencesMirrorService;

import java.util.List;
import static com.futurice.cascade.Async.*;

/**
 * Created by Paul Houghton on 25/06/2014.
 */
public class PreferencesTest {
    private static final String TAG = PreferencesTest.class.getSimpleName();
    private final ReactiveValue<String> reactiveValue;
    private final PreferencesMirrorService testMirror;

    public PreferencesTest(Context context, ReactiveValue<String> reactiveValue) {
        this.reactiveValue = reactiveValue;
        testMirror = new PreferencesMirrorService("TestMirror", context, FILE, FILE);
    }

    public void test() throws Exception {
        reactiveValue.set("Init");
        testMirror.indexAsync()
                .then((List<String> index) -> {
                    IOnErrorAction onError = e -> Async.e(TAG, "Can not assertTrue delete key", e);
                    for (String key : index) {
                        reactiveValue.set("Delete key " + key);
                        testMirror.deleteAsync(key)
                                .onError(onError)
                                .fork();
                    }

                    AltFuture lastPutAltFuture = null;
                    onError = e -> Async.e(this, "Can not assertTrue put key", e);

                    for (int i = 0; i < 1000; i++) {
//TODO Preferences assertTrue is incomplete
//            lastPutAltFuture = testMirror.putAsync("Key" + i, "Value" + i, onError)
//                    .split((IActionOne) v -> bindable.set("Put " + v))
//                    .fork();
                    }
//        lastPutAltFuture.get(1000, TimeUnit.MILLISECONDS);
                })
                .fork();
    }
}
