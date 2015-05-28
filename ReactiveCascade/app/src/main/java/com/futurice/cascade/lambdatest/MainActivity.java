/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade.lambdatest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.futurice.cascade.Async;
import com.futurice.cascade.AsyncBuilder;
import com.futurice.cascade.reactive.ReactiveValue;
import com.futurice.cascade.reactive.ui.ReactiveEditText;
import com.futurice.cascade.reactive.ui.ReactiveTextView;

import static com.futurice.cascade.Async.*;

public class MainActivity extends Activity {
    private static Async async;
    private static ReactiveValue<String> typedText;
    private static ReactiveValue<String> chainedText;
    private static ReactiveValue<String> countText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (async == null) {
            //TODO Simplify
            async = new AsyncBuilder(this).build();
            typedText = new ReactiveValue<>(WORKER, "typedText", "Type here");
            chainedText = new ReactiveValue<>(WORKER, "chainedText");
            countText = new ReactiveValue<>(WORKER, "countText");

            typedText.subscribe(chainedText);
            typedText.subscribe(s -> "" + s.length())
                    .subscribe(countText);
            typedText.fire();
        }

        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onStart() {
        super.onStart();

        ((ReactiveEditText) findViewById(R.id.editText)).setReactiveValue(typedText, true);
        ((ReactiveTextView) findViewById(R.id.textView)).setReactiveValue(chainedText, true);
        ((ReactiveTextView) findViewById(R.id.textCountView)).setReactiveValue(countText, true);

        // Flush initial value now that the wiring is done
        typedText.fire();
    }
}
