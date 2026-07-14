// ==UserScript==
// @name         网页调试助手
// @namespace    https://github.com/liriliri/eruda
// @version      1.0
// @description  网页调试助手，使用eruda用于网页调试
// @homepage     https://github.com/liriliri/eruda
// @author       eruda
// @match        *://*/*
// @run-at       document-idle
// ==/UserScript==

(function () {
    'use strict';
    var script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/eruda';
    document.body.appendChild(script);
    script.onload = function () { eruda.init() };
})()