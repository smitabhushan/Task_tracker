import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  BarChart3,
  CheckCircle2,
  ClipboardList,
  FolderKanban,
  LayoutDashboard,
  LogOut,
  Plus,
  RefreshCcw,
  Trash2,
  UserPlus,
  Users,
} from 'lucide-react';
import './styles.css';

const configuredApiUrl = import.meta.env.VITE_API_URL;
const API_URL = configuredApiUrl && configuredApiUrl !== 'undefined'
  ? configuredApiUrl.replace(/\/$/, '')
  : '';
const STATUSES = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'BLOCKED', 'DONE'];
const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];
const ROLES = ['ADMIN', 'MANAGER', 'MEMBER'];

function App() {
  const [auth, setAuth] = useState(() => {
    const saved = localStorage.getItem('taskTrackerAuth');
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    if (auth) localStorage.setItem('taskTrackerAuth', JSON.stringify(auth));
    else localStorage.removeItem('taskTrackerAuth');
  }, [auth]);

  if (!auth) {
    return <AuthScreen onAuth={setAuth} />;
  }

  return <Dashboard auth={auth} onLogout={() => setAuth(null)} />;
}

function AuthScreen({ onAuth }) {
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({
    organizationName: 'NxtWave',
    name: 'Admin',
    email: 'admin@nxtwave.com',
    password: 'password123',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const endpoint = mode === 'login' ? '/api/auth/login' : '/api/auth/register';
      const body = mode === 'login'
        ? { email: form.email, password: form.password }
        : form;
      onAuth(await request(endpoint, { method: 'POST', body }));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-panel">
        <div>
          <p className="eyebrow">Team Task Tracker</p>
          <h1>{mode === 'login' ? 'Sign in' : 'Create organization'}</h1>
        </div>

        <div className="segmented">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Login</button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button>
        </div>

        <form onSubmit={submit} className="form-grid">
          {mode === 'register' && (
            <>
              <Field label="Organization" value={form.organizationName} onChange={(organizationName) => setForm({ ...form, organizationName })} />
              <Field label="Name" value={form.name} onChange={(name) => setForm({ ...form, name })} />
            </>
          )}
          <Field label="Email" type="email" value={form.email} onChange={(email) => setForm({ ...form, email })} />
          <Field label="Password" type="password" value={form.password} onChange={(password) => setForm({ ...form, password })} />
          {error && <p className="error">{error}</p>}
          <button className="primary" disabled={loading}>{loading ? 'Working...' : mode === 'login' ? 'Login' : 'Register'}</button>
        </form>
      </section>
    </main>
  );
}

function Dashboard({ auth, onLogout }) {
  const api = useApi(auth.accessToken);
  const isAdmin = auth.user.role === 'ADMIN';
  const canManage = auth.user.role === 'ADMIN' || auth.user.role === 'MANAGER';
  const firstTab = canManage ? 'tickets' : 'tickets';

  const [activeTab, setActiveTab] = useState(firstTab);
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [tasks, setTasks] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [filters, setFilters] = useState({ status: '', priority: '', assignee: '' });
  const [toasts, setToasts] = useState([]);
  const [taskForm, setTaskForm] = useState({
    projectId: '',
    assigneeId: '',
    title: '',
    description: '',
    priority: 'MEDIUM',
    dueDate: '',
  });
  const [userForm, setUserForm] = useState({ name: '', email: '', password: 'password123', role: 'MEMBER' });
  const [projectForm, setProjectForm] = useState({ name: '', description: '' });

  async function loadAll() {
    try {
      const query = new URLSearchParams();
      if (filters.status) query.set('status', filters.status);
      if (filters.priority) query.set('priority', filters.priority);
      if (filters.assignee) query.set('assignee', filters.assignee);
      query.set('page', '0');
      query.set('limit', '50');

      const taskResult = await api(`/api/tasks?${query.toString()}`);
      setTasks(taskResult.content || []);

      if (canManage) {
        const [projectList, userList, summary] = await Promise.all([
          api('/api/projects'),
          api('/api/users'),
          api('/api/analytics/summary'),
        ]);
        setProjects(projectList);
        setUsers(userList);
        setAnalytics(summary);
      }
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  useEffect(() => {
    loadAll();
  }, [filters.status, filters.priority, filters.assignee]);

  async function createUser(event) {
    event.preventDefault();
    await saveAction(() => api('/api/users', { method: 'POST', body: userForm }), 'User created successfully');
    setUserForm({ name: '', email: '', password: 'password123', role: 'MEMBER' });
  }

  async function createProject(event) {
    event.preventDefault();
    await saveAction(() => api('/api/projects', { method: 'POST', body: projectForm }), 'Project created successfully');
    setProjectForm({ name: '', description: '' });
  }

  async function createTask(event) {
    event.preventDefault();
    const body = { ...taskForm, due_date: taskForm.dueDate || null };
    delete body.dueDate;
    await saveAction(() => api('/api/tasks', { method: 'POST', body }), 'Task created successfully');
    setTaskForm({ projectId: '', assigneeId: '', title: '', description: '', priority: 'MEDIUM', dueDate: '' });
  }

  async function changeStatus(task, status) {
    await saveAction(() => api(`/api/tasks/${task.id}/status`, { method: 'PATCH', body: { status } }), `Task moved to ${pretty(status)}`);
  }

  async function deleteTask(task) {
    if (!window.confirm(`Delete task "${task.title}"?`)) return;
    await saveAction(() => api(`/api/tasks/${task.id}`, { method: 'DELETE' }), 'Task deleted successfully');
  }

  async function deleteProject(project) {
    if (!window.confirm(`Delete project "${project.name}" and its tasks?`)) return;
    await saveAction(() => api(`/api/projects/${project.id}`, { method: 'DELETE' }), 'Project deleted successfully');
  }

  async function deleteUser(user) {
    if (!window.confirm(`Delete user "${user.name}"?`)) return;
    await saveAction(() => api(`/api/users/${user.id}`, { method: 'DELETE' }), 'User deleted successfully');
  }

  async function saveAction(action, success) {
    try {
      await action();
      showToast(success, 'success');
      await loadAll();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  function showToast(text, type = 'success') {
    const id = window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`;
    setToasts((current) => [...current, { id, text, type }]);
    window.setTimeout(() => {
      setToasts((current) => current.filter((toast) => toast.id !== id));
    }, 3600);
  }

  const tasksByStatus = useMemo(() => {
    return STATUSES.reduce((acc, status) => {
      acc[status] = tasks.filter((task) => task.status === status);
      return acc;
    }, {});
  }, [tasks]);

  const tabs = [
    { id: 'tickets', label: 'Tickets', icon: <LayoutDashboard size={16} /> },
    ...(canManage ? [{ id: 'task', label: 'Add Task', icon: <ClipboardList size={16} /> }] : []),
    ...(canManage ? [{ id: 'project', label: 'Add Project', icon: <FolderKanban size={16} /> }] : []),
    ...(isAdmin ? [{ id: 'user', label: 'Add User', icon: <UserPlus size={16} /> }] : []),
  ];

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Workspace</p>
          <h1>Team Task Tracker</h1>
        </div>
        <div className="top-actions">
          <span className="identity">{auth.user.name} / {auth.user.role}</span>
          <button className="icon-button" title="Refresh" onClick={loadAll}><RefreshCcw size={18} /></button>
          <button className="icon-button" title="Logout" onClick={onLogout}><LogOut size={18} /></button>
        </div>
      </header>

      <nav className="tabs" aria-label="Main sections">
        {tabs.map((tab) => (
          <button key={tab.id} className={activeTab === tab.id ? 'active' : ''} onClick={() => setActiveTab(tab.id)}>
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </nav>

      <ToastStack toasts={toasts} onDismiss={(id) => setToasts((current) => current.filter((toast) => toast.id !== id))} />

      {activeTab === 'tickets' && (
        <TicketsPage
          auth={auth}
          canManage={canManage}
          isAdmin={isAdmin}
          users={users}
          tasks={tasks}
          tasksByStatus={tasksByStatus}
          analytics={analytics}
          filters={filters}
          setFilters={setFilters}
          changeStatus={changeStatus}
          deleteTask={deleteTask}
        />
      )}

      {activeTab === 'task' && canManage && (
        <TaskPage
          taskForm={taskForm}
          setTaskForm={setTaskForm}
          projects={projects}
          users={users}
          tasks={tasks}
          createTask={createTask}
          deleteTask={deleteTask}
        />
      )}

      {activeTab === 'project' && canManage && (
        <ProjectPage
          projectForm={projectForm}
          setProjectForm={setProjectForm}
          projects={projects}
          createProject={createProject}
          deleteProject={deleteProject}
        />
      )}

      {activeTab === 'user' && isAdmin && (
        <UserPage
          userForm={userForm}
          setUserForm={setUserForm}
          users={users}
          currentUserId={auth.user.id}
          createUser={createUser}
          deleteUser={deleteUser}
        />
      )}
    </main>
  );
}

function TicketsPage({ canManage, isAdmin, users, tasks, tasksByStatus, analytics, filters, setFilters, changeStatus, deleteTask }) {
  return (
    <>
      <section className="toolbar">
        <Select label="Status" value={filters.status} options={['', ...STATUSES]} onChange={(status) => setFilters({ ...filters, status })} />
        <Select label="Priority" value={filters.priority} options={['', ...PRIORITIES]} onChange={(priority) => setFilters({ ...filters, priority })} />
        {canManage && <Select label="Assignee" value={filters.assignee} options={['', ...users.map((u) => String(u.id))]} labels={userLabels(users)} onChange={(assignee) => setFilters({ ...filters, assignee })} />}
      </section>

      <section className="metrics">
        <Metric icon={<ClipboardList />} label="Tasks" value={tasks.length} />
        <Metric icon={<CheckCircle2 />} label="Done" value={tasksByStatus.DONE?.length || 0} />
        {analytics && <Metric icon={<BarChart3 />} label="Avg completion" value={analytics.averageCompletionHours == null ? '-' : `${analytics.averageCompletionHours.toFixed(1)}h`} />}
        {isAdmin && <Metric icon={<Users />} label="Users" value={users.length} />}
      </section>

      <section className="board">
        {STATUSES.map((status) => (
          <div className="column" key={status}>
            <h2>{pretty(status)}</h2>
            {(tasksByStatus[status] || []).map((task) => (
              <TaskCard key={task.id} task={task} changeStatus={changeStatus} deleteTask={deleteTask} />
            ))}
          </div>
        ))}
      </section>
    </>
  );
}

function TaskPage({ taskForm, setTaskForm, projects, users, tasks, createTask, deleteTask }) {
  return (
    <section className="page-grid">
      <Panel title="Add task" icon={<ClipboardList size={18} />}>
        <form onSubmit={createTask} className="form-grid">
          <Select label="Project" value={taskForm.projectId} options={['', ...projects.map((p) => String(p.id))]} labels={projectLabels(projects)} onChange={(projectId) => setTaskForm({ ...taskForm, projectId })} />
          <Select label="Assignee" value={taskForm.assigneeId} options={['', ...users.map((u) => String(u.id))]} labels={userLabels(users)} onChange={(assigneeId) => setTaskForm({ ...taskForm, assigneeId })} />
          <Field label="Title" value={taskForm.title} onChange={(title) => setTaskForm({ ...taskForm, title })} />
          <Field label="Description" value={taskForm.description} required={false} onChange={(description) => setTaskForm({ ...taskForm, description })} />
          <Select label="Priority" value={taskForm.priority} options={PRIORITIES} onChange={(priority) => setTaskForm({ ...taskForm, priority })} />
          <Field label="Due date" type="date" value={taskForm.dueDate} onChange={(dueDate) => setTaskForm({ ...taskForm, dueDate })} />
          <button className="primary"><Plus size={16} /> Create</button>
        </form>
      </Panel>
      <ListPanel title="Existing tasks">
        {tasks.map((task) => (
          <RowItem key={task.id} title={task.title} meta={`${task.assigneeName} / ${pretty(task.status)} / ${task.due_date || 'No due date'}`} onDelete={() => deleteTask(task)} />
        ))}
      </ListPanel>
    </section>
  );
}

function ProjectPage({ projectForm, setProjectForm, projects, createProject, deleteProject }) {
  return (
    <section className="page-grid">
      <Panel title="Add project" icon={<FolderKanban size={18} />}>
        <form onSubmit={createProject} className="form-grid">
          <Field label="Name" value={projectForm.name} onChange={(name) => setProjectForm({ ...projectForm, name })} />
          <Field label="Description" value={projectForm.description} required={false} onChange={(description) => setProjectForm({ ...projectForm, description })} />
          <button className="primary"><Plus size={16} /> Create</button>
        </form>
      </Panel>
      <ListPanel title="Existing projects">
        {projects.map((project) => (
          <RowItem key={project.id} title={project.name} meta={project.description || 'No description'} onDelete={() => deleteProject(project)} />
        ))}
      </ListPanel>
    </section>
  );
}

function UserPage({ userForm, setUserForm, users, currentUserId, createUser, deleteUser }) {
  return (
    <section className="page-grid">
      <Panel title="Add user" icon={<UserPlus size={18} />}>
        <form onSubmit={createUser} className="form-grid">
          <Field label="Name" value={userForm.name} onChange={(name) => setUserForm({ ...userForm, name })} />
          <Field label="Email" type="email" value={userForm.email} onChange={(email) => setUserForm({ ...userForm, email })} />
          <Field label="Password" type="password" value={userForm.password} onChange={(password) => setUserForm({ ...userForm, password })} />
          <Select label="Role" value={userForm.role} options={ROLES} onChange={(role) => setUserForm({ ...userForm, role })} />
          <button className="primary"><Plus size={16} /> Create</button>
        </form>
      </Panel>
      <ListPanel title="Existing users">
        {users.map((user) => (
          <RowItem
            key={user.id}
            title={user.name}
            meta={`${user.email} / ${user.role}`}
            onDelete={user.id === currentUserId ? null : () => deleteUser(user)}
          />
        ))}
      </ListPanel>
    </section>
  );
}

function TaskCard({ task, changeStatus, deleteTask }) {
  const statusOptions = ['', ...STATUSES.filter((status) => status !== task.status)];

  return (
    <article className="task-card">
      <div className="task-card-head">
        <strong>{task.title}</strong>
        <span className={`priority ${task.priority.toLowerCase()}`}>{task.priority}</span>
      </div>
      <p>{task.description || 'No description'}</p>
      <div className="task-meta">
        <span>{task.assigneeName}</span>
        <span>{task.due_date || 'No due date'}</span>
      </div>
      <div className="card-actions">
        <Select label="Move" value="" options={statusOptions} placeholder="Change status" onChange={(next) => next && changeStatus(task, next)} />
        <button className="danger-icon" title="Delete task" onClick={() => deleteTask(task)}><Trash2 size={16} /></button>
      </div>
    </article>
  );
}

function RowItem({ title, meta, onDelete }) {
  return (
    <div className="row-item">
      <div>
        <strong>{title}</strong>
        <span>{meta}</span>
      </div>
      {onDelete && <button className="danger-icon" title="Delete" onClick={onDelete}><Trash2 size={16} /></button>}
    </div>
  );
}

function ToastStack({ toasts, onDismiss }) {
  return (
    <div className="toast-stack" aria-live="polite" aria-atomic="true">
      {toasts.map((toast) => (
        <div className={`toast ${toast.type}`} key={toast.id}>
          <span>{toast.text}</span>
          <button title="Dismiss notification" onClick={() => onDismiss(toast.id)}>x</button>
        </div>
      ))}
    </div>
  );
}

function Field({ label, value, onChange, type = 'text', required = true }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input type={type} value={value} onChange={(event) => onChange(event.target.value)} required={required} />
    </label>
  );
}

function Select({ label, value, options, labels = {}, placeholder = 'All', onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option || 'empty'} value={option}>{labels[option] || (option ? pretty(option) : placeholder)}</option>
        ))}
      </select>
    </label>
  );
}

function Panel({ title, icon, children }) {
  return (
    <section className="panel">
      <h2>{icon}{title}</h2>
      {children}
    </section>
  );
}

function ListPanel({ title, children }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      <div className="row-list">{children}</div>
    </section>
  );
}

function Metric({ icon, label, value }) {
  return (
    <div className="metric">
      {React.cloneElement(icon, { size: 20 })}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function useApi(token) {
  return useMemo(() => (path, options = {}) => request(path, { ...options, token }), [token]);
}

async function request(path, { method = 'GET', body, token } = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.message || `Request failed with ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}

function pretty(value) {
  return String(value).replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function userLabels(users) {
  return Object.fromEntries(users.map((user) => [String(user.id), `${user.name} (${user.role})`]));
}

function projectLabels(projects) {
  return Object.fromEntries(projects.map((project) => [String(project.id), project.name]));
}

createRoot(document.getElementById('root')).render(<App />);
