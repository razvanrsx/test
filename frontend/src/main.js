import './style.css';

const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '');

const state = {
  token: null,
  tokenExpiresAt: null,
  users: [],
  devices: [],
  monitoring: [],
  currentUser: null,
  authView: 'login',
  userSyncAt: null,
  deviceSyncAt: null,
  monitoringDay: null,
  monitoringChartType: 'line'
};

const loginSection = document.getElementById('login-section');
const loginForm = document.getElementById('login-form');
const loginStatus = document.getElementById('login-status');
const sessionSummary = document.getElementById('session-summary');
const registerSection = document.getElementById('register-section');
const registerForm = document.getElementById('register-form');
const registerStatus = document.getElementById('register-status');
const showRegisterButton = document.getElementById('show-register');
const showLoginButton = document.getElementById('show-login');
const userSyncStatus = document.getElementById('user-sync-status');
const usersTable = document.getElementById('users-table');
const userForm = document.getElementById('user-form');
const userFormTitle = document.getElementById('user-form-title');
const userStatus = document.getElementById('user-status');
const userCancelButton = document.getElementById('user-cancel');
const usersSection = document.getElementById('users-section');
const usersActionsHeader = document.getElementById('users-actions-header');
const refreshUsersButton = document.getElementById('refresh-users');
const devicesSection = document.getElementById('devices-section');
const devicesTable = document.getElementById('devices-table');
const deviceForm = document.getElementById('device-form');
const deviceFormTitle = document.getElementById('device-form-title');
const deviceStatus = document.getElementById('device-status-message');
const deviceCancelButton = document.getElementById('device-cancel');
const devicesActionsHeader = document.getElementById('devices-actions-header');
const refreshDevicesButton = document.getElementById('refresh-devices');
const deviceUserSelect = document.getElementById('device-user');
const deviceSyncStatus = document.getElementById('device-sync-status');
const monitoringSection = document.getElementById('monitoring-section');
const monitoringTable = document.getElementById('monitoring-table');
const monitoringStatus = document.getElementById('monitoring-status');
const monitoringFilter = document.getElementById('monitoring-filter');
const refreshMonitoringButton = document.getElementById('refresh-monitoring');
const monitoringDayInput = document.getElementById('monitoring-day');
const monitoringChart = document.getElementById('monitoring-chart');
const chartLineButton = document.getElementById('chart-line');
const chartBarButton = document.getElementById('chart-bar');
let monitoringPollHandle = null;

function setStatus(element, message, type) {
  if (!element) return;
  element.textContent = message || '';
  element.classList.remove('error', 'success');
  if (type === 'error') {
    element.classList.add('error');
  } else if (type === 'success') {
    element.classList.add('success');
  }
}

function parseJwt(token) {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(payload);
    return JSON.parse(decoded);
  } catch (error) {
    return null;
  }
}

function isAdmin() {
  return Boolean(state.currentUser?.roles?.includes('ADMIN'));
}

function isClient() {
  return Boolean(state.currentUser?.roles?.includes('CLIENT'));
}

function updateSessionSummary() {
  if (!sessionSummary) return;
  if (state.token && state.currentUser?.username) {
    const rolesLabel = state.currentUser.roles?.length
      ? `Role: ${state.currentUser.roles.join(', ')}`
      : 'Role: not set';
    sessionSummary.textContent = `Signed in as ${state.currentUser.username}. ${rolesLabel}.`;
  } else {
    sessionSummary.textContent = 'Not signed in.';
  }
}

function applyRoleVisibility() {
  const signedIn = Boolean(state.token);

  if (usersSection) {
    usersSection.classList.toggle('hidden', !signedIn || !isAdmin());
  }
  if (usersActionsHeader) {
    usersActionsHeader.classList.toggle('hidden', !signedIn || !isAdmin());
  }
  if (userFormTitle) {
    userFormTitle.classList.toggle('hidden', !signedIn || !isAdmin());
  }
  if (userForm) {
    userForm.classList.toggle('hidden', !signedIn || !isAdmin());
  }

  if (devicesSection) {
    devicesSection.classList.toggle('hidden', !signedIn);
  }
  if (deviceFormTitle) {
    deviceFormTitle.classList.toggle('hidden', !signedIn || !isAdmin());
  }
  if (deviceForm) {
    deviceForm.classList.toggle('hidden', !signedIn || !isAdmin());
  }
  if (devicesActionsHeader) {
    devicesActionsHeader.classList.toggle('hidden', !signedIn || !isAdmin());
  }

  if (monitoringSection) {
    monitoringSection.classList.toggle('hidden', !signedIn);
  }
}

function updateAuthVisibility() {
  const signedIn = Boolean(state.token);
  if (signedIn) {
    loginSection?.classList.add('hidden');
    registerSection?.classList.add('hidden');
    return;
  }

  const showingRegister = state.authView === 'register';
  loginSection?.classList.toggle('hidden', showingRegister);
  registerSection?.classList.toggle('hidden', !showingRegister);
}

function setCurrentUserFromToken(token) {
  if (!token) {
    if (monitoringPollHandle) {
      clearInterval(monitoringPollHandle);
      monitoringPollHandle = null;
    }
    state.currentUser = null;
    state.users = [];
    state.devices = [];
    state.monitoring = [];
    state.userSyncAt = null;
    state.deviceSyncAt = null;
    state.authView = 'login';
    renderUsers();
    renderDevices();
    renderMonitoring();
    renderMonitoringChart();
    updateSessionSummary();
    applyRoleVisibility();
    updateSyncBadges();
    updateAuthVisibility();
    return;
  }

  const payload = parseJwt(token) || {};
  const roles = Array.isArray(payload.roles)
    ? payload.roles
    : payload.roles
      ? [payload.roles]
      : [];

  state.currentUser = {
    username: payload.sub || '',
    roles,
    id: state.currentUser?.id ?? null
  };
  updateSessionSummary();
  applyRoleVisibility();
  updateAuthVisibility();
}

function startMonitoringPoll() {
  if (monitoringPollHandle) {
    clearInterval(monitoringPollHandle);
  }
  monitoringPollHandle = setInterval(() => {
    if (state.token && !monitoringSection?.classList.contains('hidden')) {
      loadMonitoring(monitoringFilter?.value ?? '');
    }
  }, 15000);
}

function syncCurrentUserRecord() {
  if (!state.currentUser?.username || !state.users?.length) {
    return;
  }

  const match = state.users.find(
    (user) => user.username?.toLowerCase() === state.currentUser.username.toLowerCase()
  );
  if (match) {
    state.currentUser.id = match.id;
  }
  updateSessionSummary();
}

async function request(path, { method = 'GET', body, headers = {}, allowNotFound = false } = {}) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...headers
    }
  };

  if (body !== undefined) {
    options.body = typeof body === 'string' ? body : JSON.stringify(body);
  }

  if (state.token) {
    options.headers.Authorization = `Bearer ${state.token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, options);
  if (!response.ok) {
    if (allowNotFound && response.status === 404) {
      return null;
    }
    const message = await safeRead(response);
    throw new Error(message || `Request failed with status ${response.status}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.headers.get('content-type')?.includes('application/json')
    ? response.json()
    : response.text();
}

async function safeRead(response) {
  try {
    const text = await response.text();
    if (!text) {
      return '';
    }
    try {
      const data = JSON.parse(text);
      if (typeof data === 'string') return data;
      if (data?.message) return data.message;
      return text;
    } catch (err) {
      return text;
    }
  } catch (err) {
    return '';
  }
}

showRegisterButton?.addEventListener('click', () => {
  state.authView = 'register';
  setStatus(loginStatus, '');
  setStatus(registerStatus, '');
  updateAuthVisibility();
});

showLoginButton?.addEventListener('click', () => {
  state.authView = 'login';
  setStatus(loginStatus, '');
  setStatus(registerStatus, '');
  updateAuthVisibility();
});

function updateAuthState(token, expiresInSeconds) {
  state.token = token;
  if (expiresInSeconds) {
    state.tokenExpiresAt = Date.now() + expiresInSeconds * 1000;
  } else {
    state.tokenExpiresAt = null;
  }
  setCurrentUserFromToken(token);
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const formData = new FormData(loginForm);
  const username = formData.get('username');
  const password = formData.get('password');
  setStatus(loginStatus, 'Signing in…');
  try {
    const result = await request('/auth/token', {
      method: 'POST',
      body: { username, password }
    });
    updateAuthState(result.token, result.expiresIn);
    setStatus(loginStatus, `Signed in. Token expires in ${Math.round(result.expiresIn / 60)} minutes.`, 'success');
    await loadUsers();
    await loadDevices();
  } catch (error) {
    updateAuthState(null, null);
    setStatus(loginStatus, error.message || 'Login failed', 'error');
  }
});

refreshUsersButton.addEventListener('click', async () => {
  await loadUsers();
});

refreshDevicesButton.addEventListener('click', async () => {
  await loadDevices();
});

refreshMonitoringButton?.addEventListener('click', async () => {
  await loadMonitoring();
});

monitoringFilter?.addEventListener('change', async (event) => {
  await loadMonitoring(event.target.value);
});

monitoringDayInput?.addEventListener('change', () => {
  state.monitoringDay = monitoringDayInput.value || null;
  renderMonitoringChart();
});

function setChartType(type) {
  state.monitoringChartType = type;
  if (chartLineButton) {
    chartLineButton.classList.toggle('active', type === 'line');
  }
  if (chartBarButton) {
    chartBarButton.classList.toggle('active', type === 'bar');
  }
  renderMonitoringChart();
}

chartLineButton?.addEventListener('click', () => setChartType('line'));
chartBarButton?.addEventListener('click', () => setChartType('bar'));

async function loadUsers() {
  if (!state.token) {
    setStatus(userStatus, 'Sign in to load users.', 'error');
    return;
  }
  setStatus(userStatus, 'Loading users…');
  try {
    const users = await request('/users');
    state.users = users;
    syncCurrentUserRecord();
    applyRoleVisibility();
    renderUsers();
    updateDeviceUserOptions();
    renderDevices();
    state.userSyncAt = Date.now();
    updateSyncBadges();
    setStatus(userStatus, `Loaded ${users.length} users.`, 'success');
  } catch (error) {
    setStatus(userStatus, error.message, 'error');
  }
}

async function loadDevices() {
  if (!state.token) {
    setStatus(deviceStatus, 'Sign in to load devices.', 'error');
    return;
  }
  setStatus(deviceStatus, 'Loading devices…');
  try {
    let path = '/devices';
    if (!isAdmin() && state.currentUser?.id) {
      path = `/devices?userId=${encodeURIComponent(state.currentUser.id)}`;
    }

    const devices = await request(path);
    state.devices = !isAdmin() && state.currentUser?.id
      ? devices.filter((device) => device.userId === state.currentUser.id)
      : devices;
    renderDevices();
    updateMonitoringFilterOptions(monitoringFilter?.value);
    if (!monitoringSection?.classList.contains('hidden')) {
      await loadMonitoring(monitoringFilter?.value ?? '');
      startMonitoringPoll();
    }
    state.deviceSyncAt = Date.now();
    updateSyncBadges();
    setStatus(deviceStatus, `Loaded ${devices.length} devices.`, 'success');
  } catch (error) {
    setStatus(deviceStatus, error.message, 'error');
  }
}

async function loadMonitoring(targetDeviceId) {
  if (!state.token) {
    setStatus(monitoringStatus, 'Sign in to view consumption.', 'error');
    return;
  }

  const normalizedTarget = targetDeviceId === undefined || targetDeviceId === null
    ? monitoringFilter?.value ?? ''
    : String(targetDeviceId);
  const deviceIdFilter = normalizedTarget ? Number(normalizedTarget) : null;

  setStatus(monitoringStatus, 'Loading consumption…');
  try {
    const path = deviceIdFilter
      ? `/monitoring/consumption/device/${deviceIdFilter}`
      : '/monitoring/consumption';

    const response = await request(path, { allowNotFound: true });
    const records = Array.isArray(response) ? response : [];

    const allowedDeviceIds = isAdmin()
      ? null
      : new Set(
          state.devices
            .filter((device) => device.userId === state.currentUser?.id)
            .map((device) => device.id)
        );

    state.monitoring = allowedDeviceIds
      ? records.filter((record) => allowedDeviceIds.has(record.deviceId))
      : records;

    if (!state.monitoringDay) {
      const firstRecord = state.monitoring[0];
      if (firstRecord?.hourStart) {
        state.monitoringDay = formatDateInput(new Date(firstRecord.hourStart));
      }
    }
    ensureMonitoringDay();

    renderMonitoring();
    renderMonitoringChart();

    const scopeLabel = deviceIdFilter ? `for device #${deviceIdFilter}` : 'for all devices';
    setStatus(
      monitoringStatus,
      `Loaded ${state.monitoring.length} hourly records ${scopeLabel}.`,
      'success'
    );
  } catch (error) {
    state.monitoring = [];
    renderMonitoring();
    renderMonitoringChart();
    setStatus(monitoringStatus, error.message, 'error');
  }
}

function renderUsers() {
  usersTable.innerHTML = '';
  if (!state.users.length) {
    usersTable.innerHTML = '<tr><td colspan="5">No users found.</td></tr>';
    return;
  }

  for (const user of state.users) {
    const row = document.createElement('tr');
    const actions = isAdmin()
      ? `<div class="actions">
          <button type="button" class="secondary edit-user" data-id="${user.id}">Edit</button>
          <button type="button" class="secondary delete-user" data-id="${user.id}">Delete</button>
        </div>`
      : '<span class="pill">Admin only</span>';
    row.innerHTML = `
      <td>${user.id ?? ''}</td>
      <td>${escapeHtml(user.username)}</td>
      <td>${escapeHtml(user.email)}</td>
      <td>${escapeHtml(user.role)}</td>
      <td>${actions}</td>
    `;
    usersTable.appendChild(row);
  }
}

function renderDevices() {
  devicesTable.innerHTML = '';
  if (!state.devices.length) {
    devicesTable.innerHTML = '<tr><td colspan="7">No devices found.</td></tr>';
    return;
  }

  for (const device of state.devices) {
    const row = document.createElement('tr');
    const owner = state.users.find((user) => user.id === device.userId);
    const ownerLabel = owner ? owner.username : (device.userId ?? '');
    const actions = isAdmin()
      ? `<div class="actions">
          <button type="button" class="secondary edit-device" data-id="${device.id}">Edit</button>
          <button type="button" class="secondary delete-device" data-id="${device.id}">Delete</button>
        </div>`
      : '<span class="pill">Read only</span>';
    row.innerHTML = `
      <td>${device.id ?? ''}</td>
      <td>${escapeHtml(device.name)}</td>
      <td>${escapeHtml(device.type)}</td>
      <td>${escapeHtml(device.status)}</td>
      <td>${device.maxConsumption != null ? escapeHtml(device.maxConsumption) : ''}</td>
      <td>${escapeHtml(ownerLabel)}</td>
      <td>${actions}</td>
    `;
    devicesTable.appendChild(row);
  }
}

function renderMonitoring() {
  monitoringTable.innerHTML = '';
  if (!state.monitoring.length) {
    monitoringTable.innerHTML =
      '<tr><td colspan="4">No consumption records yet. If you just started the stack, wait a few seconds for the simulator to send readings, then click Refresh.</td></tr>';
    return;
  }

  const deviceNames = new Map(state.devices.map((device) => [device.id, device.name]));

  const sorted = [...state.monitoring].sort((a, b) => {
    const aDate = new Date(a.hourStart);
    const bDate = new Date(b.hourStart);
    return bDate.getTime() - aDate.getTime();
  });

  for (const record of sorted) {
    const row = document.createElement('tr');
    const deviceName = deviceNames.get(record.deviceId) || '';
    row.innerHTML = `
      <td>${record.deviceId ?? ''}</td>
      <td>${escapeHtml(deviceName)}</td>
      <td>${escapeHtml(formatHour(record.hourStart))}</td>
      <td>${escapeHtml(record.consumption)}</td>
    `;
    monitoringTable.appendChild(row);
  }
}

function filterMonitoringByDay(dayString) {
  if (!dayString) {
    return [];
  }

  const target = new Date(dayString);
  if (Number.isNaN(target.getTime())) {
    return [];
  }

  return state.monitoring.filter((record) => {
    const current = new Date(record.hourStart);
    return (
      !Number.isNaN(current.getTime()) &&
      current.getFullYear() === target.getFullYear() &&
      current.getMonth() === target.getMonth() &&
      current.getDate() === target.getDate()
    );
  });
}

function renderMonitoringChart() {
  if (!monitoringChart) return;

  monitoringChart.innerHTML = '';
  ensureMonitoringDay();

  const filtered = filterMonitoringByDay(state.monitoringDay);
  if (!filtered.length) {
    const empty = document.createElement('p');
    empty.className = 'empty';
    empty.textContent =
      'No consumption records for the selected day. The simulator may need a short time to publish hourly data.';
    monitoringChart.appendChild(empty);
    return;
  }

  const hourly = new Array(24).fill(0);
  for (const record of filtered) {
    const date = new Date(record.hourStart);
    if (Number.isNaN(date.getTime())) continue;
    const hour = date.getHours();
    hourly[hour] += Number(record.consumption) || 0;
  }

  const maxValue = Math.max(...hourly);
  if (maxValue <= 0) {
    const empty = document.createElement('p');
    empty.className = 'empty';
    empty.textContent = 'No measurable consumption for the selected day.';
    monitoringChart.appendChild(empty);
    return;
  }

  const width = 720;
  const height = 260;
  const padding = 40;
  const innerWidth = width - padding * 2;
  const innerHeight = height - padding * 2;

  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);

  const axis = document.createElementNS('http://www.w3.org/2000/svg', 'line');
  axis.setAttribute('x1', padding);
  axis.setAttribute('y1', height - padding);
  axis.setAttribute('x2', width - padding);
  axis.setAttribute('y2', height - padding);
  axis.setAttribute('stroke', '#cbd2d9');
  axis.setAttribute('stroke-width', '2');
  svg.appendChild(axis);

  const yAxis = document.createElementNS('http://www.w3.org/2000/svg', 'line');
  yAxis.setAttribute('x1', padding);
  yAxis.setAttribute('y1', padding);
  yAxis.setAttribute('x2', padding);
  yAxis.setAttribute('y2', height - padding);
  yAxis.setAttribute('stroke', '#cbd2d9');
  yAxis.setAttribute('stroke-width', '2');
  svg.appendChild(yAxis);

  const step = innerWidth / 24;
  const points = [];
  hourly.forEach((value, hour) => {
    const x = padding + hour * step + step / 2;
    const y = padding + innerHeight - (value / maxValue) * innerHeight;
    points.push(`${x},${y}`);

    if (state.monitoringChartType === 'bar') {
      const bar = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
      const barWidth = step * 0.7;
      const barHeight = (value / maxValue) * innerHeight;
      bar.setAttribute('x', padding + hour * step + (step - barWidth) / 2);
      bar.setAttribute('y', padding + innerHeight - barHeight);
      bar.setAttribute('width', barWidth);
      bar.setAttribute('height', barHeight);
      bar.setAttribute('fill', '#2563eb');
      bar.setAttribute('opacity', '0.8');
      svg.appendChild(bar);
    }
  });

  if (state.monitoringChartType === 'line') {
    const polyline = document.createElementNS('http://www.w3.org/2000/svg', 'polyline');
    polyline.setAttribute('points', points.join(' '));
    polyline.setAttribute('fill', 'none');
    polyline.setAttribute('stroke', '#2563eb');
    polyline.setAttribute('stroke-width', '3');
    svg.appendChild(polyline);
  }

  for (let hour = 0; hour < 24; hour += 6) {
    const x = padding + hour * step + step / 2;
    const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    label.setAttribute('x', x);
    label.setAttribute('y', height - padding + 18);
    label.setAttribute('text-anchor', 'middle');
    label.setAttribute('fill', '#52606d');
    label.setAttribute('font-size', '12');
    label.textContent = `${hour}:00`;
    svg.appendChild(label);
  }

  const title = document.createElementNS('http://www.w3.org/2000/svg', 'text');
  title.setAttribute('x', padding);
  title.setAttribute('y', padding - 12);
  title.setAttribute('fill', '#1f2933');
  title.setAttribute('font-size', '14');
  title.setAttribute('font-weight', '600');
  title.textContent = `Consumption on ${state.monitoringDay} (kWh per hour)`;
  svg.appendChild(title);

  monitoringChart.appendChild(svg);
}

function escapeHtml(value) {
  if (value === null || value === undefined) {
    return '';
  }
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function formatHour(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatDateInput(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return '';
  }
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function ensureMonitoringDay() {
  if (!state.monitoringDay) {
    state.monitoringDay = formatDateInput(new Date());
  }
  if (monitoringDayInput && !monitoringDayInput.value) {
    monitoringDayInput.value = state.monitoringDay;
  }
}

function formatSyncTime(timestamp) {
  if (!timestamp) {
    return 'Not synchronized yet.';
  }
  return `Last synchronized at ${new Date(timestamp).toLocaleString()}`;
}

function updateSyncBadges() {
  if (userSyncStatus) {
    userSyncStatus.textContent = `User Synchronization: ${formatSyncTime(state.userSyncAt)}`;
  }
  if (deviceSyncStatus) {
    deviceSyncStatus.textContent = `Device Synchronization: ${formatSyncTime(state.deviceSyncAt)}`;
  }
}

usersTable.addEventListener('click', async (event) => {
  const target = event.target;
  if (target.matches('.edit-user')) {
    const id = Number(target.dataset.id);
    const user = state.users.find((item) => item.id === id);
    if (user) {
      fillUserForm(user);
    }
  }
  if (target.matches('.delete-user')) {
    const id = Number(target.dataset.id);
    const user = state.users.find((item) => item.id === id);
    if (user && confirm(`Delete user ${user.username}?`)) {
      setStatus(userStatus, 'Deleting user…');
      try {
        await request(`/users/${id}`, { method: 'DELETE' });
        setStatus(userStatus, 'User removed.', 'success');
        await loadUsers();
      } catch (error) {
        setStatus(userStatus, error.message, 'error');
      }
    }
  }
});

devicesTable.addEventListener('click', async (event) => {
  const target = event.target;
  if (target.matches('.edit-device')) {
    const id = Number(target.dataset.id);
    const device = state.devices.find((item) => item.id === id);
    if (device) {
      fillDeviceForm(device);
    }
  }
  if (target.matches('.delete-device')) {
    const id = Number(target.dataset.id);
    const device = state.devices.find((item) => item.id === id);
    if (device && confirm(`Delete device ${device.name}?`)) {
      setStatus(deviceStatus, 'Deleting device…');
      try {
        await request(`/devices/${id}`, { method: 'DELETE' });
        setStatus(deviceStatus, 'Device removed.', 'success');
        await loadDevices();
      } catch (error) {
        setStatus(deviceStatus, error.message, 'error');
      }
    }
  }
});

userForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const id = document.getElementById('user-id').value;
  const username = document.getElementById('user-username').value.trim();
  const password = document.getElementById('user-password').value;
  const email = document.getElementById('user-email').value.trim();
  const role = document.getElementById('user-role').value;

  if (!username || !email || !role) {
    setStatus(userStatus, 'Username, email, and role are required.', 'error');
    return;
  }

  if (!id && !password) {
    setStatus(userStatus, 'Password is required when creating a user.', 'error');
    return;
  }

  const payload = { username, email, role };
  if (password) {
    payload.password = password;
  }
  const method = id ? 'PUT' : 'POST';
  const path = id ? `/users/${id}` : '/users';
  if (id) {
    payload.id = Number(id);
  }

  setStatus(userStatus, id ? 'Updating user…' : 'Creating user…');
  try {
    await request(path, { method, body: payload });
    setStatus(userStatus, id ? 'User updated.' : 'User created.', 'success');
    resetUserForm();
    await loadUsers();
  } catch (error) {
    setStatus(userStatus, error.message, 'error');
  }
});

userCancelButton.addEventListener('click', () => {
  resetUserForm();
  setStatus(userStatus, '');
});

deviceForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const id = document.getElementById('device-id').value;
  const name = document.getElementById('device-name').value.trim();
  const type = document.getElementById('device-type').value.trim();
  const status = document.getElementById('device-status').value;
  const maxConsumptionValue = document.getElementById('device-max-consumption').value;
  const maxConsumption = maxConsumptionValue ? Number(maxConsumptionValue) : null;
  const userIdValue = deviceUserSelect.value;
  const userId = userIdValue ? Number(userIdValue) : null;

  if (!name || !type || !status || maxConsumption === null || Number.isNaN(maxConsumption) || userId === null) {
    setStatus(deviceStatus, 'Name, type, status, max consumption, and user are required.', 'error');
    return;
  }

  const payload = { name, type, status, maxConsumption, userId };
  const method = id ? 'PUT' : 'POST';
  const path = id ? `/devices/${id}` : '/devices';
  if (id) {
    payload.id = Number(id);
  }

  setStatus(deviceStatus, id ? 'Updating device…' : 'Creating device…');
  try {
    await request(path, { method, body: payload });
    setStatus(deviceStatus, id ? 'Device updated.' : 'Device created.', 'success');
    resetDeviceForm();
    await loadDevices();
  } catch (error) {
    setStatus(deviceStatus, error.message, 'error');
  }
});

deviceCancelButton.addEventListener('click', () => {
  resetDeviceForm();
  setStatus(deviceStatus, '');
});

registerForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const username = document.getElementById('register-username').value.trim();
  const email = document.getElementById('register-email').value.trim();
  const password = document.getElementById('register-password').value;

  if (!username || !email || !password) {
    setStatus(registerStatus, 'Username, email, and password are required.', 'error');
    return;
  }

  const payload = { username, email, password, role: 'CLIENT' };
  setStatus(registerStatus, 'Creating account…');
  try {
    await request('/users', { method: 'POST', body: payload });
    setStatus(registerStatus, 'Account created. Signing you in…', 'success');

    const loginResult = await request('/auth/token', {
      method: 'POST',
      body: { username, password }
    });

    updateAuthState(loginResult.token, loginResult.expiresIn);
    setStatus(loginStatus, 'Signed in after registration.', 'success');
    await loadUsers();
    await loadDevices();
  } catch (error) {
    setStatus(registerStatus, error.message || 'Registration failed.', 'error');
  }
});

function fillUserForm(user) {
  document.getElementById('user-id').value = user.id ?? '';
  document.getElementById('user-username').value = user.username ?? '';
  document.getElementById('user-password').value = '';
  document.getElementById('user-email').value = user.email ?? '';
  document.getElementById('user-role').value = user.role ?? '';
  userFormTitle.textContent = 'Edit user';
}

function fillDeviceForm(device) {
  document.getElementById('device-id').value = device.id ?? '';
  document.getElementById('device-name').value = device.name ?? '';
  document.getElementById('device-type').value = device.type ?? '';
  document.getElementById('device-status').value = device.status ?? '';
  document.getElementById('device-max-consumption').value =
    device.maxConsumption != null ? device.maxConsumption : '';
  updateDeviceUserOptions(device.userId ?? '');
  deviceFormTitle.textContent = 'Edit device';
}

function resetUserForm() {
  userForm.reset();
  document.getElementById('user-id').value = '';
  document.getElementById('user-password').value = '';
  userFormTitle.textContent = 'Create user';
}

function resetDeviceForm() {
  deviceForm.reset();
  document.getElementById('device-id').value = '';
  updateDeviceUserOptions('');
  deviceFormTitle.textContent = 'Create device';
}

function updateDeviceUserOptions(selectedId) {
  if (!deviceUserSelect) {
    return;
  }

  const currentValue =
    selectedId !== undefined
      ? selectedId === null
        ? ''
        : String(selectedId)
      : deviceUserSelect.value;

  const options = ['<option value="">Select user</option>'];
  for (const user of state.users) {
    const value = escapeHtml(String(user.id));
    options.push(`<option value="${value}">${escapeHtml(user.username)}</option>`);
  }

  const normalizedValue =
    currentValue === undefined || currentValue === null ? '' : String(currentValue);
  const sanitizedValue = escapeHtml(normalizedValue);

  const hasMatchingUser =
    normalizedValue !== '' && state.users.some((user) => String(user.id) === normalizedValue);

  if (normalizedValue !== '' && !hasMatchingUser) {
    options.push(`<option value="${sanitizedValue}">User #${escapeHtml(normalizedValue)}</option>`);
  }

  deviceUserSelect.innerHTML = options.join('');
  deviceUserSelect.value = sanitizedValue;
}

function updateMonitoringFilterOptions(selectedId) {
  if (!monitoringFilter) {
    return;
  }

  const devicesForFilter = isAdmin()
    ? state.devices
    : state.devices.filter((device) => device.userId === state.currentUser?.id);

  const currentValue =
    selectedId !== undefined && selectedId !== null
      ? String(selectedId)
      : monitoringFilter.value;

  const options = ['<option value="">All devices</option>'];
  for (const device of devicesForFilter) {
    const value = escapeHtml(String(device.id));
    const label = device.name ? `${device.name} (#${device.id})` : `Device #${device.id}`;
    options.push(`<option value="${value}">${escapeHtml(label)}</option>`);
  }

  monitoringFilter.innerHTML = options.join('');
  const sanitizedValue = currentValue ?? '';
  monitoringFilter.value = sanitizedValue;
}

// Initialize session banner and role-based visibility without preloading data.
updateSessionSummary();
applyRoleVisibility();
updateAuthVisibility();
ensureMonitoringDay();
updateSyncBadges();
setChartType(state.monitoringChartType);
