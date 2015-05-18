/*
The MIT License (MIT)

Copyright (c) 2015 Futurice Oy and individual contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.futurice.cascade.i.assertion;

/**
 * A marker interface to indicate that this is function to be performed probably at some point in the future
 * with possibly a variable number of parameters and possibly a return value.
 *
 * All extensions to this class have a maximum of one function in order to be lambda-friendly. The
 * most common use of <code>IBaseAssertion</code> is to create a functional chain of cascading asynchronous
 * actions in {@link com.futurice.cascade.i.functional.IAltFuture} implementations such as
 * {@link com.futurice.cascade.functional.SettableAltFuture} and {@link com.futurice.cascade.functional.AltFuture}.
 *
 * These assertions are typically tested only in debugOrigin builds as a form of logic validation of the library
 * and to aid developers in finding common errors. Since by the time you get to a production build
 * you have different requirements of running predictably even past runtime errors, this is often
 * less appropriate and may slow your application.
 *
 * Example:
 * <code>
 *     <pre>
 *         ThreadType.UI.subscribe(() -> updateTheScreenWithCurrentNameFromModelAndReturnStringNameForNextStepsInTheFunctionalChain())
 *            .assertTrue((String name) -> {
 *                return name != null; // We blow an Exception that is hopefully clear for troubleshooting and this will end the functional chain from this point (only in a debugOrigin build) if not true
 *            }
 *            .subscribe((String name) -> doSomethingBasedOnNameChange()) // Put the name somewhere else that it needs to be
 *            .onError((String name) -> removeSpinnerAnimationFromTheScreen()) // Clean up the UI, perhaps displaying the bad value so the user can try again. The error that triggers this may be different and less clear in a production build, but in many cases there is still an error it just comes later
 *            .fork(); // Submit for execution as soon as possible. Where is determined by your ThreadType, in this case on the system UI thread
 *     </pre>
 * </code>
 *
 * Example:
 * <code>
 *     <pre>
 *         if (Assert.DEBUG) && injectedAssertionTestLambda.call(value)) { // Do not test in production builds, let Proguard remove this
 *             throw new IllegalStateException("Expected " + expected + " but she be thusly: " + value);
 *         }
 *     </pre>
 * </code>
 *
 */
public interface IBaseAssertion {
}
