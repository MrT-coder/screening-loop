package io.github.screeningloop.chat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.patient.PatientSummary;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ChatComposer {

    private static final Map<Intent, Set<String>> ALLOWED_PLACEHOLDERS = Map.of(
            Intent.GREETING, Set.of(),
            Intent.HELP, Set.of(),
            Intent.UNSUPPORTED, Set.of(),
            Intent.CASE_STATUS, Set.of("NEXT_STEP_DATE", "STAGE_LABEL"),
            Intent.MEDICATIONS, Set.of("MEDICATION_LIST"),
            Intent.LATEST_EXAMS, Set.of("FIT_DATE", "PROCEDURE_LIST"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final int ISO_DATE_LENGTH = 10;
    private static final String PENDING_DATE = "a date that is still being confirmed";

    private final PlaceholderResolver resolver = new PlaceholderResolver();
    private final Clock clock;

    public ChatComposer(Clock clock) {
        this.clock = clock;
    }

    public String compose(Intent intent, PatientSummary summary) {
        return switch (intent) {
            case GREETING -> ReplyTemplates.GREETING;
            case HELP -> ReplyTemplates.HELP;
            case UNSUPPORTED -> ReplyTemplates.REFUSAL;
            case CASE_STATUS -> caseStatus(summary);
            case MEDICATIONS -> medications(summary);
            case LATEST_EXAMS -> latestExams(summary);
        };
    }

    private String caseStatus(PatientSummary summary) {
        CaseView chosen = pickCase(summary.cases());
        if (chosen == null) {
            return ReplyTemplates.NO_CASE;
        }
        CaseStage stage = CaseStage.fromCode(chosen.stage());
        if (stage.isTerminal()) {
            return render(Intent.CASE_STATUS, ReplyTemplates.forStage(stage), Map.of());
        }
        boolean isOverdue = chosen.dueDate() != null && parseDate(chosen.dueDate()).isBefore(today());
        String template = isOverdue ? ReplyTemplates.OVERDUE : ReplyTemplates.forStage(stage);
        String due = chosen.dueDate() == null ? PENDING_DATE : formatDate(chosen.dueDate());
        return render(Intent.CASE_STATUS, template,
                Map.of("NEXT_STEP_DATE", due, "STAGE_LABEL", stage.code().replace('-', ' ')));
    }

    private CaseView pickCase(List<CaseView> cases) {
        return cases.stream()
                .filter(view -> !CaseStage.fromCode(view.stage()).isTerminal())
                .findFirst()
                .orElse(cases.isEmpty() ? null : cases.get(0));
    }

    private String medications(PatientSummary summary) {
        if (summary.activeMedications().isEmpty()) {
            return ReplyTemplates.NO_MEDICATIONS;
        }
        return render(Intent.MEDICATIONS, ReplyTemplates.MEDICATIONS,
                Map.of("MEDICATION_LIST", String.join(", ", summary.activeMedications())));
    }

    private String latestExams(PatientSummary summary) {
        boolean hasFit = !summary.fitResults().isEmpty();
        boolean hasProcedures = !summary.recentProcedures().isEmpty();
        if (!hasFit && !hasProcedures) {
            return ReplyTemplates.NO_EXAMS;
        }
        StringBuilder reply = new StringBuilder();
        if (hasFit) {
            PatientSummary.FitResult fit = summary.fitResults().get(0);
            reply.append(render(Intent.LATEST_EXAMS, fitTemplate(fit), Map.of("FIT_DATE", formatDate(fit.date()))));
        }
        if (hasProcedures) {
            String names = String.join(", ", summary.recentProcedures().stream()
                    .map(PatientSummary.ProcedureEntry::name).toList());
            reply.append(hasFit ? " " : "")
                    .append(render(Intent.LATEST_EXAMS, ReplyTemplates.PROCEDURES, Map.of("PROCEDURE_LIST", names)));
        }
        return reply.toString();
    }

    private String fitTemplate(PatientSummary.FitResult fit) {
        if ("POS".equals(fit.interpretation())) {
            return ReplyTemplates.FIT_POSITIVE;
        }
        return "NEG".equals(fit.interpretation()) ? ReplyTemplates.FIT_NEGATIVE : ReplyTemplates.FIT_UNKNOWN;
    }

    private String render(Intent intent, String template, Map<String, String> values) {
        return resolver.resolve(template, values, ALLOWED_PLACEHOLDERS.get(intent));
    }

    private String formatDate(String isoDateOrDateTime) {
        return parseDate(isoDateOrDateTime).format(DATE_FORMAT);
    }

    private LocalDate parseDate(String isoDateOrDateTime) {
        return LocalDate.parse(isoDateOrDateTime.substring(0, ISO_DATE_LENGTH));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
