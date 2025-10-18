/*
 * Copyright 2025, Emanuel Rabina (http://www.ultraq.net.nz/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.net.ultraq.eventhorizon

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.ImmutableOptions
import groovy.transform.PackageScope
import java.lang.ref.Cleaner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utilities for creating and shutting down executors used by
 * {@link EventTarget}s.
 *
 * @author Emanuel Rabina
 */
class EventTargetExecutors {

	private static final Logger logger = LoggerFactory.getLogger(EventTargetExecutors)
	private static final Cleaner cleaner = Cleaner.create()
	private static final AtomicInteger counter = new AtomicInteger(1)
	@PackageScope
	static final AtomicInteger active = new AtomicInteger(0)

	/**
	 * Class to hold an executor instance for automatic cleanup.
	 */
	@ImmutableOptions(knownImmutables = 'executorService')
	static record ExecutorResource(ExecutorService executorService, int count) {
		public ExecutorResource {
			cleaner.register(this, new ExecutorCleanup(executorService, count))
		}
	}

	/**
	 * Cleanup action for shutting down executors.
	 */
	@ImmutableOptions(knownImmutables = 'executorService')
	private static record ExecutorCleanup(ExecutorService executorService, int count) implements Runnable {
		@Override
		void run() {
			logger.debug('Shutting down EventTarget executor service #{}', count)
			executorService.shutdown()
			active.decrementAndGet()
		}
	}

	/**
	 * Create an executor for an {@link EventTarget} that will be cleaned up on
	 * garbage collection.
	 */
	@PackageScope
	static ExecutorResource createExecutor() {

		var count = counter.getAndIncrement()
		logger.debug('Creating EventTarget executor service #{}', count)

		// The executor is a single thread executor because we want predictability
		// in the event order when received by listeners.  If I want a more
		// performant executor like a work-stealing thread pool, then I'll need to
		// come up with some way to coordinate that ordering in the
		// {@link EventTarget#trigger} method.
		var executorResource = new ExecutorResource(Executors.newSingleThreadExecutor(), count)

		active.incrementAndGet()
		return executorResource
	}
}
