const MS_PER_DAY = 24 * 60 * 60 * 1000;

export const STAGE_LABELS = {
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

export const CLOSURE_REASONS = {
  'patient-declined': 'Patient declined',
  'unreachable': 'Patient unreachable',
  'relocated': 'Patient relocated',
  'care-elsewhere': 'Receiving care elsewhere',
  'other': 'Other',
};

export function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) {
    node.className = className;
  }
  if (text !== undefined) {
    node.textContent = text;
  }
  return node;
}

export function stageLabel(stage) {
  return STAGE_LABELS[stage] || stage;
}

export function plural(count, singular) {
  return `${count} ${singular}${count === 1 ? '' : 's'}`;
}

export function daysOverdue(dueDate) {
  const due = new Date(`${dueDate}T00:00:00`);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.max(0, Math.round((today - due) / MS_PER_DAY));
}

export function stageChip(stage) {
  return element('span', 'chip chip-stage', stageLabel(stage));
}

export function tableRow(cells) {
  const row = element('tr');
  cells.forEach((cell) => {
    const td = element('td', cell.className);
    td.append(cell.node || document.createTextNode(cell.text));
    row.append(td);
  });
  return row;
}

export function buildTable(headings, rows) {
  const table = element('table');
  const head = element('tr');
  headings.forEach((title) => head.append(element('th', '', title)));
  table.append(head, ...rows);
  const wrapper = element('div', 'table-wrap');
  wrapper.append(table);
  return wrapper;
}

export function emptyState(title, detail) {
  const box = element('div', 'empty');
  box.append(element('strong', '', title), element('span', '', detail));
  return box;
}

export function shortId(reference) {
  return reference ? reference.split('/').pop() : '';
}
