// ==UserScript==
// @name         SYSUER美化辅助增强
// @namespace    https://github.com/SYSU-Tang
// @version      1.6
// @description  中大儿增强脚本，包括网页净化、在线教学平台视频自动速通、自动跳下一页、自动登录、跳过验证、自动跳转登录页。
// @author       SYSU-Tang
// @license      Apache-2.0
// @updateURL    https://github.com/SYSU-Tang/sysuer.user.js/raw/main/sysuer.meta.js
// @downloadURL  https://github.com/SYSU-Tang/sysuer.user.js/raw/main/sysuer.user.js
// @homepage     https://github.com/SYSU-Tang/sysuer.user.js
// @match        *://www.sysu.edu.cn/*
// @match        *://jwxt.sysu.edu.cn/*
// @match        *://portal.sysu.edu.cn/*
// @match        *://cas.sysu.edu.cn/esc-sso/login/page
// @match        *://lms.sysu.edu.cn/*
// @match        *://cas.sysu.edu.cn/*
// @match        *://appgw.sysu.edu.cn/*
// @match        *://visitor.sysu.edu.cn/*
// @match        *://visitor-443.webvpn.sysu.edu.cn/*
// @match        *://pay.sysu.edu.cn/*
// @grant        GM_setValue
// @grant        GM_getValue
// @grant        GM_registerMenuCommand
// ==/UserScript==

(function () {
    'use strict';
    const SYSU_GREEN = '#005826';

    /* ==================== 配置读取 ==================== */
    const config = {
        autoLogin: GM_getValue('autoLogin', true),
        autoVerify: GM_getValue('autoVerify', true),
        autoWebvpn: GM_getValue('autoWebvpn', true),
        autoJumpLogin: GM_getValue('autoJumpLogin', true),
        username: GM_getValue('username', ''),
        password: GM_getValue('password', ''),
        videoComplete: GM_getValue('videoComplete', true),
        videoJump: GM_getValue('videoJump', true),
        purify: GM_getValue('purify', true),
        removeRatermark: GM_getValue('removeRatermark', true)
    };

    const { autoLogin, autoVerify, autoWebvpn, autoJumpLogin, username, password, videoComplete, videoJump, purify, removeRatermark } = config;

    const url = window.location.href;
    const host = window.location.hostname;

    /* ==================== 悬浮按钮 ==================== */
    function createFloatingButton() {
        if (document.getElementById('sysuer-float-btn')) return;

        const btn = document.createElement('div');
        btn.id = 'sysuer-float-btn';
        btn.innerHTML = '⚙️';
        btn.title = '打开 SYSUER 脚本设置';
        btn.style.cssText = `
            position: fixed;
            bottom: 30px;
            right: 30px;
            width: 48px;
            height: 48px;
            background-color: ${SYSU_GREEN};
            color: white;
            border-radius: 50%;
            display: flex;
            justify-content: center;
            align-items: center;
            font-size: 24px;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(0, 88, 38, 0.4);
            z-index: 999998;
            transition: transform 0.3s ease, box-shadow 0.3s ease;
            user-select: none;
        `;

        // 悬停动画
        btn.addEventListener('mouseenter', () => {
            btn.style.transform = 'scale(1.1)';
            btn.style.boxShadow = '0 6px 16px rgba(0, 88, 38, 0.6)';
        });
        btn.addEventListener('mouseleave', () => {
            btn.style.transform = 'scale(1)';
            btn.style.boxShadow = '0 4px 12px rgba(0, 88, 38, 0.4)';
        });

        // 点击打开面板
        btn.addEventListener('click', createSettingsPanel);

        document.body.appendChild(btn);
    }

    /* ==================== 设置面板 GUI ==================== */
    function createSettingsPanel() {
        if (document.getElementById('sysuer-settings-panel')) return;

        // 背景遮罩
        const overlay = document.createElement('div');
        overlay.id = 'sysuer-settings-panel';
        overlay.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(0, 0, 0, 0.5); z-index: 999999;
            display: flex; justify-content: center; align-items: center;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        `;

        // 面板主体
        const panel = document.createElement('div');
        panel.style.cssText = `
            background: #fff; padding: 24px; border-radius: 12px;
            width: 320px; max-width: 90%; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            display: flex; flex-direction: column; gap: 12px;
            border-top: 5px solid ${SYSU_GREEN};
        `;

        panel.innerHTML = `
            <h3 style="margin: 0 0 10px 0; font-size: 18px; color: ${SYSU_GREEN}; text-align: center; font-weight: bold;">SYSUER 增强设置</h3>

            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                自动登录 <input type="checkbox" id="cfg-autoLogin" ${config.autoLogin ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                跳过验证 <input type="checkbox" id="cfg-autoVerify" ${config.autoVerify ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                自动跳转WebVPN <input type="checkbox" id="cfg-autoWebvpn" ${config.autoWebvpn ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                自动点击登录按钮 <input type="checkbox" id="cfg-autoJumpLogin" ${config.autoJumpLogin ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                在线教学平台视频自动速通 <input type="checkbox" id="cfg-videoComplete" ${config.videoComplete ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                在线教学平台视频完成后自动跳下一页 <input type="checkbox" id="cfg-videoJump" ${config.videoJump ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                页面净化 <input type="checkbox" id="cfg-purify" ${config.purify ? 'checked' : ''}>
            </label>
            <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">
                移除水印 <input type="checkbox" id="cfg-removeRatermark" ${config.removeRatermark ? 'checked' : ''}>
            </label>

            <hr style="border: 0; border-top: 1px dashed #ccc; margin: 5px 0;">

            <div style="display: flex; flex-direction: column; gap: 5px;">
                <label style="font-size: 12px; color: #555; font-weight: bold;">NetID 用户名:</label>
                <input type="text" id="cfg-username" value="${config.username}" style="padding: 6px; border: 1px solid #ccc; border-radius: 4px; outline-color: ${SYSU_GREEN};">
            </div>
            <div style="display: flex; flex-direction: column; gap: 5px;">
                <label style="font-size: 12px; color: #555; font-weight: bold;">NetID 密码:</label>
                <input type="password" id="cfg-password" value="${config.password}" style="padding: 6px; border: 1px solid #ccc; border-radius: 4px; outline-color: ${SYSU_GREEN};">
            </div>

            <div style="display: flex; gap: 10px; margin-top: 10px;">
                <button id="cfg-save" style="flex: 1; padding: 8px; background: ${SYSU_GREEN}; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: opacity 0.2s;">保存设置</button>
                <button id="cfg-close" style="flex: 1; padding: 8px; background: #f5f5f5; color: #333; border: 1px solid #d9d9d9; border-radius: 6px; cursor: pointer; transition: background 0.2s;">取消</button>
            </div>
        `;

        overlay.appendChild(panel);
        document.body.appendChild(overlay);

        // 按钮交互效果
        document.getElementById('cfg-save').onmouseenter = function () { this.style.opacity = '0.85'; };
        document.getElementById('cfg-save').onmouseleave = function () { this.style.opacity = '1'; };
        document.getElementById('cfg-close').onmouseenter = function () { this.style.background = '#e8e8e8'; };
        document.getElementById('cfg-close').onmouseleave = function () { this.style.background = '#f5f5f5'; };

        // 绑定事件
        document.getElementById('cfg-close').onclick = () => overlay.remove();
        document.getElementById('cfg-save').onclick = () => {
            GM_setValue('autoLogin', document.getElementById('cfg-autoLogin').checked);
            GM_setValue('autoVerify', document.getElementById('cfg-autoVerify').checked);
            GM_setValue('autoWebvpn', document.getElementById('cfg-autoWebvpn').checked);
            GM_setValue('autoJumpLogin', document.getElementById('cfg-autoJumpLogin').checked);
            GM_setValue('videoComplete', document.getElementById('cfg-videoComplete').checked);
            GM_setValue('videoJump', document.getElementById('cfg-videoJump').checked);
            GM_setValue('purify', document.getElementById('cfg-purify').checked);
            GM_setValue('removeRatermark', document.getElementById('cfg-removeRatermark').checked);
            GM_setValue('username', document.getElementById('cfg-username').value);
            GM_setValue('password', document.getElementById('cfg-password').value);

            overlay.remove();
            if (window.toast) {
                toast.success('设置已保存，刷新页面后生效！', { backgroundColor: SYSU_GREEN });
            } else {
                alert('设置已保存，刷新页面后生效！');
            }
        };
    }

    // 初始化悬浮按钮
    window.addEventListener('load', createFloatingButton);

    // 注册油猴菜单 (保留双重入口)
    GM_registerMenuCommand("⚙️ 脚本设置", createSettingsPanel);


    /* ==================== 核心逻辑功能区 ==================== */

    /* 隐藏元素 */
    function hide(selectors) {
        selectors.forEach(function (v) {
            const el = document.querySelector(v);
            if (el) el.style.display = 'none';
        });
    }
    /* 点击元素 */
    function click(el) {
        const element = document.querySelector(el);
        if (element) element.click();
    }
    /* 等待元素出现 */
    function waitElement(selector, callback, timeout = 5000, timeoutCallback = null) {
        const startTime = Date.now();
        let stopped = false;

        function check() {
            if (stopped) return;
            const element = document.querySelector(selector);
            if (element) {
                stopped = true;
                callback(element);
                return;
            }
            if (Date.now() - startTime >= timeout) {
                stopped = true;
                if (typeof timeoutCallback === 'function') {
                    timeoutCallback(selector);
                } else {
                    console.log(`[SYSUER 脚本] 等待元素 "${selector}" 超时（${timeout}ms）`);
                }
                return;
            }
            setTimeout(check, 100);
        }
        check();
    }

    // ==================== Toast 核心 ====================
    (function () {
        const COLORS = { success: SYSU_GREEN, error: '#ff4d4f', warning: '#faad14', info: '#1890ff' }; // 成功提示改为中大绿
        const containers = {};

        function getContainer(position) {
            if (containers[position]) return containers[position];
            const container = document.createElement('div');
            container.className = `toast-container-${position}`;
            const isTop = position.startsWith('top');
            const isBottom = position.startsWith('bottom');
            const isLeft = position.endsWith('left');
            const isRight = position.endsWith('right');
            const isCenter = position.endsWith('center');

            let css = `position: fixed; z-index: 9999; display: flex; flex-direction: column; pointer-events: none; gap: 10px; max-width: 90vw; padding: 10px;`;
            if (isTop) css += 'top: 0;'; else if (isBottom) css += 'bottom: 0;';
            if (isLeft) css += 'left: 0; align-items: flex-start;';
            else if (isRight) css += 'right: 0; align-items: flex-end;';
            else if (isCenter) css += 'left: 50%; transform: translateX(-50%); align-items: center;';

            container.style.cssText = css;
            container._insertMethod = isTop ? 'prepend' : 'append';
            document.body.appendChild(container);
            containers[position] = container;
            return container;
        }

        function showToast(message, options = {}) {
            if (typeof options === 'string') options = { type: options };
            if (typeof arguments[2] === 'number') options.duration = arguments[2];

            const { type = 'info', duration = 3000, position = 'top-right', direction = 'right', backgroundColor, pauseOnHover = true } = options;
            const container = getContainer(position);
            const toast = document.createElement('div');
            toast.className = 'toast-item';
            const bgColor = backgroundColor || COLORS[type] || COLORS.info;

            toast.style.cssText = `
                padding: 12px 24px; border-radius: 8px; color: #fff; font-size: 14px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                background: ${bgColor}; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); opacity: 0;
                transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55); pointer-events: auto;
                max-width: 360px; word-break: break-word; cursor: default; flex-shrink: 0;
            `;

            let transformStart = 'translateX(100%)';
            switch (direction) {
                case 'right': transformStart = 'translateX(100%)'; break;
                case 'left': transformStart = 'translateX(-100%)'; break;
                case 'top': transformStart = 'translateY(-100%)'; break;
                case 'bottom': transformStart = 'translateY(100%)'; break;
                case 'fade': transformStart = 'scale(0.95)'; break;
            }
            toast.style.transform = transformStart;
            toast.textContent = message;

            if (container._insertMethod === 'prepend') container.insertBefore(toast, container.firstChild);
            else container.appendChild(toast);

            requestAnimationFrame(() => {
                toast.style.opacity = '1';
                toast.style.transform = 'translate(0, 0) scale(1)';
            });

            let remaining = duration, startTime = Date.now(), timerId = null;

            function removeToast() {
                toast.style.opacity = '0';
                toast.style.transform = transformStart;
                setTimeout(() => {
                    if (toast.parentNode) toast.remove();
                    if (container.children.length === 0) { container.remove(); delete containers[position]; }
                }, 300);
            }

            function startTimer() {
                if (timerId) clearTimeout(timerId);
                timerId = setTimeout(removeToast, remaining);
            }

            function pauseTimer() {
                if (timerId) {
                    clearTimeout(timerId); timerId = null;
                    remaining -= (Date.now() - startTime);
                    if (remaining < 0) remaining = 0;
                }
            }

            function resumeTimer() { startTime = Date.now(); startTimer(); }
            startTimer();

            if (pauseOnHover) {
                toast.addEventListener('mouseenter', pauseTimer);
                toast.addEventListener('mouseleave', resumeTimer);
            }
            toast.addEventListener('click', () => { clearTimeout(timerId); removeToast(); });
            return { close: removeToast, pause: pauseTimer, resume: resumeTimer };
        }

        window.toast = function (message, options) { return showToast(message, options); };
        window.toast.success = (msg, options) => showToast(msg, { ...options, type: 'success' });
        window.toast.error = (msg, options) => showToast(msg, { ...options, type: 'error' });
        window.toast.warning = (msg, options) => showToast(msg, { ...options, type: 'warning' });
        window.toast.info = (msg, options) => showToast(msg, { ...options, type: 'info' });
    })();

    if (purify && host === 'www.sysu.edu.cn') {
        hide(['.ftb']);
    }
    if (purify && host === 'jwxt.sysu.edu.cn') {
        const purifyJwxt = () => {
            hide(['.sys-header', '.sys-footer', '.ant-breadcrumb']);
            if (url.includes('/jwxt/mk/')) {
                const stuCon = document.querySelector('.stu-con');
                if (stuCon) stuCon.style.padding = '0px';
            }
            if (url.includes('jwxt/mk/#/personalTrainingProgramView')) {
                hide(['.ant-tabs-bar']);
                document.querySelectorAll('col').forEach(element => { element.style.minWidth = "0px"; });
                const stuCon = document.querySelector('.stu-con');
                if (stuCon) stuCon.style.padding = '0px';
            }
            if (url.includes('jwxt/#/student')) {
                hide(['.sys-header', '.sys-footer']);
                waitElement('.invest2', content => { content.style.display = 'none'; });
                const content = document.querySelector('.ant-layout-content');
                if (content) content.style.paddingTop = '0px';
                waitElement('col', content => {
                    document.querySelectorAll('col').forEach(element => { element.style.minWidth = "0px"; });
                });
            }
            if (url.includes('jwxt/mk/studentWeb/#/stuAchievementView') || url.includes('jwxt/mk/gradua/#/completionstatusStu')) {
                waitElement('.cj-yxsh-con.cj-cx', content => { content.style.width = '100%'; content.style.margin = '0px'; });
            }
            if (url.includes('#/notice/')) {
                waitElement('main', content => { content.style.padding = '0px'; });
                waitElement('.style-bread-3mo7c', content => { content.style.maxWidth = '100%'; });
                waitElement('.style-wrapper-3Oy8W', content => { content.style.maxWidth = '100%'; });
            }
            if (url.includes('/jwxt/mk/courseSelection')) {
                click('.ant-notification-notice-close-x');
            }
        };
        window.addEventListener('load', () => {
            purifyJwxt();
            toast.info('[SYSUER 脚本] 净化页面');
        });
    }

    if (videoComplete && /lms\.sysu\.edu\.cn\/mod\/.*?\/view\.php/.test(url)) {
        let retry = 0;
        function upload(playerWrapper, playerdata, callback) {
            const data = [{
                index: 0,
                methodname: 'mod_fsresource_set_time',
                args: {
                    fsresourceid: playerdata.fsresourceid,
                    time: 4,
                    finish: 1,
                    progress: 100,
                    unique: playerWrapper.pageId,
                },
            }];

            fetch("https://lms.sysu.edu.cn/lib/ajax/service.php?sesskey=" + playerdata.sesskey, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data),
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log('Success:', data);
                    const progress = data[0].data.progress;
                    const totaltime = data[0].data.totaltime;
                    callback(progress, totaltime);
                })
                .catch(error => {
                    console.error('Error:', error);
                    // 错误处理
                });
        }
        const runVideoSpeedRun = () => {
            console.log('[SYSUER 脚本] 检测到视频页面，开始执行视频速通...');
            var sourceData = playerdata && playerdata.source ? JSON.parse(playerdata.source) : {};
            var sources = {};
            var defaultRes = '';

            if (sourceData?.FD) { sources.FD = [{ src: sourceData.FD }]; }
            if (sourceData?.LD) { sources.LD = [{ src: sourceData.LD }]; }
            if (sourceData?.SD) { sources.SD = [{ src: sourceData.SD }]; defaultRes = 'SD'; }
            if (sourceData?.HD) { sources.HD = [{ src: sourceData.HD }]; }
            if (sourceData?.OD) { sources.FHD = [{ src: sourceData.OD }]; }

            var playerWrapper = new TCPlayerWrapper(
                "fsplayer-container-id_html5_api",
                sources,
                playerdata.siteUrl + "/lib/ajax/service.php?sesskey=" + playerdata.sesskey,
                `fs_${playerdata.userid}_${playerdata.fsresourceid || 0}`,
                15 * 1000,
                playerdata.progress == 1
            );
            var duration = playerWrapper.player.duration();
            if (isNaN(duration) || duration === 0) {
                if (retry < 15) {
                    toast.error('[SYSUER 脚本] 视频时长获取失败，1秒后重试...');
                    setTimeout(runVideoSpeedRun, 1000);
                    retry++;
                } else {
                    toast.error('[SYSUER 脚本] 视频时长获取失败，15次重试均失败，脚本已退出！');
                }
            } else {
                const jump = () => {
                    if (videoJump) {
                        toast.info('[SYSUER 脚本] 视频速通完成，点击下一页...');
                        click('#next-activity-link');
                    }
                }
                upload(playerWrapper, playerdata, (progress, totaltime) => {
                    if (progress == "100") {
                        toast.success('[SYSUER 脚本] 当前视频完成');
                        if (videoJump) {
                            jump();
                        }
                    } else {
                        let count = 0;
                        const total = Math.floor((totaltime - duration) / 4) + 1;
                        const intervalId = setInterval(() => {
                            playerWrapper.viewTotalTime = 4000;
                            playerWrapper.ajaxOrder();
                            count++;
                            if (count >= total) {
                                clearInterval(intervalId);
                            }
                        }, 10);
                        toast.success('[SYSUER 脚本] 视频进度已全额提交！');
                        if (videoJump) {
                            const test = () => upload(playerWrapper, playerdata, (progress, totaltime) => {
                                if (progress == "100") {
                                    jump();
                                } else {
                                    setTimeout(test, 500);
                                }
                            });
                        }
                    }
                });
            }
        };
        if (/lms\.sysu\.edu\.cn\/mod\/fsresource\/view\.php/.test(url)) {
            if (removeWatermark) {
                watermark.remove()
            }
            let videoAttempts = 0;
            const videoInterval = setInterval(() => {
                if ((typeof playerdata !== 'undefined' && typeof TCPlayerWrapper !== 'undefined') || videoAttempts > 10) {
                    clearInterval(videoInterval);
                    if (typeof playerdata !== 'undefined') {
                        runVideoSpeedRun();
                    } else if (videoJump) {
                        toast.info('[SYSUER 脚本] 视频播放器未加载，点击下一页...');
                        click('#next-activity-link');
                    }
                }
                videoAttempts++;
            }, 500);
        } else if (videoJump) {
            toast.info('[SYSUER 脚本] 视频播放器未加载，点击下一页...');
            click('#next-activity-link');
        }
    }
    // https://cas.sysu.edu.cn/login/mfaLogin.html?appId=2256471041591329591&appUrl=https%3A%2F%2Fcas.sysu.edu.cn%2Fesc-sso%2Flogin%3Fservice%3Dhttps%253A%252F%252Fjwxt.sysu.edu.cn%252Fjwxt%252Fapi%252Fsso%252Fcas%252Flogin%253Fpattern%253Dstudent-login
    if (autoVerify && url.includes('cas.sysu.edu.cn/login/mfaLogin.html')) {
        document.cookie = 'device_trust_Cookie=true; Path=/esc-sso; Domain=cas.sysu.edu.cn;';
        toast.info('[SYSUER 脚本] 跳过验证');
        var query = new URLSearchParams(url.split('?')[1]);
        var appUrl = query.get('appUrl');
        if (appUrl) {
            appUrl = decodeURIComponent(appUrl);
            window.location.href = appUrl;
        }
    }

    if (autoWebvpn && url.includes('appgw.sysu.edu.cn/')) {
        var query = new URLSearchParams(url.split('?')[1]);
        var cb = query.get('cb');
        if (cb) {
            cb = decodeURIComponent(cb);
            window.location.href = cb.replace('.sysu.edu.cn', '-443.webvpn.sysu.edu.cn');
        }
    }

    if (url.includes('visitor.sysu.edu.cn') && document.title.includes('Access Forbidden')) {
        window.location.href = url.replace('.sysu.edu.cn', '-443.webvpn.sysu.edu.cn');
    }

    function login(username, password) {
        waitElement('.para-widget-account-psw', component => {
            var data = component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;
            data.loginModel.dataField.username = username;
            data.loginModel.dataField.password = password;
            data.passwordInputVal = 'password';
            data.$loginBtn.click();
        });
    }

    if (autoJumpLogin) {
        if (url.includes('lms.sysu.edu.cn/login/index.php?local=')) {
            window.location.href = "https://lms.sysu.edu.cn/login/index.php?authCAS=CAS";
        }
        if (/visitor.*?.sysu.edu.cn\/login/.test(url)) {
            waitElement('.netid-form .ant-btn.ant-btn-primary.ant-btn-lg.ant-btn-block.login-button', e => {
                e.click();
            });
        }
        const clickButton = {
            'jwxt.sysu.edu.cn/jwxt/#/login': 'button.ant-btn.ant-btn-primary',
            'jwxt.sysu.edu.cn': '.ant-confirm-btns>button.ant-btn.ant-btn-primary',
            'lms.sysu.edu.cn/enrol/index.php?id=': '.continuebutton btn.btn-primary',
            'lms.sysu.edu.cn': '.loginBtn',
            'portal.sysu.edu.cn/newClient/#/login': '.index-loginData-XCumn>button.ant-btn.index-submit-3jXSy',
            'pay.sysu.edu.cn': '.el-button.login_btns',
        };
        window.addEventListener('load', function () {
            Object.entries(clickButton).forEach(([key, value]) => {
                if (url.includes(key)) {
                    waitElement(value, e => {
                        e.click();
                        return;
                    });
                }
            });
        });
    }
    if (autoLogin && /cas.+?sysu\.edu\.cn\/esc-sso\/login\/page/.test(url) && username && password) {
        login(username, password);
        toast.info('[SYSUER 脚本] 自动登录中');
    }

})();