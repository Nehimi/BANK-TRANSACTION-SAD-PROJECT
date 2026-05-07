// Simulated Data (In a real app, this would come from your Java Backend)
let currentUser = null;
let transactions = [
    { type: 'DEPOSIT', amount: 500.00, date: '2023-05-01 10:30', status: 'Completed' },
    { type: 'WITHDRAW', amount: 100.00, date: '2023-05-02 14:15', status: 'Completed' },
    { type: 'TRANSFER_OUT', amount: 250.00, date: '2023-05-03 09:00', status: 'Completed' },
    { type: 'SERVICE_FEE', amount: 2.50, date: '2023-05-03 09:00', status: 'Deducted' }
];

// UI Elements
const loginSection = document.getElementById('login-section');
const dashboardSection = document.getElementById('dashboard-section');
const userInfo = document.getElementById('user-info');
const navUsername = document.getElementById('nav-username');
const displayBalance = document.getElementById('display-balance');
const displayStatus = document.getElementById('display-status');
const transactionTableBody = document.querySelector('#transaction-table tbody');

// 1. Login Logic
function login(role) {
    const accNo = document.getElementById('login-account').value;
    const pin = document.getElementById('login-pin').value;

    if (!accNo || !pin) {
        alert("Please enter both Account Number and PIN.");
        return;
    }

    // Simulate authentication
    console.log(`Logging in as ${role}...`);

    // Switch Views
    loginSection.style.display = 'none';
    dashboardSection.style.display = 'block';
    userInfo.style.display = 'flex';

    // Set User Data
    currentUser = {
        accountNumber: accNo,
        name: role === 'admin' ? 'System Administrator' : 'Abebe Kebede',
        balance: 1240.50,
        status: 'ACTIVE'
    };

    navUsername.innerText = currentUser.name;
    updateDashboard();
    renderTransactions();
}

// 2. Update Dashboard UI
function updateDashboard() {
    displayBalance.innerText = currentUser.balance.toLocaleString('en-US', { minimumFractionDigits: 2 });
    displayStatus.innerText = currentUser.status;
    displayStatus.className = `status-badge status-${currentUser.status.toLowerCase()}`;
}

// 3. Render Transaction Table
function renderTransactions() {
    transactionTableBody.innerHTML = '';
    transactions.forEach(tx => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td class="type-${tx.type.toLowerCase().split('_')[0]}">
                <i class="fa-solid ${tx.type.includes('DEPOSIT') ? 'fa-arrow-down' : 'fa-arrow-up'}"></i> 
                ${tx.type}
            </td>
            <td>$${tx.amount.toFixed(2)}</td>
            <td style="font-size: 0.8rem; color: var(--text-secondary);">${tx.date}</td>
            <td><span class="status-badge" style="background: rgba(255,255,255,0.05);">${tx.status}</span></td>
        `;
        transactionTableBody.appendChild(row);
    });
}

// 4. Modal Logic
const modalOverlay = document.getElementById('modal-overlay');
const modalTitle = document.getElementById('modal-title');
const modalBody = document.getElementById('modal-body');
const modalConfirmBtn = document.getElementById('modal-confirm-btn');

function openModal(action) {
    modalOverlay.style.display = 'flex';
    modalTitle.innerText = action.charAt(0).toUpperCase() + action.slice(1);

    if (action === 'transfer') {
        modalBody.innerHTML = `
            <div class="input-group">
                <label>Recipient Account Number</label>
                <input type="text" id="modal-target" placeholder="1000XXXX">
            </div>
            <div class="input-group">
                <label>Amount to Transfer</label>
                <input type="number" id="modal-amount" placeholder="0.00">
            </div>
            <p style="font-size: 0.75rem; color: var(--accent);">* 1% service fee will be applied.</p>
        `;
    } else {
        modalBody.innerHTML = `
            <div class="input-group">
                <label>Amount to ${action}</label>
                <input type="number" id="modal-amount" placeholder="0.00">
            </div>
        `;
    }

    modalConfirmBtn.onclick = () => processAction(action);
}

function closeModal() {
    modalOverlay.style.display = 'none';
}

// 5. Process Actions (Simulated)
function processAction(action) {
    const amountInput = document.getElementById('modal-amount');
    const amount = parseFloat(amountInput.value);

    if (isNaN(amount) || amount <= 0) {
        alert("Please enter a valid amount.");
        return;
    }

    // Business Logic Simulation
    if (action === 'withdraw' || action === 'transfer') {
        let fee = action === 'transfer' ? amount * 0.01 : (amount > 5000 ? 5 : 0);
        let total = amount + fee;

        if (currentUser.balance < total) {
            alert("Insufficient funds!");
            return;
        }
        currentUser.balance -= total;

        transactions.unshift({
            type: action.toUpperCase(),
            amount: amount,
            date: new Date().toLocaleString(),
            status: 'Completed'
        });

        if (fee > 0) {
            transactions.unshift({
                type: 'SERVICE_FEE',
                amount: fee,
                date: new Date().toLocaleString(),
                status: 'Deducted'
            });
        }
    } else if (action === 'deposit') {
        currentUser.balance += amount;
        transactions.unshift({
            type: 'DEPOSIT',
            amount: amount,
            date: new Date().toLocaleString(),
            status: 'Completed'
        });
    }

    updateDashboard();
    renderTransactions();
    closeModal();
    alert(`Success! ${action} of $${amount} processed.`);
}

function processQuickAction(action) {
    const amount = parseFloat(document.getElementById('quick-withdraw-amount').value);
    if (isNaN(amount) || amount <= 0) {
        alert("Please enter a valid amount.");
        return;
    }
    processAction(action);
}

// 6. Logout
function logout() {
    currentUser = null;
    dashboardSection.style.display = 'none';
    userInfo.style.display = 'none';
    loginSection.style.display = 'flex';
    document.getElementById('login-account').value = '';
    document.getElementById('login-pin').value = '';
}
