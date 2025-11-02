import './style.css';

const API_BASE = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '');

const state = {
  token: null,
  tokenExpiresAt: null,
  users: [],
  devices: []
};

const loginForm = document.getElementById('login-form');
const loginStatus = document.getElementById('login-status');
const usersTable = document.getElementById('users-table');
const userForm = document.getElementById('user-form');
const userFormTitle = document.getElementById('user-form-title');
const userStatus = document.getElementById('user-status');
const userCancelButton = document.getElementById('user-cancel');
const refreshUsersButton = document.getElementById('refresh-users');
const devicesTable = document.getElementById('devices-table');
const deviceForm = document.getElementById('device-form');
const deviceFormTitle = document.getElementById('device-form-title');
const deviceStatus = document.getElementById('device-status-message');
const deviceCancelButton = document.getElementById('device-cancel');
const refreshDevicesButton = document.getElementById('refresh-devices');
const deviceUserSelect = document.getElementById('device-user');

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

async function request(path, { method = 'GET', body, headers = {} } = {}) {
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

function updateAuthState(token, expiresInSeconds) {
  state.token = token;
  if (expiresInSeconds) {
    state.tokenExpiresAt = Date.now() + expiresInSeconds * 1000;
  } else {
    state.tokenExpiresAt = null;
  }
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
    await Promise.all([loadUsers(), loadDevices()]);
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

async function loadUsers() {
  setStatus(userStatus, 'Loading users…');
  try {
    const users = await request('/users');
    state.users = users;
    renderUsers();
    updateDeviceUserOptions();
    renderDevices();
    setStatus(userStatus, `Loaded ${users.length} users.`, 'success');
  } catch (error) {
    setStatus(userStatus, error.message, 'error');
  }
}

async function loadDevices() {
  setStatus(deviceStatus, 'Loading devices…');
  try {
    const devices = await request('/devices');
    state.devices = devices;
    renderDevices();
    setStatus(deviceStatus, `Loaded ${devices.length} devices.`, 'success');
  } catch (error) {
    setStatus(deviceStatus, error.message, 'error');
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
    row.innerHTML = `
      <td>${user.id ?? ''}</td>
      <td>${escapeHtml(user.username)}</td>
      <td>${escapeHtml(user.email)}</td>
      <td>${escapeHtml(user.role)}</td>
      <td>
        <div class="actions">
          <button type="button" class="secondary edit-user" data-id="${user.id}">Edit</button>
          <button type="button" class="secondary delete-user" data-id="${user.id}">Delete</button>
        </div>
      </td>
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
    row.innerHTML = `
      <td>${device.id ?? ''}</td>
      <td>${escapeHtml(device.name)}</td>
      <td>${escapeHtml(device.type)}</td>
      <td>${escapeHtml(device.status)}</td>
      <td>${device.maxConsumption != null ? escapeHtml(device.maxConsumption) : ''}</td>
      <td>${escapeHtml(ownerLabel)}</td>
      <td>
        <div class="actions">
          <button type="button" class="secondary edit-device" data-id="${device.id}">Edit</button>
          <button type="button" class="secondary delete-device" data-id="${device.id}">Delete</button>
        </div>
      </td>
    `;
    devicesTable.appendChild(row);
  }
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

// Attempt to load initial data when the page loads.
loadUsers().catch(() => {
  setStatus(userStatus, 'Sign in and click Refresh to load users.', 'error');
});

loadDevices().catch(() => {
  setStatus(deviceStatus, 'Sign in and click Refresh to load devices.', 'error');
});
