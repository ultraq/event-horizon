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

import groovy.transform.PackageScope

/**
 * An object that, when provided to the {@link EventTarget#addEventListener}
 * method, can be used to remove the event listener it was paired with, through
 * its {@link #remove} method.
 *
 * @author Emanuel Rabina
 */
class RemovalToken {

	private EventTarget eventTarget
	private Class<? extends Event> event
	private EventListener<? extends Event> listener

	/**
	 * Used internally to set which target, event, and listener to remove.
	 */
	@PackageScope
	<E extends Event> void setRemovalItems(EventTarget eventTarget, Class<E> event, EventListener<E> listener) {

		this.eventTarget = eventTarget
		this.event = event
		this.listener = listener
	}

	/**
	 * Removes the event listener that this token was paired with in the call to
	 * {@link EventTarget#addEventListener}.
	 */
	void remove() {

		eventTarget?.removeEventListener(event, listener)
	}
}
