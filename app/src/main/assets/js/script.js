const icDir = `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23F5A623'><path d='M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z'/></svg>`;
const icFile = `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23717970'><path d='M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z'/></svg>`;
let currentRoot = '';
let currentFile = '';
let currentFileDir = false;
let fileChangeAction = '';
let fileChangeFiles = null;
let currentFiles = [];
let fileSelecting = false;
let fileBusy = false;
let fileBatchMessage = '';
let batchDeleteFiles = [];
const selectedFiles = new Map();
let moveTarget = '';
let moveRequest = null;
let fileRequest = null;
let fileGeneration = 0;
const pendingActions = new Set();
let pendingDelFolder = null;
let warnToastTimer = null;
let danmakuMode = 1;
let danmakuSize = 25;
let dialogClosing = false;
let currentDialog = null;
let dialogOpener = null;
let activePanel = 1;
let activeInput = 'search';
let activeMedia = 'subtitles';

function search() {
    const word = $('#keyword').val().trim();
    if (!word) {
        warnToast(t('inputRequired'));
        return;
    }
    doAction('search', { word }, () => rememberInput('search', word));
}

function push() {
    const url = $('#push_url').val().trim();
    if (!url) {
        warnToast(t('inputRequired'));
        return;
    }
    doAction('push', { url }, () => rememberInput('push', url));
}

function setting() {
    const text = $('#setting_text').val().trim();
    if (!text) {
        warnToast(t('inputRequired'));
        return;
    }
    doAction('setting', { text, name: $('#setting_name').val() });
}

function sendDanmaku() {
    const text = $('#danmaku_text').val().trim();
    if (!text) {
        warnToast(t('inputRequired'));
        return;
    }
    doAction('danmaku', { text: `[0.0,${danmakuMode},${danmakuSize},16777215]${text}` }, () => {
        if ($('#danmaku_text').val().trim() === text) $('#danmaku_text').val('');
    });
}

function showDanmakuModeDialog() {
    $('#danmakuModeDialog .md-dialog-list-item').removeClass('active').attr('aria-pressed', 'false');
    $(`#danmakuModeDialog .md-dialog-list-item[data-val="${danmakuMode}"]`).addClass('active').attr('aria-pressed', 'true');
    openDialog('danmakuModeDialog');
}

function setDanmakuMode(val) {
    danmakuMode = val;
    $('#danmaku_mode_label').text($(`#danmakuModeDialog .md-dialog-list-item[data-val="${val}"] span`).text());
    closeDialog('danmakuModeDialog');
}

function showDanmakuSizeDialog() {
    $('#danmakuSizeDialog .md-dialog-list-item').removeClass('active').attr('aria-pressed', 'false');
    $(`#danmakuSizeDialog .md-dialog-list-item[data-val="${danmakuSize}"]`).addClass('active').attr('aria-pressed', 'true');
    openDialog('danmakuSizeDialog');
}

function setDanmakuSize(val) {
    danmakuSize = val;
    $('#danmaku_size_label').text($(`#danmakuSizeDialog .md-dialog-list-item[data-val="${val}"] span`).text());
    closeDialog('danmakuSizeDialog');
}

function doAction(action, kv, complete) {
    if (pendingActions.has(action)) return;
    pendingActions.add(action);
    const button = document.getElementById('action_' + action);
    if (button) button.disabled = true;
    return $.ajax({ url: '/action', type: 'POST', data: { ...kv, do: action }, timeout: 10000 })
        .done(() => {
            warnToast(t('actionSent'));
            complete?.();
        })
        .fail(() => warnToast(t('actionFailed')))
        .always(() => {
            pendingActions.delete(action);
            if (button) button.disabled = false;
        });
}

function openDialog(id) {
    dialogOpener = document.activeElement;
    currentDialog = document.getElementById(id);
    $('#' + id).show();
    history.pushState({...history.state, dialog: id}, '');
    const initial = currentDialog.querySelector('input:not([disabled]):not([hidden]), .md-dialog-list-item.active') || currentDialog.querySelector('button:not([disabled]):not([hidden])');
    initial.focus();
}

function closeDialog(id) {
    dialogClosing = true;
    hideDialog(id);
    history.back();
}

function hideDialog(id) {
    if (id === 'fileChangeDialog') {
        moveRequest?.abort();
        moveRequest = null;
    }
    $('#' + id).hide();
    currentDialog = null;
    const opener = dialogOpener;
    dialogOpener = null;
    if (opener?.isConnected) opener.focus();
}

function selectFile(path, dir = false) {
    currentFile = path;
    currentFileDir = dir;
    $("#fileUrl").text("file:/" + path);
    document.getElementById('file_use').hidden = dir;
    openDialog('fileInfoDialog');
}

function readFiles(info) {
    const files = info?.files;
    if (!Array.isArray(files) || files.some(file => !file || typeof file.name !== 'string' || typeof file.path !== 'string' || ![0, 1].includes(file.dir))) throw new Error('Invalid folder response');
    return files;
}

function renderFiles(files) {
    currentFiles = files;
    const entries = new Map(files.map(file => [file.path, file]));
    for (const path of selectedFiles.keys()) {
        if (entries.has(path)) selectedFiles.set(path, entries.get(path));
        else selectedFiles.delete(path);
    }
    const list = document.getElementById('file_list');
    const focused = list.contains(document.activeElement) ? { ...document.activeElement.dataset } : null;
    const rows = document.createDocumentFragment();
    for (const file of files) rows.append(createFileRow(file));
    if (!files.length) {
        const empty = document.createElement('p');
        empty.className = 'panel-hint';
        empty.textContent = t('fileEmpty');
        rows.append(empty);
    }
    list.replaceChildren(rows);
    if (focused) {
        const buttons = Array.from(list.querySelectorAll('button, input'));
        const next = buttons.find(button => button.dataset.path === focused.path && button.dataset.action === focused.action) || buttons.find(button => button.dataset.action === focused.action) || document.getElementById('file_refresh');
        next.focus({ preventScroll: true });
    }
    renderFileSelection();
}

function createFileRow(file) {
    const row = document.createElement('div');
    row.className = 'file-item';
    const open = document.createElement(fileSelecting ? 'label' : 'button');
    open.className = 'file-open';
    if (!fileSelecting) {
        open.type = 'button';
        open.disabled = fileBusy;
    }
    open.dataset.path = file.path;
    open.dataset.action = fileSelecting ? 'select' : 'open';
    if (fileSelecting) open.append(createFileCheckbox(file));
    const icon = document.createElement('img');
    icon.className = 'file-icon';
    icon.alt = '';
    icon.src = file.dir === 1 ? icDir : icFile;
    open.append(icon, createFileInfo(file));
    if (!fileSelecting) open.addEventListener('click', () => file.dir === 1 ? listFile(file.path, true) : selectFile(file.path));
    row.append(open);
    if (!fileSelecting) row.append(createFileActions(file));
    return row;
}

function createFileCheckbox(file) {
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = selectedFiles.has(file.path);
    checkbox.disabled = fileBusy;
    checkbox.dataset.path = file.path;
    checkbox.dataset.action = 'select';
    checkbox.setAttribute('aria-label', t('select') + ': ' + file.name);
    checkbox.addEventListener('change', () => {
        if (checkbox.checked) selectedFiles.set(file.path, file);
        else selectedFiles.delete(file.path);
        fileBatchMessage = '';
        renderFileSelection();
    });
    return checkbox;
}

function createFileInfo(file) {
    const info = document.createElement('span');
    info.className = 'file-info';
    const name = document.createElement('span');
    name.className = 'file-name';
    name.textContent = file.name;
    const time = document.createElement('span');
    time.className = 'file-time';
    time.textContent = file.time;
    info.append(name, time);
    return info;
}

function createFileActions(file) {
    const actions = document.createElement('button');
    actions.className = 'md-btn remote-icon-button file-actions';
    actions.type = 'button';
    actions.dataset.path = file.path;
    actions.dataset.action = 'menu';
    actions.innerHTML = '<svg aria-hidden="true" viewBox="0 0 960 960"><use href="#file-icon-actions"/></svg>';
    actions.title = t('fileActions');
    actions.setAttribute('aria-label', t('fileActions') + ': ' + file.name);
    actions.addEventListener('click', () => selectFile(file.path, file.dir === 1));
    return actions;
}

function setFileSelection(selecting) {
    if (fileBusy || selecting && !currentFiles.length) return;
    const select = document.getElementById('file_select');
    const cancel = document.getElementById('file_selection_cancel');
    const restoreFocus = document.activeElement === select || document.activeElement === cancel;
    fileSelecting = selecting;
    selectedFiles.clear();
    fileBatchMessage = '';
    renderFiles(currentFiles);
    if (restoreFocus) (selecting ? cancel : select.disabled ? document.getElementById('file_refresh') : select).focus();
}

function selectAllFiles() {
    if (fileBusy) return;
    if (selectedFiles.size === currentFiles.length) selectedFiles.clear();
    else for (const file of currentFiles) selectedFiles.set(file.path, file);
    fileBatchMessage = '';
    renderFiles(currentFiles);
}

function renderFileSelection() {
    const select = document.getElementById('file_select');
    select.hidden = fileSelecting;
    select.disabled = fileBusy || !currentFiles.length;
    document.getElementById('file_selection_cancel').hidden = !fileSelecting;
    document.getElementById('file_selection_cancel').disabled = fileBusy;
    document.getElementById('file_selection').hidden = !fileSelecting;
    document.getElementById('file_upload_actions').hidden = fileSelecting;
    const count = document.getElementById('file_selection_count');
    count.hidden = !fileSelecting;
    count.textContent = t('selectedCount', String(selectedFiles.size));
    const all = document.getElementById('file_select_all');
    all.textContent = t(currentFiles.length && selectedFiles.size === currentFiles.length ? 'clearSelection' : 'selectAll');
    all.disabled = fileBusy || !currentFiles.length;
    for (const id of ['file_move_selected', 'file_delete_selected']) document.getElementById(id).disabled = fileBusy || !selectedFiles.size;
    document.getElementById('file_refresh').disabled = fileBusy;
    const status = document.getElementById('file_batch_status');
    status.hidden = !fileBatchMessage;
    status.textContent = fileBatchMessage;
}

function showBatchDelete() {
    if (fileBusy || !selectedFiles.size) return;
    batchDeleteFiles = Array.from(selectedFiles.values());
    renderDialogFiles('batchDeleteList', batchDeleteFiles);
    document.getElementById('batchDeleteTitle').textContent = t('deleteSelected', String(batchDeleteFiles.length));
    document.getElementById('batchDeleteHint').hidden = !batchDeleteFiles.some(file => file.dir === 1);
    openDialog('batchDeleteDialog');
}

function renderDialogFiles(id, files) {
    const rows = document.createDocumentFragment();
    for (const file of files) {
        const row = document.createElement('li');
        row.textContent = file.name;
        rows.append(row);
    }
    document.getElementById(id).replaceChildren(rows);
}

function confirmBatchDelete(confirmed) {
    closeDialog('batchDeleteDialog');
    const files = batchDeleteFiles;
    batchDeleteFiles = [];
    if (confirmed) return runFileBatch('delete', files);
}

async function runFileBatch(action, files, target) {
    if (fileBusy || !files.length) return;
    const root = currentRoot, failed = [];
    let completed = 0;
    fileBusy = true;
    fileGeneration++;
    fileRequest?.abort?.();
    fileRequest = null;
    $('#loadingToast').hide();
    renderFiles(currentFiles);
    renderBreadcrumbs();
    for (const file of files) {
        fileBatchMessage = t('batchProgress', (completed + failed.length) + ' / ' + files.length);
        renderFileSelection();
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 10000);
        try {
            const response = await requestFileOperation(action, file, target, controller.signal);
            if (!response.ok) throw new Error('File operation failed');
            removeCompletedFile(file);
            completed++;
        } catch (error) {
            failed.push(file.name);
        } finally {
            clearTimeout(timeout);
        }
    }
    fileBusy = false;
    if (failed.length) {
        fileBatchMessage = t('batchDone', String(completed)) + ' ' + t('batchFailed', failed.join(', '));
        renderFiles(currentFiles);
    } else {
        setFileSelection(false);
        warnToast(t('batchDone', String(completed)));
    }
    renderBreadcrumbs();
    if (currentRoot === root) listFile(root);
}

function requestFileOperation(action, file, target, signal) {
    const url = action === 'move' ? '/move' : file.dir === 1 ? '/delFolder' : '/delFile';
    const data = action === 'move' ? { path: file.path, target } : { path: file.path };
    return fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams(data).toString(), signal });
}

function removeCompletedFile(file) {
    selectedFiles.delete(file.path);
    currentFiles = currentFiles.filter(entry => entry.path !== file.path);
}

function renderBreadcrumbs() {
    const nav = document.getElementById('file_breadcrumbs');
    nav.setAttribute('aria-label', t('localPath'));
    const refresh = document.getElementById('file_refresh');
    refresh.setAttribute('aria-label', t('refreshFiles'));
    refresh.title = t('refreshFiles');
    const path = document.getElementById('file_path');
    path.textContent = currentRoot.split('/').pop() || t('fileRoot');
    path.title = currentRoot || t('fileRoot');
    const up = document.getElementById('file_up');
    up.disabled = fileBusy || !currentRoot;
    up.setAttribute('aria-label', t('upFolder') + ': ' + path.textContent);
    up.title = t('upFolder');
}

function replaceDialog(id) {
    $('#' + currentDialog.id).hide();
    currentDialog = document.getElementById(id);
    $('#' + id).show();
    history.replaceState({...history.state, dialog: id}, '');
    currentDialog.querySelector('input:not([disabled]):not([hidden]), button:not([disabled]):not([hidden])').focus();
}

function showFileChangeDialog(action, files = null) {
    if (files && (fileBusy || !files.length)) return;
    fileChangeAction = action;
    fileChangeFiles = files;
    const moving = action === 'move';
    $('#fileChangeTitle').text(moving && files ? t('moveSelected', String(files.length)) : t(action));
    document.getElementById('fileRenameField').hidden = moving;
    document.getElementById('fileMovePicker').hidden = !moving;
    const input = document.getElementById('fileChangeValue');
    input.disabled = moving;
    input.value = currentFile.split('/').pop();
    const confirm = document.getElementById('fileChangeConfirm');
    confirm.textContent = t(moving ? 'moveHere' : 'confirm');
    confirm.disabled = moving;
    if (files) openDialog('fileChangeDialog');
    else replaceDialog('fileChangeDialog');
    if (moving) loadMoveFolders(currentRoot);
}

async function loadMoveFolders(path) {
    moveRequest?.abort();
    const controller = new AbortController();
    moveRequest = controller;
    moveTarget = path;
    const list = document.getElementById('fileMoveFolders');
    const up = document.getElementById('fileMoveUp');
    const confirm = document.getElementById('fileChangeConfirm');
    const retry = document.getElementById('fileMoveRetry');
    const restoreFocus = list.contains(document.activeElement) || document.activeElement === up;
    list.replaceChildren();
    document.getElementById('fileMovePath').textContent = path || t('fileRoot');
    up.disabled = !path;
    confirm.disabled = true;
    const status = document.getElementById('fileMoveStatus');
    status.hidden = false;
    status.textContent = t('loadingFolders');
    retry.hidden = true;
    const timeout = setTimeout(() => controller.abort(), 10000);
    try {
        const response = await fetch('/file' + path.split('/').map(encodeURIComponent).join('/'), { cache: 'no-store', signal: controller.signal });
        if (!response.ok) throw new Error('Folder unavailable');
        const files = readFiles(await response.json());
        if (moveRequest !== controller) return;
        renderMoveFolders(files);
        confirm.disabled = path === currentRoot;
    } catch (error) {
        if (moveRequest !== controller) return;
        status.textContent = t('loadFailed');
        retry.hidden = false;
    } finally {
        clearTimeout(timeout);
        if (moveRequest === controller) {
            moveRequest = null;
            if (restoreFocus && (!currentDialog.contains(document.activeElement) || document.activeElement === up)) currentDialog.querySelector('button:not([disabled]):not([hidden])').focus();
        }
    }
}

function renderMoveFolders(files) {
    const sources = fileChangeFiles || [{ path: currentFile }];
    const folders = files.filter(file => file.dir === 1 && !sources.some(source => file.path === source.path || file.path.startsWith(source.path + '/')));
    const rows = document.createDocumentFragment();
    for (const folder of folders) rows.append(createMoveFolderOption(folder));
    document.getElementById('fileMoveFolders').replaceChildren(rows);
    const status = document.getElementById('fileMoveStatus');
    status.hidden = folders.length > 0;
    status.textContent = t('noFolders');
}

function createMoveFolderOption(folder) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'md-dialog-list-item folder-option';
    const icon = document.createElement('img');
    icon.src = icDir;
    icon.alt = '';
    const name = document.createElement('span');
    name.textContent = folder.name;
    button.append(icon, name);
    button.addEventListener('click', () => loadMoveFolders(folder.path));
    return button;
}

function confirmFileChange() {
    if (document.getElementById('fileChangeConfirm').disabled) return;
    const value = fileChangeAction === 'move' ? moveTarget : $('#fileChangeValue').val().trim();
    if (fileChangeAction === 'rename' && !value) {
        warnToast(t('inputRequired'));
        return;
    }
    const path = currentFile, root = currentRoot;
    const data = fileChangeAction === 'rename' ? { path, name: value } : { path, target: value };
    closeDialog('fileChangeDialog');
    if (fileChangeFiles) return runFileBatch('move', fileChangeFiles, moveTarget);
    $.post('/' + fileChangeAction, data).done(() => {
        warnToast(t('fileChanged'));
        if (currentRoot === root) listFile(root);
    }).fail(() => warnToast(t('fileChangeFailed')));
}

function deleteSelectedFile() {
    const id = currentFileDir ? 'delFolder' : 'delFile';
    if (currentFileDir) pendingDelFolder = { path: currentFile, refreshPath: currentRoot };
    $('#' + id + 'Content').text(t('deleteConfirm', currentFile));
    replaceDialog(id);
}

function pushFile(yes) {
    closeDialog('fileInfoDialog');
    if (yes === 1) doAction('file', { path: "file:/" + currentFile });
}

function listFile(path, addHistory = false) {
    if (fileBusy) return;
    const generation = ++fileGeneration;
    fileRequest?.abort?.();
    const loadingTimer = setTimeout(() => $('#loadingToast').show(), 200);
    fileRequest = $.get('/file' + path.split('/').map(encodeURIComponent).join('/'), function (res) {
        clearTimeout(loadingTimer);
        if (generation !== fileGeneration) return;
        let files;
        try {
            files = readFiles(JSON.parse(res));
        } catch (e) {
            $('#loadingToast').hide();
            if (activePanel === 5) updateNavigation(true);
            warnToast(t('invalidResponse'));
            return;
        }
        if (currentRoot !== path) {
            fileSelecting = false;
            selectedFiles.clear();
            fileBatchMessage = '';
        }
        currentRoot = path;
        renderBreadcrumbs();
        renderFiles(files);
        if (activePanel === 5) updateNavigation(!addHistory);
        $('#loadingToast').hide();
    }).fail(function () {
        clearTimeout(loadingTimer);
        if (generation !== fileGeneration) return;
        $('#loadingToast').hide();
        if (activePanel === 5) updateNavigation(true);
        warnToast(t('loadFailed'));
    });
}

function uploadFile() {
    $('#file_uploader').click();
}

function onFileSelected() {
    const files = document.getElementById('file_uploader').files;
    if (files.length === 0) return;
    renderDialogFiles('uploadTipContent', files);
    openDialog('uploadTip');
}

async function confirmUpload(yes) {
    closeDialog('uploadTip');
    if (yes !== 1) return;
    const files = $('#file_uploader')[0].files;
    if (files.length === 0) return;
    const root = currentRoot;
    const success = await window.uploadTransfer.send(files, root);
    $('#file_uploader').val('');
    if (success && currentRoot === root) listFile(root);
}

function showNewFolderDialog() {
    openDialog('newFolder');
}

function confirmNewFolder(yes) {
    closeDialog('newFolder');
    const name = $('#newFolderContent').val().trim();
    $('#newFolderContent').val('');
    if (yes !== 1 || name.length === 0) return;
    $('#loadingToast').show();
    $.post('/newFolder', { path: currentRoot, name }, function () {
        $('#loadingToast').hide();
        listFile(currentRoot);
    }).fail(function () {
        $('#loadingToast').hide();
        warnToast(t('createFailed'));
    });
}

function confirmDelFolder(yes) {
    closeDialog('delFolder');
    if (yes !== 1 || !pendingDelFolder) {
        pendingDelFolder = null;
        return;
    }
    const { path, refreshPath } = pendingDelFolder;
    pendingDelFolder = null;
    $('#loadingToast').show();
    $.post('/delFolder', { path }, function () {
        $('#loadingToast').hide();
        listFile(refreshPath);
    }).fail(function () {
        $('#loadingToast').hide();
        warnToast(t('deleteFailed'));
    });
}

function confirmDelFile(yes) {
    closeDialog('delFile');
    if (yes !== 1) return;
    $('#loadingToast').show();
    $.post('/delFile', { path: currentFile }, function () {
        $('#loadingToast').hide();
        listFile(currentRoot);
    }).fail(function () {
        $('#loadingToast').hide();
        warnToast(t('deleteFailed'));
    });
}

function warnToast(msg) {
    $('#warnToastContent').text(msg);
    $('#warnToast').show();
    if (warnToastTimer) clearTimeout(warnToastTimer);
    warnToastTimer = setTimeout(() => {
        $('#warnToast').hide();
        warnToastTimer = null;
    }, 3500);
}

const MIDDLE = {
    2: {key: 'remoteTitle', viewBox: '0 0 960 960', html: '<use href="#remote-icon-play"/>'},
    3: {key: 'subtitles', viewBox: '0 0 24 24', html: '<path d="M0 0h24v24H0V0z" fill="none"/><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z"/>'},
    5: {key: 'local', viewBox: '0 0 24 24', html: '<path d="M0 0h24v24H0V0z" fill="none"/><path d="M9.17 6l2 2H20v10H4V6h5.17M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>'},
    6: {key: 'biliNav', viewBox: '0 0 24 24', html: '<path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z"/>'},
    7: {key: 'curtainNav', viewBox: '0 0 24 24', html: '<path d="M4 3h16v3H4zm1 4h6v13H5zm8 0h6v13h-6z"/>'}
};
let middlePanel = 2;
let curtainTimer = null;
let curtainReady = false;
const CURTAIN_COLORS = ['101114', '000000', 'FFFFFF', '8B1E3F', '1B4332', '1D3557', 'C45C26', 'F4D35E'];

function renderMiddle() {
    const item = MIDDLE[middlePanel];
    const icon = document.getElementById('midIcon');
    const label = document.getElementById('midLabel');
    if (!item || !icon || !label) return;
    icon.setAttribute('viewBox', item.viewBox);
    icon.innerHTML = item.html;
    label.textContent = t(item.key);
}

function setNavPressed(id, pressed) {
    const tab = document.getElementById(id);
    if (!tab) return;
    tab.classList.toggle('active', pressed);
    tab.setAttribute('aria-pressed', String(pressed));
}

function showMiddle() {
    showPanel(middlePanel);
}

function toggleMore(event) {
    event.stopPropagation();
    const menu = document.getElementById('moreMenu');
    const open = !menu.classList.contains('open');
    menu.classList.toggle('open', open);
    document.getElementById('moreToggle').classList.toggle('open', open);
    document.getElementById('moreToggle').setAttribute('aria-expanded', String(open));
}

function closeMore() {
    const menu = document.getElementById('moreMenu');
    const toggle = document.getElementById('moreToggle');
    if (menu) menu.classList.remove('open');
    if (toggle) {
        toggle.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
    }
}

function pickMiddle(id) {
    showPanel(id);
}

function showPanel(id, navigate = true) {
    activePanel = id;
    if (MIDDLE[id]) middlePanel = id;
    for (let i = 1; i <= 8; i++) {
        const panel = document.getElementById('panel' + i);
        if (panel) panel.classList.toggle('active', i === id);
    }
    setNavPressed('tab1', id === 1);
    setNavPressed('tab4', id === 4);
    setNavPressed('tabMid', id === middlePanel);
    renderMiddle();
    document.querySelectorAll('.md-more-item').forEach(item => {
        item.classList.toggle('current', Number(item.dataset.panel) === middlePanel);
    });
    closeMore();
    document.dispatchEvent(new Event('panelchange'));
    if (id === 4) {
        showSettingsPage('main');
        loadFeed();
    }
    if (id === 3 && activeMedia === 'online') {
        showAssrtPage('search');
        loadAssrt();
    }
    if (id === 6) loadBiliFilter();
    if (typeof watchCurtain === 'function') watchCurtain(id === 7);
    if (navigate) {
        updateNavigation();
        if (id === 5 && document.getElementById('file_list').innerHTML === '') listFile(currentRoot);
    }
}

function showInput(type, navigate = true) {
    activeInput = type;
    for (const name of ['search', 'push']) {
        const button = document.getElementById('input_tab_' + name);
        button.setAttribute('aria-selected', String(name === type));
        button.tabIndex = name === type ? 0 : -1;
        document.getElementById('input_' + name).hidden = name !== type;
    }
    if (navigate && activePanel === 1) updateNavigation();
}

const MEDIA_TABS = ['subtitles', 'online', 'danmaku'];

function showMedia(type, navigate = true) {
    activeMedia = type;
    for (const name of MEDIA_TABS) {
        const button = document.getElementById('media_tab_' + name);
        button.setAttribute('aria-selected', String(name === type));
        button.tabIndex = name === type ? 0 : -1;
        document.getElementById('media_' + name).hidden = name !== type;
    }
    if (type === 'online') {
        showAssrtPage('search');
        if (activePanel === 3) loadAssrt();
    }
    if (navigate && activePanel === 3) updateNavigation();
}

function setNavigation(tab, path, replace) {
    document.title = 'MXBox · ' + t(WEB_TABS[tab].title);
    if (replace || history.state?.tab !== tab || history.state?.path !== path) history[replace ? 'replaceState' : 'pushState']({tab, path}, '', webPageUrl(tab, path));
    const params = new URLSearchParams({from: tab});
    if (tab === 'files' && path) params.set('path', path);
    document.getElementById('remote_open').href = 'remote.html?' + params;
}

function updateNavigation(replace = false) {
    const tab = activePanel === 1 ? activeInput : activePanel === 3 ? activeMedia : Object.keys(WEB_TABS).find(name => WEB_TABS[name].panel === activePanel);
    setNavigation(tab, activePanel === 5 ? currentRoot : '', replace);
}

function restoreNavigation() {
    const route = webRoute(window.location.search);
    showInput(route.panel === 1 ? route.tab : activeInput, false);
    showMedia(route.panel === 3 ? (route.tab === 'danmaku' ? 'danmaku' : route.tab === 'online' || route.tab === 'assrt' ? 'online' : 'subtitles') : activeMedia, false);
    showPanel(route.panel, false);
    const path = route.panel === 5 ? fileBusy ? currentRoot : route.path : '';
    setNavigation(route.tab, path, true);
    if (route.panel === 5 && (currentRoot !== path || document.getElementById('file_list').innerHTML === '')) listFile(path);
}

restoreNavigation();
document.addEventListener('click', event => {
    if (event.target.closest('#moreMenu') || event.target.closest('#moreToggle')) return;
    closeMore();
});

function showSettingsPage(name) {
    document.getElementById('settings_main').classList.toggle('curtain-hidden', name !== 'main');
    document.getElementById('settings_buttons').classList.toggle('curtain-hidden', name !== 'buttons');
    if (name === 'buttons') loadHomeButtons();
}

function showAssrtPage(name) {
    document.getElementById('assrt_page_search').classList.toggle('curtain-hidden', name !== 'search');
    document.getElementById('assrt_page_token').classList.toggle('curtain-hidden', name !== 'token');
    if (name === 'token') loadAssrtToken();
}

function loadHomeButtons() {
    $.get('/action', { do: 'homeButtons' }, function (text) {
        let data;
        try { data = JSON.parse(text); } catch (e) { return; }
        renderHomeButtons(data.items || []);
    });
}

function renderHomeButtons(items) {
    const box = document.getElementById('home_buttons');
    if (!box) return;
    box.innerHTML = '';
    items.forEach((item, index) => {
        const row = document.createElement('div');
        row.className = 'library-row home-button-row';
        const label = document.createElement('label');
        label.className = 'home-button-name';
        const check = document.createElement('input');
        check.type = 'checkbox';
        check.checked = !item.hidden;
        check.setAttribute('aria-label', '显示' + item.label);
        check.onchange = () => {
            items[index].hidden = !check.checked;
            saveHomeButtons(items);
        };
        const name = document.createElement('span');
        name.textContent = item.label;
        label.appendChild(check);
        label.appendChild(name);
        const actions = document.createElement('span');
        actions.className = 'home-button-actions';
        actions.appendChild(homeMoveButton('上移', index > 0, () => moveHomeButton(items, index, -1)));
        actions.appendChild(homeMoveButton('下移', index < items.length - 1, () => moveHomeButton(items, index, 1)));
        row.appendChild(label);
        row.appendChild(actions);
        box.appendChild(row);
    });
}

function homeMoveButton(text, enabled, action) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'md-btn md-btn-outlined';
    button.textContent = text;
    button.disabled = !enabled;
    button.onclick = action;
    return button;
}

function moveHomeButton(items, index, delta) {
    const next = index + delta;
    if (next < 0 || next >= items.length) return;
    const item = items[index];
    items[index] = items[next];
    items[next] = item;
    saveHomeButtons(items);
}

function saveHomeButtons(items) {
    $.post('/action', {
        do: 'homeButtons',
        order: items.map(item => item.key).join(','),
        hidden: items.filter(item => item.hidden).map(item => item.key).join(',')
    }).done(function () {
        warnToast('已同步');
        loadHomeButtons();
    }).fail(function () {
        warnToast('保存失败');
    });
}

function loadFeed() {
    $.get('/action', { do: 'feed' }, function (text) {
        $('#feed_server').val((text || '').trim());
    });
}

function saveFeed() {
    const url = $('#feed_server').val().trim();
    if (!url) return;
    $.post('/action', { do: 'feed', url: url }).done(function (text) {
        const saved = (text || '').trim();
        if (saved) $('#feed_server').val(saved);
        warnToast(saved || '已设置');
    }).fail(function () {
        warnToast('设置失败');
    });
}

let biliFilterSnapshot = '';

function loadBiliFilter() {
    $.get('/action', { do: 'biliFilter' }, function (text) {
        try {
            const data = JSON.parse(text);
            $('#bili_filter_on').prop('checked', !!data.enabled);
            $('#bili_filter_words').val(data.words || '');
            biliFilterSnapshot = biliFilterState();
        } catch (e) {
        }
    });
}

function biliFilterState() {
    return ($('#bili_filter_on').is(':checked') ? '1' : '0') + '\n' + $('#bili_filter_words').val();
}

function saveBiliFilter() {
    const next = biliFilterState();
    if (next === biliFilterSnapshot) return;
    biliFilterSnapshot = next;
    $.post('/action', {
        do: 'biliFilter',
        enabled: next.startsWith('1') ? '1' : '0',
        words: $('#bili_filter_words').val()
    }).done(function () {
        warnToast('已保存');
    }).fail(function () {
        warnToast('保存失败');
    });
}

function watchCurtain(on) {
    if (curtainTimer) {
        clearInterval(curtainTimer);
        curtainTimer = null;
    }
    if (!on) return;
    showCurtainPage('simple');
    if (!document.getElementById('curtain_colors').childElementCount) {
        CURTAIN_COLORS.forEach(color => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'curtain-swatch';
            button.style.background = '#' + color;
            button.onclick = () => curtainPost({ do: 'color', value: color });
            document.getElementById('curtain_colors').appendChild(button);
        });
    }
    refreshCurtain(true);
    curtainTimer = setInterval(() => refreshCurtain(false), 3000);
}

function refreshCurtain(fill) {
    $.get('/curtain', { do: 'state' }).done(function (text) {
        let data;
        try { data = typeof text === 'string' ? JSON.parse(text) : text; } catch (e) { return; }
        $('#curtain_status').text(data.active ? '电视幕布已打开' : (data.reason || '请先在电视上打开幕布'));
        $('#curtain_cache').text(curtainSize(data.cache));
        renderCurtainMusic(data.music || []);
        renderCurtainPushes(data.pushes || []);
        if (!fill || curtainReady) return;
        curtainReady = true;
        if (data.color) $('#curtain_color').val('#' + data.color);
        $('#curtain_image_fit').val(data.imageFit || 'fit');
        $('#curtain_video_fit').val(data.videoFit || 'fit');
        $('#curtain_audio').val(data.audio || 'video');
        $('#curtain_music_mode').val(data.musicMode || 'list');
        $('#curtain_volume').val(data.volume == null ? 80 : data.volume);
        if (data.resident) $('#curtain_resident').val(data.resident);
        if (data.residentSize) $('#curtain_resident_size').val(data.residentSize);
        if (data.residentColor) $('#curtain_resident_color').val('#' + data.residentColor);
        $('#curtain_resident_stroke').prop('checked', data.residentStroke !== false);
    }).fail(function () {
        $('#curtain_status').text('设备已离线，请在电视上重新打开二维码');
    });
}

function renderCurtainPushes(items) {
    const box = document.getElementById('curtain_pushes');
    if (!box) return;
    const key = items.map(item => item.id).join(',');
    if (key === box.dataset.key) return;
    const chosen = new Set([...box.querySelectorAll('input:checked')].map(input => input.value));
    box.dataset.key = key;
    box.innerHTML = '';
    if (!items.length) {
        const empty = document.createElement('div');
        empty.className = 'curtain-title';
        empty.textContent = '还没有推送';
        box.appendChild(empty);
        return;
    }
    items.forEach(item => {
        const row = document.createElement('label');
        row.className = 'curtain-check curtain-push';
        const check = document.createElement('input');
        check.type = 'checkbox';
        check.value = item.id;
        check.checked = chosen.has(String(item.id));
        row.appendChild(check);
        if (item.kind === 'image') {
            const image = document.createElement('img');
            image.src = '/curtain?do=preview&id=' + encodeURIComponent(item.id);
            image.alt = '';
            row.appendChild(image);
        }
        const textNode = document.createElement('span');
        textNode.textContent = item.label || (item.kind === 'image' ? '图片' : '');
        row.appendChild(textNode);
        box.appendChild(row);
    });
}

function clearChosenPushes() {
    const ids = [...document.querySelectorAll('#curtain_pushes input:checked')].map(input => input.value);
    if (!ids.length) {
        warnToast('请先选择');
        return;
    }
    $.post('/curtain', { do: 'removePush', ids: ids.join(',') }).done(function () {
        const box = document.getElementById('curtain_pushes');
        if (box) delete box.dataset.key;
        warnToast('已删除');
        refreshCurtain(false);
    }).fail(function (xhr) {
        warnToast(xhr.responseText || '失败');
    });
}
function renderCurtainMusic(tracks) {
    const box = document.getElementById('curtain_music');
    box.innerHTML = '';
    tracks.forEach(name => {
        const row = document.createElement('div');
        row.className = 'curtain-track';
        const label = document.createElement('span');
        const cut = name.indexOf('_');
        label.textContent = cut >= 0 ? name.substring(cut + 1) : name;
        const button = document.createElement('button');
        button.className = 'md-btn md-btn-tonal';
        button.textContent = '移除';
        button.onclick = () => curtainPost({ do: 'removeMusic', name: name });
        row.appendChild(label);
        row.appendChild(button);
        box.appendChild(row);
    });
}

function curtainPost(kv) {
    $.post('/curtain', kv).done(function () {
        warnToast('已同步');
        refreshCurtain(false);
    }).fail(function (xhr) {
        warnToast(xhr.responseText || '失败');
    });
}

function curtainPick(kind) {
    const input = document.getElementById(kind === 'image' ? 'curtain_image_file' : kind === 'video' ? 'curtain_video_file' : kind === 'music' ? 'curtain_music_file' : 'curtain_push_file');
    input.onchange = function () {
        if (!this.files || !this.files[0]) return;
        curtainUpload(kind, this.files[0]);
        this.value = '';
    };
    input.click();
}

function curtainUpload(kind, file) {
    const body = new FormData();
    body.append('do', 'upload');
    body.append('kind', kind);
    body.append('file', file);
    $.ajax({
        url: '/curtain',
        method: 'POST',
        data: body,
        processData: false,
        contentType: false,
        xhr: function () {
            const xhr = $.ajaxSettings.xhr();
            if (xhr.upload) xhr.upload.onprogress = function (e) {
                if (e.lengthComputable) $('#curtain_status').text('上传 ' + Math.round(e.loaded * 100 / e.total) + '%');
            };
            return xhr;
        }
    }).done(function () {
        warnToast(kind === 'push' ? '已推送' : '已同步');
        refreshCurtain(false);
    }).fail(function (xhr) {
        warnToast(xhr.responseText || '上传失败');
    });
}

function showCurtainPage(page) {
    document.getElementById('curtain_simple').classList.toggle('curtain-hidden', page !== 'simple');
    document.getElementById('curtain_pushed').classList.toggle('curtain-hidden', page !== 'pushed');
    document.getElementById('curtain_advanced').classList.toggle('curtain-hidden', page !== 'advanced');
    if (page === 'pushed') {
        const box = document.getElementById('curtain_pushes');
        if (box) delete box.dataset.key;
        refreshCurtain(false);
    }
}

function curtainMusic(action) {
    curtainPost({ do: 'music', action: action });
}

function curtainText() {
    const text = $('#curtain_text').val().trim();
    if (!text) {
        warnToast('请输入文字');
        return;
    }
    $.post('/curtain', { do: 'text', text: text }).done(function () {
        $('#curtain_text').val('');
        warnToast('已推送');
        refreshCurtain(false);
    }).fail(function (xhr) {
        warnToast(xhr.responseText || '失败');
    });
}

function curtainResident() {
    curtainPost({
        do: 'resident',
        text: $('#curtain_resident').val(),
        size: $('#curtain_resident_size').val(),
        seconds: $('#curtain_resident_seconds').val(),
        color: $('#curtain_resident_color').val(),
        stroke: $('#curtain_resident_stroke').is(':checked') ? '1' : '0'
    });
}

function curtainClearText() {
    const box = document.getElementById('curtain_pushes');
    if (box) delete box.dataset.key;
    curtainPost({ do: 'clearText' });
}

function assrtMessage(xhr, fallback) {
    try {
        const data = JSON.parse(xhr.responseText);
        if (data && data.message) return data.message;
    } catch (e) {
    }
    return fallback;
}

function assrtData(data) {
    if (typeof data === 'string') {
        try { return JSON.parse(data); } catch (e) { return {}; }
    }
    return data || {};
}

function loadAssrtToken() {
    $.get('/assrt', { do: 'state' }).done(function (data) {
        const body = assrtData(data);
        $('#assrt_token').val('');
        $('#assrt_token_state').text(body.set ? '已设置' : '');
    });
}

function loadAssrtPlaying() {
    $.get('/assrt', { do: 'playing' }).done(function (data) {
        const body = assrtData(data);
        const title = (body.title || '').trim();
        const episode = (body.episode || '').trim();
        if (!body.playing) {
            $('#assrt_q').val('');
            return;
        }
        $('#assrt_q').val(episode ? title + ' ' + episode : title);
    });
}

function loadAssrt() {
    loadAssrtPlaying();
}

function saveAssrtToken() {
    $('#assrt_token_status').text('');
    $.post('/assrt', { do: 'token', token: $('#assrt_token').val() }).done(function (data) {
        const body = assrtData(data);
        if (body.message) {
            $('#assrt_token_status').text(body.message);
            return;
        }
        $('#assrt_token').val('');
        $('#assrt_token_state').text(body.set ? '已设置' : '');
        $('#assrt_token_status').text('');
    }).fail(function (xhr) {
        $('#assrt_token_status').text(assrtMessage(xhr, '密钥格式不对'));
    });
}

function searchAssrt() {
    $('#assrt_search_status').text('');
    $('#assrt_result_status').text('');
    $('#assrt_results').empty();
    $.get('/assrt', { do: 'search', q: $('#assrt_q').val() }).done(function (data) {
        const body = assrtData(data);
        if (body.message) {
            $('#assrt_search_status').text(body.message);
            return;
        }
        renderAssrtItems(body.items || []);
        if (!body.items || !body.items.length) $('#assrt_search_status').text('没有找到字幕');
    }).fail(function (xhr) {
        $('#assrt_search_status').text(assrtMessage(xhr, '搜索失败'));
    });
}

function renderAssrtItems(items) {
    const box = document.getElementById('assrt_results');
    box.innerHTML = '';
    items.forEach(function (item) {
        const row = document.createElement('div');
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'md-btn md-btn-tonal md-btn-full assrt-item';
        button.disabled = !!item.disabled;
        const bits = [item.name || '未命名', item.lang || '', item.format || ''].filter(Boolean);
        button.textContent = bits.join(' · ');
        if (!item.disabled) button.addEventListener('click', function () { mountAssrt(item.id, '', row); });
        row.appendChild(button);
        box.appendChild(row);
    });
}

function mountAssrt(id, file, row) {
    $('#assrt_result_status').text('');
    $.post('/assrt', { do: 'mount', id: id, file: file || '' }).done(function (data) {
        const body = assrtData(data);
        if (body.message) {
            $('#assrt_result_status').text(body.message);
            return;
        }
        if (body.choose && body.choose.length) {
            renderAssrtFiles(row, id, body.choose);
            return;
        }
        if (body.ok) $('#assrt_result_status').text('已挂载 ' + (body.name || ''));
    }).fail(function (xhr) {
        $('#assrt_result_status').text(assrtMessage(xhr, '字幕下载失败'));
    });
}

function renderAssrtFiles(row, id, names) {
    let box = row.querySelector('.assrt-files');
    if (!box) {
        box = document.createElement('div');
        box.className = 'assrt-files';
        row.appendChild(box);
    }
    box.innerHTML = '';
    names.forEach(function (name) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'md-btn md-btn-tonal md-btn-full assrt-item';
        button.textContent = name;
        button.addEventListener('click', function () { mountAssrt(id, name, row); });
        box.appendChild(button);
    });
}

function curtainSize(bytes) {
    if (!bytes) return '缓存很少';
    if (bytes < 1024 * 1024) return '缓存 ' + Math.max(1, Math.round(bytes / 1024)) + ' KB';
    return '缓存 ' + (bytes / 1024 / 1024).toFixed(1) + ' MB';
}



window.addEventListener('popstate', function () {
    if (dialogClosing) {
        dialogClosing = false;
        return;
    }
    if (currentDialog) hideDialog(currentDialog.id);
    restoreNavigation();
});

document.addEventListener('keydown', event => {
    if (!currentDialog) return;
    if (event.key === 'Escape') {
        event.preventDefault();
        closeDialog(currentDialog.id);
        return;
    }
    if (event.key !== 'Tab') return;
    const controls = currentDialog.querySelectorAll('input:not([disabled]):not([hidden]), button:not([disabled]):not([hidden])');
    const first = controls[0];
    const last = controls[controls.length - 1];
    const outside = !currentDialog.contains(document.activeElement);
    if (event.shiftKey && (document.activeElement === first || outside)) {
        event.preventDefault();
        last.focus();
    } else if (!event.shiftKey && (document.activeElement === last || outside)) {
        event.preventDefault();
        first.focus();
    }
});

$(function () {
    $('#input_tab_search, #input_tab_push').on('keydown', function (event) {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        const type = event.key === 'Home' ? 'search' : event.key === 'End' ? 'push' : this.id === 'input_tab_search' ? 'push' : 'search';
        showInput(type);
        document.getElementById('input_tab_' + type).focus();
    });
    $('#media_tab_subtitles, #media_tab_online, #media_tab_danmaku').on('keydown', function (event) {
        if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        const index = MEDIA_TABS.indexOf(this.id.slice('media_tab_'.length));
        const type = event.key === 'Home' ? MEDIA_TABS[0] : event.key === 'End' ? MEDIA_TABS[MEDIA_TABS.length - 1] : MEDIA_TABS[(index + (event.key === 'ArrowRight' ? 1 : -1) + MEDIA_TABS.length) % MEDIA_TABS.length];
        showMedia(type);
        document.getElementById('media_tab_' + type).focus();
    });
    $('.md-dialog-overlay').on('click', function (event) {
        if (event.target === this) closeDialog(this.id);
    });
    $('#keyword').on('keydown', function (e) {
        if (e.key === 'Enter') {
            this.blur();
            search();
        }
    });
    $('#push_url').on('keydown', function (e) {
        if (e.key === 'Enter') {
            this.blur();
            push();
        }
    });
    $('#danmaku_text').on('keydown', function (e) {
        if (e.key === 'Enter') {
            this.blur();
            sendDanmaku();
        }
    });
    $('#setting_name, #setting_text').on('keydown', function (e) {
        if (e.key === 'Enter') {
            this.blur();
            setting();
        }
    });
    $('#newFolderContent').on('keydown', function (e) {
        if (e.key === 'Enter') {
            this.blur();
            confirmNewFolder(1);
        }
    });
    $('#fileChangeValue').on('keydown', function (e) { if (e.key === 'Enter') confirmFileChange(); });
});
