// ==UserScript==
// @name         自由复制
// @version      1.0
// @description  解锁复制功能
// @author       网页
// @match        *://*/*
// @run-at       document-idle
// ==/UserScript==

(function () {
    'use strict';
    document.addEventListener('copy', function (e) {
        e.clipboardData.setData('text/plain', document.body.innerText);
        e.preventDefault();
    });
})()