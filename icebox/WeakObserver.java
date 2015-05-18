package com.futurice.android.rx.util;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;

/**
 * If the {@link rx.Observer} is garbage collected, the subscription is canceled
 * <p/>
 * It can optionally unsubscribeSource the subscription on first notification. This default is stay subscribed.
 * <p/>
 * It can optionally unsubscribeSource any previous singleton subscriptions to ensure only one observer
 * of a given class is active at any time to avoid multiple refire issues in transition
 *
 * @param <T>
 */
public class WeakObserver<T> {
    private static final HashMap<Class, WeakObserver> singletonWeakObserverMap = new HashMap<>();

    public static <T> Subscription observe(Observable<T> observable, Observer<T> observer) {
        return observe(observable, observer, false);
    }

    public static <T> Subscription observe(Observable<T> observable, Observer<T> observer, boolean unsubscribeOnError) {
        return doObserve(observable, observer, unsubscribeOnError).subscription;
    }

    private static <T> WeakObserver<T> doObserve(Observable<T> observable, Observer<T> observer, boolean unsubscribeOnError) {
        return new WeakObserver<T>(observable, observer, unsubscribeOnError);
    }

    public static <T> Subscription singletonObserve(Observable<T> observable, Observer<T> observer) {
        return doSingletonObserve(observable, observer, false).subscription;
    }

    public static <T> Subscription singletonObserve(Observable<T> observable, Observer<T> observer, boolean unsubscribeOnError) {
        return doSingletonObserve(observable, observer, unsubscribeOnError).subscription;
    }

    private static synchronized <T> WeakObserver<T> doSingletonObserve(Observable<T> observable, Observer<T> observer, boolean unsubscribeOnError) {
        WeakObserver oldWeakObserver = singletonWeakObserverMap.remove(observable.getClass());

        if (oldWeakObserver != null) {
            oldWeakObserver.subscription.unsubscribe();
            // There exists this brief interval during which neither singletons is subscribed
            // This is intentional to avoid the alternative, which is both singletons are subscribed split respond to events
            // On new subscription, the default is Rx sends the most recent getValue, so this gap is illusory unless you change this behaviour
        }
        WeakObserver newWeakObserver = doObserve(observable, observer, unsubscribeOnError);
        singletonWeakObserverMap.put(observable.getClass(), newWeakObserver);

        return newWeakObserver;
    }

    final Subscription subscription;
    final WeakReference<Observer<T>> weakRefObserver;
    final Class observerClass;

    private WeakObserver(final Observable<T> observable, Observer<T> observer, final boolean unsubscribeOnError) {
        weakRefObserver = new WeakReference<>(observer);
        observerClass = observer.getClass();

        subscription = observable.subscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                subscription.unsubscribe();
                WeakObserver.remove(observerClass, WeakObserver.this);
                Observer obs = dereference();
                if (obs != null) {
                    obs.onCompleted();
                }
            }

            @Override
            public void onError(Exception e) {
                if (unsubscribeOnError) {
                    subscription.unsubscribe();
                    WeakObserver.remove(observerClass, WeakObserver.this);
                }
                Observer obs = dereference();
                if (obs != null) {
                    obs.onError(e);
                }
            }

            @Override
            public void onNext(T t) {
                Observer obs = dereference();
                if (obs != null) {
                    obs.onNext(t);
                }
            }
        });
    }

    private static synchronized void remove(Class c, WeakObserver weakObserver) {
        if (singletonWeakObserverMap.get(c).equals(weakObserver)) {
            singletonWeakObserverMap.remove(c);
        }
    }

    private Observer<T> dereference() {
        Observer<T> observer = weakRefObserver.get();

        if (observer == null) {
            subscription.unsubscribe();
        }

        return observer;
    }
}
