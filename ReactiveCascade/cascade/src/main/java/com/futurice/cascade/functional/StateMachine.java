package com.futurice.cascade.functional;

import com.futurice.cascade.i.INamed;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.action.IAction;
import com.futurice.cascade.i.action.IActionOne;
import com.futurice.cascade.i.action.IOnErrorAction;
import com.futurice.cascade.reactive.ReactiveValue;

import java.util.concurrent.ConcurrentHashMap;

import static com.futurice.cascade.Async.*;

/**
 * Created by Paul Houghton on 3/28/2015.
 */
public class StateMachine<KEY> implements INamed {
    public final IThreadType threadType;
    private final ConcurrentHashMap<KEY, State> states;
    public final ReactiveValue<KEY> state;
    private final String name;
    private final IOnErrorAction onErrorAction;
    private final ImmutableValue<String> origin = originAsync();

    public StateMachine(IThreadType threadType, String name, IOnErrorAction onErrorAction, KEY... stateName) {
        if (!threadType.isInOrderExecutor()) {
            throw new IllegalArgumentException("ThreadType for a state machine must be in order executor (single threaded). Consider using Async.SINGLE_THREADED_WORKER.");
        }

        this.threadType = threadType;
        this.name = name;
        this.onErrorAction = onErrorAction;
        this.states = new ConcurrentHashMap<>(stateName.length + 1);

        for (KEY key : stateName) {
            states.put(key, null);
        }
        this.state = new ReactiveValue<>(threadType, name, null, onErrorAction);

        this.state.subscribe((IActionOne<KEY>)key -> {
            final State state = states.get(key);

            if (state == null) {
                throw new IllegalArgumentException("State '" + key + "' not found");
            }
            if (state.action != null) {
                vv(this. origin, "State change action '" + key + "'");
                state.action.call();
            }
        });
    }

    /**
     * The action that should be performed on the next transition to the specified state
     *
     * @param key
     * @param action
     */
    public <IN> void setAction(KEY key, IAction<IN> action) {
        if (states.replace(key, new State(key, action)) == null) {
            throw new IllegalArgumentException("Unknown state, can not set action for '" + key + "'");
        }
    }

    @Override // INamed
    public String getName() {
        return this.name;
    }

    private class State<IN> {
        final KEY key;
        final IAction<IN> action;

        State(KEY key, IAction<IN> action) {
            this.key = key;
            this.action = action;
        }
    }
}
