/**
 * KORTEX AI - Minimalist Frontend Controller
 * - Dynamic selective tool execution & token savings tracking
 * - Real-time SSE stream reader
 * - Custom markdown & tool telemetry rendering
 * - Vector SVG Icon System (Zero Emojis)
 * - Refined palette & modern rounded geometry
 */

(() => {
  'use strict';

  // --- SVG Icon Registry ---
  const ICONS = {
    search: `<svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>`,
    globe: `<svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>`,
    web_search: `<svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>`,
    calculator: `<svg class="icon icon-sm" viewBox="0 0 24 24"><rect x="4" y="2" width="16" height="20" rx="2"></rect><line x1="8" y1="6" x2="16" y2="6"></line><line x1="16" y1="14" x2="16" y2="18"></line><path d="M16 10h.01"></path><path d="M12 10h.01"></path><path d="M8 10h.01"></path><path d="M12 14h.01"></path><path d="M8 14h.01"></path><path d="M12 18h.01"></path><path d="M8 18h.01"></path></svg>`,
    weather: `<svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M12 2v2"></path><path d="m4.93 4.93 1.41 1.41"></path><path d="M20 12h2"></path><path d="m19.07 4.93-1.41 1.41"></path><path d="M15.947 12.65a4 4 0 0 0-5.925-4.128"></path><path d="M13 22H7a5 5 0 1 1 4.9-6H13a3 3 0 0 1 0 6Z"></path></svg>`,
    currency: `<svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="8" cy="8" r="6"></circle><path d="M18.09 10.37A6 6 0 1 1 10.34 18"></path><path d="M7 6h1.5a1 1 0 0 1 0 2H7"></path><path d="M8 5v5"></path></svg>`,
    terminal: `<svg class="icon icon-sm" viewBox="0 0 24 24"><polyline points="4 17 10 11 4 5"></polyline><line x1="12" y1="19" x2="20" y2="19"></line></svg>`,
    shield: `<svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>`,
    sun: `<svg class="icon icon-sm" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"></circle><path d="M12 2v2"></path><path d="M12 20v2"></path><path d="m4.93 4.93 1.41 1.41"></path><path d="m17.66 17.66 1.41 1.41"></path><path d="M2 12h2"></path><path d="M20 12h2"></path><path d="m6.34 17.66-1.41 1.41"></path><path d="m19.07 4.93-1.41 1.41"></path></svg>`,
    moon: `<svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"></path></svg>`,
    zap: `<svg class="icon icon-sm" viewBox="0 0 24 24"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>`,
    copy: `<svg class="icon icon-sm" viewBox="0 0 24 24"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>`,
    check: `<svg class="icon icon-sm" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"></polyline></svg>`,
    cpu: `<svg class="icon icon-sm" viewBox="0 0 24 24"><rect x="4" y="4" width="16" height="16" rx="2"></rect><rect x="9" y="9" width="6" height="6"></rect><path d="M15 2v2"></path><path d="M15 20v2"></path><path d="M2 15h2"></path><path d="M2 9h2"></path><path d="M20 15h2"></path><path d="M20 9h2"></path><path d="M9 2v2"></path><path d="M9 20v2"></path></svg>`
  };

  // --- State Management ---
  const state = {
    activeTools: new Set(['search', 'calculator', 'weather']),
    availableTools: [
      { id: 'search', name: 'Web Search', icon: 'globe', estimatedTokenCost: 400, enabledByDefault: true },
      { id: 'calculator', name: 'Calculator', icon: 'calculator', estimatedTokenCost: 300, enabledByDefault: true },
      { id: 'weather', name: 'Weather', icon: 'weather', estimatedTokenCost: 350, enabledByDefault: true },
      { id: 'currency', name: 'Currency', icon: 'currency', estimatedTokenCost: 350, enabledByDefault: false },
      { id: 'terminal', name: 'Terminal', icon: 'terminal', estimatedTokenCost: 400, enabledByDefault: false }
    ],
    totalCumulativeSavedTokens: 0,
    currentAbortController: null,
    isGenerating: false,
    conversations: [],
    activeConversationId: null,
    settings: {
      apiKey: '',
      baseUrl: '',
      model: 'gpt-4o-mini',
      theme: 'dark'
    }
  };

  // --- DOM Elements ---
  const elements = {
    sidebar: document.getElementById('sidebar'),
    toggleSidebarBtn: document.getElementById('toggle-sidebar-btn'),
    newChatBtn: document.getElementById('new-chat-btn'),
    chatHistoryList: document.getElementById('chat-history-list'),
    toolsPillsGroup: document.getElementById('tools-pills-group'),
    activeToolsHint: document.getElementById('active-tools-hint'),
    headerSavedText: document.getElementById('header-saved-text'),
    sidebarSavedTokens: document.getElementById('sidebar-saved-tokens'),
    messagesContainer: document.getElementById('messages-container'),
    messagesInner: document.getElementById('messages-inner'),
    chatTextarea: document.getElementById('chat-textarea'),
    sendBtn: document.getElementById('send-btn'),
    stopBtn: document.getElementById('stop-btn'),
    themeToggleBtn: document.getElementById('theme-toggle-btn'),
    settingsBtn: document.getElementById('settings-btn'),
    settingsModal: document.getElementById('settings-modal'),
    closeSettingsBtn: document.getElementById('close-settings-btn'),
    saveSettingsBtn: document.getElementById('save-settings-btn'),
    clearChatBtn: document.getElementById('clear-chat-btn'),
    statusIndicator: document.getElementById('status-indicator'),
    inputApiKey: document.getElementById('input-api-key'),
    inputBaseUrl: document.getElementById('input-base-url'),
    inputModel: document.getElementById('input-model'),
    suggestionsContainer: document.getElementById('prompt-suggestions')
  };

  // --- Initialization ---
  function init() {
    loadSettings();
    loadConversations();
    fetchToolsMetadata();
    fetchSystemStatus();
    setupEventListeners();
    updateTokenSavingsUI();
    applyTheme(state.settings.theme);

    if (state.conversations.length === 0) {
      createNewConversation();
    } else {
      selectConversation(state.conversations[0].id);
    }
  }

  // --- Settings Persistence ---
  function loadSettings() {
    try {
      const saved = localStorage.getItem('kortex_settings');
      if (saved) {
        state.settings = { ...state.settings, ...JSON.parse(saved) };
      }
      const savedTokens = localStorage.getItem('kortex_cumulative_saved');
      if (savedTokens) {
        state.totalCumulativeSavedTokens = parseInt(savedTokens, 10) || 0;
      }
    } catch (e) {
      console.warn('Could not load stored settings', e);
    }

    elements.inputApiKey.value = state.settings.apiKey || '';
    elements.inputBaseUrl.value = state.settings.baseUrl || '';
    elements.inputModel.value = state.settings.model || 'gpt-4o-mini';
  }

  function saveSettings() {
    state.settings.apiKey = elements.inputApiKey.value.trim();
    state.settings.baseUrl = elements.inputBaseUrl.value.trim();
    state.settings.model = elements.inputModel.value.trim() || 'gpt-4o-mini';

    localStorage.setItem('kortex_settings', JSON.stringify(state.settings));
    elements.settingsModal.classList.remove('open');

    updateStatusBadge();
  }

  async function fetchSystemStatus() {
    try {
      const res = await fetch('/api/status');
      if (res.ok) {
        const data = await res.json();
        if (data.apiKeyConfigured) {
          state.backendKeyConfigured = true;
          state.backendModel = data.activeModel || 'openrouter/free';
          updateStatusBadge();
        }
      }
    } catch (e) {
      console.warn('Could not fetch system status:', e);
    }
  }

  function updateStatusBadge() {
    const dot = elements.statusIndicator.querySelector('.status-dot');
    const label = elements.statusIndicator.querySelector('.status-label');
    if (state.settings.apiKey && state.settings.apiKey.length > 5) {
      dot.classList.remove('sim');
      label.textContent = state.settings.model;
    } else if (state.backendKeyConfigured) {
      dot.classList.remove('sim');
      label.textContent = state.backendModel || 'OpenRouter';
    } else {
      dot.classList.add('sim');
      label.textContent = 'Demo Mode (Zero-Token Live Tools)';
    }
  }

  // --- Theme ---
  function applyTheme(theme) {
    state.settings.theme = theme;
    document.documentElement.setAttribute('data-theme', theme);
    elements.themeToggleBtn.innerHTML = theme === 'dark' ? ICONS.sun : ICONS.moon;
    localStorage.setItem('kortex_settings', JSON.stringify(state.settings));
  }

  function toggleTheme() {
    applyTheme(state.settings.theme === 'dark' ? 'light' : 'dark');
  }

  // --- Dynamic Tools Fetch & Rendering with SVG Icons ---
  async function fetchToolsMetadata() {
    try {
      const res = await fetch('/api/tools');
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          state.availableTools = data;
        }
      }
    } catch (e) {
      console.warn('Using default tool definitions:', e);
    }
    renderToolPills();
  }

  function renderToolPills() {
    elements.toolsPillsGroup.innerHTML = '';

    state.availableTools.forEach(tool => {
      const pill = document.createElement('div');
      pill.className = `tool-pill ${state.activeTools.has(tool.id) ? 'active' : ''}`;
      pill.dataset.toolId = tool.id;

      const svgIcon = ICONS[tool.id] || ICONS.zap;

      pill.innerHTML = `
        <span class="pill-dot"></span>
        <span class="tool-icon">${svgIcon}</span>
        <span class="tool-name">${tool.name}</span>
        <span class="token-cost">+${tool.estimatedTokenCost}t</span>
      `;

      pill.addEventListener('click', () => toggleTool(tool.id));
      elements.toolsPillsGroup.appendChild(pill);
    });

    updateTokenSavingsUI();
  }

  function toggleTool(toolId) {
    if (state.activeTools.has(toolId)) {
      state.activeTools.delete(toolId);
    } else {
      state.activeTools.add(toolId);
    }
    renderToolPills();
  }

  function updateTokenSavingsUI() {
    let activeCost = 0;
    let totalCost = 0;

    state.availableTools.forEach(t => {
      totalCost += t.estimatedTokenCost;
      if (state.activeTools.has(t.id)) {
        activeCost += t.estimatedTokenCost;
      }
    });

    const savedPerRequest = totalCost - activeCost;
    elements.activeToolsHint.innerHTML = `Active Tools: <strong>${state.activeTools.size}/${state.availableTools.length}</strong> | Saved <strong>~${savedPerRequest} tokens</strong> / req`;
    elements.headerSavedText.textContent = `~${savedPerRequest}t saved/req`;
    elements.sidebarSavedTokens.textContent = state.totalCumulativeSavedTokens.toLocaleString();
  }

  // --- Conversations Management ---
  function loadConversations() {
    try {
      const saved = localStorage.getItem('kortex_conversations');
      if (saved) {
        state.conversations = JSON.parse(saved);
        renderHistoryList();
      }
    } catch (e) {
      console.warn('Could not load conversations', e);
    }
  }

  function saveConversations() {
    try {
      localStorage.setItem('kortex_conversations', JSON.stringify(state.conversations));
    } catch (e) {
      console.warn('Could not save conversations', e);
    }
  }

  function createNewConversation() {
    const id = 'conv_' + Date.now();
    const newConv = {
      id,
      title: 'New Conversation',
      messages: [],
      timestamp: Date.now()
    };
    state.conversations.unshift(newConv);
    saveConversations();
    renderHistoryList();
    selectConversation(id);
  }

  function selectConversation(id) {
    state.activeConversationId = id;
    renderHistoryList();
    renderActiveConversation();
  }

  function getCurrentConversation() {
    return state.conversations.find(c => c.id === state.activeConversationId);
  }

  function renderHistoryList() {
    elements.chatHistoryList.innerHTML = '';
    state.conversations.forEach(conv => {
      const li = document.createElement('li');
      li.className = `history-item ${conv.id === state.activeConversationId ? 'active' : ''}`;
      li.innerHTML = `
        <svg class="icon icon-sm" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>
        <span style="overflow:hidden;text-overflow:ellipsis;">${escapeHTML(conv.title || 'Conversation')}</span>
      `;
      li.addEventListener('click', () => selectConversation(conv.id));
      elements.chatHistoryList.appendChild(li);
    });
  }

  function renderActiveConversation() {
    elements.messagesInner.innerHTML = '';
    const conv = getCurrentConversation();
    if (!conv || conv.messages.length === 0) {
      renderWelcomeScreen();
      return;
    }

    conv.messages.forEach(msg => {
      appendMessageToDOM(msg.role, msg.content, msg.telemetry, msg.metrics, false);
    });
    scrollToBottom();
  }

  function renderWelcomeScreen() {
    const welcome = document.createElement('div');
    welcome.className = 'message-row bot';
    welcome.innerHTML = `
      <div class="message-meta">KORTEX AI · ASSISTANT</div>
      <div class="message-bubble">
        <h2 style="margin-top:0;display:flex;align-items:center;gap:8px;">
          ${ICONS.cpu}
          Welcome to Kortex
        </h2>
        <p>A fast, minimalist AI assistant engineered for streaming performance and dynamic tool execution.</p>
        <p><strong>Token Efficiency Protocol:</strong> Tools are not statically injected into prompts. Toggle the tool pills at the bottom to attach only what you need, saving hundreds of prompt tokens on every message.</p>
      </div>
    `;
    elements.messagesInner.appendChild(welcome);
  }

  // --- Message Sending & SSE Stream Handling ---
  async function sendMessage() {
    const text = elements.chatTextarea.value.trim();
    if (!text || state.isGenerating) return;

    elements.chatTextarea.value = '';
    autoResizeTextarea();

    const conv = getCurrentConversation();
    if (!conv) return;

    // Set title on first message
    if (conv.messages.length === 0) {
      conv.title = text.length > 28 ? text.substring(0, 28) + '...' : text;
      renderHistoryList();
    }

    // Append user message
    conv.messages.push({ role: 'user', content: text });
    saveConversations();
    appendMessageToDOM('user', text);
    scrollToBottom();

    // Prepare Bot Row
    state.isGenerating = true;
    setGeneratingUI(true);

    const botRow = createBotMessageElement();
    elements.messagesInner.appendChild(botRow);
    const bubbleContent = botRow.querySelector('.bubble-text');
    const cursor = botRow.querySelector('.streaming-cursor');
    const telemetryContainer = botRow.querySelector('.telemetry-container');
    const metricsContainer = botRow.querySelector('.metrics-hud');

    scrollToBottom();

    let fullBotText = '';
    let telemetryEvents = [];
    let metricsData = null;

    state.currentAbortController = new AbortController();

    try {
      const payload = {
        message: text,
        conversationId: state.activeConversationId,
        activeTools: Array.from(state.activeTools),
        apiKey: state.settings.apiKey,
        baseUrl: state.settings.baseUrl,
        model: state.settings.model
      };

      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal: state.currentAbortController.signal
      });

      if (!response.ok) {
        throw new Error(`HTTP Error ${response.status}: ${response.statusText}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop(); // Keep partial line in buffer

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const jsonStr = line.substring(5).trim();
            if (!jsonStr) continue;

            try {
              const event = JSON.parse(jsonStr);
              handleStreamEvent(event, {
                onToken: (token) => {
                  fullBotText += token;
                  bubbleContent.innerHTML = formatMarkdown(fullBotText);
                  highlightCodeBlocks(bubbleContent);
                  scrollToBottom();
                },
                onToolCall: (name, args) => {
                  const card = document.createElement('div');
                  card.className = 'tool-telemetry-card';
                  card.id = `tool-card-${name}`;
                  const icon = ICONS[name] || ICONS.zap;
                  card.innerHTML = `
                    <div class="tool-telemetry-header">
                      <span class="tool-tag">${icon} TOOL CALL: <strong>${name}</strong></span>
                      <span class="tool-status-badge">Executing...</span>
                    </div>
                    <div class="tool-telemetry-body">${escapeHTML(args)}</div>
                  `;
                  telemetryContainer.appendChild(card);
                  telemetryEvents.push({ type: 'call', name, args });
                  scrollToBottom();
                },
                onToolResult: (name, result) => {
                  const card = document.getElementById(`tool-card-${name}`);
                  if (card) {
                    card.querySelector('.tool-status-badge').textContent = 'Completed';
                    card.querySelector('.tool-status-badge').style.borderColor = 'var(--accent-green)';
                    card.querySelector('.tool-telemetry-body').textContent = result;
                  }
                  telemetryEvents.push({ type: 'result', name, result });
                  scrollToBottom();
                },
                onMetrics: (metrics) => {
                  metricsData = metrics;
                  metricsContainer.innerHTML = `
                    <span class="metric-item">Prompt: ${metrics.tokensPrompt || 0}t</span>
                    <span>·</span>
                    <span class="metric-item">Completion: ${metrics.tokensCompletion || 0}t</span>
                    <span>·</span>
                    <span class="metric-item metric-saved">${ICONS.shield} Saved: ~${metrics.tokensSaved || 0}t</span>
                  `;
                  if (metrics.tokensSaved) {
                    state.totalCumulativeSavedTokens += metrics.tokensSaved;
                    localStorage.setItem('kortex_cumulative_saved', state.totalCumulativeSavedTokens);
                    updateTokenSavingsUI();
                  }
                },
                onError: (err) => {
                  fullBotText += `\n\n> ⚠️ **Error:** ${err}`;
                  bubbleContent.innerHTML = formatMarkdown(fullBotText);
                }
              });
            } catch (err) {
              console.error('Error parsing SSE json:', err, jsonStr);
            }
          }
        }
      }

    } catch (e) {
      if (e.name === 'AbortError') {
        fullBotText += '\n\n*[Generation stopped by user]*';
      } else {
        fullBotText += `\n\n> ⚠️ **Failed to complete stream:** ${e.message}`;
      }
      bubbleContent.innerHTML = formatMarkdown(fullBotText);
    } finally {
      cursor.remove();
      state.isGenerating = false;
      setGeneratingUI(false);

      conv.messages.push({
        role: 'bot',
        content: fullBotText,
        telemetry: telemetryEvents,
        metrics: metricsData
      });
      saveConversations();
    }
  }

  function handleStreamEvent(event, handlers) {
    switch (event.type) {
      case 'token':
        if (event.content) handlers.onToken(event.content);
        break;
      case 'tool_call':
        handlers.onToolCall(event.toolName, event.toolArgs);
        break;
      case 'tool_result':
        handlers.onToolResult(event.toolName, event.toolResult);
        break;
      case 'metrics':
        handlers.onMetrics(event);
        break;
      case 'error':
        handlers.onError(event.content);
        break;
      case 'done':
        break;
    }
  }

  function stopGeneration() {
    if (state.currentAbortController) {
      state.currentAbortController.abort();
    }
  }

  function setGeneratingUI(isGenerating) {
    if (isGenerating) {
      elements.sendBtn.style.display = 'none';
      elements.stopBtn.style.display = 'flex';
      elements.chatTextarea.disabled = true;
    } else {
      elements.sendBtn.style.display = 'flex';
      elements.stopBtn.style.display = 'none';
      elements.chatTextarea.disabled = false;
      elements.chatTextarea.focus();
    }
  }

  // --- DOM Helpers ---
  function createBotMessageElement() {
    const row = document.createElement('div');
    row.className = 'message-row bot';
    row.innerHTML = `
      <div class="message-meta">KORTEX AI</div>
      <div class="telemetry-container"></div>
      <div class="message-bubble">
        <span class="bubble-text"></span>
        <span class="streaming-cursor"></span>
      </div>
      <div class="metrics-hud"></div>
    `;
    return row;
  }

  function appendMessageToDOM(role, content, telemetry, metrics, animate = true) {
    const row = document.createElement('div');
    row.className = `message-row ${role}`;

    if (role === 'user') {
      row.innerHTML = `
        <div class="message-meta">YOU</div>
        <div class="message-bubble">${escapeHTML(content)}</div>
      `;
    } else {
      row.innerHTML = `
        <div class="message-meta">KORTEX AI</div>
        <div class="telemetry-container"></div>
        <div class="message-bubble">
          <div class="bubble-text">${formatMarkdown(content)}</div>
        </div>
        <div class="metrics-hud">
          ${metrics ? `
            <span class="metric-item">Prompt: ${metrics.tokensPrompt || 0}t</span>
            <span>·</span>
            <span class="metric-item">Completion: ${metrics.tokensCompletion || 0}t</span>
            <span>·</span>
            <span class="metric-item metric-saved">${ICONS.shield} Saved: ~${metrics.tokensSaved || 0}t</span>
          ` : ''}
        </div>
      `;

      if (telemetry && telemetry.length > 0) {
        const container = row.querySelector('.telemetry-container');
        telemetry.forEach(t => {
          const card = document.createElement('div');
          card.className = 'tool-telemetry-card';
          const icon = ICONS[t.name] || ICONS.zap;
          card.innerHTML = `
            <div class="tool-telemetry-header">
              <span class="tool-tag">${icon} TOOL: <strong>${t.name}</strong></span>
              <span class="tool-status-badge" style="border-color:var(--accent-green);color:var(--accent-green)">Completed</span>
            </div>
            <div class="tool-telemetry-body">${escapeHTML(t.result || t.args || '')}</div>
          `;
          container.appendChild(card);
        });
      }

      highlightCodeBlocks(row.querySelector('.bubble-text'));
    }

    elements.messagesInner.appendChild(row);
  }

  function scrollToBottom() {
    elements.messagesContainer.scrollTop = elements.messagesContainer.scrollHeight;
  }

  function autoResizeTextarea() {
    elements.chatTextarea.style.height = 'auto';
    elements.chatTextarea.style.height = Math.min(elements.chatTextarea.scrollHeight, 140) + 'px';
  }

  // --- Clean Minimal Markdown Parser (No External Bloat) ---
  function formatMarkdown(text) {
    if (!text) return '';

    // Escape HTML first
    let html = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // Fenced Code Blocks: ```lang\ncode\n```
    html = html.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
      const language = lang || 'text';
      return `<div class="code-block-wrapper">
        <div class="code-block-header">
          <span>${language}</span>
          <button class="copy-code-btn" data-code="${encodeURIComponent(code)}">
            ${ICONS.copy}
            <span>Copy</span>
          </button>
        </div>
        <pre><code>${code}</code></pre>
      </div>`;
    });

    // Inline code `code`
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // Headers
    html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
    html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
    html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

    // Bold **text**
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

    // Italic *text*
    html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // Blockquotes > text
    html = html.replace(/^\> (.*$)/gim, '<blockquote>$1</blockquote>');

    // Bullet lists
    html = html.replace(/^\s*[-*]\s+(.*$)/gim, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>)/gim, '<ul>$1</ul>');

    // Paragraphs / linebreaks (ignore inside pre)
    const parts = html.split(/(<div class="code-block-wrapper">[\s\S]*?<\/div>)/g);
    html = parts.map(part => {
      if (part.startsWith('<div class="code-block-wrapper">')) return part;
      return part.replace(/\n\n+/g, '</p><p>').replace(/\n/g, '<br>');
    }).join('');

    return `<p>${html}</p>`;
  }

  function highlightCodeBlocks(container) {
    const copyButtons = container.querySelectorAll('.copy-code-btn');
    copyButtons.forEach(btn => {
      btn.onclick = () => {
        const rawCode = decodeURIComponent(btn.dataset.code);
        navigator.clipboard.writeText(rawCode).then(() => {
          const span = btn.querySelector('span');
          if (span) {
            span.textContent = 'Copied!';
            setTimeout(() => { span.textContent = 'Copy'; }, 1800);
          }
        });
      };
    });
  }

  function escapeHTML(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  // --- Event Listeners ---
  function setupEventListeners() {
    // Textarea input & resizing
    elements.chatTextarea.addEventListener('input', autoResizeTextarea);
    elements.chatTextarea.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });

    // Send and Stop buttons
    elements.sendBtn.addEventListener('click', sendMessage);
    elements.stopBtn.addEventListener('click', stopGeneration);

    // New Chat & Clear Chat
    elements.newChatBtn.addEventListener('click', createNewConversation);
    elements.clearChatBtn.addEventListener('click', () => {
      if (confirm('Clear messages in this conversation?')) {
        const conv = getCurrentConversation();
        if (conv) {
          conv.messages = [];
          saveConversations();
          renderActiveConversation();
        }
      }
    });

    // Sidebar toggle
    elements.toggleSidebarBtn.addEventListener('click', () => {
      elements.sidebar.classList.toggle('collapsed');
    });

    // Theme toggle
    elements.themeToggleBtn.addEventListener('click', toggleTheme);

    // Settings Modal
    elements.settingsBtn.addEventListener('click', () => {
      elements.settingsModal.classList.add('open');
    });
    elements.closeSettingsBtn.addEventListener('click', () => {
      elements.settingsModal.classList.remove('open');
    });
    elements.saveSettingsBtn.addEventListener('click', saveSettings);
    elements.settingsModal.addEventListener('click', (e) => {
      if (e.target === elements.settingsModal) {
        elements.settingsModal.classList.remove('open');
      }
    });

    // Suggestions chips
    if (elements.suggestionsContainer) {
      elements.suggestionsContainer.querySelectorAll('.suggestion-chip').forEach(chip => {
        chip.addEventListener('click', () => {
          elements.chatTextarea.value = chip.dataset.prompt || chip.textContent.trim();
          autoResizeTextarea();
          elements.chatTextarea.focus();
        });
      });
    }

    updateStatusBadge();
  }

  // Start app
  document.addEventListener('DOMContentLoaded', init);
})();
