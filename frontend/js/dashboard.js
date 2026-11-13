import { handleExpiredSession, requireStaff } from './auth.js';
import { t, tp } from './i18n.js';
import {
  buildTable, daysOverdue, element, emptyState, shortId, stageChip, stageLabel, tableRow,
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
  return value === null || value === undefined ? t('kpi.notAvailable') : value.toFixed(1);
}

function ownerText(reference) {
  return shortId(reference) || t('common.unassigned');
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
    kpiTile(t('kpi.total'), summary.total, t('kpi.total.hint')),
    kpiTile(t('kpi.open'), summary.open, t('kpi.open.hint')),
    kpiTile(t('kpi.overdue'), overdue,
      overdue > 0 ? t('kpi.overdue.hintSome') : t('kpi.overdue.hintNone'), overdue > 0 ? 'warn' : 'ok'),
    kpiTile(t('kpi.rejected'), rejected,
      rejected > 0 ? t('kpi.rejected.hintSome') : t('kpi.rejected.hintNone'), rejected > 0 ? 'danger' : 'ok'),
    kpiTile(t('kpi.unassigned'), unassigned,
      unassigned > 0 ? t('kpi.unassigned.hintSome') : t('kpi.unassigned.hintNone'), unassigned > 0 ? 'danger' : 'ok'),
    kpiTile(t('kpi.closed'), summary.closed, t('kpi.closed.hint')),
    kpiTile(t('kpi.closedWithoutCare'), summary.closedWithoutCare, t('kpi.closedWithoutCare.hint')),
    kpiTile(t('kpi.averageDays'), formatDays(summary.averageDaysToClose), t('kpi.averageDays.hint')),
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
    element('div', 'stage-share', t('dashboard.share', { share: share.toFixed(1) })));
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
  return element('span', 'chip chip-warn', tp('dashboard.daysOverdue', days));
}

function renderOverdue(summary) {
  const rows = summary.overdue.map((item) => tableRow([
    { node: caseLink(item.id), className: 'case-id' },
    { text: shortId(item.patient) },
    { node: stageChip(item.stage) },
    { text: ownerText(item.owner) },
    { text: item.dueDate || '-' },
    { node: overdueChip(item.dueDate) },
  ]));
  const container = document.getElementById('overdue');
  document.getElementById('overdue-count').textContent = tp('dashboard.overdueCount', summary.overdue.length);
  container.replaceChildren(rows.length === 0
    ? emptyState(t('dashboard.noOverdue.title'), t('dashboard.noOverdue.detail'))
    : buildTable([t('table.case'), t('table.patient'), t('table.stage'), t('table.owner'), t('table.due'),
      t('table.overdueBy')], rows));
}

function renderRejected(summary) {
  const rows = summary.rejected.map((item) => tableRow([
    { node: caseLink(item.id), className: 'case-id' },
    { text: shortId(item.patient) },
    { text: ownerText(item.owner) },
    { text: item.dueDate || '-' },
  ]));
  document.getElementById('rejected-count').textContent = t('dashboard.rejectedCount', { count: summary.rejected.length });
  document.getElementById('rejected').replaceChildren(rows.length === 0
    ? emptyState(t('dashboard.noRejected.title'), t('dashboard.noRejected.detail'))
    : buildTable([t('table.case'), t('table.patient'), t('table.owner'), t('table.due')], rows));
}

function renderOwnership(summary) {
  const rows = summary.unassigned.map((item) => tableRow([
    { node: caseLink(item.id), className: 'case-id' },
    { text: shortId(item.patient) },
    { node: stageChip(item.stage) },
  ]));
  const assigned = summary.open - summary.unassigned.length;
  document.getElementById('unassigned-count').textContent =
    t('dashboard.unassignedCount', { count: summary.unassigned.length });
  document.getElementById('unassigned').replaceChildren(rows.length === 0
    ? emptyState(t('dashboard.allOwned.title'), tp('dashboard.allOwned.detail', assigned))
    : buildTable([t('table.case'), t('table.patient'), t('table.stage')], rows));
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
      showMessage(t('dashboard.loadError'));
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
    showMessage(t('common.unreachable'));
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
