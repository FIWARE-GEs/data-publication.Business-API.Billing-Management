package org.tmf.dsmapi.settlementNoteAdvice;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.tmf.dsmapi.commons.exceptions.BadUsageException;
import org.tmf.dsmapi.commons.exceptions.UnknownResourceException;
import org.tmf.dsmapi.commons.jaxrs.Report;
import org.tmf.dsmapi.billingAccount.model.SettlementNoteAdvice;
import org.tmf.dsmapi.settlementNoteAdvice.SettlementNoteAdviceFacade;
import org.tmf.dsmapi.settlementNoteAdvice.event.SettlementNoteAdviceEvent;
import org.tmf.dsmapi.settlementNoteAdvice.event.SettlementNoteAdviceEventFacade;
import org.tmf.dsmapi.settlementNoteAdvice.event.SettlementNoteAdviceEventPublisherLocal;

@Stateless
@Path("admin/settlementNoteAdvice")
public class SettlementNoteAdviceAdminResource {

    @EJB
    SettlementNoteAdviceFacade settlementNoteAdviceFacade;
    @EJB
    SettlementNoteAdviceEventFacade eventFacade;
//    @EJB
//    SettlementNoteAdviceEventPublisherLocal publisher;

    @GET
    @Produces({"application/json"})
    public List<SettlementNoteAdvice> findAll() {
        return settlementNoteAdviceFacade.findAll();
    }

    /**
     *
     * For test purpose only
     *
     * @param entities
     * @return
     */
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response post(List<SettlementNoteAdvice> entities, @Context UriInfo info) throws UnknownResourceException {

        if (entities == null) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }

        int previousRows = settlementNoteAdviceFacade.count();
        int affectedRows=0;

        // Try to persist entities
        try {
            for (SettlementNoteAdvice entitie : entities) {
                settlementNoteAdviceFacade.checkCreation(entitie);
                settlementNoteAdviceFacade.create(entitie);
                entitie.setHref(info.getAbsolutePath() + "/" + Long.toString(entitie.getId()));
                settlementNoteAdviceFacade.edit(entitie);
                affectedRows = affectedRows + 1;
//                publisher.createNotification(entitie, new Date());
            }
        } catch (BadUsageException e) {
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode()).build();
        }

        Report stat = new Report(settlementNoteAdviceFacade.count());
        stat.setAffectedRows(affectedRows);
        stat.setPreviousRows(previousRows);

        // 201 OK
        return Response.created(null).
                entity(stat).
                build();
    }

    @PUT
    @Path("{id}")
    @Consumes({"application/json"})
    @Produces({"application/json"})
    public Response update(@PathParam("id") long id, SettlementNoteAdvice entity) throws UnknownResourceException {
        Response response = null;
        SettlementNoteAdvice settlementNoteAdvice = settlementNoteAdviceFacade.find(id);
        if (settlementNoteAdvice != null) {
            entity.setId(id);
            settlementNoteAdviceFacade.edit(entity);
//            publisher.valueChangedNotification(entity, new Date());
            // 200 OK + location
            response = Response.status(Response.Status.OK).entity(entity).build();

        } else {
            // 404 not found
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    /**
     *
     * For test purpose only
     *
     * @return
     * @throws org.tmf.dsmapi.commons.exceptions.UnknownResourceException
     */
    @DELETE
    public Report deleteAll() throws UnknownResourceException {

        eventFacade.removeAll();
        int previousRows = settlementNoteAdviceFacade.count();
        settlementNoteAdviceFacade.removeAll();
        List<SettlementNoteAdvice> pis = settlementNoteAdviceFacade.findAll();
        for (SettlementNoteAdvice pi : pis) {
            delete(pi.getId());
        }

        int currentRows = settlementNoteAdviceFacade.count();
        int affectedRows = previousRows - currentRows;

        Report stat = new Report(currentRows);
        stat.setAffectedRows(affectedRows);
        stat.setPreviousRows(previousRows);

        return stat;
    }

    /**
     *
     * For test purpose only
     *
     * @param id
     * @return
     * @throws UnknownResourceException
     */
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) throws UnknownResourceException {
        int previousRows = settlementNoteAdviceFacade.count();
        SettlementNoteAdvice entity = settlementNoteAdviceFacade.find(id);

        // Event deletion
//        publisher.deletionNotification(entity, new Date());
        try {
            //Pause for 4 seconds to finish notification
            Thread.sleep(4000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SettlementNoteAdviceAdminResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        // remove event(s) binding to the resource
        List<SettlementNoteAdviceEvent> events = eventFacade.findAll();
        for (SettlementNoteAdviceEvent event : events) {
            if (event.getEvent().getId().equals(id)) {
                eventFacade.remove(event.getId());
            }
        }
        //remove resource
        settlementNoteAdviceFacade.remove(id);

        int affectedRows = 1;
        Report stat = new Report(settlementNoteAdviceFacade.count());
        stat.setAffectedRows(affectedRows);
        stat.setPreviousRows(previousRows);

        // 200 
        Response response = Response.ok(stat).build();
        return response;
    }

    @GET
    @Produces({"application/json"})
    @Path("event")
    public List<SettlementNoteAdviceEvent> findAllEvents() {
        return eventFacade.findAll();
    }

    @DELETE
    @Path("event")
    public Report deleteAllEvent() {

        int previousRows = eventFacade.count();
        eventFacade.removeAll();
        int currentRows = eventFacade.count();
        int affectedRows = previousRows - currentRows;

        Report stat = new Report(currentRows);
        stat.setAffectedRows(affectedRows);
        stat.setPreviousRows(previousRows);

        return stat;
    }

    @DELETE
    @Path("event/{id}")
    public Response deleteEvent(@PathParam("id") String id) throws UnknownResourceException {

        int previousRows = eventFacade.count();
        List<SettlementNoteAdviceEvent> events = eventFacade.findAll();
        for (SettlementNoteAdviceEvent event : events) {
            if (event.getEvent().getId().equals(id)) {
                eventFacade.remove(event.getId());

            }
        }
        int currentRows = eventFacade.count();
        int affectedRows = previousRows - currentRows;

        Report stat = new Report(currentRows);
        stat.setAffectedRows(affectedRows);
        stat.setPreviousRows(previousRows);

        // 200 
        Response response = Response.ok(stat).build();
        return response;
    }

    /**
     *
     * @return
     */
    @GET
    @Path("count")
    @Produces({"application/json"})
    public Report count() {
        return new Report(settlementNoteAdviceFacade.count());
    }
}
