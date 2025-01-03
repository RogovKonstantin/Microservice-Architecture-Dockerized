package com.example.domain;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class EventRepository {

    private static final Logger log = LoggerFactory.getLogger(EventRepository.class);

    public Event findById(Long id) {
        log.debug("findById called with id={}", id);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Event event = session.get(Event.class, id);
            if (event == null) {
                log.warn("Event with id={} not found", id);
            } else {
                log.debug("Event with id={} found: {}", id, event);
            }
            return event;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Event> findAll() {
        log.debug("findAll called");
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Event> events = session.createQuery("from Event").list();
            log.debug("Total events found: {}", events.size());
            return events;
        }
    }

    public void save(Event event) {
        log.debug("save called with event={}", event);
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(event);
            tx.commit();
            log.info("Event saved successfully with id={}", event.getId());
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            log.error("Error while saving event", e);
            throw e;
        }
    }

    public void update(Event event) {
        log.debug("update called with event={}", event);
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.merge(event);
            tx.commit();
            log.info("Event updated successfully with id={}", event.getId());
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            log.error("Error while updating event", e);
            throw e;
        }
    }

    public void delete(Long id) {
        log.debug("delete called with id={}", id);
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Event event = session.get(Event.class, id);
            if (event != null) {
                session.remove(event);
                log.info("Event deleted with id={}", id);
            } else {
                log.warn("Event with id={} not found for deletion", id);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            log.error("Error while deleting event id={}", id, e);
            throw e;
        }
    }
}