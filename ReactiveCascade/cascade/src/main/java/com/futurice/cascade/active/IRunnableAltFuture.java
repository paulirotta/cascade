/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade.active;

/**
 * Some implementations such as {@link com.futurice.cascade.active.SettableAltFuture} are not themselves
 * runnable in their {@link com.futurice.cascade.i.IThreadType}'s {@link java.util.concurrent.ExecutorService}.
 * <p>
 * Other, such as {@link com.futurice.cascade.active.AltFuture} are, and they are marked with this interface.
 */
public interface IRunnableAltFuture<IN, OUT> extends IAltFuture<IN, OUT>, Runnable {
}
