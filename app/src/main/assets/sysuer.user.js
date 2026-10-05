// ==UserScript==
// @name         SYSUER美化辅助增强
// @namespace    https://github.com/SYSU-Tang
// @version      2.3
// @description  中大儿增强脚本，包括网页净化、在线教学平台视频自动速通、自动跳下一页、自动登录、跳过验证、自动跳转登录页、等级制查看原始分。
// @author       SYSU-Tang
// @license      Apache-2.0
// @updateURL    https://github.com/SYSU-Tang/sysuer-script/raw/refs/heads/main/sysuer.meta.js
// @downloadURL  https://github.com/SYSU-Tang/sysuer-script/raw/refs/heads/main/sysuer.user.js
// @homepage     https://github.com/SYSU-Tang/sysuer-script
// @match        *://www.sysu.edu.cn/*
// @match        *://jwxt.sysu.edu.cn/*
// @match        *://portal.sysu.edu.cn/*
// @match        *://lms.sysu.edu.cn/*
// @match        *://cas.sysu.edu.cn/*
// @match        *://appgw.sysu.edu.cn/*
// @match        *://visitor.sysu.edu.cn/*
// @match        *://visitor-443.webvpn.sysu.edu.cn/*
// @match        *://pay.sysu.edu.cn/*
// @match        *://xgxt.sysu.edu.cn/*
// @match        *://xgxt-443.webvpn.sysu.edu.cn/*
// @match        *://ecert.sysu.edu.cn/Integrated_platform/*
// @noframes
// @grant        GM_setValue
// @grant        GM_getValue
// @grant        GM_registerMenuCommand
// ==/UserScript==

(function () {
  "use strict";
  const SYSU_GREEN = "#005826";

  /* ==================== 配置读取 ==================== */
  const config = {
    autoLogin: GM_getValue("autoLogin", true),
    autoVerify: GM_getValue("autoVerify", true),
    autoWebvpn: GM_getValue("autoWebvpn", true),
    autoJumpLogin: GM_getValue("autoJumpLogin", true),
    username: GM_getValue("username", ""),
    password: GM_getValue("password", ""),
    videoComplete: GM_getValue("videoComplete", true),
    videoJump: GM_getValue("videoJump", true),
    purify: GM_getValue("purify", true),
    removeWatermark: GM_getValue("removeWatermark", true),
    gradeDisplay: GM_getValue("gradeDisplay", true),
  };

  const {
    autoLogin,
    autoVerify,
    autoWebvpn,
    autoJumpLogin,
    username,
    password,
    videoComplete,
    videoJump,
    purify,
    removeWatermark,
    gradeDisplay,
  } = config;

  const url = window.location.href;
  const host = window.location.hostname;

  /* ==================== 悬浮按钮 ==================== */
  function createFloatingButton() {
    if (document.getElementById("sysuer-float-btn")) return;

    const btn = document.createElement("div");
    btn.id = "sysuer-float-btn";
    btn.innerHTML = "⚙️";
    btn.title = "打开 SYSUER 脚本设置";
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
    btn.addEventListener("mouseenter", () => {
      btn.style.transform = "scale(1.1)";
      btn.style.boxShadow = "0 6px 16px rgba(0, 88, 38, 0.6)";
    });
    btn.addEventListener("mouseleave", () => {
      btn.style.transform = "scale(1)";
      btn.style.boxShadow = "0 4px 12px rgba(0, 88, 38, 0.4)";
    });

    // 点击打开面板
    btn.addEventListener("click", createSettingsPanel);

    document.body.appendChild(btn);
  }

  /* ==================== 设置面板 GUI ==================== */
  function createSettingsPanel() {
    if (document.getElementById("sysuer-settings-panel")) return;

    const labelStyle =
      "display: flex; align-items: center; justify-content: space-between; font-size: 14px; color: #333;";
    const toggleItems = [
      ["autoLogin", "自动登录"],
      ["autoVerify", "跳过验证"],
      ["autoWebvpn", "自动跳转WebVPN"],
      ["autoJumpLogin", "自动点击登录按钮"],
      ["videoComplete", "在线教学平台视频自动速通"],
      ["videoJump", "在线教学平台视频完成后自动跳下一页"],
      ["purify", "页面净化"],
      ["removeWatermark", "移除水印"],
      ["gradeDisplay", "等级制显示原始分"],
    ];
    const togglesHtml = toggleItems
      .map(
        ([key, label]) =>
          `<label style="${labelStyle}">${label} <input type="checkbox" id="cfg-${key}" ${config[key] ? "checked" : ""}></label>`,
      )
      .join("\n            ");

    // 背景遮罩
    const overlay = document.createElement("div");
    overlay.id = "sysuer-settings-panel";
    overlay.style.cssText = `
            position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
            background: rgba(0, 0, 0, 0.5); z-index: 999999;
            display: flex; justify-content: center; align-items: center;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        `;

    // 面板主体
    const panel = document.createElement("div");
    panel.style.cssText = `
            background: #fff; padding: 24px; border-radius: 12px;
            width: 320px; max-width: 90%; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            display: flex; flex-direction: column; gap: 12px;
            border-top: 5px solid ${SYSU_GREEN};
        `;

    panel.innerHTML = `
            <h3 style="margin: 0 0 10px 0; font-size: 18px; color: ${SYSU_GREEN}; text-align: center; font-weight: bold;">SYSUER 增强设置</h3>

            ${togglesHtml}

            <hr style="border: 0; border-top: 1px dashed #ccc; margin: 5px 0;">

            <div style="display: flex; flex-direction: column; gap: 5px;">
                <label style="font-size: 12px; color: #555; font-weight: bold;">NetID 用户名:</label>
                <input type="text" id="cfg-username" style="padding: 6px; border: 1px solid #ccc; border-radius: 4px; outline-color: ${SYSU_GREEN};">
            </div>
            <div style="display: flex; flex-direction: column; gap: 5px;">
                <label style="font-size: 12px; color: #555; font-weight: bold;">NetID 密码:</label>
                <input type="password" id="cfg-password" style="padding: 6px; border: 1px solid #ccc; border-radius: 4px; outline-color: ${SYSU_GREEN};">
            </div>

            <div style="display: flex; gap: 10px; margin-top: 10px;">
                <button id="cfg-save" style="flex: 1; padding: 8px; background: ${SYSU_GREEN}; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: opacity 0.2s;">保存设置</button>
                <button id="cfg-close" style="flex: 1; padding: 8px; background: #f5f5f5; color: #333; border: 1px solid #d9d9d9; border-radius: 6px; cursor: pointer; transition: background 0.2s;">取消</button>
            </div>
        `;

    overlay.appendChild(panel);
    document.body.appendChild(overlay);

    // 用 JS 赋值，避免把用户输入直接拼进 HTML 属性
    document.getElementById("cfg-username").value = config.username;
    document.getElementById("cfg-password").value = config.password;

    // 按钮交互效果
    document.getElementById("cfg-save").onmouseenter = function () {
      this.style.opacity = "0.85";
    };
    document.getElementById("cfg-save").onmouseleave = function () {
      this.style.opacity = "1";
    };
    document.getElementById("cfg-close").onmouseenter = function () {
      this.style.background = "#e8e8e8";
    };
    document.getElementById("cfg-close").onmouseleave = function () {
      this.style.background = "#f5f5f5";
    };

    // 绑定事件
    document.getElementById("cfg-close").onclick = () => overlay.remove();
    document.getElementById("cfg-save").onclick = () => {
      toggleItems.forEach(([key]) => {
        const checked = document.getElementById("cfg-" + key).checked;
        config[key] = checked; // 同步内存中的 config，避免重开面板显示旧值
        GM_setValue(key, checked);
      });
      const usernameVal = document.getElementById("cfg-username").value;
      const passwordVal = document.getElementById("cfg-password").value;
      config.username = usernameVal;
      config.password = passwordVal;
      GM_setValue("username", usernameVal);
      GM_setValue("password", passwordVal);

      overlay.remove();
      if (window.toast) {
        toast.success("设置已保存，刷新页面后生效！", {
          backgroundColor: SYSU_GREEN,
        });
      } else {
        alert("设置已保存，刷新页面后生效！");
      }
    };
  }

  // 初始化悬浮按钮
  window.addEventListener("load", createFloatingButton);

  // 注册油猴菜单 (保留双重入口)
  GM_registerMenuCommand("⚙️ 脚本设置", createSettingsPanel);

  /* ==================== 核心逻辑功能区 ==================== */

  /* 隐藏元素 */
  function hide(selectors) {
    selectors.forEach(function (v) {
      document
        .querySelectorAll(v)
        .forEach((el) => (el.style.display = "none"));
    });
  }
  /* 点击元素 */
  function click(el) {
    const element = document.querySelector(el);
    if (element) element.click();
  }
  /* 等待元素出现（MutationObserver 实现） */
  function waitElement(
    selector,
    callback,
    timeout = 5000,
    timeoutCallback = null,
  ) {
    let stopped = false;
    const timerId = setTimeout(fail, timeout);
    const observer = new MutationObserver(() => {
      const element = document.querySelector(selector);
      if (element) finish(element);
    });

    function finish(element) {
      if (stopped) return;
      stopped = true;
      observer.disconnect();
      clearTimeout(timerId);
      callback(element);
    }

    function fail() {
      if (stopped) return;
      stopped = true;
      observer.disconnect();
      clearTimeout(timerId);
      if (typeof timeoutCallback === "function") {
        timeoutCallback(selector);
      } else {
        console.log(
          `[SYSUER 脚本] 等待元素 "${selector}" 超时（${timeout}ms）`,
        );
      }
    }

    observer.observe(document.documentElement, {
      childList: true,
      subtree: true,
    });

    // 元素可能已存在
    const existing = document.querySelector(selector);
    if (existing) finish(existing);
  }

  /* 获取 React 组件实例（兼容新旧 React 的 key），失败时返回 null */
  function getReactStateNode(el) {
    const key = Object.keys(el).find(
      (k) =>
        k.startsWith("__reactInternalInstance") ||
        k.startsWith("__reactFiber$"),
    );
    if (!key) return null;
    try {
      return el[key]?.return?.stateNode ?? null;
    } catch (e) {
      console.warn("[SYSUER 脚本] 获取 React 组件节点失败", e);
      return null;
    }
  }

  /* 将 SYSU 域名精确替换为对应的 WebVPN 域名（已转换过则原样返回） */
  function toWebvpnUrl(target) {
    try {
      const u = new URL(target);
      if (
        u.hostname.endsWith(".sysu.edu.cn") &&
        !u.hostname.includes(".webvpn.")
      ) {
        u.hostname = u.hostname.replace(
          /\.sysu\.edu\.cn$/,
          "-443.webvpn.sysu.edu.cn",
        );
      }
      return u.href;
    } catch (e) {
      return target.replace(".sysu.edu.cn", "-443.webvpn.sysu.edu.cn");
    }
  }

  // ==================== Toast 核心 ====================
  (function () {
    const COLORS = {
      success: SYSU_GREEN,
      error: "#ff4d4f",
      warning: "#faad14",
      info: "#1890ff",
    };
    const containers = {};

    function getContainer(position) {
      if (containers[position]) return containers[position];
      const container = document.createElement("div");
      container.className = `toast-container-${position}`;
      const isTop = position.startsWith("top");
      const isBottom = position.startsWith("bottom");
      const isLeft = position.endsWith("left");
      const isRight = position.endsWith("right");
      const isCenter = position.endsWith("center");

      let css = `position: fixed; z-index: 9999; display: flex; flex-direction: column; pointer-events: none; gap: 10px; max-width: 90vw; padding: 10px;`;
      if (isTop) css += "top: 0;";
      else if (isBottom) css += "bottom: 0;";
      if (isLeft) css += "left: 0; align-items: flex-start;";
      else if (isRight) css += "right: 0; align-items: flex-end;";
      else if (isCenter)
        css += "left: 50%; transform: translateX(-50%); align-items: center;";

      container.style.cssText = css;
      container._insertMethod = isTop ? "prepend" : "append";
      document.body.appendChild(container);
      containers[position] = container;
      return container;
    }

    function showToast(message, options = {}) {
      if (typeof options === "string") options = { type: options };
      if (typeof arguments[2] === "number") options.duration = arguments[2];

      const {
        type = "info",
        duration = 3000,
        position = "top-right",
        direction = "right",
        backgroundColor,
        pauseOnHover = true,
      } = options;
      const container = getContainer(position);
      const toast = document.createElement("div");
      toast.className = "toast-item";
      const bgColor = backgroundColor || COLORS[type] || COLORS.info;

      toast.style.cssText = `
                padding: 12px 24px; border-radius: 8px; color: #fff; font-size: 14px;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                background: ${bgColor}; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); opacity: 0;
                transition: all 0.3s cubic-bezier(0.68, -0.55, 0.27, 1.55); pointer-events: auto;
                max-width: 360px; word-break: break-word; cursor: default; flex-shrink: 0;
            `;

      let transformStart = "translateX(100%)";
      switch (direction) {
        case "right":
          transformStart = "translateX(100%)";
          break;
        case "left":
          transformStart = "translateX(-100%)";
          break;
        case "top":
          transformStart = "translateY(-100%)";
          break;
        case "bottom":
          transformStart = "translateY(100%)";
          break;
        case "fade":
          transformStart = "scale(0.95)";
          break;
      }
      toast.style.transform = transformStart;
      toast.textContent = message;

      if (container._insertMethod === "prepend")
        container.insertBefore(toast, container.firstChild);
      else container.appendChild(toast);

      requestAnimationFrame(() => {
        toast.style.opacity = "1";
        toast.style.transform = "translate(0, 0) scale(1)";
      });

      let remaining = duration,
        startTime = Date.now(),
        timerId = null;

      function removeToast() {
        toast.style.opacity = "0";
        toast.style.transform = transformStart;
        setTimeout(() => {
          if (toast.parentNode) toast.remove();
          if (container.children.length === 0) {
            container.remove();
            delete containers[position];
          }
        }, 300);
      }

      function startTimer() {
        if (timerId) clearTimeout(timerId);
        timerId = setTimeout(removeToast, remaining);
      }

      function pauseTimer() {
        if (timerId) {
          clearTimeout(timerId);
          timerId = null;
          remaining -= Date.now() - startTime;
          if (remaining < 0) remaining = 0;
        }
      }

      function resumeTimer() {
        startTime = Date.now();
        startTimer();
      }
      startTimer();

      if (pauseOnHover) {
        toast.addEventListener("mouseenter", pauseTimer);
        toast.addEventListener("mouseleave", resumeTimer);
      }
      toast.addEventListener("click", () => {
        clearTimeout(timerId);
        removeToast();
      });
      return { close: removeToast, pause: pauseTimer, resume: resumeTimer };
    }

    window.toast = function (message, options) {
      return showToast(message, options);
    };
    window.toast.success = (msg, options) =>
      showToast(msg, { ...options, type: "success" });
    window.toast.error = (msg, options) =>
      showToast(msg, { ...options, type: "error" });
    window.toast.warning = (msg, options) =>
      showToast(msg, { ...options, type: "warning" });
    window.toast.info = (msg, options) =>
      showToast(msg, { ...options, type: "info" });
  })();

  if (purify) {
    if (host === "www.sysu.edu.cn") {
      hide([".ftb"]);
    }
    if (host === "lms.sysu.edu.cn") {
      hide([".editButton", "footer"]);
    }
    if (host === "xgxt.sysu.edu.cn" || host === "xgxt-443.webvpn.sysu.edu.cn") {
      hide([".banner-ca39d", "footer"]);
      waitElement(".wrap-e806d", (content) => {
        content.style.backgroundSize = "0px";
      });
      waitElement(".scrollbars-22121", (content) => {
        content.style.height = "100%";
      });
    }
    if (host === "jwxt.sysu.edu.cn") {
      const purifyJwxt = () => {
        const url = window.location.href; // 每次取最新地址，适配 hash 路由
        hide([".sys-header", ".sys-footer", ".ant-breadcrumb"]);
        if (url.includes("/jwxt/mk/")) {
          const stuCon = document.querySelector(".stu-con");
          if (stuCon) stuCon.style.padding = "0px";
        }
        if (url.includes("jwxt/mk/#/personalTrainingProgramView")) {
          hide([".ant-tabs-bar"]);
          document.querySelectorAll("col").forEach((element) => {
            element.style.minWidth = "0px";
          });
          const stuCon = document.querySelector(".stu-con");
          if (stuCon) stuCon.style.padding = "0px";
        }
        if (url.includes("jwxt/#/student")) {
          hide([".sys-header", ".sys-footer"]);
          document.querySelectorAll("col").forEach((element) => {
            element.style.minWidth = "0px";
          });
          waitElement(".invest2", (content) => {
            content.style.display = "none";
          });
          const content = document.querySelector(".ant-layout-content");
          if (content) content.style.paddingTop = "0px";
          const beautify = () => {
            document
              .querySelectorAll(".newStyle-tableCell-2c8VE")
              .forEach((element) => {
                element.style.borderRadius = "12px";
              });
            document
              .querySelectorAll(".newStyle-newcellTitleTag-1fPqj")
              .forEach((element) => {
                element.style.display = "none";
              });
            const cols = document.querySelectorAll("colgroup col");
            const width = 100 / cols.length + "%";
            document.querySelectorAll("col").forEach((element) => {
              element.style.minWidth = "0px";
              element.style.width = width;
            });
          };
          waitElement(".newStyle-tableCell-2c8VE", () => beautify());

          waitElement(".ant-table-fixed", (content) => {
            content.style.width = "100%";
          });
          waitElement(".newStyle-myTimetable-3UIjI", (content) => {
            const node = getReactStateNode(content);
            if (!node || typeof node.handleTimeTable !== "function") return;
            const handleTimeTable = node.handleTimeTable;
            node.handleTimeTable = (e) => {
              handleTimeTable(e);
              beautify();
            };
          });
          waitElement(".newStyle-table-3p9KH", (content) => {
            const legend = content.querySelector("div");
            legend.style.justifyContent = "center";
            legend.style.padding = "0px";
            legend.style.marginLeft = "0px";
          });
        }
        if (
          url.includes("jwxt/mk/studentWeb/#/stuAchievementView") ||
          url.includes("jwxt/mk/gradua/#/completionstatusStu")
        ) {
          waitElement(".cj-yxsh-con.cj-cx", (content) => {
            content.style.width = "100%";
            content.style.margin = "0px";
          });
        }
        if (url.includes("#/notice/")) {
          waitElement("main", (content) => {
            content.style.padding = "0px";
          });
          waitElement(".style-bread-3mo7c", (content) => {
            content.style.maxWidth = "100%";
          });
          waitElement(".style-wrapper-3Oy8W", (content) => {
            content.style.maxWidth = "100%";
          });
        }
        if (url.includes("/jwxt/mk/courseSelection")) {
          click(".ant-notification-notice-close-x");
        }
      };
      purifyJwxt();
      window.addEventListener("load", () => {
        purifyJwxt();
        toast.info("[SYSUER 脚本] 净化页面");
      });
      window.addEventListener("hashchange", purifyJwxt);
    }
    if (url.includes("ecert.sysu.edu.cn/Integrated_platform/modules/student")) {
      hide([".copyright"]);
    }
  }
  if (gradeDisplay) {
    const initGradeDisplay = () => {
      if (
        !window.location.href.includes(
          "jwxt/mk/studentWeb/#/stuAchievementView",
        )
      ) {
        return;
      }
      waitElement("div.stu-w", (content) => {
        const node = getReactStateNode(content);
        if (!node) {
          console.log("[SYSUER 脚本] 未能获取成绩组件的 React 节点");
          return;
        }
        const state = node.state;
        if (state.isInLetterRange) {
          function fetchAchievementRows(semester) {
            const state = node.state;
            return fetch(
              "https://jwxt.sysu.edu.cn/jwxt/achievement-manage/achievement/selfPageList",
              {
                method: "POST",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({
                  pageNo: 1,
                  pageSize: 200,
                  total: true,
                  param: { schoolSemester: semester, achievementState: null },
                }),
              },
            )
              .then((response) => response.json())
              .then((data) => {
                if (data.code !== 200) return [];
                return data.data.rows.map((row) => {
                  const semParts = (row.schoolSemester || "").split("-");
                  const year = semParts[0];
                  const scoSchoolYear = year
                    ? `${year}-${parseInt(year) + 1}`
                    : "";
                  const scoSemester = semParts[1] || "";
                  const teacherName = row.classesName || "";
                  const gradeAchievement = row.totalAchievement || "";
                  const finalAchievementStr = (
                    row.finalAchievementStr || ""
                  ).trim();
                  const scoFinalScore =
                    gradeAchievement && finalAchievementStr
                      ? `${gradeAchievement}/${finalAchievementStr}`
                      : gradeAchievement || finalAchievementStr;
                  return {
                    id: row.highestAchievementProgressId || row.id,
                    scoSchoolYear,
                    scoSemester,
                    scoCourseNumber: row.courseNum || "",
                    scoCourseName: row.courseName || "",
                    scoCourseCategory: row.courseCategoryCode || "",
                    scoCourseCategoryName: row.courseCategoryName || "",
                    scoCredit: row.credit || 0,
                    scoStudentNumber: "",
                    teachClassNumber: row.classesNum || "",
                    scoFinalScore,
                    scoPoint: row.achievementPoint || 0,
                    scoTeacherName: teacherName,
                    accessFlag: "是",
                    recordStyle: row.recordModeCode || "",
                    classesRecordModeCode: "01",
                    examCharacter: row.examNatureName || "",
                    teachNumber: "",
                    teachClassRank: "",
                    gradeMajorNumber: "",
                    gradeMajorRank: "",
                    jdjs: "1",
                    tjjs: "1",
                    isInLetterRange: state.isInLetterRange ? "1" : "0",
                    isHonorCourse: "0",
                    checkedSign: false,
                  };
                });
              });
          }

          function search() {
            const state = node.state;
            const year = state.schoolYear?.split("-")[0] || "";
            const semester = state.schoolSemester || "";
            let semesters = [];
            if (year && semester) {
              semesters = [year + "-" + semester];
            } else if (semester) {
              semesters = state.schoolYearList.map(
                (item) => item.dataNumber.split("-")[0] + "-" + semester,
              );
            } else if (year) {
              semesters = state.schoolSemesterList.map(
                (item) => year + "-" + item.value,
              );
            } else {
              state.schoolYearList.forEach((item) => {
                state.schoolSemesterList.forEach((item2) => {
                  semesters.push(
                    item.dataNumber.split("-")[0] + "-" + item2.value,
                  );
                });
              });
            }
            if (semesters.length === 0) return;
            state.achieveList = [];
            Promise.all(semesters.map((sem) => fetchAchievementRows(sem)))
              .then((lists) => {
                node.setState({ achieveList: lists.flat() });
                toast.success("[SYSUER 脚本] 等级制显示原始分成功");
              })
              .catch(() => {
                toast.error("[SYSUER 脚本] 获取原始分失败，请稍后重试");
              });
          }

          waitElement(".search-div-btn", (container) => {
            const btn = document.createElement("button");
            btn.className = "ant-btn ant-btn-primary";
            btn.textContent = "原始分";
            btn.addEventListener("click", () => search());
            container.appendChild(btn);
          });
        }
      });
    };
    initGradeDisplay();
    window.addEventListener("hashchange", initGradeDisplay);
  }
  if (videoComplete && /lms\.sysu\.edu\.cn\/mod\/.*?\/view\.php/.test(url)) {
    let retry = 0;
    function upload(playerWrapper, playerdata, callback) {
      const data = [
        {
          index: 0,
          methodname: "mod_fsresource_set_time",
          args: {
            fsresourceid: playerdata.fsresourceid,
            time: 4,
            finish: 1,
            progress: 100,
            unique: playerWrapper.pageId,
          },
        },
      ];

      fetch(
        "https://lms.sysu.edu.cn/lib/ajax/service.php?sesskey=" +
          playerdata.sesskey,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(data),
        },
      )
        .then((response) => {
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
          return response.json();
        })
        .then((res) => {
          const progress = res[0].data.progress;
          const totaltime = res[0].data.totaltime;
          callback(progress, totaltime);
        })
        .catch((error) => {
          console.error("[SYSUER 脚本] 视频进度提交失败:", error);
          toast.error("[SYSUER 脚本] 视频进度提交失败");
        });
    }
    const runVideoSpeedRun = () => {
      var sourceData = {};
      try {
        sourceData =
          playerdata && playerdata.source ? JSON.parse(playerdata.source) : {};
      } catch (e) {
        console.warn("[SYSUER 脚本] 解析视频源失败", e);
      }
      var sources = {};
      var defaultRes = "";

      if (sourceData?.FD) {
        sources.FD = [{ src: sourceData.FD }];
      }
      if (sourceData?.LD) {
        sources.LD = [{ src: sourceData.LD }];
      }
      if (sourceData?.SD) {
        sources.SD = [{ src: sourceData.SD }];
        defaultRes = "SD";
      }
      if (sourceData?.HD) {
        sources.HD = [{ src: sourceData.HD }];
      }
      if (sourceData?.OD) {
        sources.FHD = [{ src: sourceData.OD }];
      }

      var playerWrapper = new TCPlayerWrapper(
        "fsplayer-container-id_html5_api",
        sources,
        playerdata.siteUrl +
          "/lib/ajax/service.php?sesskey=" +
          playerdata.sesskey,
        `fs_${playerdata.userid}_${playerdata.fsresourceid || 0}`,
        15 * 1000,
        playerdata.progress == 1,
      );
      var duration = playerWrapper.player.duration();
      if (isNaN(duration) || duration === 0) {
        if (retry < 15) {
          toast.error("[SYSUER 脚本] 视频时长获取失败，1秒后重试...");
          setTimeout(runVideoSpeedRun, 1000);
          retry++;
        } else {
          toast.error(
            "[SYSUER 脚本] 视频时长获取失败，15次重试均失败，脚本已退出！",
          );
        }
      } else {
        const jump = () => {
          if (videoJump) {
            toast.info("[SYSUER 脚本] 视频速通完成，点击下一页...");
            click("#next-activity-link");
          }
        };
        upload(playerWrapper, playerdata, (progress, totaltime) => {
          if (Number(String(progress).replace(/,/g, "")) >= 100) {
            toast.success("[SYSUER 脚本] 当前视频完成");
            if (videoJump) {
              jump();
            }
          } else {
            let count = 0;
            const total = Math.max(
              1,
              Math.floor((duration - (Number(totaltime) || 0)) / 4),
            );
            const intervalId = setInterval(() => {
              playerWrapper.viewTotalTime = 4000;
              playerWrapper.ajaxOrder();
              count++;
              if (count >= total) {
                clearInterval(intervalId);
              }
            }, 50); // 放宽提交间隔，避免请求过于密集触发服务器限流
            if (videoJump) {
              let progressChecks = 0;
              const checkProgress = () =>
                upload(playerWrapper, playerdata, (progress, totaltime) => {
                  if (Number(String(progress).replace(/,/g, "")) >= 100) {
                    toast.success("[SYSUER 脚本] 视频进度已全额提交！");
                    jump();
                  } else if (++progressChecks < 60) {
                    setTimeout(checkProgress, 500);
                  } else {
                    toast.error("[SYSUER 脚本] 进度检查超过30秒仍未完成，已停止");
                  }
                });
              checkProgress();
            }
          }
        });
      }
    };
    if (/lms\.sysu\.edu\.cn\/mod\/fsresource\/view\.php/.test(url)) {
      if (removeWatermark) {
        window.watermark?.remove?.();
      }
      let videoAttempts = 0;
      const videoInterval = setInterval(() => {
        if (
          (typeof playerdata !== "undefined" &&
            typeof TCPlayerWrapper !== "undefined") ||
          videoAttempts > 10
        ) {
          clearInterval(videoInterval);
          if (typeof playerdata !== "undefined") {
            runVideoSpeedRun();
          } else if (videoJump) {
            toast.info("[SYSUER 脚本] 视频播放器未加载，点击下一页...");
            click("#next-activity-link");
          }
        }
        videoAttempts++;
      }, 500);
    } else if (videoJump) {
      toast.info("[SYSUER 脚本] 视频播放器未加载，点击下一页...");
      click("#next-activity-link");
    }
  }
  if (autoVerify && url.includes("cas.sysu.edu.cn/login/mfaLogin.html")) {
    document.cookie =
      "device_trust_Cookie=true; Path=/esc-sso; Domain=cas.sysu.edu.cn;";
    toast.info("[SYSUER 脚本] 跳过验证");
    // searchParams.get 已完成一次解码；appUrl 内层的 service 参数需保持编码，不再二次解码
    const appUrl = new URL(url).searchParams.get("appUrl");
    if (appUrl) {
      window.location.href = appUrl;
    }
  }

  if (autoWebvpn && url.includes("appgw.sysu.edu.cn/")) {
    const cb = new URL(url).searchParams.get("cb");
    if (cb) {
      window.location.href = toWebvpnUrl(cb);
    }
  }

  if (
    url.includes("visitor.sysu.edu.cn") &&
    document.title.includes("Access Forbidden")
  ) {
    window.location.href = toWebvpnUrl(url);
  }

  function login(username, password) {
    waitElement(".para-widget-account-psw", (component) => {
      const jqKey = Object.keys(component).find(
        (k) => k.startsWith("jQuery") && k.endsWith("2"),
      );
      if (!jqKey || !component[jqKey]?.widget_accountPsw) return;
      const data = component[jqKey].widget_accountPsw;
      data.loginModel.dataField.username = username;
      data.loginModel.dataField.password = password;
      data.passwordInputVal = "password";
      data.$loginBtn.click();
    });
  }

  if (autoJumpLogin) {
    if (url.includes("lms.sysu.edu.cn/login/index.php?local=")) {
      window.location.href =
        "https://lms.sysu.edu.cn/login/index.php?authCAS=CAS";
    }
    if (/visitor.*?.sysu.edu.cn\/login/.test(url)) {
      waitElement(
        ".netid-form .ant-btn.ant-btn-primary.ant-btn-lg.ant-btn-block.login-button",
        (e) => {
          e.click();
        },
      );
    }
    const clickButton = {
      "jwxt.sysu.edu.cn/jwxt/#/login": "button.ant-btn.ant-btn-primary",
      "jwxt.sysu.edu.cn": ".ant-modal-content button.ant-btn.ant-btn-primary",
      "lms.sysu.edu.cn/enrol/index.php?id=": ".continuebutton .btn.btn-primary",
      "lms.sysu.edu.cn": ".loginBtn",
      "portal.sysu.edu.cn/newClient/#/login":
        ".index-loginData-XCumn>button.ant-btn.index-submit-3jXSy",
      "pay.sysu.edu.cn": ".el-button.login_btns",
      "ecert.sysu.edu.cn/Integrated_platform/login":
        ".el-button.w-100.login_button",
    };
    const autoJump = () => {
      const currentUrl = window.location.href; // 每次取最新地址，适配 hash 路由
      const match = Object.entries(clickButton).find(([key]) =>
        currentUrl.includes(key),
      );
      if (match) {
        waitElement(match[1], (e) => {
          e.click();
        });
      }
    };
    autoJump();
    window.addEventListener("load", autoJump);
    window.addEventListener("hashchange", autoJump);
  }
  if (
    autoLogin &&
    /cas.+?sysu\.edu\.cn\/esc-sso\/login\/page/.test(url) &&
    username &&
    password
  ) {
    login(username, password);
    toast.info("[SYSUER 脚本] 自动登录中");
  }
})();