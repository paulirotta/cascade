/*
 * Copyright (c) 2015 Futurice GmbH. All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.futurice.cascade.systemtest;

import android.util.Log;

import com.futurice.cascade.util.AltFutureFuture;
import com.futurice.cascade.functional.ImmutableValue;
import com.futurice.cascade.functional.SettableAltFuture;
import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IActionR;
import com.futurice.cascade.i.functional.IAltFuture;
import com.futurice.cascade.reactive.ReactiveValue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.futurice.cascade.Async.SHOW_ERROR_STACK_TRACES;
import static com.futurice.cascade.Async.UI;
import static com.futurice.cascade.Async.d;
import static com.futurice.cascade.Async.v;
import static com.futurice.cascade.Async.vv;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * System exercise tests for an Aspect. By default this is Aspect.UI but overriding classes can
 * repeat these tests for another Aspect type or implementation
 */
@CallOrigin
public class ThreadTypeTest {
    private static final long TEST_TIMEOUT = 1000; //ms

    protected String tag;
    protected IThreadType threadType;

    public ThreadTypeTest() {
        // Replace these in any overriding class for a different Aspect
        tag = this.getClass().getSimpleName();
        threadType = UI;
    }

    private void logMethodStart() {
        v(tag, "Start " + Thread.currentThread().getStackTrace()[0].getMethodName());
    }

    private <IN, OUT> OUT awaitDone(IAltFuture<IN, OUT> altFuture) throws Exception {
        return new AltFutureFuture<>(altFuture).get(TEST_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * TODO This works for simple cases, but not when additional steps are spawned which generate the error
     *
     * @param action
     * @throws Throwable
     */
    private <IN> void hideIntentionalErrorStackTraces(IAction<IN> action) {
        SHOW_ERROR_STACK_TRACES = false;
        try {
            action.call();
        } catch (Exception e) {
            vv(tag, "hideIntentionalErrorStackTraces() from expected test exception: " + e);
        } finally {
            // Turn error stack traces back on only after any pending async tasks already in the queue (usually this is good enough for masking intentional exception tests on a single-threaded test setup)
            threadType.then(() -> {
                SHOW_ERROR_STACK_TRACES = true;
            });
        }
    }

    // Android Studio thinks this and other test methods is not called because they are called reflectively
    @Test
    public void thenIActionDoesSomething() throws Throwable {
        logMethodStart();
        IAltFuture altFuture = threadType.then(() -> {
            // Doing nothing
            Log.d("TEST", "Does nothing");
        });

        assertThat(altFuture.isDone()).isFalse();
        assertThat(altFuture.isCancelled()).isFalse();
        assertThat(altFuture.isForked()).isFalse();

        altFuture.fork();
        assertThat(altFuture.isForked()).isTrue();

        awaitDone(altFuture);
        assertThat(altFuture.isDone()).isTrue();
        assertThat(altFuture.isCancelled()).isFalse();
        assertThat(altFuture.isForked()).isTrue();
    }

    @Test
    public void thenIActionOnError() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            final AtomicReference<String> ar = new AtomicReference<>("empty");
            IAltFuture secondAltFuture = threadType.then(() -> d(tag, "Second catch exec"));
            IAltFuture altFuture = threadType.then(() -> {
                throw new Exception("Blah");
            })
                    .onError(e -> {
                        ar.set("yeah");
                        assertThat("blah").isEqualToIgnoringCase(e.getMessage());
                        secondAltFuture.fork();
                        return false;
                    })
                    .fork();

            awaitDone(secondAltFuture);
            assertThat("yeah").isEqualToIgnoringCase(ar.get());
        });
    }

    @Test
    public void thenIActionOnErrorAndConsume() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            final AtomicReference<String> ar = new AtomicReference<>("not set");
            IAltFuture secondAltFuture = threadType.then(() -> d(tag, "Second catch exec"));
            IAltFuture altFuture = threadType.then(() -> {
                throw new Exception("Ba");
            })
                    .onError(e -> {
                        ar.set("ya");
                        assertThat("ba").isEqualToIgnoringCase(e.getMessage());
                        secondAltFuture.fork();
                        return true;
                    })
                    .fork();

            awaitDone(altFuture);
            awaitDone(secondAltFuture);
            assertThat(ar.get()).isEqualToIgnoringCase("ya");
        });
    }

    @Test
    public void thenIActionOnErrorWithConsume_noPropagationAfterOnError() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            final AtomicReference<String> ar = new AtomicReference<>("not set");
            IAltFuture<Object, Object> secondAltFuture = threadType.then(() -> d(tag, "Second catch exec"));
            IAltFuture<Object, Object> altFuture = threadType.then(() -> {
                throw new Exception("Ba2");
            });
            altFuture.onError(e -> {
                ar.set("B: 1st onCatch");
                assertThat("B: ba2").isEqualToIgnoringCase(e.getMessage());
                secondAltFuture.fork();
                return true;
            })
                    .then(() -> ar.set("B: Foo"))
                    .onError(e -> {
                        ar.set("B: 2nd onCatch");
                        return false;
                    })
                    .fork();

            awaitDone(altFuture);
            awaitDone(secondAltFuture);

            assertThat(ar.get()).isEqualToIgnoringCase("B: 1st onCatch");
            assertThat(altFuture.isDone()).isTrue();
            assertThat(altFuture.isCancelled()).isTrue();
            assertThat(altFuture.isForked()).isTrue();
        });
    }

//    @Test
//    public void secondOnErrorIsCalledIfNoValueSent() throws Throwable {
//        logMethodStart();
//        final AtomicReference<String> ar = new AtomicReference<>("not set");
//        IAltFuture<Object, Object> secondCatch = threadType.subscribeTarget(() -> d(tag, "Second catch exec"));
//        IAltFuture<Long, Long> altFuture = new SettableAltFuture<>(threadType, Long.MAX_VALUE);
//        IAltFuture<String, String> altFuture2 = altFuture
//                .subscribeTarget(new IAction() { // Not folding to a lambda, want to be clear which case is being tested
//                    @Override
//                    public void call() throws Exception {
//                        ar.set("first call");
//                        throw new Exception("Ba2");
//                    }
//                })
//                .onError(
//                        (e, l)-> {
//                            ar.set("1st onCatch");
//                            assertThat("ba2").isEqualToIgnoringCase(e.getMessage());
//                            assertThat(Long.MAX_VALUE).isEqualTo(l);
//                        })
//                .subscribeTarget(() -> ar.set("Foo"))
//                .onError(
//                        e -> {
//                            ar.set("2nd onCatch");
//                            secondCatch.fork();
//                        })
//                .fork();
//
//        awaitDone(altFuture2);
//        awaitDone(secondCatch);
//        assertThat(ar.get()).isEqualToIgnoringCase("2nd onCatch");
//        assertThat(altFuture.isDone()).isTrue();
//        assertThat(altFuture.isCancelled()).isTrue();
//        assertThat(altFuture.isForked()).isTrue();
//    }

    @Test
    public void valueNotPassedToSecondOnError() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            final AtomicReference<String> ar = new AtomicReference<>("not set");
            IAltFuture<Object, Object> secondCatch = threadType.then(() -> d(tag, "Second catch exec"));
            IAltFuture<Object, Object> thirdCatch = threadType.then(() -> d(tag, "Third catch exec"));
            IAltFuture<?, Long> altFuture = new SettableAltFuture<>(threadType, Long.MAX_VALUE);
            IAltFuture<String, String> altFuture2 = altFuture
                    .then(() -> {
                        ar.set("C: first call");
                        throw new Exception("C: Ba2");
                    })
                    .onError(e -> {
                        ar.set("C: 1st onCatch");
                        assertThat("C: ba2").isEqualToIgnoringCase(e.getMessage());
                        return true;
                    })
                    .then(s -> {
                        ar.set(s + "C: Foo");
                        return "It was set to " + ar.get();
                    })
                    .onError(e -> {
                        //This will never be called because the up-chain onError(e,l) has absorbed the value and cancelled this, so there is not value to pass
                        ar.set(ar + " -> C: 2nd onCatch");
                        secondCatch.fork();
                        return false;
                    })
                    .then(s -> {
                        ar.set(s + "C: Bar");
                    })
                    .onError(e -> {
                        thirdCatch.fork();
                        return false;
                    })
                    .fork();
            awaitDone(altFuture);
            awaitDone(altFuture2);
            awaitDone(thirdCatch);
            assertThat(!secondCatch.isDone());
            assertThat(ar.get()).isEqualToIgnoringCase("C: 1st onCatch");
            assertThat(altFuture.isDone()).isTrue();
            assertThat(altFuture.isCancelled()).isTrue();
            assertThat(altFuture.isForked()).isTrue();
            assertThat(altFuture.isConsumed()).isFalse();
        });
    }

    @Test
    public void valuePassedToThirdOnError() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            final AtomicReference<String> ar = new AtomicReference<>("not set");
            IAltFuture<Object, Object> secondCatch = threadType.then(() -> d(tag, "Second catch exec"));
            IAltFuture<Object, Object> thirdCatch = threadType.then(() -> d(tag, "Third catch exec"));
            IAltFuture<Long, Long> altFuture = new SettableAltFuture<>(threadType, Long.MAX_VALUE)
                    .then(() -> {
                        ar.set("D: first call");
                        throw new Exception("Ba2");
                    });
            altFuture
                    .onError(e -> {
                        ar.set("D: 1st onCatch");
                        assertThat("D: ba2").isEqualToIgnoringCase(e.getMessage());
                        return true;
                    })
                    .then(() -> ar.set("D: Foo"))
                    .onError(e -> {
                        //This will never be called because the up-chain onError(e,l) has absorbed the value and cancelled this, so there is not value to pass
                        ar.set("D: 2nd onCatch");
                        secondCatch.fork();
                        return false;
                    })
                    .then(() -> ar.set("D: Bar"))
                    .onError(e -> {
                        ar.set("D: 3rd onCatch");
                        thirdCatch.fork();
                        return false;
                    })
                    .fork();

            awaitDone(altFuture);
            awaitDone(thirdCatch);
            assertThat(ar.get()).isEqualToIgnoringCase("D: 3rd onCatch");
            assertThat(altFuture.isDone()).isTrue();
            assertThat(altFuture.isCancelled()).isTrue();
            assertThat(altFuture.isForked()).isTrue();
        });
    }

    @Test
    public void thenIActionOnCatch_propagationAfterOnError() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            final AtomicReference<String> ar = new AtomicReference<>("not set");
            IAltFuture<Object, Object> e1Catch = threadType.then(() -> d(tag, "Error one exec"));
            IAltFuture<Object, Object> fooCatch = threadType.then(() -> d(tag, "Foo exec"));
            IAltFuture<Object, Object> secondCatch = threadType.then(() -> d(tag, "Second catch exec"));
            IAltFuture<Object, Object> altFuture = threadType.then(new IAction<Object>() { // Not folding to a lambda, want to be clear which case is being tested
                @Override
                public void call() throws Exception {
                    throw new Exception("Ba2");
                }
            });
            altFuture.onError(e -> {
                ar.set("A: 1st onCatch");
                e1Catch.fork();
                assertThat("ba2").isEqualToIgnoringCase(e.getMessage());
                return false;
            })
                    .then(() -> {
                        fooCatch.fork();
                        ar.set("A: Foo");
                    })
                    .onError(e -> {
                        ar.set("A: 2nd onCatch");
                        secondCatch.fork();
                        return false;
                    })
                    .fork();

            awaitDone(altFuture);
            awaitDone(e1Catch);
            awaitDone(fooCatch);
            awaitDone(secondCatch);
            assertThat("A: 2nd onCatch").isEqualToIgnoringCase(ar.get());
            assertThat(altFuture.isDone()).isTrue();
            assertThat(altFuture.isCancelled()).isTrue();
            assertThat(altFuture.isForked()).isTrue();
        });
    }

    @Test
    public void thenIActionRDoesSomething() throws Throwable {
        logMethodStart();
        int expected = 56;
        IAltFuture<?, Integer> test = threadType.then(new IActionR<Object, Integer>() { // Not folding to a lambda, want to be clear which case is being tested
            @Override
            public Integer call() throws Exception {
                return expected;
            }
        })
                .fork();

        assertThat(expected).isEqualTo(awaitDone(test));
    }

    @Test
    public void thenIActionR_returnsValue() throws Throwable {
        logMethodStart();
        Integer expected = 66;
        IAltFuture<?, Integer> test =
                threadType.then(() -> {
                    v(tag, "Do 66");
                    return expected;
                }).fork();

        v(tag, "Wait for 66");
        awaitDone(test);
        v(tag, "Notified 66");
        assertThat(expected).isEqualTo(test.get());
    }

    @Test
    public void thenIActionR_doesNothingThenReturnsValue() throws Throwable {
        logMethodStart();
        Integer expected = 66;
        IAltFuture<?, Integer> test =
                threadType.then(() -> {
                    v(tag, "Do 66");
                    return expected;
                })
                        .then(() -> v(tag, "After 66"))
                        .then(() -> v(tag, "1"))
                        .then(() -> v(tag, "2"))
                        .then(() -> v(tag, "3"))
                        .then(() -> v(tag, "After notify 66"))
                        .fork();

        v(tag, "Wait for 66");
        awaitDone(test);
        v(tag, "Notified 66");
        assertThat(expected).isEqualTo(test.get());
    }

    @Test
    public void thenIActionOneRDoesSomething() throws Throwable {
        logMethodStart();
        String expected = "aabb";
        IAltFuture<String, String> continueAltFuture = threadType.then(() -> v(tag, "Continuing"));
        IAltFuture<?, String> test = threadType.from("aa")
                .then((String s) -> {
                    v(tag, "Merge strings");
                    return s + "bb";
                })
                .then(continueAltFuture)
                .fork();

        awaitDone(continueAltFuture);
        assertThat("aabb").isEqualTo(test.get());
    }

    @Test
    public void thenIActionOneRChainCombination() throws Throwable {
        logMethodStart();
        String expected = "abcd";
        IAltFuture<String, String> test = threadType.from("a")
                .then(s -> s + "b")
                .then(s -> s + "c")
                .then(s -> s + "d")
                .fork();

        assertThat(awaitDone(test)).isEqualTo(expected);
    }

    @Test
    public void atomicValueBasicSetGet() throws Throwable {
        logMethodStart();
        String expected = "abcd";
        ReactiveValue<String> reactiveValue = new ReactiveValue<>(threadType, "empty");
        reactiveValue.set(expected);

        assertThat(reactiveValue.get()).isEqualTo(expected);
    }

    @Test
    public void settableAltFutureFork_Set() throws Throwable {
        logMethodStart();
        String expected = "yes SettableAltFuture was set";
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType);
        altFuture.fork(); // The fork execution will be delayed until and triggered by the .set() below
        altFuture.set(expected);

        assertThat(altFuture.get()).isEqualTo(expected);
    }

    @Test
    public void settableAltFutureSet_Fork_ThenAltFuture() throws Throwable {
        logMethodStart();
        String initialValue = "yes SettableAltFuture was set";
        String expected = "yes SettableAltFuture was set and augmented";
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType);
        IAltFuture<String, String> downchainAltFuture = altFuture.then((s -> s + " and augmented"));
        altFuture.set(initialValue);
        altFuture.fork();

        assertThat(awaitDone(downchainAltFuture)).isEqualTo(expected);
    }

    @Test
    public void settableAltFutureSet_Fork_ThenAltFuture_AltGenerics() throws Throwable {
        logMethodStart();
        String initialValue = "yes SettableAltFuture was set";
        String expected = "yes SettableAltFuture was set and added to";
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType);
        IAltFuture<String, String> downchainAltFuture = altFuture.then(s -> s + " and added to");
        altFuture.fork();
        altFuture.set(initialValue);

        assertThat(awaitDone(altFuture)).isEqualTo(initialValue);
        assertThat(awaitDone(downchainAltFuture)).isEqualTo(expected);
    }

    @Test
    public void settableAltFutureFork_Set_ThenAltFuture() throws Throwable {
        logMethodStart();
        String initialValue = "yes SettableAltFuture was set";
        String expected = "yes SettableAltFuture was set and augmented";
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType);
        IAltFuture<String, String> downchainAltFuture = altFuture.then((s -> s + " and augmented"));
        altFuture.set(initialValue);
        altFuture.fork();

        assertThat(awaitDone(downchainAltFuture)).isEqualTo(expected);
    }

    @Test
    public void execute_and_simple_wait() throws Throwable {
        logMethodStart();
        AtomicBoolean done = new AtomicBoolean(false);

        threadType.execute((IAction) () -> done.set(true));

        long timeout = System.currentTimeMillis() + 10000;
        while (!done.get() && System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
        }
        assertThat(done.get()).isTrue();
    }

    @Test
    public void executeNext_and_simple_wait() throws Throwable {
        logMethodStart();
        AtomicBoolean done = new AtomicBoolean(false);

        threadType.execute((IAction) () -> done.set(true));

        long timeout = System.currentTimeMillis() + 10000;
        while (!done.get() && System.currentTimeMillis() < timeout) {
            Thread.sleep(100);
        }
        assertThat(done.get()).isTrue();
    }

    @Test
    public <IN> void settableAltFutureFork_Set_ThenAltFuture_AltGenerics() throws Throwable {
        logMethodStart();
        String initialValue = "yes SettableAltFuture was set";
        String expected = "yes SettableAltFuture was set and added to";
        SettableAltFuture<IN, String> altFuture = new SettableAltFuture<>(threadType);
        IAltFuture<String, String> downchainAltFuture = altFuture.then(s -> s + " and added to");
        altFuture.fork();
        altFuture.set(initialValue);

        assertThat(awaitDone(downchainAltFuture)).isEqualTo(expected);
    }

    @Test
    public void immutableValue_NotYetSet() throws Throwable {
        logMethodStart();
        String expected = "yes ImmutableValue was set";
        ImmutableValue<String> immutableValue = new ImmutableValue<>();
        assertThat(immutableValue.isSet()).isFalse();
    }

    @Test
    public void immutableValue_Set() throws Throwable {
        logMethodStart();
        String expected = "yes ImmutableValue was set";
        ImmutableValue<String> immutableValue = new ImmutableValue<>();
        immutableValue.set(expected);
        assertThat(immutableValue.get()).isEqualTo(expected);
    }

    @Test
    public void immutableValue_SetThenActionPreviouslyAdded() throws Throwable {
        logMethodStart();
        String expected = "yes ImmutableValue was set";
        ImmutableValue<String> immutableValue = new ImmutableValue<>();
        ImmutableValue<Integer> otherImmutableValue = new ImmutableValue<>();
        ImmutableValue<Integer> yetAnotherImmutableValue = new ImmutableValue<>();
        immutableValue.then(s -> otherImmutableValue.set(s.length()));
        immutableValue.then(s -> yetAnotherImmutableValue.set(s.length() + 1));
        immutableValue.set(expected);
        assertThat(otherImmutableValue.get()).isEqualTo(26);
        assertThat(yetAnotherImmutableValue.get()).isEqualTo(27);
    }

    @Test
    public void immutableValue_SetThenActionAddedLater() throws Throwable {
        logMethodStart();
        String expected = "ImmutableValue was set";
        ImmutableValue<String> immutableValue = new ImmutableValue<>();
        ImmutableValue<Integer> otherImmutableValue = new ImmutableValue<>();
        ImmutableValue<Integer> yetAnotherImmutableValue = new ImmutableValue<>();
        immutableValue.set(expected);
        immutableValue.then(s -> otherImmutableValue.set(s.length()));
        immutableValue.then(s -> yetAnotherImmutableValue.set(s.length() + 1));
        assertThat(otherImmutableValue.get()).isEqualTo(22);
        assertThat(yetAnotherImmutableValue.get()).isEqualTo(23);
    }

    @Test
    public void immutableValue_BlowsExceptionOnEarlyGet() throws Throwable {
        hideIntentionalErrorStackTraces(() -> {
            logMethodStart();
            ImmutableValue<String> immutableValue = new ImmutableValue<>();
            try {
                immutableValue.get();
                throw new AssertionError("We expected an Exception before this point");
            } catch (IllegalStateException e) {
                // Expected
            }
        });
    }

    @Test
    public void immutableValue_EarlySafeGet() throws Throwable {
        logMethodStart();
        String expected = "yes ImmutableValue was set";
        ImmutableValue<String> immutableValue = new ImmutableValue<>();
        assertThat(immutableValue.safeGet()).isEqualTo(null);
    }

    //@Test TODO messed up test
    public void reactiveBind_Then_functionalFork_Then_Set() throws Throwable {
        logMethodStart();
        String expected = "yes binding set";
        ReactiveValue<String> reactiveValue = new ReactiveValue<>(threadType, "first binding", "not set");
        ReactiveValue<String> secondReactiveValue = new ReactiveValue<>(threadType, "second binding", "not set either");
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType, "AV set");
        ImmutableValue<String> immutableValue = new ImmutableValue<String>().then(altFuture::fork);
        reactiveValue.subscribe(() -> {
            secondReactiveValue.set(expected);
            if (!immutableValue.isSet()) {
                immutableValue.set(expected);
            }
        });
        reactiveValue.set(expected);

        awaitDone(altFuture);
        assertThat(secondReactiveValue.get()).isEqualTo(expected);
    }

    //@Test TODO messed up test
    public void functionalFork_Then_Set_Then_reactiveBind() throws Throwable {
        logMethodStart();
        String expected = "yes binding set";
        ReactiveValue<String> reactiveValue = new ReactiveValue<>(threadType, "first binding", "not set");
        ReactiveValue<String> secondReactiveValue = new ReactiveValue<>(threadType, "second binding", "not set either");
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType);
        altFuture.fork(); // The fork execution will be delayed until and triggered by the .set() below
        reactiveValue.set(expected);
        awaitDone(altFuture);

        reactiveValue.subscribe(threadType, () -> secondReactiveValue.set(expected));
        reactiveValue.subscribe(threadType, (IActionOne<String>) altFuture::set);

        assertThat(secondReactiveValue.get()).isEqualTo(expected);
        assertThat(altFuture.get()).isEqualTo(expected);
    }

    //@Test TODO messed up test
    public void reactiveBind_Then_functionalSet_Then_Fork() throws Throwable {
        logMethodStart();
        String expected = "yes binding set";
        ReactiveValue<String> reactiveValue = new ReactiveValue<>(threadType, "first binding", "not set");
        ReactiveValue<String> secondReactiveValue = new ReactiveValue<>(threadType, "second binding", "not set either");
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType);
        reactiveValue.subscribe(threadType, () -> secondReactiveValue.set(expected));
        reactiveValue.subscribe(threadType, (IActionOne<String>) altFuture::set);
        altFuture.set(expected);
        altFuture.fork();
        awaitDone(altFuture);

        assertThat(secondReactiveValue.get()).isEqualTo(expected);
        assertThat(altFuture.get()).isEqualTo(expected);
    }

    //@Test TODO messed up test
    public void functionalSet_Then_Fork_Then_reactiveBind() throws Throwable {
        logMethodStart();
        String expected = "yes binding set";
        ReactiveValue<String> reactiveValue = new ReactiveValue<>(threadType, "first binding", "not set");
        ReactiveValue<String> secondReactiveValue = new ReactiveValue<>(threadType, "second binding", "not set either");
        SettableAltFuture<?, String> altFuture = new SettableAltFuture<>(threadType, expected);
        altFuture.fork();
        awaitDone(altFuture);

        reactiveValue.subscribe(threadType, () -> secondReactiveValue.set(expected));
        reactiveValue.subscribe(threadType, (String s) -> altFuture.set(s));

        assertThat(secondReactiveValue.get()).isEqualTo(expected);
        assertThat(altFuture.get()).isEqualTo(expected);
    }
}
