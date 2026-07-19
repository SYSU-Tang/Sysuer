// ==UserScript==
// @name         SYSUER美化辅助增强
// @namespace    https://github.com/SYSU-Tang；https://github.com/anyvolunteer
// @version      1.4.Final
// @description  中大儿增强脚本(基于SYSUER美化辅助增强1.2）解决刷课可用性问题，包括网页净化、在线教学平台视频自动速通、(防丢进度双重校验)、(强力阻断实名水印)、自动跳下一页、自动登录、跳过验证等。
// @author       SYSU-Tang/anyvolunteer
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
// @grant        GM_setValue
// @grant        GM_getValue
// @grant        GM_registerMenuCommand
// @run-at       document-start
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
        removeWatermark: GM_getValue('removeWatermark', true) // 新增去水印配置
    };

    const { autoLogin, autoVerify, autoWebvpn, autoJumpLogin, username, password, videoComplete, videoJump, purify, removeWatermark } = config;

    const url = window.location.href;
    const host = window.location.hostname;

    /* ==================== 💧 强力去水印核心模块 ==================== */
    if (removeWatermark) {
        // 1. CSS 绝对压制（页面未完全加载时即可生效，防止闪烁）
        const style = document.createElement('style');
        style.innerHTML = `
            #wm_div_id, div[id^="mask_div_id"] {
                display: none !important;
                opacity: 0 !important;
                visibility: hidden !important;
                z-index: -999999 !important;
                pointer-events: none !important;
            }
        `;
        document.documentElement.appendChild(style);

        // 2. DOM 动态猎杀（等 DOM 准备好后监听，防止水印插件强行重置 CSS 或复活）
        document.addEventListener('DOMContentLoaded', () => {
            const killWatermark = () => {
                const wm = document.getElementById('wm_div_id');
                if (wm) {
                    wm.remove();
                    console.log('[SYSUER 脚本] 已成功拦截并清除实名水印');
                }
            };
            killWatermark(); // 初始清理一次

            // 开启守卫，有人敢建，我就敢删
            const observer = new MutationObserver(() => killWatermark());
            observer.observe(document.body, { childList: true, subtree: true });
        });
    }

    /* 以下功能需等页面完全加载后再执行 */
    window.addEventListener('load', function() {

        /* ==================== 悬浮按钮 ==================== */
        function createFloatingButton() {
            if (document.getElementById('sysuer-float-btn')) return;

            const btn = document.createElement('div');
            btn.id = 'sysuer-float-btn';
            btn.innerHTML = '⚙️';
            btn.title = '打开 SYSUER 脚本设置';
            btn.style.cssText = `
                position: fixed; bottom: 30px; right: 30px; width: 48px; height: 48px;
                background-color: ${SYSU_GREEN}; color: white; border-radius: 50%;
                display: flex; justify-content: center; align-items: center; font-size: 24px;
                cursor: pointer; box-shadow: 0 4px 12px rgba(0, 88, 38, 0.4);
                z-index: 999998; transition: transform 0.3s ease, box-shadow 0.3s ease; user-select: none;
            `;
            btn.addEventListener('mouseenter', () => { btn.style.transform = 'scale(1.1)'; btn.style.boxShadow = '0 6px 16px rgba(0, 88, 38, 0.6)'; });
            btn.addEventListener('mouseleave', () => { btn.style.transform = 'scale(1)'; btn.style.boxShadow = '0 4px 12px rgba(0, 88, 38, 0.4)'; });
            btn.addEventListener('click', createSettingsPanel);
            document.body.appendChild(btn);
        }

        /* ==================== 设置面板 GUI ==================== */
        function createSettingsPanel() {
            if (document.getElementById('sysuer-settings-panel')) return;
            const overlay = document.createElement('div');
            overlay.id = 'sysuer-settings-panel';
            overlay.style.cssText = `
                position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
                background: rgba(0, 0, 0, 0.5); z-index: 999999;
                display: flex; justify-content: center; align-items: center;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            `;
            const panel = document.createElement('div');
            panel.style.cssText = `
                background: #fff; padding: 24px; border-radius: 12px; width: 340px; max-width: 90%;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15); display: flex; flex-direction: column; gap: 10px; border-top: 5px solid ${SYSU_GREEN};
            `;
            panel.innerHTML = `
                <h3 style="margin: 0 0 10px 0; font-size: 18px; color: ${SYSU_GREEN}; text-align: center; font-weight: bold;">SYSUER 增强设置</h3>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">自动登录 <input type="checkbox" id="cfg-autoLogin" ${config.autoLogin ? 'checked' : ''}></label>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">跳过验证 <input type="checkbox" id="cfg-autoVerify" ${config.autoVerify ? 'checked' : ''}></label>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">自动跳转WebVPN <input type="checkbox" id="cfg-autoWebvpn" ${config.autoWebvpn ? 'checked' : ''}></label>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">自动点击登录按钮 <input type="checkbox" id="cfg-autoJumpLogin" ${config.autoJumpLogin ? 'checked' : ''}></label>
                <hr style="border: 0; border-top: 1px dashed #ccc; margin: 2px 0;">
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">阻断实名水印 (强力) <input type="checkbox" id="cfg-removeWatermark" ${config.removeWatermark ? 'checked' : ''}></label>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">平台视频自动速通 <input type="checkbox" id="cfg-videoComplete" ${config.videoComplete ? 'checked' : ''}></label>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">视频完成后自动跳下一页 <input type="checkbox" id="cfg-videoJump" ${config.videoJump ? 'checked' : ''}></label>
                <label style="display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;">页面净化 <input type="checkbox" id="cfg-purify" ${config.purify ? 'checked' : ''}></label>
                <hr style="border: 0; border-top: 1px dashed #ccc; margin: 2px 0;">
                <div style="display: flex; flex-direction: column; gap: 5px;"><label style="font-size: 12px; color: #555; font-weight: bold;">NetID 用户名:</label><input type="text" id="cfg-username" value="${config.username}" style="padding: 6px; border: 1px solid #ccc; border-radius: 4px; outline-color: ${SYSU_GREEN};"></div>
                <div style="display: flex; flex-direction: column; gap: 5px;"><label style="font-size: 12px; color: #555; font-weight: bold;">NetID 密码:</label><input type="password" id="cfg-password" value="${config.password}" style="padding: 6px; border: 1px solid #ccc; border-radius: 4px; outline-color: ${SYSU_GREEN};"></div>
                <div style="display: flex; gap: 10px; margin-top: 10px;">
                    <button id="cfg-save" style="flex: 1; padding: 8px; background: ${SYSU_GREEN}; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: opacity 0.2s;">保存设置</button>
                    <button id="cfg-close" style="flex: 1; padding: 8px; background: #f5f5f5; color: #333; border: 1px solid #d9d9d9; border-radius: 6px; cursor: pointer; transition: background 0.2s;">取消</button>
                </div>
            `;
            overlay.appendChild(panel); document.body.appendChild(overlay);
            document.getElementById('cfg-close').onclick = () => overlay.remove();
            document.getElementById('cfg-save').onclick = () => {
                ['autoLogin','autoVerify','autoWebvpn','autoJumpLogin','videoComplete','videoJump','purify','removeWatermark'].forEach(k => GM_setValue(k, document.getElementById(`cfg-${k}`).checked));
                ['username','password'].forEach(k => GM_setValue(k, document.getElementById(`cfg-${k}`).value));
                overlay.remove();
                window.toast ? toast.success('设置已保存，刷新页面后生效！', { backgroundColor: SYSU_GREEN }) : alert('设置已保存，刷新页面后生效！');
            };
        }
        createFloatingButton();
        GM_registerMenuCommand("⚙️ 脚本设置", createSettingsPanel);

        /* ==================== 辅助函数 ==================== */
        function hide(selectors) { selectors.forEach(v => { const el = document.querySelector(v); if (el) el.style.display = 'none'; }); }
        function click(el) { const element = document.querySelector(el); if (element) element.click(); }
        function waitElement(selector, callback, timeout = 5000) {
            const start = Date.now();
            function check() {
                const el = document.querySelector(selector);
                if (el) return callback(el);
                if (Date.now() - start >= timeout) return;
                setTimeout(check, 100);
            }
            check();
        }

        /* ==================== Toast 核心 ==================== */
        (function () {
            const COLORS = { success: SYSU_GREEN, error: '#ff4d4f', warning: '#faad14', info: '#1890ff' };
            const containers = {};
            function getContainer(position) {
                if (containers[position]) return containers[position];
                const container = document.createElement('div');
                container.className = `toast-container-${position}`;
                const isTop = position.startsWith('top'), isBottom = position.startsWith('bottom'), isLeft = position.endsWith('left'), isRight = position.endsWith('right'), isCenter = position.endsWith('center');
                let css = `position: fixed; z-index: 9999; display: flex; flex-direction: column; pointer-events: none; gap: 10px; max-width: 90vw; padding: 10px;`;
                if (isTop) css += 'top: 0;'; else if (isBottom) css += 'bottom: 0;';
                if (isLeft) css += 'left: 0; align-items: flex-start;'; else if (isRight) css += 'right: 0; align-items: flex-end;'; else if (isCenter) css += 'left: 50%; transform: translateX(-50%); align-items: center;';
                container.style.cssText = css; container._insertMethod = isTop ? 'prepend' : 'append';
                document.body.appendChild(container); containers[position] = container; return container;
            }
            function showToast(message, options = {}) {
                const { type = 'info', duration = 3000, position = 'top-right', direction = 'right', backgroundColor, pauseOnHover = true } = options;
                const container = getContainer(position); const toastEl = document.createElement('div');
                const bgColor = backgroundColor || COLORS[type] || COLORS.info;
                toastEl.style.cssText = `
                    padding: 12px 24px; border-radius: 8px; color: #fff; font-size: 14px; font-family: sans-serif;
                    background: ${bgColor}; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); opacity: 0;
                    transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55); pointer-events: auto; max-width: 360px; cursor: default;
                `;
                let transStart = 'translateX(100%)';
                if(direction==='left') transStart='translateX(-100%)'; else if(direction==='top') transStart='translateY(-100%)'; else if(direction==='bottom') transStart='translateY(100%)'; else if(direction==='fade') transStart='scale(0.95)';
                toastEl.style.transform = transStart; toastEl.textContent = message;
                if (container._insertMethod === 'prepend') container.insertBefore(toastEl, container.firstChild); else container.appendChild(toastEl);
                requestAnimationFrame(() => { toastEl.style.opacity = '1'; toastEl.style.transform = 'translate(0, 0) scale(1)'; });
                let remaining = duration, start = Date.now(), timerId = null;
                function removeToast() {
                    toastEl.style.opacity = '0'; toastEl.style.transform = transStart;
                    setTimeout(() => { if (toastEl.parentNode) toastEl.remove(); if (container.children.length === 0) { container.remove(); delete containers[position]; } }, 300);
                }
                function startTimer() { if (timerId) clearTimeout(timerId); timerId = setTimeout(removeToast, remaining); }
                function pauseTimer() { if (timerId) { clearTimeout(timerId); timerId = null; remaining -= (Date.now() - start); if (remaining < 0) remaining = 0; } }
                function resumeTimer() { start = Date.now(); startTimer(); }
                startTimer();
                if (pauseOnHover) { toastEl.addEventListener('mouseenter', pauseTimer); toastEl.addEventListener('mouseleave', resumeTimer); }
                toastEl.addEventListener('click', () => { clearTimeout(timerId); removeToast(); });
                return { close: removeToast };
            }
            window.toast = function (msg, opt) { return showToast(msg, opt); };
            window.toast.success = (msg, opt) => showToast(msg, { ...opt, type: 'success' });
            window.toast.error = (msg, opt) => showToast(msg, { ...opt, type: 'error' });
            window.toast.warning = (msg, opt) => showToast(msg, { ...opt, type: 'warning' });
            window.toast.info = (msg, opt) => showToast(msg, { ...opt, type: 'info' });
        })();

        /* ==================== 页面净化 ==================== */
        if (purify && host === 'www.sysu.edu.cn') hide(['.ftb']);
        if (purify && host === 'jwxt.sysu.edu.cn') {
            const purifyJwxt = () => {
                hide(['.sys-header', '.sys-footer', '.ant-breadcrumb']);
                if (url.includes('/jwxt/mk/')) { const sc = document.querySelector('.stu-con'); if (sc) sc.style.padding = '0px'; }
                if (url.includes('jwxt/mk/#/personalTrainingProgramView')) { hide(['.ant-tabs-bar']); document.querySelectorAll('col').forEach(e => e.style.minWidth = "0px"); const sc = document.querySelector('.stu-con'); if (sc) sc.style.padding = '0px'; }
                if (url.includes('jwxt/#/student')) { hide(['.sys-header', '.sys-footer']); waitElement('.invest2', c => c.style.display = 'none'); const c = document.querySelector('.ant-layout-content'); if (c) c.style.paddingTop = '0px'; waitElement('col', () => document.querySelectorAll('col').forEach(e => e.style.minWidth = "0px")); }
                if (url.includes('jwxt/mk/studentWeb/#/stuAchievementView') || url.includes('jwxt/mk/gradua/#/completionstatusStu')) waitElement('.cj-yxsh-con.cj-cx', c => { c.style.width = '100%'; c.style.margin = '0px'; });
                if (url.includes('#/notice/')) { waitElement('main', c => c.style.padding = '0px'); waitElement('.style-bread-3mo7c', c => c.style.maxWidth = '100%'); waitElement('.style-wrapper-3Oy8W', c => c.style.maxWidth = '100%'); }
                if (url.includes('/jwxt/mk/courseSelection')) click('.ant-notification-notice-close-x');
            };
            purifyJwxt(); toast.info('[SYSUER 脚本] 净化页面');
        }

        /* ==================== 🛑 刷课防丢进度核心逻辑 ==================== */
        if (videoComplete && /lms\.sysu\.edu\.cn\/mod\/.*?\/view\.php/.test(url)) {
            let retry = 0;
            const runVideoSpeedRun = () => {
                console.log('[SYSUER 脚本] 检测到视频页面，开始执行视频速通...');
                var sourceData = playerdata && playerdata.source ? JSON.parse(playerdata.source) : {};
                var sources = {};
                if (sourceData?.FD) { sources.FD = [{ src: sourceData.FD }]; }
                if (sourceData?.LD) { sources.LD = [{ src: sourceData.LD }]; }
                if (sourceData?.SD) { sources.SD = [{ src: sourceData.SD }]; }
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
                        toast.error('[SYSUER 脚本] 获取视频失败已达上限，脚本退出。');
                    }
                } else {
                    let count = 0;
                    const total = Math.floor(duration / 4) + 1;
                    const intervalId = setInterval(() => {
                        playerWrapper.viewTotalTime = 4000;
                        playerWrapper.ajaxOrder();
                        count++;
                        if (count >= total) {
                            clearInterval(intervalId);
                            startHybridValidation(total);
                        }
                    }, 10);
                }
            };

            function startHybridValidation(totalPackets) {
                const waitTimeMs = 5000 + (totalPackets * 80);
                const waitSec = (waitTimeMs / 1000).toFixed(0);
                toast.info(`[自动刷课] 数据已提交服务器，预计排队耗时 ${waitSec} 秒，请勿离开...`, { duration: waitTimeMs });

                setTimeout(() => {
                    startUIListen();
                }, waitTimeMs);
            }

            function startUIListen() {
                toast.warning('[自动刷课] 正在校验 UI 进度，确认 100% 后将自动跳转...', { duration: 3000 });
                let attempts = 0;
                const maxAttempts = 60;

                const uiTimer = setInterval(() => {
                    attempts++;
                    const progressSpan = document.querySelector('.num-bfjd span');
                    const progressP = document.querySelector('.num-bfjd');
                    let currentProgress = -1;

                    if (progressSpan && progressSpan.innerText) {
                        currentProgress = parseInt(progressSpan.innerText.trim(), 10);
                    } else if (progressP && progressP.innerText) {
                        currentProgress = parseInt(progressP.innerText.replace('%', '').trim(), 10);
                    }

                    if (currentProgress >= 100) {
                        clearInterval(uiTimer);
                        toast.success('[自动刷课] 恭喜！进度已 100% 落盘！', { duration: 4000 });
                        executeSafeJump();
                    } else if (attempts >= maxAttempts) {
                        clearInterval(uiTimer);
                        toast.error('[自动刷课] 等待 UI 确认超时，强制执行跳转！', { duration: 4000 });
                        executeSafeJump();
                    }
                }, 1000);
            }

            function executeSafeJump() {
                if (videoJump) {
                    setTimeout(() => {
                        toast.info('[SYSUER 脚本] 自动点击下一页...', { duration: 2000 });
                        click('#next-activity-link');
                    }, 1500);
                }
            }

            if (/lms\.sysu\.edu\.cn\/mod\/fsresource\/view\.php/.test(url)) {
                let videoAttempts = 0;
                const videoInterval = setInterval(() => {
                    if ((typeof playerdata !== 'undefined' && typeof TCPlayerWrapper !== 'undefined') || videoAttempts > 10) {
                        clearInterval(videoInterval);
                        if (typeof playerdata !== 'undefined') {
                            runVideoSpeedRun();
                        } else if (videoJump) {
                            toast.info('[SYSUER 脚本] 并非视频资源，点击下一页...');
                            click('#next-activity-link');
                        }
                    }
                    videoAttempts++;
                }, 500);
            } else if (videoJump) {
                toast.info('[SYSUER 脚本] 并非视频资源，点击下一页...');
                click('#next-activity-link');
            }
        }

        /* ==================== 自动登录与跳转 ==================== */
        if (autoVerify && url.includes('cas.sysu.edu.cn/login/mfaLogin.html')) {
            document.cookie = 'device_trust_Cookie=true; Path=/esc-sso; Domain=cas.sysu.edu.cn;';
            toast.info('[SYSUER 脚本] 跳过验证');
            var query = new URLSearchParams(url.split('?')[1]);
            var appUrl = query.get('appUrl');
            if (appUrl) window.location.href = decodeURIComponent(appUrl);
        }
        if (autoWebvpn && url.includes('appgw.sysu.edu.cn/')) {
            var q = new URLSearchParams(url.split('?')[1]);
            var cb = q.get('cb');
            if (cb) window.location.href = decodeURIComponent(cb).replace('.sysu.edu.cn', '-443.webvpn.sysu.edu.cn');
        }
        if (url.includes('visitor.sysu.edu.cn') && document.title.includes('Access Forbidden')) {
            window.location.href = url.replace('.sysu.edu.cn', '-443.webvpn.sysu.edu.cn');
        }
        function login(u, p) {
            waitElement('.para-widget-account-psw', comp => {
                var data = comp[Object.keys(comp).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;
                data.loginModel.dataField.username = u;
                data.loginModel.dataField.password = p;
                data.passwordInputVal = 'password';
                data.$loginBtn.click();
            });
        }
        if (autoJumpLogin) {
            if (url.includes('lms.sysu.edu.cn/login/index.php?local=')) window.location.href = "https://lms.sysu.edu.cn/login/index.php?authCAS=CAS";
            if (/visitor.*?.sysu.edu.cn\/login/.test(url)) waitElement('.netid-form .ant-btn.ant-btn-primary.ant-btn-lg.ant-btn-block.login-button', e => e.click());
            const clickButton = {
                'jwxt.sysu.edu.cn/jwxt/#/login': 'button.ant-btn.ant-btn-primary',
                'jwxt.sysu.edu.cn': '.ant-confirm-btns>button.ant-btn.ant-btn-primary',
                'lms.sysu.edu.cn/enrol/index.php?id=': '.continuebutton btn.btn-primary',
                'lms.sysu.edu.cn': '.loginBtn',
                'portal.sysu.edu.cn/newClient/#/login': '.index-loginData-XCumn>button.ant-btn.index-submit-3jXSy'
            };
            Object.entries(clickButton).forEach(([key, value]) => {
                if (url.includes(key)) waitElement(value, e => e.click());
            });
        }
        if (autoLogin && /cas.+?sysu\.edu\.cn\/esc-sso\/login\/page/.test(url) && username && password) {
            login(username, password);
            toast.info('[SYSUER 脚本] 自动登录中');
        }
    }); // 结束 load 监听
})();
