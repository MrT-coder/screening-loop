const ACTOR_KEY = 'screening-loop.actor';
const DEFAULT_ACTOR = 'Practitioner/1903';
const MS_PER_DAY = 24 * 60 * 60 * 1000;
const CLOSED_STAGES = ['closed', 'closed-without-care'];

const STAGE_LABELS = {
  'result-issued': 'Result issued',
  'patient-notified': 'Patient notified',
  'acknowledged': 'Acknowledged',
  'confirmation-done': 'Confirmation done',
  'referral-requested': 'Referral requested',
  'referral-rejected': 'Referral rejected',
  'referral-accepted': 'Referral accepted',
  'appointment-booked': 'Appointment booked',
  'treatment-or-surveillance': 'Treatment or surveillance',
  'closed': 'Closed with evidence',
  'closed-without-care': 'Closed without care',
};

const form = document.getElementById('actor-form');
const actorInput = document.getElementById('actor');
const message = document.getElementById('message');
const content = document.getElementById('content');

function readStoredActor() {
  try {
    return localStorage.getItem(ACTOR_KEY) || DEFAULT_ACTOR;
  } catch (error) {
    return DEFAULT_ACTOR;
  }
}

function storeActor(actor) {
  try {
    localStorage.setItem(ACTOR_KEY, actor);
  } catch (error) {
    return;
  }
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text !== undefined) {
    node.textContent = text;
  }
  return node;
}

function stageLabel(stage) {
  return STAGE_LABELS[stage] || stage;
}

function formatDays(value) {
  return value === null || value === undefined ? 'n/a' : value.toFixed(1);
}

function daysOverdue(dueDate) {
  const due = new Date(`${dueDate}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.max(0, Math.round((today - due) / MS_PER_DAY));
}

function plural(count, singular) {
  return `${count} ${singular}${count === 1 ? '' : 's'}`;
}

function kpiTile(label, value, hint, tone) {
  const tile = element('div', tone ? `kpi ${tone}` : 'kpi');
  tile.append(element('div', 'label', label), element('div', 'value', String(value)), element('div', 'hint', hint));
  return tile;
}

function renderKpis(summary) {
  const overdue = summary.overdue.length;
  const rejected = summary.rejected.length;
  const unassigned = summary.unassigned.length;
  const tiles = [
    kpiTile('Total cases', summary.total, 'Positive FIT results tracked'),
    kpiTile('Open', summary.open, 'Still in the pathway'),
    kpiTile('Overdue', overdue, overdue > 0 ? 'Past their due date' : 'Nothing past due', overdue > 0 ? 'warn' : 'ok'),
    kpiTile('Rejected referrals', rejected, rejected > 0 ? 'Need a new referral' : 'None pending',
      rejected > 0 ? 'danger' : 'ok'),
    kpiTile('Unassigned', unassigned, unassigned > 0 ? 'Open cases without owner' : 'Every open case has an owner',
      unassigned > 0 ? 'danger' : 'ok'),
    kpiTile('Closed with evidence', summary.closed, 'Closure backed by a record'),
    kpiTile('Closed without care', summary.closedWithoutCare, 'Recorded human decision'),
    kpiTile('Avg days to close', formatDays(summary.averageDaysToClose), 'Closed cases only'),
  ];
  document.getElementById('kpis').replaceChildren(...tiles);
}

function stageTile(stage, count, index, total, max) {
  const classes = ['stage'];
  if (stage === 'referral-rejected') {
    classes.push('is-rejected');
  }
  if (CLOSED_STAGES.includes(stage)) {
    classes.push('is-closed');
  }
  const tile = element('div', classes.join(' '));
  const top = element('div', 'stage-top');
  top.append(element('span', '', String(index + 1).padStart(2, '0')), element('span', 'stage-count', String(count)));
  const track = element('div', 'stage-track');
  const bar = element('div', 'stage-bar');
  bar.style.width = `${(count / max) * 100}%`;
  track.append(bar);
  const share = total === 0 ? 0 : (count / total) * 100;
  tile.append(top, element('div', 'stage-name', stageLabel(stage)), track,
    element('div', 'stage-share', `${share.toFixed(1)}% of cases`));
  return tile;
}

function renderStages(summary) {
  const counts = Object.entries(summary.byStage);
  const max = Math.max(1, ...counts.map(([, count]) => count));
  document.getElementById('stages').replaceChildren(
    ...counts.map(([stage, count], index) => stageTile(stage, count, index, summary.total, max)));
}

function stageChip(stage) {
  return element('span', 'chip chip-stage', stageLabel(stage));
}

function overdueChip(dueDate) {
  const days = dueDate ? daysOverdue(dueDate) : 0;
  return element('span', 'chip chip-warn', `${plural(days, 'day')} overdue`);
}

function tableRow(cells) {
  const row = element('tr');
  cells.forEach((cell) => {
    const td = element('td', cell.className);
    td.append(cell.node || document.createTextNode(cell.text));
    row.append(td);
  });
  return row;
}

function buildTable(headings, rows) {
  const table = element('table');
  const head = element('tr');
  headings.forEach((title) => head.append(element('th', '', title)));
  table.append(head, ...rows);
  const wrapper = element('div', 'table-wrap');
  wrapper.append(table);
  return wrapper;
}

function emptyState(title, detail) {
  const box = element('div', 'empty');
  box.append(element('strong', '', title), element('span', '', detail));
  return box;
}

function renderOverdue(summary) {
  const rows = summary.overdue.map((item) => tableRow([
    { text: item.id, className: 'case-id' },
    { text: item.patient },
    { node: stageChip(item.stage) },
    { text: item.owner || 'Unassigned' },
    { text: item.dueDate || '-' },
    { node: overdueChip(item.dueDate) },
  ]));
  const container = document.getElementById('overdue');
  document.getElementById('overdue-count').textContent = `${plural(summary.overdue.length, 'case')} need attention`;
  container.replaceChildren(rows.length === 0
    ? emptyState('No overdue cases', 'Every open case is within its due date.')
    : buildTable(['Case', 'Patient', 'Stage', 'Owner', 'Due', 'Overdue by'], rows));
}

function renderRejected(summary) {
  const rows = summary.rejected.map((item) => tableRow([
    { text: item.id, className: 'case-id' },
    { text: item.patient },
    { text: item.owner || 'Unassigned' },
    { text: item.dueDate || '-' },
  ]));
  document.getElementById('rejected-count').textContent = `${summary.rejected.length} pending`;
  document.getElementById('rejected').replaceChildren(rows.length === 0
    ? emptyState('No rejected referrals', 'No referral is waiting to be requested again.')
    : buildTable(['Case', 'Patient', 'Owner', 'Due'], rows));
}

function renderOwnership(summary) {
  const rows = summary.unassigned.map((item) => tableRow([
    { text: item.id, className: 'case-id' },
    { text: item.patient },
    { node: stageChip(item.stage) },
  ]));
  const assigned = summary.open - summary.unassigned.length;
  document.getElementById('unassigned-count').textContent = `${summary.unassigned.length} unassigned`;
  document.getElementById('unassigned').replaceChildren(rows.length === 0
    ? emptyState('Every open case has an owner', `${plural(assigned, 'open case')} with an assigned practitioner.`)
    : buildTable(['Case', 'Patient', 'Stage'], rows));
}

function showMessage(text) {
  message.textContent = text;
  message.hidden = false;
  content.hidden = true;
}

async function load(actor) {
  message.hidden = true;
  try {
    const response = await fetch('/api/dashboard', { headers: { 'X-Actor': actor } });
    if (!response.ok) {
      showMessage(response.status === 400
        ? 'The actor must be an existing Practitioner, for example Practitioner/1903.'
        : `The dashboard could not be loaded (HTTP ${response.status}).`);
      return;
    }
    const summary = await response.json();
    renderKpis(summary);
    renderStages(summary);
    renderOverdue(summary);
    renderRejected(summary);
    renderOwnership(summary);
    content.hidden = false;
  } catch (error) {
    showMessage('The server is not reachable.');
  }
}

form.addEventListener('submit', (event) => {
  event.preventDefault();
  const actor = actorInput.value.trim();
  storeActor(actor);
  load(actor);
});

actorInput.value = readStoredActor();
load(actorInput.value);
