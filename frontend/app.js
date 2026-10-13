const categories = [
  "groceries",
  "Utilities(bills)",
  "transportation",
  "dining out",
  "entertainment",
  "shopping",
  "travel",
  "education",
];

const app = document.querySelector("#app");
const toast = document.querySelector("#toast");
let csrfToken = "";
let session = { authenticated: false };
let config = {};
let expenses = [];
let toastTimer;

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  })[character]);
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("visible");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 3300);
}

async function getCsrfToken() {
  const response = await fetch("/api/csrf", { credentials: "same-origin" });
  if (!response.ok) throw new Error("Could not initialize secure requests.");
  const data = await response.json();
  csrfToken = data.token;
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  if (options.body && !(options.body instanceof URLSearchParams)) {
    headers.set("Content-Type", "application/json");
  }
  if (["POST", "PUT", "PATCH", "DELETE"].includes((options.method || "GET").toUpperCase())) {
    if (!csrfToken) await getCsrfToken();
    headers.set("X-XSRF-TOKEN", csrfToken);
  }

  const response = await fetch(path, { ...options, headers, credentials: "same-origin" });
  if (response.status === 204) return null;
  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json") ? await response.json() : null;
  if (!response.ok) {
    throw new Error(data?.message || "The request could not be completed.");
  }
  return data;
}

function setRoute(route, replace = false) {
  const method = replace ? "replaceState" : "pushState";
  history[method]({}, "", route);
  render();
}

function routeLink(route, text, className = "") {
  return `<a class="${className}" href="${route}" data-route="${route}">${text}</a>`;
}

function authShell(content) {
  return `
    <div class="auth-layout">
      <section class="auth-visual">
        <a class="brand" href="/" data-route="/"><span class="brand-mark">↗</span> Expenses Tracker</a>
        <div class="visual-copy">
          <h1>Your money, in better focus.</h1>
          <p>A calmer way to keep up with everyday spending, one expense at a time.</p>
        </div>
        <div class="visual-foot">Private by design. Your expenses belong to you.</div>
      </section>
      <section class="auth-content"><div class="auth-card">${content}</div></section>
    </div>`;
}

function renderLogin() {
  const params = new URLSearchParams(location.search);
  const alert = params.has("error")
    ? '<div class="form-error visible" role="alert">Username or password is incorrect.</div>'
    : params.has("registrationSuccess")
      ? '<div class="form-success visible" role="status">Account created. Sign in to continue.</div>'
      : "";
  const oauth = [
    config.googleLoginEnabled
      ? '<a href="/oauth2/authorization/google">Continue with Google</a>'
      : "",
    config.facebookLoginEnabled
      ? '<a href="/oauth2/authorization/facebook">Continue with Facebook</a>'
      : "",
  ].filter(Boolean).join("");

  app.innerHTML = authShell(`
    <h2>Welcome back</h2>
    <p class="subtitle">Sign in to see where your money goes.</p>
    ${alert}
    <div id="login-error" class="form-error" role="alert"></div>
    <form id="login-form" class="form-stack">
      <label class="field">Username<input name="username" autocomplete="username" required /></label>
      <label class="field">Password<input name="password" type="password" autocomplete="current-password" required /></label>
      <button class="button full" type="submit">Sign in</button>
    </form>
    <div class="auth-links">
      ${routeLink("/register", "Create an account")}
      ${config.passwordResetEnabled ? '<a href="/forgot-password">Forgot password?</a>' : ""}
    </div>
    ${oauth ? `<div class="separator">or</div><div class="oauth-links">${oauth}</div>` : ""}
  `);
}

function renderRegister() {
  app.innerHTML = authShell(`
    <h2>Create your account</h2>
    <p class="subtitle">Get started with a clearer view of your spending.</p>
    <div id="register-error" class="form-error" role="alert"></div>
    <div id="register-success" class="form-success" role="status"></div>
    <form id="register-form" class="form-stack">
      <label class="field">Username<input name="username" autocomplete="username" required /></label>
      <label class="field">First name<input name="firstName" autocomplete="given-name" required /></label>
      <label class="field">Last name<input name="lastName" autocomplete="family-name" required /></label>
      <label class="field">Email<input name="email" type="email" autocomplete="email" required /></label>
      <label class="field">Password<input name="password" type="password" autocomplete="new-password" required /></label>
      <button class="button full" type="submit">Create account</button>
    </form>
    <div class="auth-links">${routeLink("/", "Already have an account? Sign in")}</div>
  `);
}

function shortDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? escapeHtml(value)
    : new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(date);
}

function renderDashboard() {
  const firstName = escapeHtml(session.firstName || session.username || "there");
  const total = expenses.reduce((sum, expense) => sum + Number(expense.amount || 0), 0);
  const categoryOptions = categories.map((category) =>
    `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`).join("");
  const rows = expenses.map((expense) => `
    <tr>
      <td><span class="expense-category">${escapeHtml(expense.category)}</span></td>
      <td class="amount">${Number(expense.amount).toLocaleString()}</td>
      <td>${shortDate(expense.dateTime)}</td>
      <td>${escapeHtml(expense.description || "—")}</td>
      <td><div class="row-actions">
        <button class="button secondary small" type="button" data-edit="${expense.id}" aria-label="Edit expense">Edit</button>
        <button class="button danger small" type="button" data-delete="${expense.id}" aria-label="Delete expense">Delete</button>
      </div></td>
    </tr>`).join("");

  app.innerHTML = `
    <div class="dashboard">
      <header class="topbar">
        <a class="brand" href="/dashboard"><span class="brand-mark">↗</span> Expenses Tracker</a>
        <div class="top-actions">
          <button class="button" type="button" data-add>＋ Add expense</button>
          <button class="button secondary" type="button" data-logout>Sign out</button>
        </div>
      </header>
      <section class="dashboard-intro">
        <div><p class="welcome">Welcome back, ${firstName}</p><h1 class="page-title">Expense overview</h1></div>
        <div class="summary-card">
          <span class="summary-icon">↗</span>
          <span><span class="summary-label">Showing total</span><strong class="summary-value">${total.toLocaleString()}</strong></span>
        </div>
      </section>
      <section class="panel">
        <div class="panel-heading"><h2>Your expenses</h2><span class="subtitle" style="margin:0;font-size:12px">${expenses.length} entries</span></div>
        <form id="filter-form" class="filter-grid">
          <select class="filter-control" name="category" aria-label="Category">
            <option value="all">All categories</option>${categoryOptions}
          </select>
          <input class="filter-control" name="from" type="number" min="0" placeholder="Min amount" aria-label="Minimum amount" />
          <input class="filter-control" name="to" type="number" min="0" placeholder="Max amount" aria-label="Maximum amount" />
          <select class="filter-control" name="month" aria-label="Month">
            <option value="all">Any month</option>
            ${Array.from({ length: 12 }, (_, i) => `<option value="${String(i + 1).padStart(2, "0")}">${new Date(2000, i).toLocaleString(undefined, { month: "long" })}</option>`).join("")}
          </select>
          <select class="filter-control" name="year" aria-label="Year">
            <option value="all">Any year</option>
            ${Array.from({ length: 7 }, (_, i) => `<option value="${new Date().getFullYear() - i}">${new Date().getFullYear() - i}</option>`).join("")}
          </select>
          <button class="button small" type="submit">Filter</button>
        </form>
        ${rows ? `
          <div class="table-scroll"><table class="expense-table">
            <thead><tr><th>Category</th><th>Amount</th><th>Date & time</th><th>Description</th><th></th></tr></thead>
            <tbody>${rows}</tbody>
          </table></div>` : `
          <div class="empty-state"><strong>No expenses to show</strong>Add an expense or adjust your filters to get started.</div>`}
      </section>
    </div>`;
}

function renderExpenseModal(expense = null) {
  const options = categories.map((category) =>
    `<option value="${escapeHtml(category)}" ${expense?.category === category ? "selected" : ""}>${escapeHtml(category)}</option>`).join("");
  const dateValue = expense?.dateTime ? expense.dateTime.slice(0, 16) : "";
  const overlay = document.createElement("div");
  overlay.className = "modal-backdrop";
  overlay.innerHTML = `
    <section class="modal" role="dialog" aria-modal="true" aria-labelledby="expense-modal-title">
      <div class="modal-head"><h2 id="expense-modal-title">${expense ? "Edit expense" : "Add an expense"}</h2>
        <button class="icon-button" type="button" data-close aria-label="Close">×</button>
      </div>
      <div id="expense-error" class="form-error" role="alert"></div>
      <form id="expense-form" class="form-stack" ${expense ? `data-expense-id="${expense.id}"` : ""}>
        <label class="field">Category<select name="category" required>${options}</select></label>
        <label class="field">Amount<input name="amount" type="number" min="0" step="1" value="${expense ? Number(expense.amount) : ""}" required /></label>
        <label class="field">Date and time<input name="dateTime" type="datetime-local" value="${escapeHtml(dateValue)}" required /></label>
        <label class="field">Description<textarea name="description" maxlength="400">${escapeHtml(expense?.description || "")}</textarea></label>
        <div class="modal-actions">
          <button class="button secondary" type="button" data-close>Cancel</button>
          <button class="button" type="submit">${expense ? "Save changes" : "Add expense"}</button>
        </div>
      </form>
    </section>`;
  app.append(overlay);
  overlay.querySelector("[name=category]").value = expense?.category || categories[0];
  overlay.querySelector("[name=dateTime]").focus();
}

function setError(id, message) {
  const element = document.querySelector(`#${id}`);
  if (!element) return;
  element.textContent = message;
  element.classList.toggle("visible", Boolean(message));
}

async function loadExpenses(query = "") {
  expenses = await api(`/api/expenses${query}`);
  renderDashboard();
}

async function render() {
  if (!session.authenticated) {
    if (location.pathname === "/register") renderRegister();
    else renderLogin();
    return;
  }
  if (location.pathname !== "/dashboard") {
    history.replaceState({}, "", "/dashboard");
  }
  try {
    await loadExpenses();
  } catch (error) {
    if (error.message.includes("sign in")) {
      session = { authenticated: false };
      renderLogin();
      return;
    }
    app.innerHTML = `<div class="loading">${escapeHtml(error.message)}</div>`;
  }
}

document.addEventListener("click", async (event) => {
  const route = event.target.closest("[data-route]");
  if (route) {
    event.preventDefault();
    setRoute(route.dataset.route);
    return;
  }
  if (event.target.closest("[data-add]")) {
    renderExpenseModal();
    return;
  }
  if (event.target.closest("[data-close]") || event.target.classList.contains("modal-backdrop")) {
    event.target.closest(".modal-backdrop")?.remove();
    return;
  }
  const editButton = event.target.closest("[data-edit]");
  if (editButton) {
    const expense = expenses.find((item) => item.id === Number(editButton.dataset.edit));
    if (expense) renderExpenseModal(expense);
    return;
  }
  const deleteButton = event.target.closest("[data-delete]");
  if (deleteButton) {
    if (!window.confirm("Delete this expense? This cannot be undone.")) return;
    try {
      await api(`/api/expenses/${deleteButton.dataset.delete}`, { method: "DELETE" });
      await loadExpenses();
      showToast("Expense deleted.");
    } catch (error) {
      showToast(error.message);
    }
    return;
  }
  if (event.target.closest("[data-logout]")) {
    try {
      await api("/logout", { method: "POST" });
      session = { authenticated: false };
      await getCsrfToken();
      setRoute("/");
    } catch (error) {
      showToast(error.message);
    }
  }
});

document.addEventListener("submit", async (event) => {
  const form = event.target;
  event.preventDefault();

  if (form.id === "login-form") {
    const button = form.querySelector("button[type=submit]");
    button.disabled = true;
    try {
      const data = new URLSearchParams(new FormData(form));
      await api("/authenticateTheUser", { method: "POST", body: data });
      csrfToken = "";
      await getCsrfToken();
      session = await api("/api/session");
      setRoute("/dashboard", true);
    } catch (error) {
      setError("login-error", error.message);
    } finally {
      button.disabled = false;
    }
    return;
  }

  if (form.id === "register-form") {
    const values = Object.fromEntries(new FormData(form));
    try {
      const response = await api("/api/register", { method: "POST", body: JSON.stringify(values) });
      setError("register-success", response.message);
      form.reset();
      window.setTimeout(() => setRoute("/"), 900);
    } catch (error) {
      setError("register-error", error.message);
    }
    return;
  }

  if (form.id === "filter-form") {
    const values = new FormData(form);
    const query = new URLSearchParams();
    for (const key of ["category", "from", "to", "month", "year"]) {
      if (values.get(key)) query.set(key, values.get(key));
    }
    try {
      await loadExpenses(`?${query}`);
    } catch (error) {
      showToast(error.message);
    }
    return;
  }

  if (form.id === "expense-form") {
    const values = Object.fromEntries(new FormData(form));
    const editingId = form.dataset.expenseId;
    values.amount = Number(values.amount);
    const method = editingId ? "PUT" : "POST";
    const url = editingId ? `/api/expenses/${editingId}` : "/api/expenses";
    try {
      await api(url, { method, body: JSON.stringify(values) });
      form.closest(".modal-backdrop").remove();
      await loadExpenses();
      showToast(editingId ? "Expense updated." : "Expense added.");
    } catch (error) {
      setError("expense-error", error.message);
    }
  }
});

window.addEventListener("popstate", render);

async function start() {
  try {
    await getCsrfToken();
    [config, session] = await Promise.all([api("/api/config"), api("/api/session")]);
    if (session.authenticated && location.pathname === "/") {
      history.replaceState({}, "", "/dashboard");
    }
    await render();
  } catch (error) {
    app.innerHTML = `<div class="loading">${escapeHtml(error.message)}</div>`;
  }
}

start();
