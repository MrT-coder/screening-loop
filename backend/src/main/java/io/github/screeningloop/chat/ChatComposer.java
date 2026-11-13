package io.github.screeningloop.chat;

import io.github.screeningloop.api.CaseView;
import io.github.screeningloop.cases.CaseStage;
import io.github.screeningloop.patient.PatientSummary;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
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
    private static final int ISO_DATE_LENGTH = 10;

    private final PlaceholderResolver resolver = new PlaceholderResolver();
    private final RewriteValidator validator = new RewriteValidator(resolver);
    private final Clock clock;
    private final ReplyRewriter rewriter;

    public ChatComposer(Clock clock) {
        this(clock, ReplyRewriter.none());
    }

    public ChatComposer(Clock clock, ReplyRewriter rewriter) {
        this.clock = clock;
        this.rewriter = rewriter;
    }

    public String compose(Intent intent, PatientSummary summary) {
        return compose(intent, summary, Language.EN);
    }

    public String compose(Intent intent, PatientSummary summary, Language language) {
        ReplyTemplates templates = ReplyTemplates.of(language);
        return switch (intent) {
            case GREETING -> templates.greeting();
            case HELP -> templates.help();
            case UNSUPPORTED -> templates.refusal();
            case CASE_STATUS -> caseStatus(summary, templates);
            case MEDICATIONS -> medications(summary, templates);
            case LATEST_EXAMS -> latestExams(summary, templates);
        };
    }

    private String caseStatus(PatientSummary summary, ReplyTemplates templates) {
        CaseView chosen = pickCase(summary.cases());
        if (chosen == null) {
            return templates.noCase();
        }
        CaseStage stage = CaseStage.fromCode(chosen.stage());
        if (stage.isTerminal()) {
            return render(Intent.CASE_STATUS, templates.forStage(stage), Map.of(), templates);
        }
        boolean isOverdue = chosen.dueDate() != null && parseDate(chosen.dueDate()).isBefore(today());
        String template = isOverdue ? templates.overdue() : templates.forStage(stage);
        String due = chosen.dueDate() == null ? templates.pendingDate() : formatDate(chosen.dueDate(), templates);
        return render(Intent.CASE_STATUS, template,
                Map.of("NEXT_STEP_DATE", due, "STAGE_LABEL", templates.stageLabel(stage)), templates);
    }

    private CaseView pickCase(List<CaseView> cases) {
        return cases.stream()
                .filter(view -> !CaseStage.fromCode(view.stage()).isTerminal())
                .findFirst()
                .orElse(cases.isEmpty() ? null : cases.get(0));
    }

    private String medications(PatientSummary summary, ReplyTemplates templates) {
        if (summary.activeMedications().isEmpty()) {
            return templates.noMedications();
        }
        return render(Intent.MEDICATIONS, templates.medications(),
                Map.of("MEDICATION_LIST", String.join(", ", summary.activeMedications())), templates);
    }

    private String latestExams(PatientSummary summary, ReplyTemplates templates) {
        boolean hasFit = !summary.fitResults().isEmpty();
        boolean hasProcedures = !summary.recentProcedures().isEmpty();
        if (!hasFit && !hasProcedures) {
            return templates.noExams();
        }
        StringBuilder reply = new StringBuilder();
        if (hasFit) {
            PatientSummary.FitResult fit = summary.fitResults().get(0);
            reply.append(render(Intent.LATEST_EXAMS, fitTemplate(fit, templates),
                    Map.of("FIT_DATE", formatDate(fit.date(), templates)), templates));
        }
        if (hasProcedures) {
            String names = String.join(", ", summary.recentProcedures().stream()
                    .map(PatientSummary.ProcedureEntry::name).toList());
            reply.append(hasFit ? " " : "")
                    .append(render(Intent.LATEST_EXAMS, templates.procedures(),
                            Map.of("PROCEDURE_LIST", names), templates));
        }
        return reply.toString();
    }

    private String fitTemplate(PatientSummary.FitResult fit, ReplyTemplates templates) {
        if ("POS".equals(fit.interpretation())) {
            return templates.fitPositive();
        }
        return "NEG".equals(fit.interpretation()) ? templates.fitNegative() : templates.fitUnknown();
    }

    private String render(Intent intent, String template, Map<String, String> values, ReplyTemplates templates) {
        Set<String> allowed = ALLOWED_PLACEHOLDERS.get(intent);
        String wording = rewriter.rewrite(template, templates.language())
                .filter(candidate -> validator.isValid(template, candidate, allowed, templates.reassurancePhrase()))
                .orElse(template);
        return resolver.resolve(wording, values, allowed);
    }

    private String formatDate(String isoDateOrDateTime, ReplyTemplates templates) {
        return parseDate(isoDateOrDateTime).format(templates.dateFormat());
    }

    private LocalDate parseDate(String isoDateOrDateTime) {
        return LocalDate.parse(isoDateOrDateTime.substring(0, ISO_DATE_LENGTH));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
