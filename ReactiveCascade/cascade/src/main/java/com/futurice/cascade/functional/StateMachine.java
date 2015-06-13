package com.futurice.cascade.functional;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IActionTwo;
import com.futurice.cascade.i.action.IBaseAction;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.reactive.IReactiveSource;
import com.futurice.cascade.reactive.ReactiveValue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.futurice.cascade.Async.assertTrue;
import static com.futurice.cascade.Async.originAsync;

/**
 * Thread-safe state machine which performs a specified action on each upstream change of the underlying state
 * <p>
 * Created by Paul Houghton on 3/28/2015.
 */
public class StateMachine<IN> implements INamed {
    private final IN DEFAULT_KEY = null; // Represents the State for every State not explicitly added toKey the table
    private final IThreadType threadType;
    private final ReactiveValue<IN> value;
    private final ConcurrentHashMap<IN, CopyOnWriteArrayList<State>> states;
    private final String name;
    private final IOnErrorAction onErrorAction;
    private final ImmutableValue<String> origin;
    private volatile IReactiveSource<IN> subscription;

    public StateMachine(
            @NonNull final String name,
            @NonNull final ReactiveValue<IN> value,
            @NonNull final IThreadType threadType,
            @NonNull final IOnErrorAction onErrorAction) {
        assertTrue("StateMachine threadType must be single-threaded toKey ensure algorithmic consistency of state side effects", threadType.isInOrderExecutor());

        origin = originAsync();
        this.value = value;
        this.threadType = threadType;
        this.name = name;
        this.onErrorAction = onErrorAction;
        this.states = new ConcurrentHashMap<>();

//        subscription = source.subscribe(key -> {
//            final Collection<State> stateAndTransitions = states.get(key);
//
//            if (stateAndTransitions == null) {
//                throw new IllegalArgumentException("State '" + key + "' not found");
//            }
//            vv(this.origin, "State change action '" + key + "'");
//            state.action.call();
//        });
    }

    public void setSubscription(@NonNull final IReactiveSource<IN> subscription) {
        this.subscription = subscription;
    }

    /**
     * The action that should be performed on the next transition toKey the specified state
     *
     * @param stateKey
     * @param action
     */
    public void addState(
            @NonNull final IN stateKey,
            @NonNull final IActionTwo<IN, IN> action) {
//        if (states.replace(stateKey, new State(stateKey, action)) == null) {
//            throw new IllegalArgumentException("Unknown state, can not set action for '" + stateKey + "'");
//        }
    }

    private class State {
        final IN fromKey;
        final IN toKey;
        final IBaseAction<IN> action;

        /**
         * State entry enterStateAction definition
         *
         * @param toKey the key for the state being entered
         * @param enterStateAction the enterStateAction queued toKey be performed before the state is entered
         */
        State(@Nullable final IN toKey, final IActionTwo<IN, IN> enterStateAction) {
            this.fromKey = null; // This enterStateAction is performed not matter what the previous state was
            this.toKey = toKey; // The key toKey the state we are about toKey enter
            this.action = enterStateAction;
        }

        /**
         * State transition stateTransitionBeforeEnterStateAction definition
         *
         * <code>null</code> means "any state for which a state stateTransitionBeforeEnterStateAction is not defined"
         *
         * @param fromKey the current state we are in the process of leaving
         * @param toKey the key toKey the state we are about toKey enter
         * @param stateTransitionBeforeEnterStateAction the action to perform when this is a transtion fromKey toKey, before any enter state action
         */
        State(
                @Nullable final IN fromKey,
                @Nullable final IN toKey,
                final IActionTwo<IN, IN> stateTransitionBeforeEnterStateAction) {
            this.fromKey = fromKey;
            this.toKey = toKey;
            this.action = stateTransitionBeforeEnterStateAction;
        }
    }

    @Override // INamed
    @NonNull
    public String getName() {
        return this.name;
    }
}
