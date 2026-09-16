/**
 * ShikshaPlus Support AI - Official Enterprise Client Script
 * Built with Vanilla JavaScript
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const chatForm = document.getElementById('chatForm');
  const messageInput = document.getElementById('messageInput');
  const sendBtn = document.getElementById('sendBtn');
  const messagesContainer = document.getElementById('messagesContainer');
  const chatBody = document.getElementById('chatBody');
  const typingIndicator = document.getElementById('typingIndicator');
  const clearChatBtn = document.getElementById('clearChatBtn');
  const welcomeCard = document.getElementById('welcomeCard');
  const connectionBanner = document.getElementById('connectionBanner');
  const bannerUrl = document.getElementById('bannerUrl');
  const retryConnectionBtn = document.getElementById('retryConnectionBtn');
  const statusIndicator = document.getElementById('statusIndicator');

  // Theme Toggle Elements
  const themeToggleBtn = document.getElementById('themeToggleBtn');
  const sunIcon = document.getElementById('sunIcon');
  const moonIcon = document.getElementById('moonIcon');
  const brandLogoDark = document.getElementById('brandLogoDark');
  const brandLogoLight = document.getElementById('brandLogoLight');

  // Settings Modal Elements
  const settingsBtn = document.getElementById('settingsBtn');
  const settingsModal = document.getElementById('settingsModal');
  const closeSettingsBtn = document.getElementById('closeSettingsBtn');
  const backendUrlInput = document.getElementById('backendUrlInput');
  const saveSettingsBtn = document.getElementById('saveSettingsBtn');
  const resetSettingsBtn = document.getElementById('resetSettingsBtn');

  // Configure Backend URL
  const defaultBackendUrl = window.location.origin.startsWith('http') && !window.location.origin.includes('file:')
    ? window.location.origin
    : 'http://localhost:8080';

  let backendUrl = localStorage.getItem('shikshaplus_backend_url') || defaultBackendUrl;
  let isSending = false;

  backendUrlInput.value = backendUrl;
  bannerUrl.textContent = backendUrl;

  // --------------------------------------------------------------------------
  // Theme Management (Defaults to Official Obsidian Dark Mode)
  // --------------------------------------------------------------------------
  function initTheme() {
    const savedTheme = localStorage.getItem('shikshaplus_theme') || 'dark';
    applyTheme(savedTheme === 'dark');
  }

  function applyTheme(isDark) {
    if (isDark) {
      document.documentElement.classList.add('dark');
      if (sunIcon) sunIcon.classList.remove('hidden');
      if (moonIcon) moonIcon.classList.add('hidden');
      if (brandLogoDark) brandLogoDark.classList.remove('hidden');
      if (brandLogoLight) brandLogoLight.classList.add('hidden');
      localStorage.setItem('shikshaplus_theme', 'dark');
    } else {
      document.documentElement.classList.remove('dark');
      if (sunIcon) sunIcon.classList.add('hidden');
      if (moonIcon) moonIcon.classList.remove('hidden');
      if (brandLogoDark) brandLogoDark.classList.add('hidden');
      if (brandLogoLight) brandLogoLight.classList.remove('hidden');
      localStorage.setItem('shikshaplus_theme', 'light');
    }
  }

  themeToggleBtn.addEventListener('click', () => {
    const isDark = document.documentElement.classList.contains('dark');
    applyTheme(!isDark);
  });

  initTheme();

  // Test backend connectivity
  checkBackendHealth();

  // Auto-resize input textarea
  messageInput.addEventListener('input', () => {
    messageInput.style.height = 'auto';
    const newHeight = Math.min(messageInput.scrollHeight, 140);
    messageInput.style.height = newHeight + 'px';
  });

  // Handle Enter key (Enter sends, Shift+Enter adds newline)
  messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      if (!isSending && messageInput.value.trim().length > 0) {
        chatForm.dispatchEvent(new Event('submit', { cancelable: true }));
      }
    }
  });

  // Quick FAQ Chips click
  document.querySelectorAll('.chip').forEach(chip => {
    chip.addEventListener('click', () => {
      const query = chip.getAttribute('data-query');
      if (query && !isSending) {
        sendMessage(query);
      }
    });
  });

  // Form submission
  chatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const text = messageInput.value.trim();
    if (!text || isSending) return;
    sendMessage(text);
  });

  // Clear Chat / New Conversation
  clearChatBtn.addEventListener('click', async () => {
    if (confirm('Start a new chat and reset conversation history?')) {
      messagesContainer.innerHTML = '';
      if (welcomeCard) {
        welcomeCard.classList.remove('hidden');
      }
      
      try {
        await fetch(`${backendUrl}/reset`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        });
      } catch (err) {
        console.warn('Reset request failed:', err);
      }
    }
  });

  // Settings Modal controls
  settingsBtn.addEventListener('click', () => {
    settingsModal.classList.toggle('hidden');
  });

  closeSettingsBtn.addEventListener('click', () => {
    settingsModal.classList.add('hidden');
  });

  saveSettingsBtn.addEventListener('click', () => {
    let customUrl = backendUrlInput.value.trim().replace(/\/+$/, '');
    if (!customUrl) customUrl = defaultBackendUrl;
    backendUrl = customUrl;
    localStorage.setItem('shikshaplus_backend_url', backendUrl);
    bannerUrl.textContent = backendUrl;
    settingsModal.classList.add('hidden');
    checkBackendHealth();
  });

  resetSettingsBtn.addEventListener('click', () => {
    backendUrl = defaultBackendUrl;
    localStorage.removeItem('shikshaplus_backend_url');
    backendUrlInput.value = backendUrl;
    bannerUrl.textContent = backendUrl;
    settingsModal.classList.add('hidden');
    checkBackendHealth();
  });

  retryConnectionBtn.addEventListener('click', () => {
    checkBackendHealth();
  });

  /**
   * Main function to send message to the backend
   */
  async function sendMessage(text) {
    isSending = true;
    updateSendButtonState(true);

    if (welcomeCard) {
      welcomeCard.classList.add('hidden');
    }

    appendMessage(text, 'user');
    messageInput.value = '';
    messageInput.style.height = 'auto';

    showTypingIndicator();
    scrollToBottom();

    try {
      const response = await fetch(`${backendUrl}/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain;charset=UTF-8',
          'Accept': 'text/plain, application/json, */*'
        },
        body: text
      });

      if (!response.ok) {
        throw new Error(`Server returned HTTP ${response.status}: ${response.statusText}`);
      }

      const contentType = response.headers.get('content-type') || '';
      let reply = '';
      if (contentType.includes('application/json')) {
        const data = await response.json();
        reply = data.reply || data.response || JSON.stringify(data);
      } else {
        reply = await response.text();
      }

      hideTypingIndicator();
      appendMessage(reply, 'bot');
      hideConnectionBanner();

    } catch (error) {
      console.error('Chat error:', error);
      hideTypingIndicator();

      const errorMsg = `⚠️ **Could not reach ShikshaPlus AI engine**.\n\nPlease verify that Spring Boot is running at \`${backendUrl}\`.\n\n*Details:* ${error.message}`;
      appendMessage(errorMsg, 'bot', true);
      showConnectionBanner();
    } finally {
      isSending = false;
      updateSendButtonState(false);
      messageInput.focus();
      scrollToBottom();
    }
  }

  /**
   * Append message bubble with 3D squircle avatar
   */
  function appendMessage(content, sender, isError = false) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}`;

    const timeString = formatTime(new Date());

    // Avatar
    const avatarDiv = document.createElement('div');
    avatarDiv.className = 'message-avatar';
    if (sender === 'bot') {
      const isDark = document.documentElement.classList.contains('dark');
      const iconPath = isDark ? 'assets/icons/shikshaplus_app_icon_dark.png' : 'assets/icons/shikshaplus_app_icon_light.png';
      avatarDiv.innerHTML = `<img src="${iconPath}" alt="ShikshaPlus AI" />`;
    } else {
      avatarDiv.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
          <circle cx="12" cy="7" r="4"></circle>
        </svg>
      `;
    }

    // Message Bubble
    const bubbleDiv = document.createElement('div');
    bubbleDiv.className = 'message-bubble';
    if (isError) {
      bubbleDiv.style.borderColor = 'rgba(239, 68, 68, 0.4)';
      bubbleDiv.style.background = 'rgba(239, 68, 68, 0.12)';
      bubbleDiv.style.color = '#F87171';
    }

    // Format content with markdown
    const formattedHtml = parseSimpleMarkdown(content);
    bubbleDiv.innerHTML = formattedHtml;

    // Meta (timestamp & copy button)
    const metaDiv = document.createElement('div');
    metaDiv.className = 'message-meta';

    const timeSpan = document.createElement('span');
    timeSpan.textContent = timeString;
    metaDiv.appendChild(timeSpan);

    if (sender === 'bot') {
      const copyBtn = document.createElement('button');
      copyBtn.className = 'copy-btn';
      copyBtn.title = 'Copy response';
      copyBtn.innerHTML = `
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
          <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
        </svg>
      `;
      copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(content).then(() => {
          copyBtn.innerHTML = `
            <svg viewBox="0 0 24 24" fill="none" stroke="#10B981" stroke-width="2.5">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          `;
          setTimeout(() => {
            copyBtn.innerHTML = `
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
              </svg>
            `;
          }, 2000);
        });
      });
      metaDiv.appendChild(copyBtn);
    }

    bubbleDiv.appendChild(metaDiv);

    messageDiv.appendChild(avatarDiv);
    messageDiv.appendChild(bubbleDiv);
    messagesContainer.appendChild(messageDiv);
    scrollToBottom();
  }

  /**
   * Lightweight markdown-like formatter
   */
  function parseSimpleMarkdown(text) {
    if (!text) return '';

    let sanitized = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Code blocks
    sanitized = sanitized.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');

    // Inline code
    sanitized = sanitized.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Bold
    sanitized = sanitized.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    sanitized = sanitized.replace(/__([^_]+)__/g, '<strong>$1</strong>');

    // Italic
    sanitized = sanitized.replace(/(^|[^*])\*([^*]+)\*([^*]|$)/g, '$1<em>$2</em>$3');

    // URLs
    sanitized = sanitized.replace(
      /(https?:\/\/[^\s]+)/g,
      '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
    );

    // Lists
    const lines = sanitized.split('\n');
    let inList = false;
    let result = '';

    for (let i = 0; i < lines.length; i++) {
      const line = lines[i].trim();
      if (/^[-*]\s+/.test(line)) {
        if (!inList) {
          result += '<ul>';
          inList = true;
        }
        result += `<li>${line.replace(/^[-*]\s+/, '')}</li>`;
      } else if (/^\d+\.\s+/.test(line)) {
        if (!inList) {
          result += '<ol>';
          inList = true;
        }
        result += `<li>${line.replace(/^\d+\.\s+/, '')}</li>`;
      } else {
        if (inList) {
          result += '</ul>';
          inList = false;
        }
        if (line.length > 0) {
          result += `<p>${line}</p>`;
        }
      }
    }

    if (inList) result += '</ul>';

    return result || sanitized;
  }

  function showTypingIndicator() {
    typingIndicator.classList.remove('hidden');
    messagesContainer.appendChild(typingIndicator);
  }

  function hideTypingIndicator() {
    typingIndicator.classList.add('hidden');
  }

  function scrollToBottom() {
    chatBody.scrollTop = chatBody.scrollHeight;
  }

  function updateSendButtonState(disabled) {
    sendBtn.disabled = disabled;
    messageInput.disabled = disabled;
  }

  function formatTime(date) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  async function checkBackendHealth() {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 4000);

      const res = await fetch(`${backendUrl}/status`, {
        method: 'GET',
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        hideConnectionBanner();
        statusIndicator.textContent = 'Engine Online • 24/7';
      } else {
        statusIndicator.textContent = 'Engine Active';
      }
    } catch (err) {
      console.warn('Backend ping check failed:', err);
    }
  }

  function showConnectionBanner() {
    connectionBanner.classList.remove('hidden');
    statusIndicator.textContent = 'Engine Offline';
  }

  function hideConnectionBanner() {
    connectionBanner.classList.add('hidden');
    statusIndicator.textContent = 'Engine Online • 24/7';
  }
});
