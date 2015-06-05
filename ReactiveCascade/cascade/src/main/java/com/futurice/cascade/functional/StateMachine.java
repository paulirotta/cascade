package com.futurice.cascade.functional;

import android.support.annotation.NonNull;

import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.i.reactive.IReactiveSource;

import java.util.concurrent.ConcurrentHashMap;

import static com.futurice.cascade.Async.assertTrue;
import static com.futurice.cascade.Async.originAsync;
import static com.futurice.cascade.Async.vv;

/**
 * Thread-safe state machine which performs a specified action on each upstream change of the underlying state
 * <p>
 * Created by Paul Houghton on 3/28/2015.
 */
public class StateMachine<IN> implements INamed {
    private final IThreadType threadType;
    private final IReactiveSource<IN> source;
    private final ConcurrentHashMap<IN, IAction<IN>> states;
    private final String name;
    private final IOnErrorAction onErrorAction;
    private final ImmutableValue<String> origin = originAsync();

    public StateMachine(
            @NonNull final String name,
            @NonNull final IReactiveSource<IN> source,
            @NonNull final IThreadType threadType,
            @NonNull final IAction<IN> defaultAction,
            @NonNull final IOnErrorAction onErrorAction) {
        assertTrue("StateMachine threadType must be single-threaded to ensure algorithmic concsistency of downstream effects", threadType.isInOrderExecutor());

        this.source = source;
        this.threadType = threadType;
        this.name = name;
        this.onErrorAction = onErrorAction;
        this.states = new ConcurrentHashMap<>(stateName.length + 1);

        for (IN key : stateName) {
            states.put(key, null);
        }
        source.subscribe(key -> {
            final State state = states.get(key);

            if (state == null) {
                throw new IllegalArgumentException("State '" + key + "' not found");
            }
            vv(this.origin, "State change action '" + key + "'");
            state.action.call();
        });
    }

    /**
     * The action that should be performed on the next transition to the specified state
     *
     * @param key
     * @param action
     */
    public void addState(
            @NonNull final IN key,
            @NonNull final IAction<IN> action) {
        if (states.replace(key, new State(key, action)) == null) {
            throw new IllegalArgumentException("Unknown state, can not set action for '" + key + "'");
        }
    }

    @Override // INamed
    @NonNull
    public String getName() {
        return this.name;
    }
}
