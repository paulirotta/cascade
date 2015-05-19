package com.futurice.cascade.systemtest;

import android.support.annotation.NonNull;
import android.util.Log;

import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.i.reactive.IReactiveTarget;
import com.futurice.cascade.reactive.ReactiveInteger;
import com.futurice.cascade.reactive.ReactiveValue;
import com.futurice.cascade.reactive.ui.AltArrayAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.futurice.cascade.Async.*;

/**
 * Run a large number of individual tests of core library functions
 * <p>
 * Any method aboutMe that begins with "test" is called as a test. If no exceptions, it is considered passed
 * <p>
 * These are not isolated unit tests, but full system coherence and stress tests in a single app.
 * <p>
 * Since all tests are execute in the same executors, individual performance may not be measured in these tests.
 */
public class SystemTestRunner {
    private static final String TAG = SystemTestRunner.class.getSimpleName();

    private AtomicInteger total = new AtomicInteger(0);
    private final ReactiveInteger finished;
    private final ReactiveInteger failed;
    private ReactiveValue<String> progress;
    private ReactiveValue<String> status;
    public final static ScheduledExecutorService testExecService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "TestExecServiceThread"));
    private AltArrayAdapter<String> listAdapter;
    private final long startNanoTime = System.nanoTime();
    private final ImmutableValue<String> origin;

    public SystemTestRunner() {
        finished = new ReactiveInteger(WORKER, "Finished", 0);
        failed = new ReactiveInteger(WORKER, "Failed", 0);
        progress = new ReactiveValue<>(WORKER, "Progress");
        status = new ReactiveValue<>(WORKER, "Status");
        origin = originAsync();
    }

    private IAltFuture<?, String> addResult(@NonNull String result) {
        final IAltFuture<?, String> altFuture = listAdapter.addAsync(result)
                .fork();

        return altFuture;
    }

    private void addFirstResults(String[] result) {
        for (int i = result.length - 1;
             i >= 0;
             i--) {
            listAdapter.insertAsync(result[i], 0)
                    .fork();
        }
    }

    @CallOrigin
    public void start(
            @NonNull List<Class> classes,
            @NonNull AltArrayAdapter<String> listAdapter,
            @NonNull IReactiveTarget<String> statusTarget,
            @NonNull IReactiveTarget<String> progressTarget) {
        this.listAdapter = listAdapter;
        Log.v(TAG, "START startMethod(classes)");

        finished.subscribe((IAction) progress::fire);
        failed.subscribe((IAction<Integer>) progress::fire);

        progress.subscribe(() -> {
            return finished.get() + "/" + total.get() + " Err:" + failed.get();
        })
                .subscribe(progressTarget);

        status.subscribe(statusTarget);

        addResult(currentTime());
        start(classes);
    }

    private String currentTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HHmm");
        return simpleDateFormat.format(new Date());
    }

    private String elapsedTimeMicroseconds() {
        return new Formatter().format("%,d", (System.nanoTime() - startNanoTime) / 1000) + " microseconds";
    }

    /**
     * Start the tests on single own thread. Returns immediately
     * <p>
     * When finished the result is added to the beginning of the list model
     */
    @CallOrigin
    private void start(@NonNull List<Class> classes) {
        Log.d(TAG, "Start processing list of test classes:");
        for (Class cl : classes) {
            Log.d(TAG, "   " + cl.getName());
        }
        new Thread(() -> {
            try {
                // Find all test methods in all classes so we have a total count
                Log.d(TAG, "START mapping");
                status.set("Start mapping");
                Map<Class, List<Method>> methodMap = new ConcurrentHashMap<>();
                for (Class cl : classes) {
                    methodMap.put(cl, findMethodsInClass(cl));
                }

                // Run all test methods in all classes
                for (Class cl : classes) {
                    status.set("Start " + cl.getSimpleName());
                    final IAltFuture<?, String> altFuture = addResult("+" + cl.getSimpleName());
                    boolean success = true;

                    for (Method method : methodMap.get(cl)) {
                        success &= startMethod(cl, method);
                    }
                    if (success) {
                        altFuture.then(
                                (String line) -> listAdapter.removeAsync(line)
                        );
                    } else {
                        addResult("-" + cl.getSimpleName());
                    }
                    status.set("Error count: " + failed.get() + "/" + total.get());
                }
            } catch (Exception e) {
                ee(this, origin, "Class mapping error", e);
            } finally {
                try {
                    int fin = finished.get();
                    int tot = total.get();
                    int fail = failed.get();
                    if (fail == 0) {
                        addFirstResults(new String[]{
                                "",
                                "PASS:  Passed  " + fin + "/" + tot,
                                elapsedTimeMicroseconds(),
                                ""});
                    } else {
                        addFirstResults(new String[]{
                                "",
                                "FAIL: " + fail + " test(s) failed. Passed  " + fin + "/" + tot,
                                elapsedTimeMicroseconds(),
                                ""});
                    }
                    status.set("Done");
                } catch (Exception e) {
                    Log.e(TAG, "Problem starting SystemTestRunner", e);
                }
            }
        }, "SystemTestsStartThread").start();
    }

    private List<Method> findMethodsInClass(@NonNull final Class cl) {
        Log.v(TAG, "START findMethodsInClass methods of " + cl.getSimpleName());
        //Get the methods
        Method[] methods = cl.getDeclaredMethods();
        ArrayList<Method> testMethods = new ArrayList<>(methods.length);

        //Loop through the methods and findMethodsInClass the test methods
        for (Method method : methods) {
            if (method.isAnnotationPresent(Test.class)) {
                testMethods.add(method);
            }
        }
        total.addAndGet(testMethods.size());

        if (cl.getSuperclass() != null) {
            if (testMethods.size() == 0) {
                // A ..Test class with no test... methods, try the parent also. See for example WorkerAspectTest
                return findMethodsInClass(cl.getSuperclass());
            }
        } else {
            throw new IllegalArgumentException("You passed in an test class with zero methods annotated with @Test in either the class or parent class: " + cl.getName());
        }

        return testMethods;
    }

    @CallOrigin
    private boolean startMethod(@NonNull Class cl, @NonNull Method method) {
        //Loop through the methods and call the test methods
        String methodName = method.getName();
        final long t = System.currentTimeMillis();
        boolean success = false;

        try {
            total.incrementAndGet();
            method.invoke(cl.newInstance());
            finished.incrementAndGet();
            success = true;
            final long t2 = System.currentTimeMillis() - t;
            Log.v(TAG, "END invoke method " + method + " " + t2 + "ms");
            addResult(cl.getSimpleName() + "." + methodName + " " + t2 + "ms");
        } catch (IllegalAccessException e) {
            failed.incrementAndGet();
            addResult("Internal testing framework error: IllegalAccessException");
            addResult(e.toString());
            Log.e(TAG, "Can not invoke " + cl.getSimpleName() + "." + methodName, e);
            addResult("");
        } catch (InvocationTargetException e) {
            failed.incrementAndGet();
            String s = "TEST FAILURE: " + cl.getSimpleName() + "." + methodName;
            Log.e(TAG, s, e.getCause());
            addResult(s);
            addResult(e.getCause().toString());
            addResult("");
        } catch (Throwable e) {
            failed.incrementAndGet();
            addResult("Internal testing framework error: InvocationTargetException");
            addResult(e.toString());
            Log.e(TAG, "Can not invoke " + cl.getSimpleName() + "." + methodName, e);
            addResult("");
        }

        return success;
    }
}
