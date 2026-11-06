(function () {
  var page = { num: 1, size: 10, total: 0, pages: 1 };
  var current = "events";
  var eventCache = [];
  var editing = null;

  function $(id) { return document.getElementById(id); }

  function esc(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function showLogin(show) {
    $("loginScreen").style.display = show ? "grid" : "none";
    $("adminShell").style.display = show ? "none" : "grid";
  }

  function requireAuth() {
    if (!API.getToken()) {
      showLogin(true);
      return false;
    }
    showLogin(false);
    return true;
  }

  function setNav(name) {
    current = name;
    document.querySelectorAll(".side-btn").forEach(function (b) {
      b.classList.toggle("active", b.dataset.panel === name);
    });
    document.querySelectorAll(".panel-view").forEach(function (p) {
      p.style.display = p.dataset.panel === name ? "block" : "none";
    });
    var titles = {
      overview: "赛事总览",
      events: "赛事管理",
      runners: "选手档案",
      registers: "报名管理",
      results: "成绩管理",
      ranks: "成绩排名"
    };
    $("pageTitle").textContent = titles[name] || name;
    page.num = 1;
    loadPanel();
  }

  function loadPanel() {
    if (current === "overview") return loadOverview();
    if (current === "events") return loadEvents();
    if (current === "runners") return loadRunners();
    if (current === "registers") return loadRegisters();
    if (current === "results") return loadResults();
    if (current === "ranks") return loadRanks();
  }

  function renderPager(mountId, onChange) {
    var el = $(mountId);
    el.innerHTML =
      '<button type="button" class="btn btn-ghost btn-sm" id="' + mountId + 'Prev">上一页</button>' +
      "<span>第 " + page.num + " / " + (page.pages || 1) + " 页 · 共 " + (page.total || 0) + " 条</span>" +
      '<button type="button" class="btn btn-ghost btn-sm" id="' + mountId + 'Next">下一页</button>';
    $(mountId + "Prev").onclick = function () {
      if (page.num > 1) { page.num--; onChange(); }
    };
    $(mountId + "Next").onclick = function () {
      if (page.num < page.pages) { page.num++; onChange(); }
    };
  }

  function applyPage(data) {
    page.total = data.total || 0;
    page.pages = data.pages || 1;
    page.num = data.pageNum || page.num;
    page.size = data.pageSize || page.size;
  }

  function ensureEvents(selectIds) {
    return API.get("/api/event/list", { pageNum: 1, pageSize: 100 }).then(function (res) {
      eventCache = (res.data && res.data.list) || [];
      (selectIds || []).forEach(function (id) {
        var sel = $(id);
        if (!sel) return;
        var cur = sel.value;
        sel.innerHTML = '<option value="">全部赛事</option>' + eventCache.map(function (e) {
          return '<option value="' + e.id + '">' + esc(e.eventName) + "</option>";
        }).join("");
        if (cur) sel.value = cur;
      });
      return eventCache;
    });
  }

  /* ---------- login ---------- */
  function doLogin() {
    var username = ($("loginUser").value || "").trim();
    var password = $("loginPass").value || "";
    API.post("/api/login", { username: username, password: password }, { noAuth: true })
      .then(function (res) {
        API.setToken(res.data.token);
        API.toast("登录成功", "ok");
        showLogin(false);
        setNav("overview");
      })
      .catch(function (e) {
        API.toast(e.message || "登录失败", "err");
      });
  }

  function logout() {
    API.setToken("");
    showLogin(true);
  }

  /* ---------- overview ---------- */
  function loadOverview() {
    ensureEvents(["ovEvent"]).then(function () {
      var id = $("ovEvent").value || (eventCache[0] && eventCache[0].id);
      if (!id) {
        $("ovStats").innerHTML = '<div class="empty">暂无赛事</div>';
        return;
      }
      $("ovEvent").value = id;
      API.get("/api/statistics/eventOverview/" + id).then(function (res) {
        var d = res.data || {};
        $("ovStats").innerHTML =
          '<div class="stat-row">' +
            stat("报名人数", d.registeredCount != null ? d.registeredCount : d.totalRegister != null ? d.totalRegister : pick(d, ["registerCount", "registrationCount", "total"])) +
            stat("完赛人数", pick(d, ["finishCount", "finishedCount", "completeCount"])) +
            stat("男性", pick(d, ["maleCount", "male"])) +
            stat("女性", pick(d, ["femaleCount", "female"])) +
          "</div>" +
          '<pre class="panel muted" style="white-space:pre-wrap;font-size:.85rem">' + esc(JSON.stringify(d, null, 2)) + "</pre>";
      }).catch(function (e) {
        $("ovStats").innerHTML = '<div class="empty">' + esc(e.message) + "</div>";
      });
    }).catch(function (e) {
      if (e.code === 401) showLogin(true);
      API.toast(e.message, "err");
    });
  }

  function pick(obj, keys) {
    for (var i = 0; i < keys.length; i++) {
      if (obj[keys[i]] != null) return obj[keys[i]];
    }
    return "-";
  }

  function stat(lab, val) {
    return '<div class="stat"><div class="lab">' + lab + '</div><div class="num">' + esc(String(val)) + "</div></div>";
  }

  /* ---------- events ---------- */
  function loadEvents() {
    var params = {
      pageNum: page.num,
      pageSize: page.size,
      eventName: ($("evName").value || "").trim() || undefined,
      eventType: $("evType").value || undefined,
      status: $("evStatus").value || undefined
    };
    API.get("/api/event/list", params).then(function (res) {
      applyPage(res.data || {});
      var list = (res.data && res.data.list) || [];
      $("evTable").innerHTML =
        '<div class="table-wrap"><table class="data"><thead><tr>' +
        "<th>ID</th><th>名称</th><th>类型</th><th>日期</th><th>名额</th><th>状态</th><th>操作</th>" +
        "</tr></thead><tbody>" +
        list.map(function (e) {
          return "<tr>" +
            "<td>" + e.id + "</td>" +
            "<td>" + esc(e.eventName) + "</td>" +
            "<td>" + API.eventTypeText(e.eventType) + "</td>" +
            "<td>" + API.fmtDay(e.eventDate) + "</td>" +
            "<td>" + (e.registeredCount || 0) + "/" + (e.quota || 0) + "</td>" +
            "<td>" + API.statusText(e.status) + "</td>" +
            "<td>" +
              '<button type="button" class="btn btn-ghost btn-sm" data-edit-event="' + e.id + '">编辑</button> ' +
              '<button type="button" class="btn btn-danger btn-sm" data-del-event="' + e.id + '">删除</button>' +
            "</td></tr>";
        }).join("") +
        "</tbody></table></div>";
      renderPager("evPager", loadEvents);
      $("evTable").querySelectorAll("[data-edit-event]").forEach(function (btn) {
        btn.onclick = function () { openEventModal(Number(btn.getAttribute("data-edit-event"))); };
      });
      $("evTable").querySelectorAll("[data-del-event]").forEach(function (btn) {
        btn.onclick = function () { delEvent(Number(btn.getAttribute("data-del-event"))); };
      });
    }).catch(authCatch);
  }

  function toLocalInput(v) {
    if (!v) return "";
    var s = String(v).replace(" ", "T");
    if (s.length >= 16) return s.slice(0, 16);
    return s;
  }

  function openEventModal(id) {
    editing = id || null;
    showForm("event");
    $("modalTitle").textContent = id ? "编辑赛事" : "新增赛事";
    var fields = {
      eventName: "", eventType: "1", eventDate: "", startLocation: "", distanceKm: "",
      quota: "", entryFee: "", status: "0", signupStart: "", signupEnd: "", gunTime: "",
      cutOffTime: "", signupRule: "", pickupInfo: "", routeDesc: ""
    };
    Object.keys(fields).forEach(function (k) {
      if ($("f_" + k)) $("f_" + k).value = fields[k];
    });
    if (!id) {
      openModal();
      return;
    }
    API.get("/api/event/detail/" + id).then(function (res) {
      var e = res.data || {};
      Object.keys(fields).forEach(function (k) {
        if (!$("f_" + k)) return;
        var v = e[k];
        if (v == null) v = "";
        if (k === "signupStart" || k === "signupEnd" || k === "gunTime") {
          v = toLocalInput(v);
        } else if (k === "eventDate") {
          v = String(v).slice(0, 10);
        }
        $("f_" + k).value = v;
      });
      openModal();
    }).catch(authCatch);
  }

  function saveEvent() {
    var body = {
      eventName: $("f_eventName").value.trim(),
      eventType: Number($("f_eventType").value),
      eventDate: $("f_eventDate").value,
      startLocation: $("f_startLocation").value.trim(),
      distanceKm: Number($("f_distanceKm").value),
      quota: Number($("f_quota").value),
      entryFee: Number($("f_entryFee").value),
      status: Number($("f_status").value),
      signupStart: normDt($("f_signupStart").value),
      signupEnd: normDt($("f_signupEnd").value),
      gunTime: normDt($("f_gunTime").value),
      cutOffTime: $("f_cutOffTime").value.trim(),
      signupRule: $("f_signupRule").value.trim(),
      pickupInfo: $("f_pickupInfo").value.trim(),
      routeDesc: $("f_routeDesc").value.trim()
    };
    var req = editing
      ? API.put("/api/event/update", Object.assign({ id: editing }, body))
      : API.post("/api/event/add", body);
    req.then(function () {
      API.toast("保存成功", "ok");
      closeModal();
      loadEvents();
    }).catch(authCatch);
  }

  function normDt(v) {
    if (!v) return null;
    if (v.length === 16) return v + ":00";
    return v.replace("T", " ");
  }

  function delEvent(id) {
    if (!confirm("确认删除赛事 #" + id + "？")) return;
    API.del("/api/event/delete/" + id).then(function () {
      API.toast("已删除", "ok");
      loadEvents();
    }).catch(authCatch);
  }

  /* ---------- runners ---------- */
  function loadRunners() {
    API.get("/api/runner/list", {
      pageNum: page.num,
      pageSize: page.size,
      name: ($("rnName").value || "").trim() || undefined,
      phone: ($("rnPhone").value || "").trim() || undefined
    }).then(function (res) {
      applyPage(res.data || {});
      var list = (res.data && res.data.list) || [];
      $("rnTable").innerHTML =
        '<div class="table-wrap"><table class="data"><thead><tr>' +
        "<th>ID</th><th>姓名</th><th>性别</th><th>手机</th><th>出生日期</th><th>身份证</th><th>操作</th>" +
        "</tr></thead><tbody>" +
        list.map(function (r) {
          return "<tr>" +
            "<td>" + r.id + "</td>" +
            "<td>" + esc(r.name) + "</td>" +
            "<td>" + API.genderText(r.gender) + "</td>" +
            "<td>" + esc(r.phone) + "</td>" +
            "<td>" + API.fmtDay(r.birthDate) + "</td>" +
            "<td>" + esc(r.idCard) + "</td>" +
            "<td>" +
              '<button type="button" class="btn btn-ghost btn-sm" data-edit-runner="' + r.id + '">编辑</button> ' +
              '<button type="button" class="btn btn-danger btn-sm" data-del-runner="' + r.id + '">删除</button>' +
            "</td></tr>";
        }).join("") +
        "</tbody></table></div>";
      renderPager("rnPager", loadRunners);
      $("rnTable").querySelectorAll("[data-edit-runner]").forEach(function (btn) {
        btn.onclick = function () { openRunnerModal(Number(btn.getAttribute("data-edit-runner"))); };
      });
      $("rnTable").querySelectorAll("[data-del-runner]").forEach(function (btn) {
        btn.onclick = function () { delRunner(Number(btn.getAttribute("data-del-runner"))); };
      });
    }).catch(authCatch);
  }

  function openRunnerModal(id) {
    editing = id || null;
    $("modalTitle").textContent = id ? "编辑选手" : "新增选手";
    ["name", "gender", "idCard", "phone", "birthDate", "emergencyContact"].forEach(function (k) {
      if ($("f_" + k)) $("f_" + k).value = k === "gender" ? "1" : "";
    });
    showForm("runner");
    if (!id) { openModal(); return; }
    API.get("/api/runner/detail/" + id).then(function (res) {
      var r = res.data || {};
      $("f_name").value = r.name || "";
      $("f_gender").value = r.gender || 1;
      $("f_idCard").value = r.idCard || "";
      $("f_phone").value = r.phone || "";
      $("f_birthDate").value = API.fmtDay(r.birthDate).replace(/^-$/, "");
      $("f_emergencyContact").value = r.emergencyContact || "";
      openModal();
    }).catch(authCatch);
  }

  function saveRunner() {
    var body = {
      name: $("f_name").value.trim(),
      gender: Number($("f_gender").value),
      idCard: $("f_idCard").value.trim(),
      phone: $("f_phone").value.trim(),
      birthDate: $("f_birthDate").value,
      emergencyContact: $("f_emergencyContact").value.trim()
    };
    var req = editing
      ? API.put("/api/runner/update", Object.assign({ id: editing }, body))
      : API.post("/api/runner/add", body);
    req.then(function () {
      API.toast("保存成功", "ok");
      closeModal();
      loadRunners();
    }).catch(authCatch);
  }

  function delRunner(id) {
    if (!confirm("确认删除选手 #" + id + "？")) return;
    API.del("/api/runner/delete/" + id).then(function () {
      API.toast("已删除", "ok");
      loadRunners();
    }).catch(authCatch);
  }

  /* ---------- registers ---------- */
  function loadRegisters() {
    ensureEvents(["rgEvent"]).then(function () {
      return API.get("/api/register/list", {
        pageNum: page.num,
        pageSize: page.size,
        eventId: $("rgEvent").value || undefined,
        name: ($("rgName").value || "").trim() || undefined
      });
    }).then(function (res) {
      applyPage(res.data || {});
      var list = (res.data && res.data.list) || [];
      $("rgTable").innerHTML =
        '<div class="table-wrap"><table class="data"><thead><tr>' +
        "<th>ID</th><th>赛事</th><th>选手</th><th>号码布</th><th>支付</th><th>状态</th><th>报名时间</th><th>操作</th>" +
        "</tr></thead><tbody>" +
        list.map(function (r) {
          var id = r.id != null ? r.id : r.registrationId;
          return "<tr>" +
            "<td>" + id + "</td>" +
            "<td>" + esc(r.eventName || r.event_name || "") + "</td>" +
            "<td>" + esc(r.name || r.runnerName || "") + "</td>" +
            "<td>" + esc(r.bibNumber || r.bib_number || "-") + "</td>" +
            "<td>" + API.payText(r.payStatus != null ? r.payStatus : r.pay_status) + "</td>" +
            "<td>" + API.regStatusText(r.status) + "</td>" +
            "<td>" + API.fmtDate(r.registerTime || r.register_time) + "</td>" +
            "<td>" +
              '<button type="button" class="btn btn-ghost btn-sm" data-bib="' + id + '">号码布</button> ' +
              '<button type="button" class="btn btn-danger btn-sm" data-cancel="' + id + '">取消</button>' +
            "</td></tr>";
        }).join("") +
        "</tbody></table></div>";
      renderPager("rgPager", loadRegisters);
      $("rgTable").querySelectorAll("[data-bib]").forEach(function (btn) {
        btn.onclick = function () {
          var bib = prompt("输入号码布");
          if (!bib) return;
          API.put("/api/register/bib", {
            registrationId: Number(btn.getAttribute("data-bib")),
            bibNumber: bib
          }).then(function () {
            API.toast("已分配", "ok");
            loadRegisters();
          }).catch(authCatch);
        };
      });
      $("rgTable").querySelectorAll("[data-cancel]").forEach(function (btn) {
        btn.onclick = function () {
          if (!confirm("确认取消该报名？")) return;
          API.post("/api/register/cancel", {
            registrationId: Number(btn.getAttribute("data-cancel"))
          }).then(function () {
            API.toast("已取消", "ok");
            loadRegisters();
          }).catch(authCatch);
        };
      });
    }).catch(authCatch);
  }

  function openRegisterModal() {
    editing = null;
    $("modalTitle").textContent = "提交报名";
    showForm("register");
    ensureEvents(["f_regEventId"]).then(function () {
      $("f_regEventId").innerHTML = eventCache.map(function (e) {
        return '<option value="' + e.id + '">' + esc(e.eventName) + "</option>";
      }).join("");
      openModal();
    });
  }

  function saveRegister() {
    API.post("/api/register/submit", {
      eventId: Number($("f_regEventId").value),
      runnerId: Number($("f_regRunnerId").value)
    }).then(function () {
      API.toast("报名成功", "ok");
      closeModal();
      loadRegisters();
    }).catch(authCatch);
  }

  /* ---------- results ---------- */
  function loadResults() {
    ensureEvents(["rsEvent"]).then(function () {
      return API.get("/api/result/list", {
        pageNum: page.num,
        pageSize: page.size,
        eventId: $("rsEvent").value || undefined
      });
    }).then(function (res) {
      applyPage(res.data || {});
      var list = (res.data && res.data.list) || [];
      $("rsTable").innerHTML =
        '<div class="table-wrap"><table class="data"><thead><tr>' +
        "<th>ID</th><th>报名ID</th><th>选手</th><th>号码布</th><th>枪声</th><th>净成绩</th><th>状态</th><th>完赛时间</th>" +
        "</tr></thead><tbody>" +
        list.map(function (r) {
          return "<tr>" +
            "<td>" + (r.id || "-") + "</td>" +
            "<td>" + (r.registrationId || r.registration_id || "-") + "</td>" +
            "<td>" + esc(r.name || r.runnerName || "") + "</td>" +
            "<td>" + esc(r.bibNumber || r.bib_number || "-") + "</td>" +
            "<td>" + API.secToClock(r.chipTime != null ? r.chipTime : r.chip_time) + "</td>" +
            "<td>" + API.secToClock(r.netTime != null ? r.netTime : r.net_time) + "</td>" +
            "<td>" + API.finishText(r.finishStatus != null ? r.finishStatus : r.finish_status) + "</td>" +
            "<td>" + API.fmtDate(r.finishTime || r.finish_time) + "</td>" +
            "</tr>";
        }).join("") +
        "</tbody></table></div>";
      renderPager("rsPager", loadResults);
    }).catch(authCatch);
  }

  function openResultModal() {
    editing = null;
    $("modalTitle").textContent = "录入成绩";
    showForm("result");
    $("f_registrationId").value = "";
    $("f_chipTime").value = "";
    $("f_netTime").value = "";
    $("f_finishStatus").value = "1";
    $("f_finishTime").value = "";
    openModal();
  }

  function saveResult() {
    var body = {
      registrationId: Number($("f_registrationId").value),
      chipTime: Number($("f_chipTime").value),
      netTime: Number($("f_netTime").value),
      finishStatus: Number($("f_finishStatus").value),
      finishTime: normDt($("f_finishTime").value)
    };
    API.post("/api/result/input", body).then(function () {
      API.toast("录入成功", "ok");
      closeModal();
      loadResults();
    }).catch(authCatch);
  }

  /* ---------- ranks ---------- */
  function loadRanks() {
    ensureEvents(["rkEvent"]).then(function () {
      var id = $("rkEvent").value || (eventCache[0] && eventCache[0].id);
      if (!id) {
        $("rkTable").innerHTML = '<div class="empty">暂无赛事</div>';
        return;
      }
      $("rkEvent").value = id;
      var mode = $("rkMode").value || "event";
      var url =
        mode === "gender" ? "/api/result/genderRanking/" + id :
        mode === "age" ? "/api/result/ageRanking/" + id :
        "/api/result/eventRanking/" + id;
      return API.get(url, { pageNum: 1, pageSize: 50 }).then(function (res) {
        var data = res.data;
        if (mode === "gender") {
          $("rkTable").innerHTML =
            "<h3 class='section-kicker'>男子</h3>" + rankTable((data && data.maleList) || data.male || []) +
            "<h3 class='section-kicker' style='margin-top:1rem'>女子</h3>" + rankTable((data && data.femaleList) || data.female || []);
          return;
        }
        if (mode === "age") {
          var keys = Object.keys(data || {}).filter(function (k) { return Array.isArray(data[k]); });
          if (!keys.length) {
            $("rkTable").innerHTML = '<pre class="panel">' + esc(JSON.stringify(data, null, 2)) + "</pre>";
            return;
          }
          $("rkTable").innerHTML = keys.map(function (k) {
            return "<h3 class='section-kicker' style='margin-top:.8rem'>" + esc(k) + "</h3>" + rankTable(data[k]);
          }).join("");
          return;
        }
        var list = (data && data.list) || data || [];
        $("rkTable").innerHTML = rankTable(list);
      });
    }).catch(authCatch);
  }

  function rankTable(list) {
    if (!list || !list.length) return '<div class="empty">暂无数据</div>';
    return '<div class="table-wrap"><table class="data"><thead><tr>' +
      "<th>名次</th><th>号码布</th><th>姓名</th><th>净成绩</th><th>性别</th>" +
      "</tr></thead><tbody>" +
      list.map(function (r, i) {
        var rank = r.rank != null ? r.rank : i + 1;
        return "<tr>" +
          '<td class="' + (rank === 1 ? "rank-1" : "") + '">' + rank + "</td>" +
          "<td>" + esc(r.bibNumber || r.bib_number || "-") + "</td>" +
          "<td>" + esc(r.name || r.runnerName || "") + "</td>" +
          "<td>" + (r.netTimeStr || API.secToClock(r.netTime != null ? r.netTime : r.net_time)) + "</td>" +
          "<td>" + API.genderText(r.gender) + "</td>" +
          "</tr>";
      }).join("") +
      "</tbody></table></div>";
  }

  /* ---------- modal form switching ---------- */
  function showForm(name) {
    document.querySelectorAll("[data-form]").forEach(function (el) {
      el.style.display = el.getAttribute("data-form") === name ? "grid" : "none";
    });
    $("modalSave").dataset.form = name;
  }

  function openModal() { $("modalMask").classList.add("open"); }
  function closeModal() { $("modalMask").classList.remove("open"); }

  function authCatch(e) {
    API.toast(e.message || "请求失败", "err");
    if (e.code === 401) showLogin(true);
  }

  function exportCsv(kind) {
    var eventId = $("rgEvent") && $("rgEvent").value;
    if (kind === "result") eventId = $("rsEvent") && $("rsEvent").value;
    if (!eventId) {
      API.toast("请先选择赛事", "err");
      return;
    }
    var url = kind === "result"
      ? "/api/result/export?eventId=" + encodeURIComponent(eventId)
      : "/api/register/export?eventId=" + encodeURIComponent(eventId);
    var token = API.getToken();
    fetch(url, { headers: { Authorization: "Bearer " + token } })
      .then(function (res) {
        if (!res.ok) throw new Error("导出失败");
        return res.blob();
      })
      .then(function (blob) {
        var a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = kind + "-" + eventId + ".xlsx";
        a.click();
        URL.revokeObjectURL(a.href);
      })
      .catch(function (e) { API.toast(e.message, "err"); });
  }

  document.addEventListener("DOMContentLoaded", function () {
    $("btnLogin").onclick = doLogin;
    $("loginPass").addEventListener("keydown", function (e) {
      if (e.key === "Enter") doLogin();
    });
    $("btnLogout").onclick = logout;
    document.querySelectorAll(".side-btn").forEach(function (btn) {
      btn.onclick = function () { setNav(btn.dataset.panel); };
    });

    $("btnEvSearch").onclick = function () { page.num = 1; loadEvents(); };
    $("btnEvAdd").onclick = function () { showForm("event"); openEventModal(null); };
    $("btnRnSearch").onclick = function () { page.num = 1; loadRunners(); };
    $("btnRnAdd").onclick = function () { openRunnerModal(null); };
    $("btnRgSearch").onclick = function () { page.num = 1; loadRegisters(); };
    $("btnRgAdd").onclick = openRegisterModal;
    $("btnRgExport").onclick = function () { exportCsv("register"); };
    $("btnRsSearch").onclick = function () { page.num = 1; loadResults(); };
    $("btnRsAdd").onclick = openResultModal;
    $("btnRsExport").onclick = function () { exportCsv("result"); };
    $("btnRkSearch").onclick = loadRanks;
    $("btnOvRefresh").onclick = loadOverview;

    $("modalClose").onclick = closeModal;
    $("modalCancel").onclick = closeModal;
    $("modalSave").onclick = function () {
      var form = $("modalSave").dataset.form;
      if (form === "event") saveEvent();
      else if (form === "runner") saveRunner();
      else if (form === "register") saveRegister();
      else if (form === "result") saveResult();
    };
    $("modalMask").addEventListener("click", function (e) {
      if (e.target === $("modalMask")) closeModal();
    });

    if (requireAuth()) setNav("overview");
  });
})();
