/**
 * VoxLink Web Portal - Main JavaScript
 * Handles UI interactions, API calls, and state management
 */

// ============================================
// Global State
// ============================================
const state = {
    token: localStorage.getItem('voxlink_token'),
    username: localStorage.getItem('voxlink_username'),
    currentWorkspaceId: null,
    currentInviteCode: null
};

// API Base URL
const API_BASE = '';

// ============================================
// Utility Functions
// ============================================
function showError(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.style.display = 'block';
        setTimeout(() => {
            element.style.display = 'none';
        }, 5000);
    }
}

function showSuccess(elementId, message) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = message;
        element.style.display = 'block';
        setTimeout(() => {
            element.style.display = 'none';
        }, 3000);
    }
}

function formatNumber(num) {
    return num.toLocaleString();
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ============================================
// API Calls
// ============================================
async function apiGet(endpoint) {
    const response = await fetch(API_BASE + endpoint, {
        headers: {
            'Authorization': state.token ? `Bearer ${state.token}` : ''
        }
    });
    return response.json();
}

async function apiPost(endpoint, data) {
    const response = await fetch(API_BASE + endpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': state.token ? `Bearer ${state.token}` : ''
        },
        body: JSON.stringify(data)
    });
    return response.json();
}

// ============================================
// Statistics Page Functions
// ============================================
async function loadStatistics() {
    try {
        const stats = await apiGet('/api/stats');

        // Update user stats
        const totalUsersEl = document.getElementById('totalUsers');
        if (totalUsersEl) totalUsersEl.textContent = formatNumber(stats.totalUsers || 0);

        const onlineUsersEl = document.getElementById('onlineUsers');
        if (onlineUsersEl) onlineUsersEl.textContent = formatNumber(stats.onlineUsers || 0);

        const idleUsersEl = document.getElementById('idleUsers');
        if (idleUsersEl) idleUsersEl.textContent = formatNumber((stats.idleUsers || 0) + (stats.dndUsers || 0));

        const onlinePercent = stats.onlinePercentage || 0;
        const onlinePercentEl = document.getElementById('onlinePercent');
        if (onlinePercentEl) onlinePercentEl.textContent = Math.round(onlinePercent) + '%';

        const onlinePercentLabelEl = document.getElementById('onlinePercentLabel');
        if (onlinePercentLabelEl) onlinePercentLabelEl.textContent = Math.round(onlinePercent) + '%';

        const onlineProgressBarEl = document.getElementById('onlineProgressBar');
        if (onlineProgressBarEl) onlineProgressBarEl.style.width = onlinePercent + '%';

        // Update workspace stats
        const totalWorkspacesEl = document.getElementById('totalWorkspaces');
        if (totalWorkspacesEl) totalWorkspacesEl.textContent = formatNumber(stats.totalWorkspaces || 0);

        const publicWorkspacesEl = document.getElementById('publicWorkspaces');
        if (publicWorkspacesEl) publicWorkspacesEl.textContent = formatNumber(stats.publicWorkspaces || 0);

        const privateWorkspacesEl = document.getElementById('privateWorkspaces');
        if (privateWorkspacesEl) privateWorkspacesEl.textContent = formatNumber(stats.privateWorkspaces || 0);

        // Update channel stats
        const totalChannelsEl = document.getElementById('totalChannels');
        if (totalChannelsEl) totalChannelsEl.textContent = formatNumber(stats.totalChannels || 0);

        const textChannelsEl = document.getElementById('textChannels');
        if (textChannelsEl) textChannelsEl.textContent = formatNumber(stats.textChannels || 0);

        const voiceChannelsEl = document.getElementById('voiceChannels');
        if (voiceChannelsEl) voiceChannelsEl.textContent = formatNumber(stats.voiceChannels || 0);

        const announcementChannelsEl = document.getElementById('announcementChannels');
        if (announcementChannelsEl) announcementChannelsEl.textContent = formatNumber(stats.announcementChannels || 0);

        // Update message stats
        const totalMessagesTodayEl = document.getElementById('totalMessagesToday');
        if (totalMessagesTodayEl) totalMessagesTodayEl.textContent = formatNumber(stats.totalMessagesToday || 0);

        const totalMessagesAllTimeEl = document.getElementById('totalMessagesAllTime');
        if (totalMessagesAllTimeEl) totalMessagesAllTimeEl.textContent = formatNumber(stats.totalMessagesAllTime || 0);

        const messagesPerMinuteEl = document.getElementById('messagesPerMinute');
        if (messagesPerMinuteEl) messagesPerMinuteEl.textContent = (stats.messagesPerMinute || 0).toFixed(1);

        // Update file stats
        const totalFilesEl = document.getElementById('totalFiles');
        if (totalFilesEl) totalFilesEl.textContent = formatNumber(stats.totalFiles || 0);

        const storageUsedEl = document.getElementById('storageUsed');
        if (storageUsedEl) storageUsedEl.textContent = stats.formattedStorage || '0 B';

        // Update connection stats
        const activeConnectionsEl = document.getElementById('activeConnections');
        if (activeConnectionsEl) activeConnectionsEl.textContent = formatNumber(stats.activeConnections || 0);

        const peakConnectionsEl = document.getElementById('peakConnections');
        if (peakConnectionsEl) peakConnectionsEl.textContent = formatNumber(stats.peakConnectionsToday || 0);

        const serverUptimeEl = document.getElementById('serverUptime');
        if (serverUptimeEl) serverUptimeEl.textContent = stats.serverUptime || '--';

        // Update timestamp
        const timestampEl = document.getElementById('timestamp');
        if (timestampEl) timestampEl.textContent = new Date().toLocaleString();

    } catch (error) {
        console.error('Failed to load statistics:', error);
    }
}

// ============================================
// Dashboard Functions
// ============================================
async function loadDashboard() {
    if (!state.token) {
        window.location.href = '/login';
        return;
    }

    document.getElementById('username').textContent = state.username || 'User';
    await loadUserWorkspaces();
}

async function loadUserWorkspaces() {
    try {
        // This would call your actual API endpoint
        // For now, show demo data or make real API call
        const workspacesList = document.getElementById('workspacesList');
        if (workspacesList) {
            // Show loading state, then workspaces
            workspacesList.innerHTML = '<div class="loading-spinner">Loading workspaces...</div>';
            // TODO: Replace with actual API call
            // const workspaces = await apiGet('/api/user/workspaces');
            // renderWorkspaces(workspaces);
        }
    } catch (error) {
        console.error('Failed to load workspaces:', error);
    }
}

function renderWorkspaces(workspaces) {
    const container = document.getElementById('workspacesList');
    if (!container) return;

    if (!workspaces || workspaces.length === 0) {
        container.innerHTML = '<div class="empty-state">No workspaces yet. Create one or accept an invitation!</div>';
        return;
    }

    document.getElementById('workspaceCount').textContent = workspaces.length;

    container.innerHTML = workspaces.map(ws => `
        <div class="workspace-item">
            <div class="workspace-info">
                <h3>${escapeHtml(ws.name)}</h3>
                <p>${escapeHtml(ws.description) || 'No description'}</p>
            </div>
            <div class="workspace-actions">
                <button class="btn-outline btn-small" onclick="openWorkspace(${ws.id})">Open</button>
                <button class="btn-outline btn-small" onclick="showInviteModal(${ws.id}, '${escapeHtml(ws.name)}')">Invite</button>
            </div>
        </div>
    `).join('');
}

function openWorkspace(workspaceId) {
    // Redirect to desktop app or show workspace details
    alert(`Opening workspace ${workspaceId} in VoxLink desktop app`);
}

// ============================================
// Invite Functions
// ============================================
async function validateInviteCode() {
    const urlParams = new URLSearchParams(window.location.search);
    const inviteCode = urlParams.get('code') || extractInviteCodeFromPath();

    if (!inviteCode) return;

    state.currentInviteCode = inviteCode;

    try {
        const result = await apiGet(`/api/validate-invite?code=${inviteCode}`);

        document.getElementById('loadingState').style.display = 'none';

        if (result.valid) {
            document.getElementById('validInviteState').style.display = 'block';
            document.getElementById('workspaceName').textContent = result.workspaceName;
            document.getElementById('workspaceDescription').textContent = result.description || 'No description';
            document.getElementById('memberCount').textContent = result.memberCount + ' members';
            document.getElementById('workspaceIcon').innerHTML = result.workspaceName.charAt(0).toUpperCase();

            if (state.token) {
                document.getElementById('acceptInviteBtn').style.display = 'block';
                document.querySelector('#authButtons .btn-outline').style.display = 'none';
            } else {
                document.getElementById('acceptInviteBtn').style.display = 'none';
                document.querySelector('#authButtons .btn-outline').style.display = 'inline-block';
            }
        } else {
            document.getElementById('errorState').style.display = 'block';
            document.getElementById('errorMessage').textContent = result.error || 'Invite link is invalid or has expired';
        }
    } catch (error) {
        document.getElementById('loadingState').style.display = 'none';
        document.getElementById('errorState').style.display = 'block';
        document.getElementById('errorMessage').textContent = 'Unable to validate invite. Please try again.';
    }
}

function extractInviteCodeFromPath() {
    const path = window.location.pathname;
    if (path.startsWith('/invite/')) {
        return path.substring(8);
    }
    return null;
}

async function acceptInvite() {
    if (!state.token) {
        window.location.href = `/login?invite=${state.currentInviteCode}`;
        return;
    }

    try {
        const result = await apiPost('/api/join', {
            inviteCode: state.currentInviteCode,
            token: state.token
        });

        if (result.success) {
            document.getElementById('acceptInviteBtn').style.display = 'none';
            document.getElementById('joinedState').style.display = 'block';
        } else {
            alert('Failed to join workspace: ' + (result.message || 'Unknown error'));
        }
    } catch (error) {
        alert('Error joining workspace: ' + error.message);
    }
}

// ============================================
// Create Workspace Modal
// ============================================
function showCreateWorkspaceModal() {
    const modal = document.getElementById('createWorkspaceModal');
    if (modal) {
        modal.classList.add('show');
        document.getElementById('workspaceName').value = '';
        document.getElementById('workspaceDescription').value = '';
        document.getElementById('workspacePublic').checked = false;
    }
}

function hideCreateWorkspaceModal() {
    const modal = document.getElementById('createWorkspaceModal');
    if (modal) modal.classList.remove('show');
}

async function createWorkspace() {
    const name = document.getElementById('workspaceName').value.trim();
    const description = document.getElementById('workspaceDescription').value.trim();
    const isPublic = document.getElementById('workspacePublic').checked;

    if (!name) {
        alert('Please enter a workspace name');
        return;
    }

    try {
        // TODO: Implement create workspace API call
        // const result = await apiPost('/api/workspaces', { name, description, isPublic });
        alert('Workspace creation will be implemented soon');
        hideCreateWorkspaceModal();
        loadUserWorkspaces();
    } catch (error) {
        alert('Failed to create workspace: ' + error.message);
    }
}

// ============================================
// Invite Modal
// ============================================
function showInviteModal(workspaceId, workspaceName) {
    state.currentWorkspaceId = workspaceId;
    document.getElementById('inviteWorkspaceName').textContent = workspaceName;
    document.getElementById('inviteModal').classList.add('show');
    generateInviteLink();
}

function hideInviteModal() {
    document.getElementById('inviteModal').classList.remove('show');
}

async function generateInviteLink() {
    const expiresDays = parseInt(document.getElementById('expiryDays').value);
    const maxUses = parseInt(document.getElementById('maxUses').value);

    try {
        // TODO: Implement generate invite API call
        // const result = await apiPost('/api/invites', {
        //     workspaceId: state.currentWorkspaceId,
        //     expiresDays: expiresDays,
        //     maxUses: maxUses
        // });
        // const inviteUrl = `${window.location.origin}/invite/${result.inviteCode}`;
        // document.getElementById('inviteLink').value = inviteUrl;

        const dummyCode = 'ABCDEF123456';
        const inviteUrl = `${window.location.origin}/invite/${dummyCode}`;
        document.getElementById('inviteLink').value = inviteUrl;
    } catch (error) {
        alert('Failed to generate invite link: ' + error.message);
    }
}

function copyInviteLink() {
    const input = document.getElementById('inviteLink');
    input.select();
    document.execCommand('copy');
    alert('Invite link copied to clipboard!');
}

// ============================================
// Login/Register Functions
// ============================================
async function handleLogin() {
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;

    if (!username || !password) {
        showError('loginError', 'Please enter username and password');
        return;
    }

    try {
        // TODO: Implement login API call
        // const result = await apiPost('/api/login', { username, password });

        // For demo, simulate successful login
        const result = { success: true, token: 'demo-token', username: username };

        if (result.success) {
            state.token = result.token;
            state.username = result.username;
            localStorage.setItem('voxlink_token', result.token);
            localStorage.setItem('voxlink_username', result.username);

            const inviteCode = new URLSearchParams(window.location.search).get('invite');
            if (inviteCode) {
                window.location.href = `/invite/${inviteCode}`;
            } else {
                window.location.href = '/dashboard';
            }
        } else {
            showError('loginError', result.message || 'Login failed');
        }
    } catch (error) {
        showError('loginError', 'Login failed. Please try again.');
    }
}

async function handleRegister() {
    const username = document.getElementById('regUsername').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const displayName = document.getElementById('regDisplayName').value.trim();
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;

    if (!username || !email || !password) {
        showError('registerError', 'Please fill in all required fields');
        return;
    }

    if (password !== confirmPassword) {
        showError('registerError', 'Passwords do not match');
        return;
    }

    if (password.length < 6) {
        showError('registerError', 'Password must be at least 6 characters');
        return;
    }

    try {
        // TODO: Implement register API call
        // const result = await apiPost('/api/register', { username, email, displayName, password });

        // For demo, simulate successful registration
        const result = { success: true };

        if (result.success) {
            showSuccess('registerSuccess', 'Account created successfully! Please login.');
            setTimeout(() => {
                document.getElementById('showLoginBtn').click();
            }, 2000);
        } else {
            showError('registerError', result.message || 'Registration failed');
        }
    } catch (error) {
        showError('registerError', 'Registration failed. Please try again.');
    }
}

function handleLogout() {
    localStorage.removeItem('voxlink_token');
    localStorage.removeItem('voxlink_username');
    state.token = null;
    state.username = null;
    window.location.href = '/';
}

// ============================================
// Page Initialization
// ============================================
function initPage() {
    const path = window.location.pathname;

    // Statistics page
    if (path === '/stats') {
        loadStatistics();
        setInterval(loadStatistics, 30000);

        const refreshBtn = document.getElementById('refreshBtn');
        if (refreshBtn) {
            refreshBtn.addEventListener('click', loadStatistics);
        }
    }

    // Dashboard page
    if (path === '/dashboard') {
        loadDashboard();

        const createBtn = document.getElementById('createWorkspaceBtn');
        if (createBtn) {
            createBtn.addEventListener('click', showCreateWorkspaceModal);
        }

        const joinBtn = document.getElementById('joinWorkspaceBtn');
        if (joinBtn) {
            joinBtn.addEventListener('click', () => {
                const inviteCode = document.getElementById('inviteCodeInput').value.trim();
                if (inviteCode) {
                    window.location.href = `/invite/${inviteCode}`;
                } else {
                    document.getElementById('inviteMessage').textContent = 'Please enter an invite code';
                }
            });
        }
    }

    // Login/Register page
    if (path === '/login') {
        const showRegisterBtn = document.getElementById('showRegisterBtn');
        const showLoginBtn = document.getElementById('showLoginBtn');
        const loginBtn = document.getElementById('loginBtn');
        const registerBtn = document.getElementById('registerBtn');

        if (showRegisterBtn) {
            showRegisterBtn.addEventListener('click', (e) => {
                e.preventDefault();
                document.getElementById('loginForm').style.display = 'none';
                document.getElementById('registerForm').style.display = 'block';
            });
        }

        if (showLoginBtn) {
            showLoginBtn.addEventListener('click', (e) => {
                e.preventDefault();
                document.getElementById('registerForm').style.display = 'none';
                document.getElementById('loginForm').style.display = 'block';
            });
        }

        if (loginBtn) loginBtn.addEventListener('click', handleLogin);
        if (registerBtn) registerBtn.addEventListener('click', handleRegister);

        const inviteCode = new URLSearchParams(window.location.search).get('invite');
        if (inviteCode) {
            document.getElementById('inviteInfo').style.display = 'block';
        }
    }

    // Invite page
    if (path.startsWith('/invite/')) {
        validateInviteCode()

        const acceptBtn = document.getElementById('acceptInviteBtn');
        if (acceptBtn) {
            acceptBtn.addEventListener('click', acceptInvite);
        }
    }

    // Modal close handlers
    const modals = document.querySelectorAll('.modal');
    const closeButtons = document.querySelectorAll('.modal-close');

    closeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            btn.closest('.modal').classList.remove('show');
        });
    });

    modals.forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                modal.classList.remove('show');
            }
        });
    });

    const cancelCreateBtn = document.getElementById('cancelCreateBtn');
    if (cancelCreateBtn) {
        cancelCreateBtn.addEventListener('click', hideCreateWorkspaceModal);
    }

    const confirmCreateBtn = document.getElementById('confirmCreateBtn');
    if (confirmCreateBtn) {
        confirmCreateBtn.addEventListener('click', createWorkspace);
    }

    const copyInviteBtn = document.getElementById('copyInviteBtn');
    if (copyInviteBtn) {
        copyInviteBtn.addEventListener('click', copyInviteLink);
    }

    const generateInviteBtn = document.getElementById('generateInviteBtn');
    if (generateInviteBtn) {
        generateInviteBtn.addEventListener('click', generateInviteLink);
    }

    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            handleLogout();
        });
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', initPage);