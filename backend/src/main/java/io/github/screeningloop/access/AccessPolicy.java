package io.github.screeningloop.access;

import io.github.screeningloop.cases.CaseStage;

public class AccessPolicy {

    public void requireCanOpen(Actor actor) {
        if (actor.role() != Role.COORDINATOR) {
            throw new ForbiddenActionException("Only a coordinator can open a case");
        }
    }

    public void requireCanAdvance(Actor actor, String currentOwner, CaseStage target) {
        if (actor.role() == Role.COORDINATOR) {
            return;
        }
        if (actor.role() == Role.GUEST) {
            throw new ForbiddenActionException("A guest can only read cases");
        }
        if (target == CaseStage.CLOSED_WITHOUT_CARE) {
            throw new ForbiddenActionException("Only a coordinator can close a case without care");
        }
        if (!actor.practitioner().equals(currentOwner)) {
            throw new ForbiddenActionException("A clinician can only advance the cases they own");
        }
    }
}
