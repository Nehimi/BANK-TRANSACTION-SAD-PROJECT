// EthioBank Frontend Integration Script
let currentUser = null;
const API_BASE = "/api"; 

// UI Elements
const loginSection = document.getElementById('login-section');
const dashboardSection = document.getElementById('dashboard-section');
const userInfo = document.getElementById('user-info');
const navUsername = document.getElementById('nav-username');
const displayBalance = document.getElementById('display-balance');
const displayStatus = document.getElementById('display-status');
const lastFourDigits = document.getElementById('last-four');
const transactionTableBody = document.querySelector('#transaction-table tbody');

// 1. Login Logic
async function login(role) {
    const accNo = document.getElementById('login-account').value;
    const pin = document.getElementById('login-pin').value;

    if (!accNo || !pin) {
        showFeedback("Please enter both Account Number and PIN.", "error");
        return;
    }

    setLoading(true);

    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                role: role,
                identifier: accNo,
                pin: pin
            })
        });

        const data = await response.json();

        if (response.ok) {
            // Authentication successful
            await fetchAccountDetails(accNo);
            
            // Switch Views with a slight delay for smooth transition
            setTimeout(() => {
                loginSection.style.display = 'none';
                dashboardSection.style.display = 'block';
                userInfo.style.display = 'flex';
                document.querySelector('.user-role-badge').innerText = role.charAt(0).toUpperCase() + role.slice(1);
                setLoading(false);
            }, 500);
        } else {
            setLoading(false);
            showFeedback(data.message || "Login failed", "error");
        }
    } catch (error) {
        setLoading(false);
        console.error("Login error:", error);
        showFeedback("Server connection error. Ensure the Java backend is running on port 8081.", "error");
    }
}

// 2. Fetch Account Details
async function fetchAccountDetails(accNo) {
    try {
        const response = await fetch(`${API_BASE}/account/${accNo}`);
        const data = await response.json();

        if (response.ok) {
            currentUser = data;
            navUsername.innerText = currentUser.accountHolder;
            updateDashboardUI();
            await fetchTransactionHistory(accNo);
        }
    } catch (error) {
        console.error("Fetch account error:", error);
    }
}

// 3. Update Dashboard UI
function updateDashboardUI() {
    if (!currentUser) return;
    
    // Animate balance update
    animateValue(displayBalance, parseFloat(displayBalance.innerText.replace(/,/g, '')), currentUser.balance, 1000);
    
    displayStatus.innerText = currentUser.status;
    displayStatus.className = `status-indicator status-${currentUser.status.toLowerCase()}`;
    
    // Show last 4 digits of account number
    const accStr = currentUser.accountNumber.toString();
    lastFourDigits.innerText = accStr.slice(-4);
}

// 4. Fetch Transaction History
let fullTransactionHistory = [];

async function fetchTransactionHistory(accNo) {
    try {
        const response = await fetch(`${API_BASE}/history/${accNo}`);
        fullTransactionHistory = await response.json();
        renderTransactions(fullTransactionHistory);
    } catch (error) {
        console.error("Fetch history error:", error);
        transactionTableBody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: var(--accent);">Failed to load transactions.</td></tr>';
    }
}

// 5. Render Transaction Table
function renderTransactions(transactions, limit = 4) {
    if (!transactions || transactions.length === 0) {
        transactionTableBody.innerHTML = '<tr><td colspan="4" style="text-align: center; padding: 3rem; color: var(--text-secondary);">No transactions found.</td></tr>';
        return;
    }

    // Apply limit if specified (for dashboard)
    const displayTx = limit ? transactions.slice(0, limit) : transactions;

    transactionTableBody.innerHTML = '';
    displayTx.forEach(tx => {
        const isIncome = tx.type.includes('DEPOSIT') || tx.type.includes('IN');
        const row = document.createElement('tr');
        row.className = 'animate-fade';
        
        let txDate;
        if (Array.isArray(tx.timestamp)) {
            txDate = new Date(tx.timestamp[0], tx.timestamp[1]-1, tx.timestamp[2], tx.timestamp[3], tx.timestamp[4], tx.timestamp[5]);
        } else if (typeof tx.timestamp === 'string') {
            txDate = new Date(tx.timestamp);
        } else {
            txDate = new Date();
        }

        row.innerHTML = `
            <td>
                <div class="type-cell">
                    <div class="type-icon ${isIncome ? 'income-icon' : 'expense-icon'}">
                        <i class="fa-solid ${isIncome ? 'fa-arrow-down' : 'fa-arrow-up'}"></i>
                    </div>
                    <div>
                        <div style="font-weight: 600;">${formatType(tx.type)}</div>
                        <div style="font-size: 0.75rem; color: var(--text-secondary);">ID: #${tx.transactionId || 'N/A'}</div>
                    </div>
                </div>
            </td>
            <td style="font-weight: 700; color: ${isIncome ? 'var(--success)' : 'var(--text-primary)'}">
                ${isIncome ? '+' : '-'}ETB ${tx.amount.toFixed(2)}
            </td>
            <td>
                <div style="font-size: 0.9rem;">${txDate.toLocaleDateString()}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary);">${txDate.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</div>
            </td>
        `;
        transactionTableBody.appendChild(row);
    });
}

function formatType(type) {
    return type.split('_').map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(' ');
}

// 6. Modal Logic
const modalOverlay = document.getElementById('modal-overlay');
const modalTitle = document.getElementById('modal-title');
const modalBody = document.getElementById('modal-body');
const modalConfirmBtn = document.getElementById('modal-confirm-btn');

function showAllTransactions() {
    if (!fullTransactionHistory || fullTransactionHistory.length === 0) return;

    modalOverlay.style.display = 'flex';
    modalTitle.innerText = "Full Transaction History";
    
    modalBody.innerHTML = `
        <div class="table-container" style="max-height: 400px; overflow-y: auto;">
            <table style="width: 100%; border-collapse: collapse;">
                <tbody id="full-history-body"></tbody>
            </table>
        </div>
    `;

    const fullBody = document.getElementById('full-history-body');
    fullTransactionHistory.forEach(tx => {
        const isIncome = tx.type.includes('DEPOSIT') || tx.type.includes('IN');
        let txDate;
        if (Array.isArray(tx.timestamp)) {
            txDate = new Date(tx.timestamp[0], tx.timestamp[1]-1, tx.timestamp[2], tx.timestamp[3], tx.timestamp[4], tx.timestamp[5]);
        } else if (typeof tx.timestamp === 'string') {
            txDate = new Date(tx.timestamp);
        } else {
            txDate = new Date();
        }

        const row = document.createElement('tr');
        row.innerHTML = `
            <td style="padding: 1rem; border-bottom: 1px solid var(--glass-border);">
                <div style="font-weight: 600;">${formatType(tx.type)}</div>
                <div style="font-size: 0.75rem; color: var(--text-secondary);">${txDate.toLocaleDateString()} ${txDate.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</div>
            </td>
            <td style="padding: 1rem; border-bottom: 1px solid var(--glass-border); text-align: right; font-weight: 700; color: ${isIncome ? 'var(--success)' : 'var(--text-primary)'}">
                ${isIncome ? '+' : '-'}ETB ${tx.amount.toFixed(2)}
            </td>
        `;
        fullBody.appendChild(row);
    });

    modalConfirmBtn.style.display = 'none'; 
}

function openModal(action) {
    modalOverlay.style.display = 'flex';
    modalTitle.innerText = action.charAt(0).toUpperCase() + action.slice(1);
    modalConfirmBtn.style.display = 'inline-flex';

    if (action === 'transfer') {
        modalBody.innerHTML = `
            <div class="input-group">
                <label>Recipient Account Number</label>
                <div class="input-wrapper">
                    <i class="fa-solid fa-user-tag"></i>
                    <input type="text" id="modal-target" placeholder="Recipient account number">
                </div>
            </div>
            <div class="input-group">
                <label>Amount to Transfer</label>
                <div class="input-wrapper">
                    <i class="fa-solid fa-money-bill-wave"></i>
                    <input type="number" id="modal-amount" placeholder="0.00">
                </div>
            </div>
            <div style="background: rgba(255, 204, 0, 0.1); padding: 1rem; border-radius: 12px; border: 1px solid rgba(255, 204, 0, 0.2); display: flex; gap: 0.8rem; align-items: center;">
                <i class="fa-solid fa-circle-exclamation" style="color: #ffcc00;"></i>
                <p style="font-size: 0.8rem; color: #ffcc00; margin: 0;">A 1% service fee will be applied to this transfer.</p>
            </div>
        `;
    } else {
        modalBody.innerHTML = `
            <div class="input-group">
                <label>Amount to ${action}</label>
                <div class="input-wrapper">
                    <i class="fa-solid fa-money-bill-wave"></i>
                    <input type="number" id="modal-amount" placeholder="0.00">
                </div>
            </div>
        `;
    }

    modalConfirmBtn.onclick = () => processAction(action);
}

function closeModal() {
    modalOverlay.style.display = 'none';
}

// 7. Process Actions
async function processAction(action) {
    const amountInput = document.getElementById('modal-amount');
    const amount = parseFloat(amountInput.value);

    if (isNaN(amount) || amount <= 0) {
        showFeedback("Please enter a valid amount.", "error");
        return;
    }

    modalConfirmBtn.disabled = true;
    modalConfirmBtn.innerText = "Processing...";

    let payload = { accountNumber: currentUser.accountNumber, amount: amount };
    let endpoint = `${API_BASE}/${action}`;

    if (action === 'transfer') {
        const targetAcc = document.getElementById('modal-target').value;
        if (!targetAcc) {
            showFeedback("Please enter target account number.", "error");
            modalConfirmBtn.disabled = false;
            modalConfirmBtn.innerText = "Confirm Action";
            return;
        }
        payload = { fromAccount: currentUser.accountNumber, toAccount: targetAcc, amount: amount };
    }

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (response.ok) {
            showFeedback(data.message, "success");
            closeModal();
            // Refresh data
            await fetchAccountDetails(currentUser.accountNumber);
        } else {
            showFeedback(data.message || "Action failed", "error");
        }
    } catch (error) {
        console.error("Action error:", error);
        showFeedback("Communication error with server.", "error");
    } finally {
        modalConfirmBtn.disabled = false;
        modalConfirmBtn.innerText = "Confirm Action";
    }
}

function processQuickAction(action) {
    const amountInput = document.getElementById('quick-withdraw-amount');
    const amount = parseFloat(amountInput.value);
    if (isNaN(amount) || amount <= 0) {
        showFeedback("Please enter a valid amount.", "error");
        return;
    }

    // Reuse processAction logic without opening modal
    const originalBody = modalBody.innerHTML; // Store
    modalBody.innerHTML = `<input type="number" id="modal-amount" value="${amount}">`;
    
    processAction(action).then(() => {
        amountInput.value = '';
    });
}

// 8. Helpers
function setLoading(isLoading) {
    const btn = document.querySelector('#login-section .btn-primary');
    if (isLoading) {
        btn.innerHTML = '<div class="loading-spinner" style="width: 20px; height: 20px; margin: 0;"></div>';
        btn.disabled = true;
    } else {
        btn.innerHTML = '<span>Sign In</span> <i class="fa-solid fa-arrow-right"></i>';
        btn.disabled = false;
    }
}

function showFeedback(message, type) {
    // Simple alert for now, but stylized
    alert(`${type.toUpperCase()}: ${message}`);
}

function animateValue(obj, start, end, duration) {
    let startTimestamp = null;
    const step = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        const value = progress * (end - start) + start;
        obj.innerText = value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        if (progress < 1) {
            window.requestAnimationFrame(step);
        }
    };
    window.requestAnimationFrame(step);
}

// 9. Logout
function logout() {
    currentUser = null;
    dashboardSection.style.display = 'none';
    userInfo.style.display = 'none';
    loginSection.style.display = 'flex';
    document.getElementById('login-account').value = '';
    document.getElementById('login-pin').value = '';
}

