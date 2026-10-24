const WEB_TABS = {
    search: {panel: 1, title: 'search'}, push: {panel: 1, title: 'push'},
    play: {panel: 2, title: 'remoteTitle'}, subtitles: {panel: 3, title: 'localSubtitle'}, online: {panel: 3, title: 'onlineSubtitle'}, danmaku: {panel: 3, title: 'danmaku'},
    settings: {panel: 4, title: 'settings'}, files: {panel: 5, title: 'local'},
    bili: {panel: 6, title: 'biliNav'}, curtain: {panel: 7, title: 'curtainNav'}, assrt: {panel: 3, title: 'onlineSubtitle'}
};

const WEB_TAB_NUMBERS = {1: 'search', 2: 'push', 3: 'danmaku', 4: 'settings', 5: 'files', 6: 'bili', 7: 'curtain', 8: 'assrt'};

function webRoute(search) {
    const params = new URLSearchParams(search), raw = params.get('tab');
    const name = Object.hasOwn(WEB_TABS, raw) ? raw : WEB_TAB_NUMBERS[raw] || 'search';
    return {tab: name, panel: WEB_TABS[name].panel, path: name === 'files' ? params.get('path') || '' : ''};
}

function webPageUrl(tab, path = '') {
    const params = new URLSearchParams({tab});
    if (tab === 'files' && path) params.set('path', path);
    return '/?' + params;
}

function remoteReturnRoute(search) {
    const params = new URLSearchParams(search);
    params.set('tab', params.get('from') || 'play');
    return webRoute(params);
}

function returnWebPage(tab, path = '') {
    const url = webPageUrl(tab, path), navigation = window.navigation;
    const previous = navigation?.entries()[navigation.currentEntry.index - 1];
    if (previous?.url === new URL(url, window.location.href).href) history.back();
    else window.location.replace(url);
}

function bindWebReturn(link, tab, path = '') {
    link.href = webPageUrl(tab, path);
    link.addEventListener('click', event => {
        if (event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        returnWebPage(tab, path);
    });
}

if (typeof module !== 'undefined') module.exports = {WEB_TABS, webRoute, webPageUrl, remoteReturnRoute, returnWebPage, bindWebReturn};
