package com.futurice.cascade.i;

/**
 * Aspect is combination of thread/typed-group-of-threads AND the program state during which a
 * given piece of asychronous function result or side-effect is desired.
 */
public interface IAspect extends IThreadType, IBindingContext {
}
