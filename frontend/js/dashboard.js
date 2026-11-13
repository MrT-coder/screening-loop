import { handleExpiredSession, requireStaff } from './auth.js';
import {
  buildTable, daysOverdue, element, emptyState, plural, shortId, stageChip, stageLabel, tableRow,
} from './ui.js';

const CLOSED_STAGES = ['closed', 'closed-without-care'];

const message = document.getElementById('message');
const content = document.getElementById('content');

function caseLink(id) {
  const link = element('a', 'case-link', id);
  link.href = `cases.html?case=${encodeURIComponent(id)}`;
  return link;
}

function formatDays(value) {
  return value === null || value === undefined ? 'n/a' : value.toFixed(1);
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

function overdueChip(dueDate) {
  const days = dueDate ? daysOverdue(dueDate) : 0;
  return element('span', 'chip chip-warn', `${plural(days, 'day')} overdue`);
}

function renderOverdue(summary) {
  const rows = summary.overdue.map((item) => tableRow([
    { node: caseLink(item.id), className: 'case-id' },
    { text: shortId(item.patient) },
    { node: stageChip(item.stage) },
    { text: shortId(item.owner) || 'Unassigned' },
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
    { node: caseLink(item.id), className: 'case-id' },
    { text: shortId(item.patient) },
    { text: shortId(item.owner) || 'Unassigned' },
    { text: item.dueDate || '-' },
  ]));
  document.getElementById('rejected-count').textContent = `${summary.rejected.length} pending`;
  document.getElementById('rejected').replaceChildren(rows.length === 0
    ? emptyState('No rejected referrals', 'No referral is waiting to be requested again.')
    : buildTable(['Case', 'Patient', 'Owner', 'Due'], rows));
}

function renderOwnership(summary) {
  const rows = summary.unassigned.map((item) => tableRow([
    { node: caseLink(item.id), className: 'case-id' },
    { text: shortId(item.patient) },
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

async function load() {
  try {
    const response = await fetch('/api/dashboard');
    if (handleExpiredSession(response)) {
      return;
    }
    if (!response.ok) {
      showMessage('The dashboard could not be loaded. Please reload and try again.');
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
  } finally {
    document.getElementById('footer').hidden = false;
  }
}

async function start() {
  if (await requireStaff()) {
    await load();
  }
}

start();
