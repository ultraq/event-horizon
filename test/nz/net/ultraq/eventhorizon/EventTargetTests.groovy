/*
 * Copyright 2021, Emanuel Rabina (http://www.ultraq.net.nz/)
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

import spock.lang.Retry
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

/**
 * Tests for the event target trait which includes the event registration and
 * triggering mechanisms.
 *
 * @author Emanuel Rabina
 */
class EventTargetTests extends Specification {

	private class TestEventTarget implements EventTarget<TestEventTarget> {}

	private class TestEvent implements Event {}

	private class TestSubclassEvent extends TestEvent {}

	def cleanup() {
		System.gc()
		Thread.sleep(1000)
		assert EventTargetExecutors.active.get() == 0
	}

	def 'Handler invoked for exact event class matches'() {
		given:
			var target = new TestEventTarget()
			var event = new TestEvent()
			var handled = false
			target.on(TestEvent) { _ ->
				handled = true
			}
		when:
			target.trigger(event)
		then:
			new PollingConditions().eventually { ->
				assert handled
			}
	}

	def 'Handler invoked for subclass event matches'() {
		given:
			var target = new TestEventTarget()
			var event = new TestSubclassEvent()
			var handled = false
			target.on(TestEvent) { _ ->
				handled = true
			}
		when:
			target.trigger(event)
		then:
			new PollingConditions().eventually { ->
				assert handled
			}
	}

	def 'Exceptions in handlers do not prevent execution of further handlers'() {
		given:
			var target = new TestEventTarget()
			var event = new TestEvent()
			var handled1 = false
			var handled2 = false
			target.on(TestEvent) { _ ->
				handled1 = true
				throw new Exception()
			}
			target.on(TestEvent) { _ ->
				handled2 = true
			}
		when:
			target.trigger(event)
		then:
			new PollingConditions().eventually { ->
				assert handled1
				assert handled2
			}
			notThrown(Exception)
	}

	def 'Use relay to forward events to other objects'() {
		given:
			var target = new TestEventTarget()
			var newTarget = new TestEventTarget()
			var event = new TestEvent()
			var handled1 = false
			var handled2 = false
			target.on(TestEvent) { _ ->
				handled1 = true
			}
			newTarget.on(TestEvent) { _ ->
				handled2 = true
			}
			target.relay(TestEvent, newTarget)
		when:
			target.trigger(event)
		then:
			new PollingConditions().eventually { ->
				assert handled1
				assert handled2
			}
	}

	def 'Remove an event listener with the off method'() {
		given:
			var target = new TestEventTarget()
			var event = new TestEvent()
			var handled = false
			var listener = { _ ->
				handled = true
			}
			target.on(TestEvent, listener)
		when:
			target.off(TestEvent, listener)
			target.trigger(event)
		then:
			new PollingConditions(delay: 0.5f).eventually { ->
				handled == false
			}
	}

	@Retry
	def 'Remove an event listener with the removal token'() {
		given:
			var target = new TestEventTarget()
			var event = new TestEvent()
			var handled = false
			var listener = { _ ->
				handled = true
			}
			var removalToken = new RemovalToken()
			target.on(TestEvent, removalToken, listener)
		when:
			removalToken.remove()
			target.trigger(event)
		then:
			new PollingConditions(delay: 0.5f).eventually { ->
				handled == false
			}
	}
}
