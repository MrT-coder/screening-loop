import { handleExpiredSession, requireStaff } from './auth.js';
import { postJson } from './http.js';
import { getLanguage, t, tp } from './i18n.js';
import {
  CLOSURE_REASON_CODES, buildTable, closureReasonLabel, daysOverdue, element, emptyState, formatDate, shortId,
  stageChip, stageLabel, tableRow,
} from './ui.js';

const TERMINAL_STAGES = ['closed', 'closed-without-care'];
const KNOWN_STATUSES = [403, 404, 409];

const message = document.getElementById('message');
const content = document.getElementById('content');
const detail = document.getElementById('detail');
const form = document.getElementById('transition-form');
const formError = document.getElementById('form-error');
const targetSelect = document.getElementById('target');

let user = null;
let openCases = [];
let filter = 'all';
let selectedId = null;
let selectedPatientId = null;

function statusMessage(status) {
  return KNOWN_STATUSES.includes(status) ? t(`cases.error.${status}`) : null;
}

function ownerText(reference) {
  return shortId(reference) || t('common.unassigned');
}

function showMessage(text) {
  message.textContent = text;
  message.hidden = !text;
}

function isOverdue(item) {
  return Boolean(item.dueDate) && daysOverdue(item.dueDate) > 0;
}

function matchesFilter(item) {
  if (filter === 'mine') {
    return item.owner === user.practitioner;
  }
  return filter === 'overdue' ? isOverdue(item) : true;
}

function selectCase(id) {
  const url = new URL(window.location.href);
  url.searchParams.set('case', id);
  window.history.replaceState(null, '', url);
  openDetail(id);
}

function caseLink(id) {
  const link = element('a', 'case-link', id);
  link.href = `?case=${encodeURIComponent(id)}`;
  link.addEventListener('click', (event) => {
    event.preventDefault();
    selectCase(id);
  });
  return link;
}

function renderList() {
  const visible = openCases.filter(matchesFilter);
  document.getElementById('list-count').textContent = tp('unit.case', visible.length);
  const rows = visible.map((item) => {
    const row = tableRow([
      { node: caseLink(item.id), className: 'case-id' },
      { text: shortId(item.patient) },
      { node: stageChip(item.stage) },
      { text: ownerText(item.owner) },
      { text: item.dueDate || '-' },
    ]);
    row.classList.toggle('is-selected', item.id === selectedId);
    return row;
  });
  document.getElementById('case-list').replaceChildren(rows.length === 0
    ? emptyState(t('cases.empty.title'), t('cases.empty.detail'))
    : buildTable([t('table.case'), t('table.patient'), t('table.stage'), t('table.owner'), t('table.due')], rows));
}

function fact(term, value) {
  return [element('dt', '', term), element('dd', '', value)];
}

function renderFacts(summary) {
  const due = summary.dueDate || '-';
  const late = isOverdue(summary) ? ` (${tp('dashboard.daysOverdue', daysOverdue(summary.dueDate))})` : '';
  document.getElementById('detail-facts').replaceChildren(
    ...fact(t('cases.fact.case'), summary.id),
    ...fact(t('cases.fact.patient'), shortId(summary.patient)),
    ...fact(t('cases.fact.fit'), shortId(summary.fitResult)),
    ...fact(t('cases.fact.owner'), ownerText(summary.owner)),
    ...fact(t('cases.fact.due'), due + late));
  document.getElementById('detail-stage').replaceChildren(stageChip(summary.stage));
}

function listBlock(title, items, emptyText) {
  const block = element('div');
  block.append(element('h4', '', title));
  if (items.length === 0) {
    block.append(element('span', 'small', emptyText));
    return block;
  }
  const list = element('ul');
  items.forEach((text) => list.append(element('li', '', text)));
  block.append(list);
  return block;
}

function renderPatient(patient) {
  const years = patient.age === null ? null : t('cases.patient.years', { age: patient.age });
  const demographics = [patient.name, patient.gender, years]
    .filter(Boolean).join(', ');
  const none = t('common.noneRecorded');
  const header = element('div');
  header.append(element('h4', '', t('cases.fact.patient')), element('span', '', demographics || shortId(patient.id)));
  document.getElementById('patient-summary').replaceChildren(
    header,
    listBlock(t('cases.patient.fit'),
      patient.fitResults.map((r) => `${formatDate(r.date)}: ${r.value} ${r.unit} (${r.interpretation})`), none),
    listBlock(t('cases.patient.conditions'), patient.conditions.map((c) => `${c.name} (${c.status})`), none),
    listBlock(t('cases.patient.medications'), patient.activeMedications, none),
    listBlock(t('cases.patient.procedures'),
      patient.recentProcedures.map((p) => `${formatDate(p.date)}: ${p.name}`), none));
}

function renderHistory(history) {
  const items = history.map((entry) => {
    const item = element('li');
    item.append(
      element('strong', '', stageLabel(entry.stage)),
      element('span', 'when', t('cases.historyBy', {
        when: entry.recordedAt.replace('T', ' ').replace('Z', ' UTC'),
        who: entry.agents.map(shortId).join(', '),
      })));
    return item;
  });
  document.getElementById('history').replaceChildren(...(items.length === 0
    ? [element('li', '', t('cases.noHistory'))] : items));
}

function canAdvance(summary) {
  if (TERMINAL_STAGES.includes(summary.stage)) {
    return { allowed: false, note: t('cases.note.closed') };
  }
  if (user.role === 'GUEST') {
    return { allowed: false, note: t('cases.note.guest') };
  }
  if (user.role === 'CLINICIAN' && summary.owner !== user.practitioner) {
    return { allowed: false, note: t('cases.note.owner') };
  }
  return { allowed: true, note: '' };
}

function allowedTargetsFor(targets) {
  return user.role === 'COORDINATOR' ? targets : targets.filter((target) => target !== 'closed-without-care');
}

function option(value, label) {
  const node = element('option', '', label);
  node.value = value;
  return node;
}

function updateFormFields() {
  const target = targetSelect.value;
  const isTerminal = TERMINAL_STAGES.includes(target);
  document.getElementById('due-field').hidden = isTerminal;
  document.getElementById('evidence-field').hidden = target !== 'closed';
  document.getElementById('reason-field').hidden = target !== 'closed-without-care';
}

function prepareForm(summary, targets) {
  const { allowed, note } = canAdvance(summary);
  const choices = allowedTargetsFor(targets);
  const noteElement = document.getElementById('form-note');
  noteElement.textContent = note;
  noteElement.hidden = allowed;
  form.hidden = !allowed || choices.length === 0;
  formError.hidden = true;
  form.reset();
  targetSelect.replaceChildren(...choices.map((target) => option(target, stageLabel(target))));
  document.getElementById('reason').replaceChildren(
    ...CLOSURE_REASON_CODES.map((code) => option(code, closureReasonLabel(code))));
  document.getElementById('owner').value = summary.owner || user.practitioner;
  updateFormFields();
}

async function fetchJson(url) {
  const response = await fetch(url);
  if (handleExpiredSession(response)) {
    throw new Error(t('cases.error.session'));
  }
  if (!response.ok) {
    throw new Error(statusMessage(response.status) || t('cases.error.load'));
  }
  return response.json();
}

async function openDetail(id) {
  selectedId = id;
  renderList();
  showMessage('');
  try {
    const found = await fetchJson(`/api/cases/${encodeURIComponent(id)}`);
    selectedPatientId = shortId(found.summary.patient);
    document.getElementById('chat-log').replaceChildren();
    renderFacts(found.summary);
    renderHistory(found.history);
    prepareForm(found.summary, found.allowedTargets);
    detail.hidden = false;
    document.getElementById('patient-summary').replaceChildren(element('span', 'small', t('cases.loading')));
    renderPatient(await fetchJson(`/api/patients/${shortId(found.summary.patient)}/summary`));
  } catch (error) {
    showMessage(error.message);
  }
}

function addBubble(text, kind) {
  const log = document.getElementById('chat-log');
  log.append(element('div', `bubble ${kind}`, text));
  log.scrollTop = log.scrollHeight;
}

async function askAboutPatient(text) {
  addBubble(text, 'me');
  try {
    const response = await postJson(`/api/patients/${encodeURIComponent(selectedPatientId)}/chat`,
      { text, language: getLanguage() });
    addBubble(response.ok ? (await response.json()).reply : t('cases.chat.failed'), 'bot');
  } catch (error) {
    addBubble(t('common.unreachable'), 'bot');
  }
}

async function problemText(response) {
  const known = statusMessage(response.status);
  if (known && response.status !== 403) {
    return known;
  }
  if (getLanguage() !== 'en') {
    return known || t('cases.error.save');
  }
  try {
    const problem = await response.json();
    return problem.detail || known || t('cases.error.save');
  } catch (error) {
    return known || t('cases.error.save');
  }
}

function buildBody() {
  const data = new FormData(form);
  const target = String(data.get('target'));
  const body = { target, owner: String(data.get('owner')).trim() };
  if (!TERMINAL_STAGES.includes(target)) {
    body.dueDate = String(data.get('dueDate')) || null;
  }
  if (target === 'closed') {
    body.evidence = String(data.get('evidence')).split(',').map((entry) => entry.trim()).filter(Boolean);
  }
  if (target === 'closed-without-care') {
    body.reason = String(data.get('reason'));
  }
  return body;
}

function showFormError(text, field) {
  formError.textContent = text;
  formError.hidden = false;
  (field || formError).focus();
}

function firstFormProblem() {
  const body = buildBody();
  if (!body.owner) {
    return { text: t('cases.error.owner'), field: document.getElementById('owner') };
  }
  if (!TERMINAL_STAGES.includes(body.target) && !body.dueDate) {
    return { text: t('cases.error.dueDate'), field: document.getElementById('due-date') };
  }
  if (body.target === 'closed' && body.evidence.length === 0) {
    return { text: t('cases.error.evidence'), field: document.getElementById('evidence') };
  }
  return null;
}

async function submitTransition(event) {
  event.preventDefault();
  formError.hidden = true;
  const problem = firstFormProblem();
  if (problem) {
    showFormError(problem.text, problem.field);
    return;
  }
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  try {
    const response = await postJson(`/api/cases/${encodeURIComponent(selectedId)}/transitions`, buildBody());
    if (handleExpiredSession(response)) {
      return;
    }
    if (!response.ok) {
      showFormError(await problemText(response));
      return;
    }
    await loadList();
    await openDetail(selectedId);
    showMessage(t('cases.updated'));
  } catch (error) {
    showFormError(t('common.unreachable'));
  } finally {
    button.disabled = false;
  }
}

async function loadList() {
  openCases = await fetchJson('/api/cases');
  renderList();
}

function bindEvents() {
  targetSelect.addEventListener('change', updateFormFields);
  form.addEventListener('submit', submitTransition);
  document.getElementById('chat-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    input.value = '';
    if (text) {
      askAboutPatient(text);
    }
  });
  document.getElementById('chat-chips').addEventListener('click', (event) => {
    const button = event.target.closest('button[data-text]');
    if (button) {
      askAboutPatient(button.dataset.text);
    }
  });
  document.querySelectorAll('.filter').forEach((button) => button.addEventListener('click', () => {
    filter = button.dataset.filter;
    document.querySelectorAll('.filter').forEach((other) => other.classList.toggle('is-active', other === button));
    renderList();
  }));
}

async function start() {
  user = await requireStaff();
  if (!user) {
    return;
  }
  bindEvents();
  try {
    await loadList();
    content.hidden = false;
    const requested = new URLSearchParams(window.location.search).get('case');
    if (requested) {
      await openDetail(requested);
    }
  } catch (error) {
    showMessage(error.message || t('common.unreachable'));
  } finally {
    document.getElementById('footer').hidden = false;
  }
}

start();
