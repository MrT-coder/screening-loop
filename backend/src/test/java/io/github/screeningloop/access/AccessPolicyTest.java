package io.github.screeningloop.access;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.screeningloop.cases.CaseStage;
import org.junit.jupiter.api.Test;

class AccessPolicyTest {

    private static final String OWNER = "Practitioner/gp-1";
    private static final String OTHER = "Practitioner/gp-2";

    private final AccessPolicy policy = new AccessPolicy();
    private final Actor coordinator = new Actor("Practitioner/coord", Role.COORDINATOR);
    private final Actor owner = new Actor(OWNER, Role.CLINICIAN);
    private final Actor guest = new Actor("Practitioner/guest", Role.GUEST);

    @Test
    void onlyACoordinatorCanOpenACase() {
        assertThatCode(() -> policy.requireCanOpen(coordinator)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireCanOpen(owner)).isInstanceOf(ForbiddenActionException.class);
        assertThatThrownBy(() -> policy.requireCanOpen(guest)).isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void aClinicianCanAdvanceOnlyTheCasesTheyOwn() {
        assertThatCode(() -> policy.requireCanAdvance(owner, OWNER, CaseStage.PATIENT_NOTIFIED))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireCanAdvance(owner, OTHER, CaseStage.PATIENT_NOTIFIED))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void aCoordinatorCanAdvanceAnyCase() {
        assertThatCode(() -> policy.requireCanAdvance(coordinator, OTHER, CaseStage.REFERRAL_REQUESTED))
                .doesNotThrowAnyException();
    }

    @Test
    void aGuestCanNeverAdvanceACase() {
        assertThatThrownBy(() -> policy.requireCanAdvance(guest, OWNER, CaseStage.PATIENT_NOTIFIED))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void onlyACoordinatorCanCloseWithoutCare() {
        assertThatCode(() -> policy.requireCanAdvance(coordinator, OWNER, CaseStage.CLOSED_WITHOUT_CARE))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.requireCanAdvance(owner, OWNER, CaseStage.CLOSED_WITHOUT_CARE))
                .isInstanceOf(ForbiddenActionException.class)
                .hasMessageContaining("coordinator");
    }
}
