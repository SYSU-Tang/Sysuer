// ==UserScript==
// @name         Welearn助手
// @namespace    https://bbs.tampermonkey.net.cn/
// @version      2.5.2
// @author       恶搞之家
// @description  自动选择Welearn平台的选择题、判断题、填空题和下拉框答案
// @match        *://welearn.sflep.com/Student/StudyCourse.aspx*
// @match        *://welearn.sflep.com/student/StudyCourse.aspx*
// @match        *://welearn.sflep.com/student/studyCourse.aspx*
// @match        *://welearn.sflep.com/student/Studycourse.aspx*
// @match        *://welearn.sflep.com/student/studycourse.aspx*
// @match        *://welearn.sflep.com/course/trycourse.aspx*
// @match        *://welearn.sflep.com/course/Trycourse.aspx*
// @match        *://welearn.sflep.com/course/TryCourse.aspx*
// @match        *://welearn.sflep.com/*/TryCourse.aspx*
// @grant        GM_getValue
// @grant        GM_setValue
// ==/UserScript==

(function () {
    'use strict';
    var script = document.createElement('script');
    script.src = 'https://sysu-tang.github.io/WeLearn_Assistant.js';
    document.body.appendChild(script);
})()