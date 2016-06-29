package com.reactivecascade.systemtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.reactivecascade.Async;
import com.reactivecascade.AsyncBuilder;
import com.reactivecascade.reactive.ReactiveValue;
import com.reactivecascade.reactive.ui.AltArrayAdapter;
import com.reactivecascade.reactive.ui.ReactiveTextView;

import java.util.ArrayList;

import static com.reactivecascade.Async.*;


public class MainActivity extends Activity {
    private static Async async;

    private static ReactiveValue<String> progress;
    private static ReactiveValue<String> result;
    private SystemTestRunner systemTestRunner;
    private static AltArrayAdapter<String> systemTestResultsListAdapter;
    private final static ArrayList<String> testResultModel = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (async == null) {
            async = new AsyncBuilder(this)
                    .setFailFast(false)
                    .build(); // Fail fast on first exception is a bad idea when running tests which intentionally throw exceptions
        }

        setContentView(R.layout.activity_main);

        if (systemTestRunner == null) {
            systemTestRunner = new SystemTestRunner();
            progress = new ReactiveValue<>("MainActivity Progress", "Progress..");
            result = new ReactiveValue<>("MainActivity Result", "Result..");
            systemTestResultsListAdapter = new AltArrayAdapter<>(this, R.layout.list_item_layout, R.id.list_item, testResultModel);
        }

        ((ListView) findViewById(R.id.test_results_list)).setAdapter(systemTestResultsListAdapter);
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

    @Override
    protected void onStart() {
        final ArrayList<Class> classes = new ArrayList<>();

//            classes.add(UIThreadTypeTest.class);
        classes.add(WorkerThreadTypeTest.class);
//            classes.add(SerialWorkerThreadTypeTest.class);
//            classes.add(TwoThreadThreadTypeTest.class);
//            classes.add(FixedThreadPoolTwoThreadThreadTypeTest.class);
//            classes.add(InOrderThreadTypeTest.class);

//        systemTestRunner.start(classes, systemTestResultsListAdapter, progress, result);
        systemTestRunner.start(
                classes,
                systemTestResultsListAdapter,
                ((ReactiveTextView) findViewById(R.id.test_progress_textview)).getReactiveValue(),
                ((ReactiveTextView) findViewById(R.id.test_result_textview)).getReactiveValue());

//        progress.subscribe(((ReactiveTextView) findViewById(R.id.test_progress_textview)).getReactiveValue());
//        result.subscribe(((ReactiveTextView) findViewById(R.id.test_result_textview)).getReactiveValue());

        super.onStart();
    }
}
