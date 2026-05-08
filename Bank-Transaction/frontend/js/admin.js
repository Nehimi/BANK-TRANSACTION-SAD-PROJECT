// EthioBank Admin Integration Script
const API_BASE = "http://localhost:8081/api";

let allAccounts = [];
let allTransactions = [];
let showingAllTransactions = false;

// 1. Admin Authentication
async function handleAdminLogin() {
    const id = document.getElementById('admin-id').value;
    const key = document.getElementById('admin-key').value;

    if (!id || !key) {
        alert("Please enter credentials");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role: 'admin', identifier: id, pin: key })
        });

        const data = await res.json();
        if (data.status === 'success') {
            localStorage.setItem('ethiobank_adminId', id);
            document.getElementById('admin-login-screen').style.display = 'none';
            document.getElementById('admin-dashboard-screen').style.display = 'block';
            loadAccounts();
        } else {
            alert(data.message);
        }
    } catch (err) {
        alert("Server error connecting to EthioBank Secure Node");
    }
}

// 2. Load and Manage Accounts
async function loadAccounts() {
    try {
        const res = await fetch(`${API_BASE}/admin/accounts`);
        allAccounts = await res.json();
        renderAccounts(allAccounts);

        const totalBalance = allAccounts.reduce((sum, acc) => sum + acc.balance, 0);
        document.getElementById('stat-total-accounts').innerText = allAccounts.length;
        document.getElementById('stat-total-balance').innerText = `ETB ${totalBalance.toLocaleString()}`;
    } catch (err) {
        console.error("Error loading accounts:", err);
    }
}

function renderAccounts(accounts) {
    const tbody = document.getElementById('accounts-tbody');
    tbody.innerHTML = '';

    accounts.forEach(acc => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${acc.accountNumber}</td>
            <td style="font-weight: 600;">${acc.accountHolder}</td>
            <td style="font-weight: 700;">ETB ${acc.balance.toLocaleString()}</td>
            <td><span class="status-badge status-${acc.status.toLowerCase()}">${acc.status}</span></td>
            <td>
                <div class="action-buttons-group">
                    ${acc.status === 'ACTIVE'
                ? `<button class="btn-action btn-freeze" onclick="updateStatus('${acc.accountNumber}', 'FROZEN')" title="Freeze Account">Freeze</button>`
                : `<button class="btn-action btn-activate" onclick="updateStatus('${acc.accountNumber}', 'ACTIVE')" title="Activate Account">Activate</button>`
            }
                    <button class="btn-action btn-undo" onclick="handleUndo('${acc.accountNumber}')" title="Undo Last Transaction">
                        <i class="fa-solid fa-rotate-left"></i>
                    </button>
                    <button class="btn-action btn-reset" onclick="handleResetPin('${acc.accountNumber}')" title="Reset PIN">
                        <i class="fa-solid fa-key"></i>
                    </button>
                    <button class="btn-action btn-delete" onclick="handleDeleteAccount('${acc.accountNumber}')" title="Delete Account">
                        <i class="fa-solid fa-trash-can"></i>
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

function filterAccounts() {
    const query = document.getElementById('account-search').value.toLowerCase();
    const filtered = allAccounts.filter(acc =>
        acc.accountNumber.toLowerCase().includes(query) ||
        acc.accountHolder.toLowerCase().includes(query)
    );
    renderAccounts(filtered);
}

// 3. Admin Actions
async function updateStatus(accNo, status) {
    try {
        const res = await fetch(`${API_BASE}/admin/update-status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: accNo, status: status })
        });
        const data = await res.json();
        if (data.status === 'success') {
            loadAccounts();
        } else {
            alert(data.message);
        }
    } catch (err) {
        alert("Failed to update account status");
    }
}

async function handleUndo(accNo) {
    if (!confirm(`Are you sure you want to undo the last transaction for account ${accNo}?`)) return;

    try {
        const res = await fetch(`${API_BASE}/admin/undo`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: accNo })
        });
        const data = await res.json();
        alert(data.message);
        if (data.status === 'success') {
            loadAccounts();
        }
    } catch (err) {
        alert("Undo failed");
    }
}

async function handleResetPin(accNo) {
    const newPin = prompt("Enter new 4-digit PIN for account " + accNo, "1234");
    if (!newPin || newPin.length !== 4) {
        alert("Invalid PIN. Must be 4 digits.");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/admin/reset-pin`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: accNo, newPin: newPin })
        });
        const data = await res.json();
        alert(data.message);
    } catch (err) {
        alert("Reset failed");
    }
}

async function handleDeleteAccount(accNo) {
    if (!confirm(`CRITICAL: Are you sure you want to PERMANENTLY DELETE account ${accNo}? This action cannot be undone.`)) return;

    try {
        const res = await fetch(`${API_BASE}/admin/delete-account/${accNo}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        alert(data.message);
        if (data.status === 'success') {
            loadAccounts();
        }
    } catch (err) {
        alert("Deletion failed");
    }
}

// 4. Create Account
async function handleCreateAccount() {
    const account = {
        accountNumber: document.getElementById('new-acc-no').value,
        accountHolder: document.getElementById('new-acc-name').value,
        pinCode: document.getElementById('new-acc-pin').value,
        balance: parseFloat(document.getElementById('new-acc-balance').value),
        status: 'ACTIVE'
    };

    if (!account.accountNumber || !account.accountHolder || !account.pinCode) {
        alert("Please fill all required fields");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/admin/create-account`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(account)
        });
        const data = await res.json();
        alert(data.message);
        if (data.status === 'success') {
            document.getElementById('new-acc-no').value = '';
            document.getElementById('new-acc-name').value = '';
            document.getElementById('new-acc-pin').value = '';
            showTab('accounts');
            loadAccounts();
        }
    } catch (err) {
        alert("Failed to create account");
    }
}

// 5. Audit Logs
async function loadTransactions() {
    try {
        const res = await fetch(`${API_BASE}/admin/transactions`);
        allTransactions = await res.json();
        showingAllTransactions = false;
        renderTransactions();
    } catch (err) {
        console.error("Error loading transactions:", err);
    }
}

function renderTransactions() {
    const tbody = document.getElementById('transactions-tbody');
    tbody.innerHTML = '';

    // Sort by ID descending
    const sorted = [...allTransactions].sort((a, b) => b.transactionId - a.transactionId);

    // Limit to 10 if not showing all
    const displayList = showingAllTransactions ? sorted : sorted.slice(0, 10);

    displayList.forEach(tx => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>#${tx.transactionId}</td>
            <td><b>${tx.type}</b></td>
            <td>${tx.accountNumber}</td>
            <td style="color: ${tx.type.includes('DEPOSIT') || tx.type.includes('IN') ? 'var(--success)' : 'var(--accent)'}">
                ${tx.type.includes('DEPOSIT') || tx.type.includes('IN') ? '+' : '-'}ETB ${tx.amount.toFixed(2)}
            </td>
            <td>${formatDate(tx.timestamp)}</td>
        `;
        tbody.appendChild(row);
    });

    // Show/hide the 'View All' button
    const viewAllBtn = document.getElementById('view-all-tx-container');
    if (viewAllBtn) {
        viewAllBtn.style.display = (allTransactions.length > 10 && !showingAllTransactions) ? 'block' : 'none';
    }
}

function showAllTransactions() {
    showingAllTransactions = true;
    renderTransactions();
}

function filterTransactions() {
    const query = document.getElementById('tx-search').value.toLowerCase();
    const filtered = allTransactions.filter(tx =>
        tx.accountNumber.toLowerCase().includes(query) ||
        tx.type.toLowerCase().includes(query)
    );

    // When filtering, we usually want to see all matches, not just 10
    const tbody = document.getElementById('transactions-tbody');
    tbody.innerHTML = '';

    filtered.sort((a, b) => b.transactionId - a.transactionId).forEach(tx => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>#${tx.transactionId}</td>
            <td><b>${tx.type}</b></td>
            <td>${tx.accountNumber}</td>
            <td style="color: ${tx.type.includes('DEPOSIT') || tx.type.includes('IN') ? 'var(--success)' : 'var(--accent)'}">
                ${tx.type.includes('DEPOSIT') || tx.type.includes('IN') ? '+' : '-'}ETB ${tx.amount.toFixed(2)}
            </td>
            <td>${formatDate(tx.timestamp)}</td>
        `;
        tbody.appendChild(row);
    });

    document.getElementById('view-all-tx-container').style.display = 'none';
}

function formatDate(timestamp) {
    if (!timestamp) return "N/A";

    // Handle Java LocalDateTime format [YYYY, MM, DD, HH, mm, ss] or String
    let date;
    if (Array.isArray(timestamp)) {
        date = new Date(timestamp[0], timestamp[1] - 1, timestamp[2], timestamp[3], timestamp[4], timestamp[5]);
    } else {
        date = new Date(timestamp);
    }

    if (isNaN(date.getTime())) return "N/A";

    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 6. Navigation (Professional Hash Routing)
function showTab(tab) {
    // Update the URL hash without reloading
    location.hash = tab;

    document.querySelectorAll('.tab-content').forEach(t => t.style.display = 'none');
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const targetTab = document.getElementById(`tab-${tab}`);
    if (targetTab) {
        targetTab.style.display = 'block';
    }

    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        if (item.getAttribute('onclick').includes(tab)) {
            item.classList.add('active');
        }
    });

    if (tab === 'accounts') loadAccounts();
    if (tab === 'transactions') loadTransactions();

    const titles = {
        'accounts': ['Account Management', 'Manage all customer accounts and statuses'],
        'transactions': ['System Audit Logs', 'Monitor every transaction happening in the bank'],
        'create': ['Onboard Customer', 'Create a new high-security bank account']
    };

    if (titles[tab]) {
        document.getElementById('page-title').innerText = titles[tab][0];
        document.getElementById('page-desc').innerText = titles[tab][1];
    }
}

// Handle routing on load and back/forward buttons
window.addEventListener('hashchange', () => {
    const tab = location.hash.replace('#', '') || 'accounts';
    showTab(tab);
});

window.addEventListener('load', () => {
    const savedAdmin = localStorage.getItem('ethiobank_adminId');
    if (savedAdmin) {
        document.getElementById('admin-login-screen').style.display = 'none';
        document.getElementById('admin-dashboard-screen').style.display = 'flex';
        loadAccounts();
    }

    const tab = location.hash.replace('#', '') || 'accounts';
    // If logged in, show the tab
    if (document.getElementById('admin-dashboard-screen').style.display !== 'none') {
        showTab(tab);
    }
});

function adminLogout() {
    localStorage.removeItem('ethiobank_adminId');
    window.location.href = 'admin.html';
}
