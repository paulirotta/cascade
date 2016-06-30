/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.md , https://github.com/futurice/cascade and http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paulirotta@gmail.com
*/
package com.reactivecascade.i;

/**
 * Aspect is combination of thread/typed-group-of-threads AND the program state during which a
 * given piece of asychronous function result or side-effect is desired.
 */
public interface IAspect extends IThreadType, IBindingContext {
}
