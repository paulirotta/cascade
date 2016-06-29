FROM AtomicValue.java

    /**
     * Additional operations are suitable for out-of-order atomic summing operations since they
     * are associative.
     * <p>
     * TODO Support primitive numeric other than Integer through runtime generic reflection
     * TODO Support string accumulation but preserve initial order by creating a ordered prototype concurrent list which values can be inserted into slots in arbitrary order
     *
     * @param value
     * @return
     */
    public OUT add(OUT value) {
        //TODO Support other primitive numeric types through runtime generic reflection
        //TODO More clear error handling if this is not of a supported type for addition

        if (!value.getClass().isInstance(Integer.class)) {
            throw new UnsupportedOperationException("add only works on Integers at the moment");
        }

        final int i = (Integer) value;
        boolean success;
        OUT updatedSum;

        do {
            int sum = (Integer) this.valueAR.get();
            success = compareAndSet(sum, updatedSum = (OUT) new Integer(sum + i));
        } while (!success);

        return updatedSum;
    }

    /**
     * Multiplication operations are suitable for out-of-order atomic summing operations since they
     * are associative.
     * <p>
     *
     * @param value
     * @return
     */
    public OUT multiply(OUT value) {
        final int i = (Integer) value;
        boolean success;
        OUT updatedProduct;

        do {
            int product = (Integer) this.valueAR.get();
            success = compareAndSet(product, updatedProduct = (OUT) new Integer(product * i));
        } while (!success);

        return updatedProduct;
    }


------------------------ FROM SettableAltFuture ----------------------------------------


    /**
     * TODO flush() will guarantee that all actions within the Aspect, regardless of chain, that were fork()ed before this point are completed before proceeding. It is an aspect-wide explicit dependency
     * <p>
     * Proceed only after all operations {@link #fork()}ed before hitting the wall have completed.
     * <p>
     * This is a simple way to guarantee all actions within an {@link Async},
     * not just actions upstream in this chain, have completed before continuing. Depending on circumstance
     * a concurrency wall() may have a significant negative performance impact and should be seen
     * as a special circumstances solution only. You have no knowledge and generally should have no
     * linkage between what this chain is doing and how many and how slow are the actions going on
     * elsewhere in your application. But so much for the ideal world- with real world testing and
     * side effects, you may need it or you may want to test to see if you need it.
     * <p>
     * "Jumping the wall": note that new tasks added to the aspect after the wall was created will queue
     * normally. Since they were forked after the wall, they may execute before the wall completes.
     * The wall completing only means actions forked before the wall was created will complete before
     * it proceeds. If jumping the wall is a real problem, does that mean you actually want a dedicated
     * {@link com.reactivecascade.i.IAspect} such as {@link com.reactivecascade.DefaultAspect} instead?
     *
     * @return
     */
    public AltFuture<T> flush(IAspect aspect, String reason) {
        if (aspect.isInOrderExecutor()) {
            return aspect.then(() -> aspect.d(TAG, "Passed the wall: " + reason));
        }
        throw new UnsupportedOperationException("Need to implement flush()");
        // Probably here we want to proceed only after a snapshot of the queue and N previous tasks where N is the max concurrency of the Aspect gives a list of pending actions which we can proceed after
//        return aspect.subscribeTarget(this, aspect::wall());
    }

    public AltFuture<T> flush(String reason) {
        return wall(aspect, reason);
    }


    /**
     * Execute the onFireAction after this <code>AltFuture</code> finishes.
     *
     * @param aspect
     * @param list
     * @param onFireAction
     * @return
     */
    @Override
    public <P, N> IAltFuture<T, List<N>> then(IAspect aspect, IAltFuture<T, List<P>> list, IActionOneR<T, N> action) {
        assertNotNullThisAltFuture(list);
        assertNotNullThisAction(action);

        return new AltFuture<>(aspect, () -> {
            final ConcurrentIterator<P> iterator = new ConcurrentIterator<>(list.get());
            final List<T> outputList = new ArrayList<>(iterator.size());
            Pair<Integer, P> next;

            //TODO Spawn Aspect.maxConcurrency() - 1 additional tasks to work through the list at the same time
            while ((next = iterator.next()).second != null) {
                outputList.set(next.first, action.call(next.second));
            }

            return outputList;
        });
    }



/**
 * Multiple threads may pull objects until they receive null indicating the iteration is complete
 * <p>
 * TODO ICEBOX, Disabled until list operations are finished. A bit messy, probably a cleaner formulation will be found
 * TODO The source array may not contain null. Use ZEN
 * <p>
 */
    private static class ConcurrentIterator<I> {
        private final Object[] list;
        private final AtomicInteger iAI = new AtomicInteger(0);

        ConcurrentIterator(List<I> list) {
            this.list = list.toArray();
        }

        Pair<Integer, I> next() {
            I value = null;
            final int i = iAI.getAndIncrement();

            if (i < list.length) {
                value = (I) list[i];
            }

            return new Pair<>(i, value);
        }

        int size() {
            return list.length;
        }
    }


