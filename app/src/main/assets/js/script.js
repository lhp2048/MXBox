const icDir = `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23F5A623'><path d='M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z'/></svg>`;
const icFile = `data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23717970'><path d='M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm4 18H6V4h7v5h5v11z'/></svg>`;
let currentRoot = '';
let currentFile = '';
let currentParent = '';
let longPressTimer = null;
let longPressTriggered = false;
let pendingDelFolder = null;
let warnToastTimer = null;
let danmakuMode = 1;
let danmakuSize = 25;
let dialogClosing = false;

function search() {
    const word = $('#keyword').val().trim();
    if (!word) return;
    if (new URLSearchParams(window.location.search).get('target') === 'bili') {
        $.post('/action', { do: 'bili', word: word }).done(function () {
            warnToast('已发送到电视');
        }).fail(function () {
            warnToast('发送失败');
        });
        return;
    }
    doAction('search', { word: word });
}

function push() {
    doAction('push', { url: $('#push_url').val() });
}

function setting() {
    doAction('setting', { text: $('#setting_text').val(), name: $('#setting_name').val() });
}

function saveFeed() {
    const url = $('#feed_server').val().trim();
    if (!url) return;
    $.post('/action', { do: 'feed', url: url }).done(function (text) {
        warnToast(text || '已设置');
    }).fail(function () {
        warnToast('设置失败');
    });
}

function sendDanmaku() {
    const text = $('#danmaku_text').val().trim();
    if (!text) return;
    doAction('danmaku', { text: `[0.0,${danmakuMode},${danmakuSize},16777215]${text}` });
    $('#danmaku_text').val('');
}

function showDanmakuModeDialog() {
    $('#danmakuModeDialog .md-dialog-list-item').removeClass('active');
    $(`#danmakuModeDialog .md-dialog-list-item[data-val="${danmakuMode}"]`).addClass('active');
    openDialog('danmakuModeDialog');
}

function setDanmakuMode(val, label) {
    danmakuMode = val;
    $('#danmaku_mode_label').text(label);
    closeDialog('danmakuModeDialog');
}

function showDanmakuSizeDialog() {
    $('#danmakuSizeDialog .md-dialog-list-item').removeClass('active');
    $(`#danmakuSizeDialog .md-dialog-list-item[data-val="${danmakuSize}"]`).addClass('active');
    openDialog('danmakuSizeDialog');
}

function setDanmakuSize(val, label) {
    danmakuSize = val;
    $('#danmaku_size_label').text(label);
    closeDialog('danmakuSizeDialog');
}

function doAction(action, kv) {
    $.post('/action', { ...kv, do: action });
}

function openDialog(id) {
    $('#' + id).show();
    history.pushState({ dialog: id }, '');
}

function closeDialog(id) {
    dialogClosing = true;
    $('#' + id).hide();
    history.back();
}

function startLongPress(callback) {
    longPressTriggered = false;
    longPressTimer = setTimeout(() => {
        longPressTriggered = true;
        callback();
    }, 500);
}

function cancelLongPress() {
    if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
}

function escPath(s) {
    return s.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

function escHtml(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function buildParentItem() {
    return `<a class="file-item" href="javascript:void(0)" onclick="history.back()">
    <img class="file-icon" src="${icDir}" alt="">
    <div class="file-info"><div class="file-name">..</div></div>
    </a>`;
}

function buildDirItem(name, time, path) {
    const ep = escPath(path);
    return `<a class="file-item" href="javascript:void(0)" ontouchstart="startLongPress(()=>showDelFolderDialog('${ep}',currentRoot))" ontouchmove="cancelLongPress()" ontouchend="cancelLongPress()" oncontextmenu="return false" onclick="if(!longPressTriggered)listFile('${ep}',true)">
    <img class="file-icon" src="${icDir}" alt="">
    <div class="file-info"><div class="file-name">${escHtml(name)}</div><div class="file-time">${escHtml(time)}</div></div>
    </a>`;
}

function buildFileItem(name, time, path) {
    const ep = escPath(path);
    return `<a class="file-item" href="javascript:void(0)" ontouchstart="startLongPress(()=>showDelFileDialog('${ep}'))" ontouchmove="cancelLongPress()" ontouchend="cancelLongPress()" oncontextmenu="return false" onclick="if(!longPressTriggered)selectFile('${ep}')">
    <img class="file-icon" src="${icFile}" alt="">
    <div class="file-info"><div class="file-name">${escHtml(name)}</div><div class="file-time">${escHtml(time)}</div></div>
    </a>`;
}

function addFile(node) {
    $('#file_list').append(node);
}

function selectFile(path) {
    currentFile = path;
    $("#fileUrl").text("file:/" + path);
    openDialog('fileInfoDialog');
}

function pushFile(yes) {
    closeDialog('fileInfoDialog');
    if (yes === 1) doAction('file', { path: "file:/" + currentFile });
}

function listFile(path, addHistory = false) {
    const loadingTimer = setTimeout(() => $('#loadingToast').show(), 200);
    $.get('/file' + path, function (res) {
        clearTimeout(loadingTimer);
        let info;
        try {
            info = JSON.parse(res);
        } catch (e) {
            $('#loadingToast').hide();
            warnToast('响应格式错误');
            return;
        }
        const parent = info.parent;
        currentRoot = path;
        currentParent = parent;
        const array = info.files;
        if (path === '' && array.length === 0) warnToast('可能没有存储权限');
        $('#file_list').html('');
        if (parent !== '.') addFile(buildParentItem());
        array.forEach(node => {
            if (node.dir === 1) addFile(buildDirItem(node.name, node.time, node.path));
            else addFile(buildFileItem(node.name, node.time, node.path));
        });
        if (addHistory) history.pushState(path, '');
        $('#loadingToast').hide();
    }).fail(function () {
        clearTimeout(loadingTimer);
        $('#loadingToast').hide();
        warnToast('载入失败');
    });
}

function uploadFile() {
    $('#file_uploader').click();
}

function onFileSelected() {
    const files = $('#file_uploader')[0].files;
    if (files.length === 0) return;
    const tip = Array.from(files).map(f => f.name).join(', ');
    $('#uploadTipContent').text(tip);
    openDialog('uploadTip');
}

function confirmUpload(yes) {
    closeDialog('uploadTip');
    if (yes !== 1) return;
    const files = $('#file_uploader')[0].files;
    if (files.length === 0) return;
    const formData = new FormData();
    formData.append('path', currentRoot);
    Array.from(files).forEach((f, i) => formData.append('files-' + i, f));
    $('#loadingToast').show();
    $.ajax({
        url: '/upload',
        type: 'post',
        data: formData,
        processData: false,
        contentType: false,
        complete: function () {
            $('#loadingToast').hide();
            $('#file_uploader').val('');
            listFile(currentRoot);
        }
    });
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
        warnToast('新建失败');
    });
}

function showDelFolderDialog(path, refreshPath) {
    pendingDelFolder = { path, refreshPath };
    $('#delFolderContent').text('是否删除 ' + path);
    openDialog('delFolder');
}

function confirmDelFolder(yes) {
    closeDialog('delFolder');
    if (yes !== 1 || !pendingDelFolder) { pendingDelFolder = null; return; }
    const { path, refreshPath } = pendingDelFolder;
    pendingDelFolder = null;
    $('#loadingToast').show();
    $.post('/delFolder', { path }, function () {
        $('#loadingToast').hide();
        listFile(refreshPath);
    }).fail(function () {
        $('#loadingToast').hide();
        warnToast('删除失败');
    });
}

function showDelFileDialog(path) {
    currentFile = path;
    $('#delFileContent').text('是否删除 ' + path);
    openDialog('delFile');
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
        warnToast('删除失败');
    });
}

function warnToast(msg) {
    $('#warnToastContent').text(msg);
    $('#warnToast').show();
    if (warnToastTimer) clearTimeout(warnToastTimer);
    warnToastTimer = setTimeout(() => { $('#warnToast').hide(); warnToastTimer = null; }, 1000);
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
        row.className = 'curtain-track';
        const label = document.createElement('label');
        label.className = 'curtain-check';
        const check = document.createElement('input');
        check.type = 'checkbox';
        check.checked = !item.hidden;
        check.onchange = () => {
            items[index].hidden = !check.checked;
            saveHomeButtons(items);
        };
        label.appendChild(check);
        label.appendChild(document.createTextNode(item.label));
        const actions = document.createElement('span');
        actions.className = 'curtain-row';
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
    button.className = 'md-btn md-btn-tonal';
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

function showPanel(id) {
    if (MIDDLE[id]) {
        middlePanel = id;
        $('#midLabel').text(MIDDLE[id].label);
        $('#midIconPath').attr('d', MIDDLE[id].icon);
    }
    for (let i = 1; i <= 8; i++) {
        const panel = document.getElementById('panel' + i);
        if (panel) panel.classList.toggle('active', i === id);
    }
    document.getElementById('tab1').classList.toggle('active', id === 1);
    document.getElementById('tabMid').classList.toggle('active', id === middlePanel);
    document.getElementById('tab4').classList.toggle('active', id === 4);
    $('.md-more-item').removeClass('current');
    $('.md-more-item[data-panel="' + middlePanel + '"]').addClass('current');
    closeMore();
    if (id === 5 && document.getElementById('file_list').innerHTML === '') listFile('');
    if (id === 4) loadHomeButtons();
    if (id === 6) loadBiliFilter();
    if (id === 8) loadAssrt();
    watchCurtain(id === 7);
}

function showMiddle() {
    showPanel(middlePanel);
}

function toggleMore(event) {
    event.stopPropagation();
    $('#moreMenu').toggleClass('open');
}

function closeMore() {
    $('#moreMenu').removeClass('open');
}

function pickMiddle(id) {
    showPanel(id);
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

const MIDDLE = {
    2: { label: '推送', icon: 'M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM19 18H6c-2.21 0-4-1.79-4-4 0-2.05 1.53-3.76 3.56-3.97l1.07-.11.5-.95C8.08 7.14 9.94 6 12 6c2.62 0 4.88 1.86 5.39 4.43l.3 1.5 1.53.11c1.56.1 2.78 1.41 2.78 2.96 0 1.65-1.35 3-3 3zM8 13h2.55v3h2.9v-3H16l-4-4z' },
    3: { label: '弹幕', icon: 'M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z' },
    5: { label: '本地', icon: 'M9.17 6l2 2H20v10H4V6h5.17M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z' },
    6: { label: 'Bilibili', icon: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z' },
    7: { label: '幕布', icon: 'M4,3h16v3H4zM5,7h6v13H5zM13,7h6v13h-6z' },
    8: { label: '字幕', icon: 'M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zM4 12h4v2H4v-2zm10 6H4v-2h10v2zm6 0h-4v-2h4v2zm0-4H10v-2h10v2z' }
};
let middlePanel = 2;
let curtainTimer = null;
let curtainReady = false;

const CURTAIN_COLORS = ['101114', '000000', 'FFFFFF', '8B1E3F', '1B4332', '1D3557', 'C45C26', 'F4D35E'];

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

function loadAssrt() {
    $.get('/assrt', { do: 'state' }).done(function (data) {
        const body = assrtData(data);
        $('#assrt_token').val('');
        $('#assrt_token_state').text(body.set ? '已设置' : '');
    });
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

const tab = parseInt(new URLSearchParams(window.location.search).get('tab')) || 1;
history.replaceState(null, '');
showPanel(tab);
if (new URLSearchParams(window.location.search).get('target') === 'bili') {
    document.title = 'B站搜索';
    $('#keyword').attr('placeholder', '请输入 B站 搜索内容');
}

window.addEventListener('popstate', function () {
    if (dialogClosing) { dialogClosing = false; return; }
    const visible = $('.md-dialog-overlay:visible');
    if (visible.length) { visible.first().hide(); return; }
    listFile(currentParent);
});

$(function () {
    $('#keyword').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); search(); } });
    $('#push_url').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); push(); } });
    $('#danmaku_text').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); sendDanmaku(); } });
    $('#setting_name, #setting_text').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); setting(); } });
    $('#feed_server').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); saveFeed(); } });
    $('#assrt_token').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); saveAssrtToken(); } });
    $('#assrt_q').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); searchAssrt(); } });
    $('#newFolderContent').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); confirmNewFolder(1); } });
    $('#curtain_text').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); curtainText(); } });
    $('#curtain_color').on('change', function () { curtainPost({ do: 'color', value: this.value }); });
    $('#curtain_image_fit').on('change', function () { curtainPost({ do: 'imageFit', mode: this.value }); });
    $('#curtain_video_fit').on('change', function () { curtainPost({ do: 'videoFit', mode: this.value }); });
    $('#curtain_audio').on('change', function () { curtainPost({ do: 'audio', source: this.value }); });
    $('#curtain_music_mode').on('change', function () { curtainPost({ do: 'musicMode', mode: this.value }); });
    $('#curtain_volume').on('change', function () { curtainPost({ do: 'volume', value: this.value }); });
    $('#bili_filter_on').on('change', saveBiliFilter);
    $('#bili_filter_words').on('keydown', function (e) { if (e.key === 'Enter') { this.blur(); } });
    $('#bili_filter_words').on('blur', saveBiliFilter);
    $(document).on('click', function () { closeMore(); });
});
