/*
 * Copyright 2019, Emanuel Rabina (http://www.ultraq.net.nz/)
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

import groovy.transform.PackageScope
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Inspired by the DOM, an event target is a class that can emit events which
 * can be listened for by the appropriate event listeners.
 *
 * @param <T> The class implementing this trait, eg: {@code class MyClass implements EventTarget<MyClass>}
 * @author Emanuel Rabina
 */
trait EventTarget<T> {

	private static final Logger logger = LoggerFactory.getLogger(EventTarget)

	@Lazy
	private final ExecutorService executorService = { Executors.newSingleThreadExecutor() }()
	private final Queue<Event> eventQueue = new ConcurrentLinkedQueue<>()
	private final List<Tuple2<Class<? extends Event>, EventListener<? extends Event>>> eventListeners = new CopyOnWriteArrayList<>()

	/**
	 * Register an event listener on this event target.
	 *
	 * @param eventClass
	 *   The event type, including any of its subclasses, to listen for.
	 * @param eventListener
	 *   The listener to invoke when the event is fired.
	 * @param removalToken
	 *   If provided, then this token can be used to remove the event listener
	 *   later with a call to its {@link RemovalToken#remove} method.
	 * @return This object so it can be chained.
	 */
	public <E extends Event> T addEventListener(Class<E> eventClass, EventListener<E> eventListener, RemovalToken removalToken = null) {

		eventListeners << new Tuple2<>(eventClass, eventListener)
		removalToken?.setRemovalItems(this, eventClass, eventListener)
		return (T)this
	}

	/**
	 * An alias for {@link #removeEventListener}.
	 */
	public <E extends Event> T off(Class<E> eventClass, EventListener<E> eventListener) {

		return removeEventListener(eventClass, eventListener)
	}

	/**
	 * An alias for {@link #addEventListener}.
	 */
	public <E extends Event> T on(Class<E> eventClass, EventListener<E> eventListener, RemovalToken removalToken = null) {

		return addEventListener(eventClass, eventListener, removalToken)
	}

	/**
	 * Re-fire the specified event, including any subclasses of the event, through
	 * the given event target, effectively forwarding events to another object.
	 *
	 * @param eventClass
	 *   The event type, including any of its subclasses, to re-fire on
	 *   {@code newTarget}.
	 * @param newTarget
	 *   The object ot receive and re-fire the events.
	 * @return This object so it can be chained.
	 */
	public <E extends Event> T relay(Class<E> eventClass, EventTarget newTarget) {

		addEventListener(eventClass) { event ->
			newTarget.trigger(event)
		}
		return (T)this
	}

	/**
	 * Deregister an event listener on this event target.
	 *
	 * @param eventClass
	 * @param eventListener
	 * @return This object so it can be chained.
	 */
	public <E extends Event> T removeEventListener(Class<E> eventClass, EventListener<E> eventListener) {

		eventListeners.removeIf { tuple ->
			return tuple.v1 == eventClass && tuple.v2 == eventListener
		}
		return (T)this
	}

	/**
	 * Fire an event, invoking all listeners registered for that event (including
	 * any listeners registered for the event's parent classes), using the
	 * built-in {@link ExecutorService}.
	 * <p>
	 * Events will be processed in a separate thread, and in a FIFO manner,
	 * ensuring that this method won't block while it waits on event handlers, and
	 * to allow some kind of predictability in the way/order events are processed.
	 * Exceptions that arise from any event listeners will be logged but not
	 * impact other listeners from running.
	 *
	 * @param event
	 * @return This object so it can be chained.
	 */
	public <E extends Event> T trigger(E event) {

		return trigger(event, executorService)
	}

	/**
	 * A {@link #trigger} implementation with a supplied {@code ExecutorService}.
	 *
	 * @param event
	 * @param executorService
	 *   A specific {@code ExecutorService} whose {@code execute} method will be
	 *   used for processing the event.
	 */
	public <E extends Event> T trigger(E event, ExecutorService executorService) {

		eventQueue.add(event)
		executorService.execute { ->
			Thread.currentThread().name = "${this.class.simpleName} event handler"
			var nextEvent = eventQueue.remove()
			eventListeners.each { tuple ->
				def (eventClass, listener) = tuple
				if (eventClass.isInstance(nextEvent)) {
					try {
						listener.handleEvent(nextEvent)
					}
					catch (Exception ex) {
						logger.error('An error occurred while processing {} events on {}', nextEvent.class.simpleName, this.class.simpleName, ex)
					}
				}
			}
		}
		return (T)this
	}
}
