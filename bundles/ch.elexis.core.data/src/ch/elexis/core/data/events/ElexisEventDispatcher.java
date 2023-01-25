/*******************************************************************************
 * Copyright (c) 2009-2018, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 	  MEDEVIT <office@medevit.at> - several major changes
 *******************************************************************************/

package ch.elexis.core.data.events;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.ListenerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.common.ElexisEventTopics;
import ch.elexis.core.data.interfaces.IPersistentObject;
import ch.elexis.core.data.server.ServerEventMapper;
import ch.elexis.core.data.service.ContextServiceHolder;
import ch.elexis.core.data.util.NoPoUtil;
import ch.elexis.core.jdt.Nullable;
import ch.elexis.core.model.IContact;
import ch.elexis.core.model.ICoverage;
import ch.elexis.core.model.IDocumentLetter;
import ch.elexis.core.model.IEncounter;
import ch.elexis.core.model.IMandator;
import ch.elexis.core.model.IPatient;
import ch.elexis.core.model.IPrescription;
import ch.elexis.core.model.IUser;
import ch.elexis.core.model.Identifiable;
import ch.elexis.core.services.holder.ElexisServerServiceHolder;
import ch.elexis.data.Anwender;
import ch.elexis.data.Brief;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Mandant;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Prescription;
import ch.elexis.data.User;

/**
 * The Elexis event dispatcher system manages and distributes the information of
 * changing, creating, deleting and selecting PersistentObjects. An event is
 * fired when such an action occures. This might be due to a user interaction or
 * to an non-interactive job.
 *
 * A view that handles user selection of PersistentObjects MUST fire an
 * appropriate Event through
 * ElexisEventdispatcher.getinstance().fire(ElexisEvent ee) Notification of
 * deletion, modification and creation of PeristentObjects occurs transparently
 * via the PersistentObject base class.
 *
 * A client that wishes to be informed on such events must register an
 * ElexisEventListener. The catchElexisEvent() Method of this listener is called
 * in a non-UI-thread an should be finished as fast as possible. If lengthy
 * operations are neccessary, these must be sheduled in a separate thread, The
 * Listener can specify objects, classes and event types it wants to be
 * informed. If no such filter is given, it will be informed about all events.
 *
 * @since 3.0.0 major changes, switch to {@link ElexisContext}
 * @since 3.4 switched listeners to {@link ListenerList}
 * @since 3.7 move from Eclipse job to ScheduledExecutorService
 * @since 3.8 must explicitly {@link #start()} queue execution
 */
public final class ElexisEventDispatcher implements Runnable {
	private static Logger log = LoggerFactory.getLogger(ElexisEventDispatcher.class);

	private final ListenerList<ElexisEventListener> listeners;
	private static ElexisEventDispatcher theInstance;
	private final Queue<ElexisEvent> eventQueue;
	private final ArrayList<ElexisEvent> eventCopy;
	private transient boolean bStop = false;

	private final ElexisContext elexisUIContext;

	private List<Integer> blockEventTypes;

	private volatile IPerformanceStatisticHandler performanceStatisticHandler;

	private ScheduledExecutorService service;

	public static synchronized ElexisEventDispatcher getInstance() {
		if (theInstance == null) {
			theInstance = new ElexisEventDispatcher();
		}
		return theInstance;
	}

	private ElexisEventDispatcher() {
		listeners = new ListenerList<ElexisEventListener>();
		eventQueue = new ConcurrentLinkedQueue<ElexisEvent>();
		eventCopy = new ArrayList<ElexisEvent>(50);

		elexisUIContext = new ElexisContext();

		service = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * Start the ElexisEventDispatch scheduler; that is, start delivering events
	 *
	 * @since 3.8
	 */
	public void start() {
		service.scheduleAtFixedRate(this, 0, 25, TimeUnit.MILLISECONDS);
	}

	/**
	 * Add listeners for ElexisEvents. The listener tells the system via its
	 * getElexisEventFilter method, what classes it will catch. If a dispatcher for
	 * that class was registered, the call will be routed to that dispatcher.
	 *
	 * @param el one ore more ElexisEventListeners that have to return valid values
	 *           on el.getElexisEventFilter()
	 */
	public void addListeners(final ElexisEventListener... els) {
		for (ElexisEventListener el : els) {
			listeners.add(el);
		}
	}

	/**
	 * remove listeners. If a listener was added before, it will be removed.
	 * Otherwise nothing will happen
	 *
	 * @param el The Listener to remove
	 */
	public void removeListeners(ElexisEventListener... els) {
		for (ElexisEventListener el : els) {
			listeners.remove(el);
		}
	}

	/**
	 * Set a list of {@link ElexisEvent} types that will be skipped when fired. Is
	 * used for example on import when many new objects are created, and we are not
	 * interest in calling the event listeners. Set to null to reset.
	 *
	 * @param blockEventTypes
	 */
	public synchronized void setBlockEventTypes(List<Integer> blockEventTypes) {
		this.blockEventTypes = blockEventTypes;
	}

	/**
	 * Fire an ElexisEvent. The class concerned is named in ee.getObjectClass. If a
	 * dispatcher for that class was registered, the event will be forwarded to that
	 * dispatcher. Otherwise, it will be sent to all registered listeners. The call
	 * to the dispatcher or the listener will always be in a separate thread and not
	 * in the UI thread.So care has to be taken if the callee has to change the UI
	 * Note: Only one Event is dispatched at a given time. If more events arrive,
	 * they will be pushed into a FIFO-Queue. If more than one equivalent event is
	 * pushed into the queue, only the last entered will be dispatched.
	 *
	 * @param ee the event to fire.
	 */
	public void fire(final ElexisEvent... ees) {
		for (ElexisEvent ee : ees) {
//			Throwable trace = new Throwable().fillInStackTrace();
//			log.info("TRACE ", trace);
			if (blockEventTypes != null && blockEventTypes.contains(ee.getType())) {
				continue;
			}
			// Those are single events
			if (ee.getPriority() == ElexisEvent.PRIORITY_SYNC && ee.getType() != ElexisEvent.EVENT_SELECTED) {
				doDispatch(ee);
				continue;
			}

			if (ee.getType() == ElexisEvent.EVENT_USER_CHANGED) {
				elexisUIContext.setSelection(ee.getObjectClass(), ee.getObject());
				doDispatch(ee);
				return;
			}

			transalteAndPostOsgiEvent(ee.getType(), ee.getObject() != null ? ee.getObject() : ee.getGenericObject(),
					ee.getObjectClass());

			int eventType = ee.getType();
			if (eventType == ElexisEvent.EVENT_SELECTED || eventType == ElexisEvent.EVENT_DESELECTED) {

				List<ElexisEvent> eventsToThrow = null;
				synchronized (eventQueue) {
					eventsToThrow = elexisUIContext.setSelection(ee.getObjectClass(),
							(eventType == ElexisEvent.EVENT_SELECTED) ? ee.getObject() : null);
					for (ElexisEvent elexisEvent : eventsToThrow) {
						removeExisting(elexisEvent);
						eventQueue.offer(elexisEvent);
					}
				}
				continue;
			} else if (eventType == ElexisEvent.EVENT_MANDATOR_CHANGED) {
				elexisUIContext.setSelection(Mandant.class, ee.getObject());
			} else if (eventType == ElexisEvent.EVENT_USER_CHANGED) {
				elexisUIContext.setSelection(Anwender.class, ee.getObject());
			}

			synchronized (eventQueue) {
				eventQueue.offer(ee);
			}

			if (ElexisServerServiceHolder.get() != null && ElexisServerServiceHolder.get().deliversRemoteEvents()) {
				ch.elexis.core.common.ElexisEvent mapEvent = ServerEventMapper.mapEvent(ee);
				if (mapEvent != null) {
					ElexisServerServiceHolder.get().postEvent(mapEvent);
				}
			}
		}
	}

	public void transalteAndPostOsgiEvent(int eventType, Object object, Class<?> clazz) {
		if (object != null) {
			Optional<Class<?>> modelInterface = getCoreModelInterfaceForElexisClass(object.getClass());
			if (modelInterface.isPresent() && object instanceof PersistentObject) {
				Optional<?> identifiable = NoPoUtil.loadAsIdentifiable((PersistentObject) object, modelInterface.get());
				if (identifiable.isPresent()) {
					if (eventType == ElexisEvent.EVENT_CREATE) {
						ContextServiceHolder.get().postEvent(ElexisEventTopics.EVENT_CREATE, identifiable.get());
					} else if (eventType == ElexisEvent.EVENT_UPDATE) {
						ContextServiceHolder.get().postEvent(ElexisEventTopics.EVENT_UPDATE, identifiable.get());
					} else if (eventType == ElexisEvent.EVENT_DELETE) {
						ContextServiceHolder.get().postEvent(ElexisEventTopics.EVENT_DELETE, identifiable.get());
					} else if (eventType == ElexisEvent.EVENT_SELECTED) {
						ContextServiceHolder.get().setTyped(identifiable.get());
					} else if (eventType == ElexisEvent.EVENT_DESELECTED) {
						ContextServiceHolder.get().removeTyped(modelInterface.get());
					} else {
						log.warn("Event typ [" + eventType + "] not mapped for [" + object + "]", new Throwable());
					}
				} else {
					log.warn("Could not load [" + object + "] as [" + modelInterface.get() + "]");
				}
			} else {
				log.warn("Unknown model class for [" + object + "]");
			}
		} else if (clazz != null) {
			if (eventType == ElexisEvent.EVENT_RELOAD) {
				ContextServiceHolder.get().postEvent(ElexisEventTopics.EVENT_RELOAD, clazz);
			} else {
				log.warn("Event typ [" + eventType + "] not mapped for [" + clazz + "]", new Throwable());
			}
		}
	}

	private void removeExisting(ElexisEvent elexisEvent) {
		Iterator<ElexisEvent> queueIter = eventQueue.iterator();
		while (queueIter.hasNext()) {
			ElexisEvent queuedEvent = (ElexisEvent) queueIter.next();
			if (queuedEvent.getType() == elexisEvent.getType()
					&& queuedEvent.getObjectClass() == elexisEvent.getObjectClass()) {
				// remove old selection of same class
				if (elexisEvent.getType() == ElexisEvent.EVENT_SELECTED) {
					queueIter.remove();
				}
			}
		}
	}

	/**
	 * find the last selected object of a given type
	 *
	 * @param template tha class defining the object to find
	 * @return the last object of the given type or null if no such object is
	 *         selected
	 */
	public static IPersistentObject getSelected(final Class<?> template) {
		Optional<Class<?>> ciOpt = getCoreModelInterfaceForElexisClass(template);
		if (ciOpt.isPresent()) {
			Optional<?> selected = Optional.empty();
			if (Anwender.class == template) {
				selected = ContextServiceHolder.get().getActiveUserContact();
			} else {
				selected = ContextServiceHolder.get().getTyped(ciOpt.get());
			}
			if (selected.isPresent() && selected.get() instanceof Identifiable) {
				return NoPoUtil.loadAsPersistentObject((Identifiable) selected.get(), template);
			}
		} else {
			LoggerFactory.getLogger(ElexisEventDispatcher.class)
					.warn("Unknown code model interface for [" + template + "]");
		}
		return null;
	}

	public static Optional<Class<?>> getCoreModelInterfaceForElexisClass(Class<?> elexisClazz) {
		if (elexisClazz == User.class) {
			return Optional.of(IUser.class);
		} else if (elexisClazz == Anwender.class) {
			return Optional.of(IContact.class);
		} else if (elexisClazz == Mandant.class) {
			return Optional.of(IMandator.class);
		} else if (elexisClazz == Patient.class) {
			return Optional.of(IPatient.class);
		} else if (elexisClazz == Konsultation.class) {
			return Optional.of(IEncounter.class);
		} else if (elexisClazz == Fall.class) {
			return Optional.of(ICoverage.class);
		} else if (elexisClazz == Prescription.class) {
			return Optional.of(IPrescription.class);
		} else if (elexisClazz == Brief.class) {
			return Optional.of(IDocumentLetter.class);
		}
		return Optional.empty();
	}

	/**
	 * inform the system that an object has been selected
	 *
	 * @param po the object that is selected now
	 */
	public static void fireSelectionEvent(PersistentObject po) {
		if (po != null) {
			getInstance().fire(new ElexisEvent(po, po.getClass(), ElexisEvent.EVENT_SELECTED));
		}
	}

	/**
	 * inform the system, that several objects have been selected
	 *
	 * @param objects
	 */
	public static void fireSelectionEvents(PersistentObject... objects) {
		if (objects != null) {
			ElexisEvent[] ees = new ElexisEvent[objects.length];
			for (int i = 0; i < objects.length; i++) {
				ees[i] = new ElexisEvent(objects[i], objects[i].getClass(), ElexisEvent.EVENT_SELECTED);
			}
			getInstance().fire(ees);
		}
	}

	/**
	 * inform the system, that no object of the specified type is selected anymore
	 *
	 * @param clazz the class of which selection was removed
	 */
	public static void clearSelection(Class<?> clazz) {
		if (clazz != null) {
			getInstance().fire(new ElexisEvent(null, clazz, ElexisEvent.EVENT_DESELECTED));
		}
	}

	/**
	 * inform the system, that all object of a specified class have to be reloaded
	 * from storage
	 *
	 * @param clazz the clazz whose objects are invalidated
	 */
	public static void reload(Class<?> clazz) {
		if (clazz != null) {
			getInstance().fire(new ElexisEvent(null, clazz, ElexisEvent.EVENT_RELOAD));
		}
	}

	/**
	 * inform the system, that the specified object has changed some values or
	 * properties
	 *
	 * @param po the object that was modified
	 */
	public static void update(PersistentObject po) {
		if (po != null) {
			getInstance().fire(new ElexisEvent(po, po.getClass(), ElexisEvent.EVENT_UPDATE));
		}
	}

	/**
	 * @return the currently selected {@link Patient}
	 */
	public @Nullable static Patient getSelectedPatient() {
		return (Patient) getSelected(Patient.class);
	}

	/**
	 *
	 * @return the currently selected {@link Mandant}
	 * @since 3.1
	 */
	public @Nullable static Mandant getSelectedMandator() {
		return (Mandant) getSelected(Mandant.class);
	}

	/**
	 * Cancel rescheduling of the EventDispatcher. Events already in the event queue
	 * will still be dispatched.
	 */
	public void shutDown() {
		bStop = true;
	}

	@Override
	public void run() {
		// copy all events so doDispatch is outside of synchronization,
		// and event handling can run code in display thread without deadlock
		synchronized (eventQueue) {
			while (!eventQueue.isEmpty()) {
				eventCopy.add(eventQueue.poll());
			}
			eventQueue.notifyAll();
		}

		for (ElexisEvent event : eventCopy) {
			doDispatch(event);
		}
		eventCopy.clear();

		if (bStop) {
			service.shutdown();
		}
	}

	private void doDispatch(final ElexisEvent ee) {
		if (ee != null) {
			log.info("Dispatch Event [" + ee + "]");
			for (ElexisEventListener l : listeners) {
				if (ee.matches(l.getElexisEventFilter())) {
					// handle performance statistics if necessary
					if (performanceStatisticHandler != null) {
						startStatistics(ee, l);
					}

					try {
						l.catchElexisEvent(ee);
					} catch (Exception e) {
						log.error(ee.toString(), e);
						throw e;
					}

					// handle performance statistics if necessary
					if (performanceStatisticHandler != null) {
						endStatistics(ee, l);
					}
				}
			}
		}
	}

	private void endStatistics(ElexisEvent ee, ElexisEventListener l) {
		if (!(l instanceof ElexisEventListenerImpl)) {
			performanceStatisticHandler.endCatchEvent(ee, l);
		}
	}

	private void startStatistics(ElexisEvent ee, ElexisEventListener l) {
		if (l instanceof ElexisEventListenerImpl) {
			((ElexisEventListenerImpl) l).setPerformanceStatisticHandler(performanceStatisticHandler);
		} else {
			performanceStatisticHandler.startCatchEvent(ee, l);
		}
	}

	/**
	 * Let the dispatcher Thread empty the queue. If the queue is empty, this method
	 * returns immediately. Otherwise, the current thread waits until it is empty or
	 * the provided wasit time has expired.
	 *
	 * @param millis The time to wait bevor returning
	 * @return false if waiting was interrupted
	 */
	public boolean waitUntilEventQueueIsEmpty(long millis) {
		synchronized (eventQueue) {
			if (!eventQueue.isEmpty()) {
				try {
					eventQueue.wait(millis);
					return true;
				} catch (InterruptedException e) {
					// janusode
				}
			}
		}
		return false;
	}

	public void dump() {
		StringBuilder sb = new StringBuilder();
		sb.append("ElexisEventDispatcher dump: \n");
		for (ElexisEventListener el : listeners) {
			ElexisEvent filter = el.getElexisEventFilter();
			sb.append(el.getClass().getName()).append(": ");
			if (filter != null && filter.getObjectClass() != null && filter.getObjectClass().getName() != null) {
				sb.append(filter.type).append(" / ").append(filter.getObjectClass().getName());
			}
			sb.append(StringUtils.LF);

		}
		sb.append("\n--------------\n");
		log.debug(sb.toString());
	}

	/**
	 * Method to set a {@link IPerformanceStatisticHandler} implementation. Setting
	 * null, will disable calling the statistic handler.
	 *
	 * @param handler
	 */
	public void setPerformanceStatisticHandler(IPerformanceStatisticHandler handler) {
		this.performanceStatisticHandler = handler;
	}

	/**
	 * Statistics handler interface.
	 *
	 */
	public interface IPerformanceStatisticHandler {
		void startCatchEvent(ElexisEvent ee, ElexisEventListener listener);

		void endCatchEvent(ElexisEvent ee, ElexisEventListener listener);
	}
}
