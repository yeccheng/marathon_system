(function () {
  var state = {
    events: [],
    eventId: null,
    lastEvent: null,
    sharePayload: null,
    shareText: "",
    inviteText: "",
    inviteUrl: ""
  };
  var RUNNER_TOKEN_KEY = "gunstart_runner_token";
  var RUNNER_PHONE_KEY = "gunstart_runner_phone";

  function $(id) { return document.getElementById(id); }

  function getRunnerToken() {
    try { return localStorage.getItem(RUNNER_TOKEN_KEY) || ""; } catch (e) { return ""; }
  }
  function setRunnerSession(token, phone, name) {
    try {
      if (token) localStorage.setItem(RUNNER_TOKEN_KEY, token);
      if (phone) localStorage.setItem(RUNNER_PHONE_KEY, phone);
      if (name) localStorage.setItem("gunstart_runner_name", name);
    } catch (e) {}
  }
  function clearRunnerSession() {
    try {
      localStorage.removeItem(RUNNER_TOKEN_KEY);
      localStorage.removeItem(RUNNER_PHONE_KEY);
      localStorage.removeItem("gunstart_runner_name");
    } catch (e) {}
  }
  function runnerHeaders() {
    var t = getRunnerToken();
    return t ? { "X-Runner-Token": t } : {};
  }

  function showView(name) {
    document.querySelectorAll(".view").forEach(function (el) {
      el.classList.toggle("is-active", el.dataset.view === name);
    });
    document.querySelectorAll("[data-nav]").forEach(function (a) {
      a.classList.toggle("is-active", a.getAttribute("data-nav") === name);
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function parseHash() {
    var h = (location.hash || "#home").replace(/^#/, "");
    var parts = h.split("/");
    return { name: parts[0] || "home", id: parts[1] || null };
  }

  function route() {
    var r = parseHash();
    if (r.name === "event" && r.id) {
      showView("event");
      loadEventDetail(r.id);
      return;
    }
    if (r.name === "signup" && r.id) {
      showView("signup");
      prepareSignup(r.id);
      return;
    }
    if (r.name === "events") {
      showView("events");
      loadEvents();
      return;
    }
    if (r.name === "archive") {
      showView("archive");
      loadArchive();
      return;
    }
    if (r.name === "my") {
      showView("my");
      return;
    }
    if (r.name === "faq") {
      showView("faq");
      renderFaq();
      return;
    }
    if (r.name === "assistant") {
      showView("assistant");
      ensureAssistantReady();
      return;
    }
    if (r.name === "rank") {
      showView("rank");
      ensureRankEvents().then(function () {
        if (r.id) {
          $("rankEvent").value = String(r.id);
        }
        loadRank();
      });
      return;
    }
    showView("home");
    loadHomePreview();
  }

  function monthDay(dateStr) {
    if (!dateStr) return { m: "--", d: "--" };
    var p = String(dateStr).slice(0, 10).split("-");
    return { m: p[1] || "--", d: p[2] || "--" };
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function renderEventRows(list, mountId, mode) {
    var mount = $(mountId);
    if (!list || !list.length) {
      mount.innerHTML = '<div class="empty">' + (mode === "archive" ? "暂无赛事" : "暂无开放报名的赛事") + "</div>";
      return;
    }
    mount.innerHTML = list.map(function (e) {
      var md = monthDay(e.eventDate);
      var canSignup = e.status === 0 || e.status === "0";
      var phase = e.phaseLabel || API.statusText(e.status);
      var statusPill = '<span class="pill' + (canSignup ? " signal" : "") + '">' +
        escapeHtml(phase) + "</span>";
      var feeNum = e.entryFee != null ? e.entryFee : null;
      var feeText = feeNum != null ? "¥" + feeNum : "询价";
      var actions;
      if (mode === "archive") {
        actions =
          '<div style="display:flex;gap:.4rem;flex-wrap:wrap;justify-content:flex-end">' +
            '<a class="btn btn-signal btn-sm" href="#rank/' + e.id + '">查成绩</a>' +
            '<a class="btn btn-ghost btn-sm" href="#event/' + e.id + '">详情</a>' +
          "</div>";
      } else if (canSignup) {
        actions =
          '<div style="display:flex;gap:.4rem;flex-wrap:wrap;justify-content:flex-end">' +
            '<a class="btn btn-signal btn-sm" href="#signup/' + e.id + '">报名 ' + feeText + "</a>" +
            '<button type="button" class="btn btn-ghost btn-sm" data-invite="' + e.id + '">邀请好友</button>' +
          "</div>";
      } else {
        actions = '<a class="btn btn-ghost btn-sm" href="#event/' + e.id + '">详情</a>';
      }
      return (
        '<div class="event-row">' +
          '<div class="event-date">' + md.d + '<small>' + md.m + " 月</small></div>" +
          '<div class="event-main">' +
            '<a class="event-name" href="#event/' + e.id + '">' + escapeHtml(e.eventName || "") + "</a>" +
            '<div class="event-meta">' +
              statusPill +
              '<span class="pill">' + API.eventTypeText(e.eventType) + "</span>" +
              "<span>" + escapeHtml(e.startLocation || "") + "</span>" +
              "<span>" + (e.distanceKm != null ? e.distanceKm + " km" : "") + "</span>" +
              '<span class="pill fee">' + feeText + " 报名费</span>" +
              (canSignup ? "<span>余 " + (e.remainQuota != null ? e.remainQuota : "-") + " 名</span>" : "") +
              (e.hasResults ? '<span class="pill">成绩样例可查</span>' : "") +
            "</div>" +
          "</div>" +
          '<div class="event-fee" aria-label="报名费"><strong>' + feeText + "</strong><small>报名费</small></div>" +
          '<div class="event-actions">' + actions + "</div>" +
        "</div>"
      );
    }).join("");
    mount.querySelectorAll("[data-invite]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var id = btn.getAttribute("data-invite");
        var ev = (state.events || []).concat(state.allEvents || []).find(function (x) {
          return String(x.id) === String(id);
        });
        if (ev) openInvite(ev);
        else openInvite({ id: id, eventName: "马拉松赛事" });
      });
    });
  }

  function loadEvents() {
    return API.get("/api/public/events", null, { noAuth: true })
      .then(function (res) {
        state.events = res.data || [];
        renderEventRows(state.events, "eventsMount", "open");
      })
      .catch(function (e) {
        API.toast(e.message, "err");
        $("eventsMount").innerHTML = '<div class="empty">加载失败</div>';
      });
  }

  function loadArchive() {
    return API.get("/api/public/events/all", null, { noAuth: true })
      .then(function (res) {
        state.allEvents = res.data || [];
        renderEventRows(state.allEvents, "archiveMount", "archive");
      })
      .catch(function (e) {
        API.toast(e.message, "err");
        $("archiveMount").innerHTML = '<div class="empty">加载失败</div>';
      });
  }

  function loadHomePreview() {
    return API.get("/api/public/events", null, { noAuth: true })
      .then(function (res) {
        state.events = res.data || [];
        renderEventRows(state.events.slice(0, 3), "homeEvents", "open");
      })
      .catch(function () {
        $("homeEvents").innerHTML = '<div class="empty">赛事暂不可用</div>';
      });
  }

  function loadEventDetail(id) {
    $("eventDetail").innerHTML = '<div class="empty">加载中…</div>';
    Promise.all([
      API.get("/api/public/event/" + id, null, { noAuth: true }),
      API.get("/api/public/event/" + id + "/stats", null, { noAuth: true }).catch(function () {
        return { data: null };
      })
    ])
      .then(function (pair) {
        var e = (pair[0] && pair[0].data) || {};
        var stats = (pair[1] && pair[1].data) || null;
        state.eventId = e.id;
        state.lastEvent = e;
        var open = e.status === 0 || e.status === "0";
        var remain = e.remainQuota != null ? e.remainQuota : 0;
        var feeText = e.entryFee != null ? "¥" + e.entryFee : "-";
        var cta = open && remain > 0
          ? '<a class="btn btn-signal" href="#signup/' + e.id + '">我要报名 · ' + feeText + "</a>" +
            '<p class="cta-hint">' + escapeHtml(e.phaseLabel || "报名中") + " · 剩余 " + remain + " 名</p>"
          : '<button type="button" class="btn btn-ghost" disabled>暂不可报名</button>' +
            '<p class="cta-hint">' + API.statusText(e.status) + "</p>";
        var resultNote = e.resultHint
          ? '<p class="cta-hint" style="margin-top:.35rem">' + escapeHtml(e.resultHint) + "</p>"
          : "";

        $("eventDetail").innerHTML =
          '<div class="detail-poster ' + posterClass(e) + ' anim-rise">' +
            '<div class="detail-poster-body">' +
              '<p class="hero-official">组委会官方赛事页</p>' +
              '<div class="section-kicker">EVENT DETAIL</div>' +
              '<h1 class="detail-title">' + escapeHtml(e.eventName || "") + "</h1>" +
              (e.phaseLabel ? '<p class="pill signal" style="display:inline-flex;margin-top:.55rem">' + escapeHtml(e.phaseLabel) + "</p>" : "") +
              '<p class="detail-poster-meta">' + escapeHtml(API.fmtDay(e.eventDate) || "") +
                " · " + escapeHtml(e.startLocation || "") +
                (e.distanceKm != null ? (" · " + e.distanceKm + " km") : "") +
                " · 报名费 " + feeText +
              "</p>" +
            "</div>" +
          "</div>" +
          renderCommitteeBar(e) +
          renderSponsorStrip(e) +
          '<div class="detail-grid">' +
            '<div class="detail-block anim-rise-d1">' +
              "<h3>赛道与规则</h3>" +
              "<p>" + escapeHtml(e.routeDesc || "暂无赛道简介") + "</p>" +
              "<h3>报名规则</h3>" +
              "<p>" + escapeHtml(e.signupRule || "暂无") + "</p>" +
              "<h3>关门时间</h3>" +
              "<p>" + escapeHtml(e.cutOffTime || "暂无") + "</p>" +
              renderPickupCards(e) +
            "</div>" +
            '<div class="fact-stack anim-rise-d2">' +
              fact("比赛日", API.fmtDay(e.eventDate)) +
              fact("发令", API.fmtDate(e.gunTime)) +
              fact("距离", (e.distanceKm != null ? e.distanceKm + " km" : "-")) +
              fact("类型", API.eventTypeText(e.eventType)) +
              fact("起点", e.startLocation || "-") +
              fact("报名费", feeText) +
              fact("已报名 / 名额", (e.registeredCount || 0) + " / " + (e.quota || 0)) +
              fact("剩余名额", remain) +
              fact("报名窗口", API.fmtDate(e.signupStart) + " → " + API.fmtDate(e.signupEnd)) +
              (e.finisherCount ? fact("成绩样例", e.finisherCount + " 条可查") : "") +
              '<div class="cta-stack">' + cta + resultNote +
                '<div style="display:flex;gap:.55rem;flex-wrap:wrap">' +
                  '<a class="btn btn-ghost btn-sm" href="#rank/' + e.id + '">' +
                    (e.hasResults ? "查看成绩样例" : "查看排名") + "</a>" +
                  '<button type="button" class="btn btn-ghost btn-sm" id="btnJumpGuide">参赛须知</button>' +
                  '<a class="btn btn-ghost btn-sm" href="#faq">常见问题</a>' +
                  '<a class="btn btn-ghost btn-sm" href="#my">我的报名</a>' +
                  '<button type="button" class="btn btn-ghost btn-sm" id="btnShareInvite">邀请跑团好友</button>' +
                "</div>" +
              "</div>" +
            "</div>" +
          "</div>" +
          renderServiceStrip(e) +
          renderCourseViz(e) +
          '<div id="guideAnchor"></div>' +
          renderRunnerGuide(e) +
          renderRefundPanel(e) +
          renderFinishStats(stats) +
          '<div class="section" style="padding-top:0;padding-bottom:3rem"><div class="panel">' +
            "<strong>组委会联系方式</strong>" +
            '<p class="muted" style="margin-top:.55rem">热线：029-8888-2026（工作日 9:00–18:00） · 邮箱：race@gunstart.xian</p>' +
          "</div></div>";
        mountRouteMap();
        function jumpGuide() {
          var el = $("runnerGuide") || $("guideAnchor");
          if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
        }
        ["btnJumpGuide", "btnJumpGuide2"].forEach(function (id) {
          var jump = $(id);
          if (jump) jump.onclick = jumpGuide;
        });
        var shareInvite = $("btnShareInvite");
        if (shareInvite) {
          shareInvite.onclick = function () { openInvite(e); };
        }
      })
      .catch(function (err) {
        $("eventDetail").innerHTML = '<div class="empty">' + escapeHtml(err.message) + "</div>";
      });
  }

  var DEFAULT_GUIDE = {
    handbook: "一、参赛资格：年满16周岁，身体健康。\n二、体检：建议赛前3个月内完成体检（心电图、血压）。\n三、成绩证明：完赛后可在「我的报名」下载成绩证明与电子确认函。",
    medical: "请如实申报病史。心脏病、高血压未控制等情况请勿勉强参赛。比赛日设医疗点与 AED。",
    insurance: "报名费已含基础赛事意外险。如需提高保额，可自行向保险公司加购。退赛成功后报名费原路退回。",
    transport: "建议地铁出行。西马起点附近：大雁塔 / 大唐不夜城站。城墙赛：永宁门 / 钟楼站。赛日部分道路管制，请提前查封路公告。",
    hotel: "建议赛前一晚入住起点 3 公里内。曲江、南门、钟楼沿线酒店民宿均可。",
    contact: "组委会热线：029-8888-2026（工作日 9:00–18:00）\n邮箱：race@gunstart.xian\n现场服务：领物处与起点医疗服务点",
    sponsors: ["西安文旅", "发令台运动", "秦岭矿泉水", "长安保险", "城墙文创"],
    landmarks: [
      { km: 0, name: "大雁塔起点" },
      { km: 8, name: "曲江池" },
      { km: 15, name: "大唐芙蓉园" },
      { km: 21, name: "永宁门" },
      { km: 30, name: "钟楼" },
      { km: 42.2, name: "城墙南门终点" }
    ],
    pickup: {
      date: "赛前 2–3 天",
      hours: "10:00–18:00",
      venue: "西安国际会展中心",
      entrance: "B2 南门安检入口",
      queue: "预计排队 15–40 分钟，建议错峰午后",
      bring: "身份证原件、电子确认函、报名手机号"
    }
  };

  var DEFAULT_AIDS = [
    { km: 5, name: "5公里补给", landmark: "大雁塔北广场", items: "水、电解质、医疗" },
    { km: 10, name: "10公里补给", landmark: "曲江池西岸", items: "水、能量胶、香蕉" },
    { km: 15, name: "15公里补给", landmark: "大唐芙蓉园外", items: "水、电解质、海绵" },
    { km: 21.1, name: "半马折返补给", landmark: "城墙·永宁门外", items: "水、能量胶、医疗、厕所" },
    { km: 30, name: "30公里补给", landmark: "钟楼南大街", items: "水、能量胶、香蕉、医疗" },
    { km: 35, name: "35公里补给", landmark: "回民街外围", items: "水、电解质、海绵" },
    { km: 40, name: "40公里补给", landmark: "南门瓮城附近", items: "水、可乐、医疗" }
  ];

  function parseGuide(e) {
    var g = {};
    try {
      if (e && e.guideJson) g = typeof e.guideJson === "string" ? JSON.parse(e.guideJson) : e.guideJson;
    } catch (ignore) {}
    var out = {};
    Object.keys(DEFAULT_GUIDE).forEach(function (k) {
      out[k] = (g && g[k]) ? g[k] : DEFAULT_GUIDE[k];
    });
    if (g && g.pickup) out.pickup = g.pickup;
    if (g && g.landmarks) out.landmarks = g.landmarks;
    if (g && g.sponsors) out.sponsors = g.sponsors;
    return out;
  }

  function parsePickup(e) {
    var g = parseGuide(e);
    if (g.pickup && g.pickup.venue) return g.pickup;
    var raw = (e && e.pickupInfo) || "";
    if (!raw) return DEFAULT_GUIDE.pickup;
    // 兼容「日期…；时段…；地点…」短句
    function pick(key) {
      var m = raw.match(new RegExp(key + "[：:]?\\s*([^；;]+)"));
      return m ? m[1].trim() : "";
    }
    return {
      date: pick("日期") || DEFAULT_GUIDE.pickup.date,
      hours: pick("时段") || DEFAULT_GUIDE.pickup.hours,
      venue: pick("地点") || DEFAULT_GUIDE.pickup.venue,
      entrance: pick("入口") || DEFAULT_GUIDE.pickup.entrance,
      queue: pick("预计排队") || pick("排队") || DEFAULT_GUIDE.pickup.queue,
      bring: pick("携带") || DEFAULT_GUIDE.pickup.bring
    };
  }

  function renderPickupCards(e) {
    var p = parsePickup(e);
    var cells = [
      ["领物日期", p.date],
      ["开放时段", p.hours],
      ["领物地点", p.venue],
      ["入口指引", p.entrance],
      ["排队提示", p.queue],
      ["请携带", p.bring]
    ];
    return (
      '<div class="pickup-board">' +
        '<div class="pickup-board-head"><strong>参赛包领取</strong><span>请按下列信息到场，出示证件与确认函</span></div>' +
        '<div class="pickup-grid">' +
          cells.map(function (c) {
            return '<div class="pickup-card"><div class="lab">' + c[0] +
              '</div><div class="val">' + escapeHtml(c[1] || "-") + "</div></div>";
          }).join("") +
        "</div>" +
      "</div>"
    );
  }

  function renderSponsorStrip(e) {
    var g = parseGuide(e);
    var list = (g.sponsors && g.sponsors.length) ? g.sponsors : DEFAULT_GUIDE.sponsors;
    return (
      '<div class="sponsor-strip detail-sponsor">' +
        '<span class="sponsor-kicker">本场合作伙伴</span>' +
        '<div class="sponsor-logos">' +
          list.map(function (s) { return "<span>" + escapeHtml(String(s)) + "</span>"; }).join("") +
        "</div>" +
      "</div>"
    );
  }

  function renderCommitteeBar(e) {
    return (
      '<div class="official-bar detail-official">' +
        '<div class="official-bar-inner">' +
          "<div><strong>赛事</strong><span>" + escapeHtml(e.eventName || "西马系赛事") + "</span></div>" +
          "<div><strong>组委会</strong><span>发令台赛事中心</span></div>" +
          "<div><strong>热线</strong><span>029-8888-2026</span></div>" +
          "<div><strong>起点</strong><span>" + escapeHtml(e.startLocation || "-") + "</span></div>" +
        "</div>" +
      "</div>"
    );
  }

  function posterClass(e) {
    var t = Number(e && e.eventType);
    if (t === 2) return "poster-half";
    if (t === 3) return "poster-mini";
    return "poster-full";
  }

  function renderRunnerGuide(e) {
    var g = parseGuide(e);
    var blocks = [
      { k: "handbook", t: "参赛手册 · 资格与成绩证明" },
      { k: "medical", t: "体检与健康提示" },
      { k: "insurance", t: "保险说明" },
      { k: "transport", t: "交通与停车" },
      { k: "hotel", t: "附近住宿" },
      { k: "contact", t: "组委会联系方式" }
    ];
    var cards = blocks.map(function (b) {
      return (
        '<div class="panel guide-card"><h4>' + b.t + "</h4><p>" +
        escapeHtml(String(g[b.k] || "")) + "</p></div>"
      );
    }).join("");
    return (
      '<div class="section" style="padding-top:0" id="runnerGuide">' +
        '<div class="section-head"><div><div class="section-kicker">RUNNER GUIDE</div>' +
        '<h2 class="section-title" style="font-size:1.6rem">参赛须知</h2>' +
        '<p class="section-sub">手册 · 体检 · 保险 · 交通住宿 · 客服热线 029-8888-2026</p></div></div>' +
        '<div class="guide-grid">' + cards + "</div>" +
      "</div>"
    );
  }

  function renderServiceStrip(e) {
    return (
      '<div class="section" style="padding-top:0">' +
        '<div class="service-strip">' +
          "<div><strong>客服热线</strong><span>029-8888-2026</span></div>" +
          "<div><strong>保险</strong><span>报名费含基础意外险</span></div>" +
          "<div><strong>退赛</strong><span>赛前7天可原路退费</span></div>" +
          '<button type="button" class="btn btn-ghost btn-sm" id="btnJumpGuide2">查看完整参赛须知</button>' +
        "</div>" +
      "</div>"
    );
  }

  function renderFinishStats(stats) {
    if (!stats || !stats.finishers) {
      return (
        '<div class="section" style="padding-top:0"><div class="panel">' +
          "<strong>往年 / 本场完赛数据</strong>" +
          '<p class="muted" style="margin-top:.4rem">成绩录入后将显示完赛人数、平均净成绩与分段分布。</p>' +
        "</div></div>"
      );
    }
    var total = Number(stats.finishers) || 1;
    function bar(label, n) {
      var pct = Math.max(2, Math.round((Number(n) || 0) / total * 100));
      return (
        '<div class="stats-bar-row"><span>' + label + '</span><div class="stats-bar-track">' +
          '<div class="stats-bar-fill" style="width:' + pct + '%"></div></div>' +
          "<span>" + (n || 0) + "</span></div>"
      );
    }
    return (
      '<div class="section" style="padding-top:0"><div class="panel">' +
        "<strong>完赛成绩分布（样例）</strong>" +
        '<p class="muted" style="margin-top:.35rem">样例完赛 ' + stats.finishers +
          " 人 · 冠军净成绩 " + escapeHtml(stats.bestNetStr || "-") +
          " · 平均 " + escapeHtml(stats.avgNetStr || "-") +
          " · 男 " + (stats.maleCount || 0) + " / 女 " + (stats.femaleCount || 0) +
          "。正式赛事规模以当日芯片完赛为准。</p>" +
        '<div class="stats-bars">' +
          bar("3 小时内", stats.under3h) +
          bar("3–4 小时", stats.h3to4) +
          bar("4–5 小时", stats.h4to5) +
          bar("5 小时以上", stats.over5h) +
        "</div></div></div>"
    );
  }

  function fact(lab, val) {
    return (
      '<div class="fact"><div class="lab">' + lab + '</div><div class="val">' +
      escapeHtml(String(val)) + "</div></div>"
    );
  }

  function parseJsonArr(raw) {
    if (!raw) return [];
    if (Array.isArray(raw)) return raw;
    try { return JSON.parse(raw); } catch (e) { return []; }
  }

  function renderCourseViz(e) {
    var aids = parseJsonArr(e.aidStations);
    if (!aids.length) aids = DEFAULT_AIDS.slice();
    // 英文旧数据换成中文默认
    if (aids.length && /aid|Half|water/i.test(String(aids[0].name || "") + String(aids[0].items || ""))) {
      aids = DEFAULT_AIDS.slice();
    }
    var profile = parseJsonArr(e.routeProfile);
    var g = parseGuide(e);
    var landmarks = (g.landmarks && g.landmarks.length) ? g.landmarks : DEFAULT_GUIDE.landmarks;
    var gpx = e.routeGpx || "";
    var mapHtml = gpx
      ? '<div class="route-map" id="routeMap" data-gpx="' + escapeHtml(gpx) + '"></div>' +
        '<div class="route-map-meta">' +
          "<span>赛道手册路线图（含西安地标标注）</span>" +
          '<a href="' + escapeHtml(gpx) + '" download>下载 GPX</a>' +
        "</div>"
      : "";
    var landmarkChips =
      '<div class="landmark-strip">' +
        landmarks.map(function (lm) {
          return '<span class="landmark-chip"><em>' + escapeHtml(String(lm.km)) +
            "K</em>" + escapeHtml(lm.name || "") + "</span>";
        }).join("") +
      "</div>";
    var svg = "";
    if (profile.length >= 2) {
      var maxE = Math.max.apply(null, profile.map(function (p) { return Number(p.elev) || 0; }));
      var minE = Math.min.apply(null, profile.map(function (p) { return Number(p.elev) || 0; }));
      var maxK = Number(profile[profile.length - 1].km) || 42.2;
      var span = Math.max(maxE - minE, 20);
      var pts = profile.map(function (p) {
        var x = (Number(p.km) / maxK) * 1000;
        var y = 120 - ((Number(p.elev) - minE) / span) * 100;
        return x + "," + y;
      }).join(" ");
      svg =
        '<svg class="elev-svg" viewBox="0 0 1000 140" preserveAspectRatio="none" aria-label="海拔剖面">' +
          '<polyline fill="none" stroke="#e23b2a" stroke-width="4" points="' + pts + '" />' +
          '<polyline fill="rgba(226,59,42,.12)" stroke="none" points="0,140 ' + pts + ' 1000,140" />' +
        "</svg>";
    }
    var aidHtml = aids.length
      ? '<div class="aid-km-board">' +
          aids.map(function (a) {
            return (
              '<div class="aid-km-card">' +
                '<div class="km-plate">' + escapeHtml(String(a.km)) + "<small>KM</small></div>" +
                '<div class="aid-km-body">' +
                  "<strong>" + escapeHtml(a.name || "补给站") + "</strong>" +
                  (a.landmark ? '<div class="aid-landmark">地标 · ' + escapeHtml(a.landmark) + "</div>" : "") +
                  '<div class="muted">' + escapeHtml(a.items || "水、能量胶") + "</div>" +
                "</div>" +
              "</div>"
            );
          }).join("") +
        "</div>"
      : "";
    return (
      '<div class="section" style="padding-top:0">' +
        '<div class="course-viz">' +
          '<div class="section-kicker">COURSE BOOK</div>' +
          "<h3>赛道手册 · 地标与补给</h3>" +
          '<p class="muted" style="margin:.35rem 0 .85rem">按公里牌阅读：先看地标，再看补给物资。</p>' +
          landmarkChips +
          mapHtml + svg +
          (aidHtml ? "<h4 class=\"aid-heading\">补给站（按公里牌）</h4>" + aidHtml : "") +
        "</div>" +
      "</div>"
    );
  }

  function renderRefundPanel(e) {
    var fee = e.entryFee != null ? e.entryFee : "-";
    return (
      '<div class="section" style="padding-top:0"><div class="panel" id="refundPanel">' +
        "<strong>退赛退款说明</strong>" +
        '<p class="muted" style="margin-top:.45rem;white-space:pre-line">' +
          escapeHtml(e.cancelRule || "") +
        "</p>" +
        '<div class="fact-stack" style="margin-top:.75rem">' +
          fact("可退金额", "全额报名费 ¥" + fee) +
          fact("到账时间", "原路退回，约 3–10 个工作日") +
          fact("操作入口", "我的报名 → 申请退赛退款（赛前 7 天窗口内）") +
          fact("逾期处理", "请联系组委会热线 029-8888-2026") +
        "</div>" +
      "</div></div>"
    );
  }

  function mountRouteMap() {
    var el = $("routeMap");
    if (!el || typeof L === "undefined") return;
    var gpxUrl = el.getAttribute("data-gpx");
    if (!gpxUrl) return;
    if (el._leaflet_id) {
      try { el._leaflet_id = null; el.innerHTML = ""; } catch (ignore) {}
    }
    var map = L.map(el, { scrollWheelZoom: false }).setView([34.25, 108.95], 12);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 18,
      attribution: "&copy; OpenStreetMap"
    }).addTo(map);
    fetch(gpxUrl, { cache: "no-store" })
      .then(function (r) { return r.text(); })
      .then(function (xml) {
        var pts = parseGpxPoints(xml);
        if (!pts.length) {
          el.insertAdjacentHTML("afterend", '<p class="muted">GPX 暂无轨迹点</p>');
          return;
        }
        var latlngs = pts.map(function (p) { return [p.lat, p.lon]; });
        var line = L.polyline(latlngs, { color: "#e23b2a", weight: 4, opacity: 0.9 }).addTo(map);
        L.circleMarker(latlngs[0], { radius: 7, color: "#f0b429", fillColor: "#f0b429", fillOpacity: 1 })
          .bindPopup("起点").addTo(map);
        L.circleMarker(latlngs[latlngs.length - 1], { radius: 7, color: "#e23b2a", fillColor: "#e23b2a", fillOpacity: 1 })
          .bindPopup("终点").addTo(map);
        var landmarks = [
          { name: "大雁塔 · 起点", lat: 34.2184, lon: 108.9642 },
          { name: "曲江池", lat: 34.2055, lon: 108.9860 },
          { name: "大唐芙蓉园", lat: 34.2145, lon: 108.9728 },
          { name: "城墙·永宁门", lat: 34.2508, lon: 108.9420 },
          { name: "钟楼", lat: 34.2610, lon: 108.9425 },
          { name: "回民街外围", lat: 34.2635, lon: 108.9402 },
          { name: "大唐不夜城", lat: 34.2168, lon: 108.9678 }
        ];
        landmarks.forEach(function (lm) {
          L.marker([lm.lat, lm.lon], {
            title: lm.name
          }).bindPopup("<strong>" + lm.name + "</strong><br/>赛道手册地标").addTo(map);
        });
        map.fitBounds(line.getBounds(), { padding: [28, 28] });
        setTimeout(function () { map.invalidateSize(); }, 80);
      })
      .catch(function () {
        el.insertAdjacentHTML("afterend", '<p class="muted">路线图加载失败，可直接下载 GPX</p>');
      });
  }

  function parseGpxPoints(xmlText) {
    var out = [];
    try {
      var doc = new DOMParser().parseFromString(xmlText, "application/xml");
      var nodes = doc.getElementsByTagName("trkpt");
      if (!nodes.length) nodes = doc.getElementsByTagName("rtept");
      for (var i = 0; i < nodes.length; i++) {
        var lat = parseFloat(nodes[i].getAttribute("lat"));
        var lon = parseFloat(nodes[i].getAttribute("lon"));
        if (!isNaN(lat) && !isNaN(lon)) out.push({ lat: lat, lon: lon });
      }
    } catch (e) {}
    return out;
  }

  function clearSignupForm() {
    ["suName", "suIdCard", "suPhone", "suBirth", "suEmName", "suEmPhone"].forEach(function (id) {
      if ($(id)) $(id).value = "";
    });
    if ($("suGender")) $("suGender").value = "1";
    if ($("suShirt")) $("suShirt").value = "";
  }

  function prepareSignup(id) {
    $("signupEventId").value = id;
    clearSignupForm();
    $("signupMsg").textContent = "加载赛事信息…";
    document.querySelectorAll("#signupSteps .step").forEach(function (el, idx) {
      el.classList.toggle("is-on", idx === 0);
      el.classList.remove("is-done");
    });
    API.get("/api/public/event/" + id, null, { noAuth: true })
      .then(function (res) {
        var e = res.data || {};
        state.lastEvent = e;
        if (e.status !== 0 && e.status !== "0") {
          $("signupMsg").textContent = "该赛事当前不可报名（" + API.statusText(e.status) + "）";
          $("btnSignupSubmit").disabled = true;
          return;
        }
        $("btnSignupSubmit").disabled = false;
        $("signupMsg").textContent =
          (e.eventName || "") + " · 余 " + (e.remainQuota != null ? e.remainQuota : "-") +
          " 名 · 报名费 ¥" + (e.entryFee != null ? e.entryFee : "-") +
          " · 步骤：填表 → 支付 → 完成";
      })
      .catch(function (e) {
        $("signupMsg").textContent = e.message;
        $("btnSignupSubmit").disabled = true;
      });
  }

  function submitSignup() {
    var emName = ($("suEmName") && $("suEmName").value || "").trim();
    var emPhone = ($("suEmPhone") && $("suEmPhone").value || "").trim();
    var body = {
      eventId: Number($("signupEventId").value),
      name: ($("suName").value || "").trim(),
      gender: Number($("suGender").value),
      idCard: ($("suIdCard").value || "").trim(),
      phone: ($("suPhone").value || "").trim(),
      birthDate: $("suBirth").value,
      shirtSize: ($("suShirt").value || "").trim(),
      emergencyName: emName,
      emergencyPhone: emPhone,
      emergencyContact: emName + " " + emPhone
    };
    if (!body.shirtSize) {
      API.toast("请选择参赛服尺码", "err");
      return;
    }
    if (!emName || !/^1[3-9]\d{9}$/.test(emPhone)) {
      API.toast("请填写紧急联系人姓名与 11 位手机号", "err");
      return;
    }
    $("btnSignupSubmit").disabled = true;
    API.post("/api/public/signup", body, { noAuth: true })
      .then(function (res) {
        var d = res.data || {};
        try {
          sessionStorage.setItem("gunstart_letter", JSON.stringify({
            registrationId: d.registrationId,
            phone: body.phone,
            name: d.name || body.name,
            eventName: d.eventName,
            eventDate: d.eventDate,
            entryFee: d.entryFee,
            orderNo: d.orderNo,
            shirtSize: d.shirtSize || body.shirtSize,
            bibHint: d.bibHint,
            payStatus: 0
          }));
        } catch (ignore) {}
        clearSignupForm();
        API.toast("资料已提交，请完成支付", "ok");
        $("myPhone").value = body.phone;
        location.hash = "#my";
        setTimeout(function () { queryMy(); }, 200);
      })
      .catch(function (e) {
        API.toast(e.message || "报名失败", "err");
        $("btnSignupSubmit").disabled = false;
      });
  }

  function readLetter() {
    try {
      var raw = sessionStorage.getItem("gunstart_letter");
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function clearLetter() {
    try { sessionStorage.removeItem("gunstart_letter"); } catch (e) {}
  }

  function cashierHtml(prefix) {
    return (
      '<div class="cashier-row" data-cashier="' + prefix + '">' +
        '<label class="is-on"><input type="radio" name="' + prefix + 'Ch" value="wechat" checked /> 微信支付</label>' +
        '<label><input type="radio" name="' + prefix + 'Ch" value="alipay" /> 支付宝</label>' +
      "</div>"
    );
  }

  function bindCashierStyles(root) {
    (root || document).querySelectorAll("[data-cashier]").forEach(function (row) {
      row.querySelectorAll("label").forEach(function (lab) {
        var input = lab.querySelector("input");
        if (!input) return;
        input.onchange = function () {
          row.querySelectorAll("label").forEach(function (l) { l.classList.remove("is-on"); });
          lab.classList.add("is-on");
        };
      });
    });
  }

  function selectedChannel(prefix) {
    var el = document.querySelector('input[name="' + prefix + 'Ch"]:checked');
    return el ? el.value : "wechat";
  }

  function renderConfirmLetter(letter) {
    if (!letter) return "";
    var fee = letter.entryFee != null ? "¥" + letter.entryFee : "报名费";
    var unpaid = letter.payStatus === 0 || letter.payStatus === "0";
    var bibPart = letter.bibNumber
      ? (' · 号码布 <strong class="fee-tag">' + escapeHtml(String(letter.bibNumber)) + "</strong>")
      : "";
    return (
      '<div class="confirm-letter" id="confirmLetter" data-reg="' +
        escapeHtml(String(letter.registrationId || "")) + '">' +
        '<div class="section-kicker">CONFIRMATION</div>' +
        "<h3>" + (unpaid ? "待支付确认函" : "报名确认函") + "</h3>" +
        "<p><strong>" + escapeHtml(letter.eventName || "") + "</strong> · " +
          API.fmtDay(letter.eventDate) +
          " · " + escapeHtml(letter.name || "") + bibPart + "</p>" +
        '<p class="muted">订单 ' + escapeHtml(letter.orderNo || "-") +
          " · 尺码 " + escapeHtml(letter.shirtSize || "-") +
          (unpaid ? " · 待付 <strong class=\"fee-tag\">" + escapeHtml(String(fee)) + "</strong>" : "") +
          (!unpaid && letter.payVoucher ? " · 凭证 " + escapeHtml(letter.payVoucher) : "") +
        "</p>" +
        (unpaid
          ? cashierHtml("letter") +
            '<div class="rc-actions" style="margin-top:.55rem">' +
              '<button type="button" class="btn btn-signal btn-sm" id="btnConfirmPay">确认支付 ' +
                escapeHtml(String(fee)) + "</button>" +
              '<button type="button" class="btn btn-ghost btn-sm" id="btnDismissLetter">稍后支付</button>' +
            "</div>"
          : '<div class="letter-actions">' +
              '<button type="button" class="btn btn-signal btn-sm" id="btnDownloadLetter">保存确认函图片</button>' +
              '<button type="button" class="btn btn-ghost btn-sm" id="btnCopyLetter">复制确认文案</button>' +
              (letter.bibNumber
                ? '<button type="button" class="btn btn-ghost btn-sm" id="btnDownloadBib">保存电子号牌</button>'
                : "") +
              '<button type="button" class="btn btn-ghost btn-sm" id="btnDismissLetter">收起</button>' +
            "</div>") +
      "</div>"
    );
  }

  function openInvite(e) {
    if (!e) return;
    var feeText = e.entryFee != null ? "¥" + e.entryFee : "详见页面";
    var url = location.origin + location.pathname + "#signup/" + e.id;
    var text =
      "【发令台 · 一起报名】\n" +
      "赛事：" + (e.eventName || "马拉松") + "\n" +
      "比赛日：" + (API.fmtDay(e.eventDate) || "-") + "\n" +
      "报名费：" + feeText + "\n" +
      "报名入口：" + url + "\n" +
      "跑团好友一起来，占名额别拖。";
    state.inviteText = text;
    state.inviteUrl = url;
    $("invitePreview").textContent = text;
    $("inviteLink").textContent = url;
    $("inviteMask").classList.add("open");
  }

  function copyText(text, okMsg) {
    if (!text) return;
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(function () {
        API.toast(okMsg || "已复制", "ok");
      }).catch(function () {
        prompt("请手动复制", text);
      });
    } else {
      prompt("请手动复制", text);
    }
  }

  function letterShareText(letter) {
    if (!letter) return "";
    return (
      "【发令台报名确认函】\n" +
      "赛事：" + (letter.eventName || "-") + "\n" +
      "姓名：" + (letter.name || "-") + "\n" +
      "手机：" + (letter.phone || "-") + "\n" +
      "比赛日：" + (API.fmtDay(letter.eventDate) || "-") + "\n" +
      "订单号：" + (letter.orderNo || "-") + "\n" +
      (letter.bibNumber ? ("号码布：" + letter.bibNumber + "\n") : "") +
      "尺码：" + (letter.shirtSize || "-") + "\n" +
      "支付：" + (letter.payChannelLabel || API.channelText(letter.payChannel) || "-") +
        (letter.payVoucher ? (" / " + letter.payVoucher) : "") + "\n" +
      "领物请出示本确认函与有效证件。客服 029-8888-2026"
    );
  }

  function confirmPay(registrationId, phone, channel) {
    return API.post("/api/public/confirmPay", {
      registrationId: Number(registrationId),
      phone: phone,
      channel: channel || "wechat"
    }, { noAuth: true }).then(function (res) {
      var letter = res.data || {};
      letter.payStatus = 1;
      try { sessionStorage.setItem("gunstart_letter", JSON.stringify(letter)); } catch (e) {}
      API.toast("支付成功", "ok");
      queryMy();
    }).catch(function (e) {
      API.toast(e.message || "支付失败", "err");
    });
  }

  function downloadConfirmLetter(letter) {
    if (!letter) return;
    var canvas = document.createElement("canvas");
    canvas.width = 900;
    canvas.height = 1200;
    var ctx = canvas.getContext("2d");
    ctx.fillStyle = "#1a120f";
    ctx.fillRect(0, 0, 900, 1200);
    ctx.fillStyle = "#f0b429";
    ctx.fillRect(0, 0, 900, 12);
    ctx.fillStyle = "#f7f1e8";
    ctx.font = "600 28px sans-serif";
    ctx.fillText("发令台 GUNSTART · 报名确认函", 48, 80);
    ctx.fillStyle = "#f0b429";
    ctx.font = "700 42px serif";
    ctx.fillText(letter.eventName || "马拉松赛事", 48, 160);
    ctx.fillStyle = "#cfc3b4";
    ctx.font = "22px sans-serif";
    var lines = [
      "姓名：" + (letter.name || "-"),
      "手机：" + (letter.phone || "-"),
      "比赛日：" + (API.fmtDay(letter.eventDate) || "-"),
      "订单号：" + (letter.orderNo || "-"),
      "报名号：" + (letter.registrationId || "-"),
      "号码布：" + (letter.bibNumber || "待发"),
      "参赛服：" + (letter.shirtSize || "-"),
      "支付凭证：" + (letter.payVoucher || "-"),
      "支付方式：" + (letter.payChannelLabel || API.channelText(letter.payChannel) || "-"),
      "",
      "领物时请出示本确认函与有效身份证件。",
      letter.bibHint || "号码布预计比赛日前 7–14 天发放。"
    ];
    var y = 240;
    lines.forEach(function (line) {
      wrapCanvasText(ctx, line, 48, y, 800, 34);
      y += 52;
    });
    ctx.fillStyle = "#e23b2a";
    ctx.font = "20px sans-serif";
    ctx.fillText("组委会热线 029-8888-2026", 48, 1140);
    var a = document.createElement("a");
    a.download = "确认函-" + (letter.orderNo || letter.registrationId || "gunstart") + ".png";
    a.href = canvas.toDataURL("image/png");
    a.click();
  }

  function downloadEBib(info) {
    if (!info) return;
    var bib = info.bibNumber || info.bib || "";
    if (!bib || bib === "待发" || bib === "-") {
      API.toast("号码布尚未发放", "err");
      return;
    }
    var canvas = document.createElement("canvas");
    canvas.width = 720;
    canvas.height = 900;
    var ctx = canvas.getContext("2d");
    var g = ctx.createLinearGradient(0, 0, 720, 900);
    g.addColorStop(0, "#1a1210");
    g.addColorStop(1, "#2a1814");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, 720, 900);
    ctx.strokeStyle = "#f0b429";
    ctx.lineWidth = 10;
    ctx.strokeRect(28, 28, 664, 844);
    ctx.fillStyle = "#f0b429";
    ctx.font = "600 28px sans-serif";
    ctx.fillText("GUNSTART · 电子号牌", 56, 90);
    ctx.fillStyle = "#cfc3b4";
    ctx.font = "22px sans-serif";
    wrapCanvasText(ctx, info.eventName || "马拉松赛事", 56, 140, 600, 32);
    ctx.fillStyle = "#f7f1e8";
    ctx.font = "700 40px sans-serif";
    ctx.fillText(info.name || info.runnerName || "-", 56, 230);
    ctx.fillStyle = "#f0b429";
    ctx.font = "700 160px Teko, Impact, sans-serif";
    var bibW = ctx.measureText(String(bib)).width;
    ctx.fillText(String(bib), Math.max(56, (720 - bibW) / 2), 480);
    ctx.fillStyle = "#b39a90";
    ctx.font = "500 22px sans-serif";
    ctx.fillText("比赛日 " + (API.fmtDay(info.eventDate) || "-"), 56, 580);
    ctx.fillText("请妥善保存，领物与检录时核对号码。", 56, 630);
    ctx.fillStyle = "#e23b2a";
    ctx.font = "20px sans-serif";
    ctx.fillText("组委会热线 029-8888-2026", 56, 820);
    var a = document.createElement("a");
    a.download = "号牌-" + bib + ".png";
    a.href = canvas.toDataURL("image/png");
    a.click();
    API.toast("电子号牌已保存", "ok");
  }

  function parseSec(v) {
    if (v == null || v === "") return null;
    var n = Number(v);
    return isNaN(n) ? null : n;
  }

  function paceLabel(secPerKm) {
    if (secPerKm == null || !(secPerKm > 0)) return "-";
    var total = Math.round(secPerKm);
    var m = Math.floor(total / 60);
    var s = total % 60;
    return m + "'" + (s < 10 ? "0" : "") + s + "/km";
  }

  /** 分段配速折线：5K / 10K / 半马 / 终点 */
  function renderPaceChart(result) {
    if (!result) return "";
    var dist = Number(result.distanceKm) || 42.2;
    var s5 = parseSec(result.split5k);
    var s10 = parseSec(result.split10k);
    var s21 = parseSec(result.split21k);
    var net = parseSec(result.netTime);
    var pts = [];
    if (s5 != null && s5 > 0) pts.push({ km: 5, label: "5K", cum: s5 });
    if (s10 != null && s10 > 0) pts.push({ km: 10, label: "10K", cum: s10 });
    if (s21 != null && s21 > 0) pts.push({ km: 21.1, label: "半马", cum: s21 });
    if (net != null && net > 0 && dist > 0) pts.push({ km: dist, label: "终点", cum: net });
    if (pts.length < 2) return "";

    var segs = [];
    var prevKm = 0;
    var prevCum = 0;
    pts.forEach(function (p) {
      var dKm = p.km - prevKm;
      var dSec = p.cum - prevCum;
      if (dKm > 0 && dSec > 0) {
        segs.push({
          label: p.label,
          km: p.km,
          pace: dSec / dKm,
          paceText: paceLabel(dSec / dKm)
        });
      }
      prevKm = p.km;
      prevCum = p.cum;
    });
    if (!segs.length) return "";

    var paces = segs.map(function (s) { return s.pace; });
    var minP = Math.min.apply(null, paces);
    var maxP = Math.max.apply(null, paces);
    if (maxP === minP) { minP -= 10; maxP += 10; }
    var w = 320, h = 120, padL = 28, padR = 12, padT = 18, padB = 28;
    var plotW = w - padL - padR;
    var plotH = h - padT - padB;
    var coords = segs.map(function (s, i) {
      var x = padL + (segs.length === 1 ? plotW / 2 : (i / (segs.length - 1)) * plotW);
      var y = padT + ((s.pace - minP) / (maxP - minP)) * plotH;
      return { x: x, y: y, s: s };
    });
    var line = coords.map(function (c, i) {
      return (i === 0 ? "M" : "L") + c.x.toFixed(1) + " " + c.y.toFixed(1);
    }).join(" ");
    var dots = coords.map(function (c) {
      return '<circle cx="' + c.x.toFixed(1) + '" cy="' + c.y.toFixed(1) +
        '" r="4.5" fill="#f0b429" stroke="#1a120f" stroke-width="2"/>' +
        '<text x="' + c.x.toFixed(1) + '" y="' + (c.y - 10).toFixed(1) +
        '" text-anchor="middle" class="pace-val">' + escapeHtml(c.s.paceText) + "</text>" +
        '<text x="' + c.x.toFixed(1) + '" y="' + (h - 8) +
        '" text-anchor="middle" class="pace-lab">' + escapeHtml(c.s.label) + "</text>";
    }).join("");
    var pills = segs.map(function (s) {
      return '<span class="pill">' + escapeHtml(s.label) + " " + escapeHtml(s.paceText) + "</span>";
    }).join("");

    return (
      '<div class="pace-chart-wrap">' +
        '<div class="pace-chart-head">分段配速</div>' +
        '<svg class="pace-chart" viewBox="0 0 ' + w + " " + h + '" role="img" aria-label="分段配速折线">' +
          '<path d="' + line + '" fill="none" stroke="#e23b2a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>' +
          dots +
        "</svg>" +
        '<div class="split-row">' + pills + "</div>" +
      "</div>"
    );
  }

  function wrapCanvasText(ctx, text, x, y, maxW, lineH) {
    var s = String(text || "");
    var line = "";
    for (var i = 0; i < s.length; i++) {
      var test = line + s.charAt(i);
      if (ctx.measureText(test).width > maxW && line) {
        ctx.fillText(line, x, y);
        line = s.charAt(i);
        y += lineH;
      } else {
        line = test;
      }
    }
    if (line) ctx.fillText(line, x, y);
  }

  function refreshSessionBar() {
    var bar = $("sessionBar");
    var token = getRunnerToken();
    var phone = "";
    var name = "";
    try {
      phone = localStorage.getItem(RUNNER_PHONE_KEY) || "";
      name = localStorage.getItem("gunstart_runner_name") || "";
    } catch (e) {}
    if (!token) {
      bar.style.display = "none";
      bar.innerHTML = "";
      return;
    }
    bar.style.display = "block";
    bar.innerHTML =
      "<strong>" + escapeHtml(name || "已登录") + "</strong>" +
      '<span class="muted"> · ' + escapeHtml(phone) + " · 个人中心会话有效</span> " +
      '<button type="button" class="btn btn-ghost btn-sm" id="btnLogoutRunner">退出</button>';
    $("btnLogoutRunner").onclick = function () {
      clearRunnerSession();
      refreshSessionBar();
      $("noticeMount").innerHTML = "";
      API.toast("已退出个人中心", "ok");
    };
  }

  function isOtpNotice(n) {
    var t = String((n && n.title) || "");
    var c = String((n && n.content) || "");
    return t.indexOf("验证码") >= 0 || c.indexOf("验证码") >= 0;
  }

  function renderNoticeList(list) {
    var clean = (list || []).filter(function (n) { return !isOtpNotice(n); });
    if (!clean.length) {
      $("noticeMount").innerHTML = "";
      return;
    }
    $("noticeMount").innerHTML =
      '<div class="panel"><div class="section-kicker">INBOX</div><h3 style="font-family:var(--display);font-size:1.6rem;margin:.2rem 0 .7rem">站内通知</h3>' +
      '<div class="notice-list">' +
      clean.slice(0, 8).map(function (n) {
        return '<div class="notice-item"><div class="t">' + escapeHtml(n.title || "") +
          '</div><div class="muted">' + escapeHtml(n.content || "") + "</div></div>";
      }).join("") +
      "</div></div>";
  }

  function loadNotices(phone) {
    var headers = runnerHeaders();
    var qs = [];
    if (phone) qs.push("phone=" + encodeURIComponent(phone));
    qs.push("_t=" + Date.now());
    var url = "/api/public/notices?" + qs.join("&");
    return fetch(url, { headers: headers, cache: "no-store" })
      .then(function (res) { return res.json(); })
      .then(function (json) {
        if (!json || json.code !== 200) throw new Error((json && json.msg) || "通知加载失败");
        renderNoticeList(json.data || []);
      })
      .catch(function () {
        $("noticeMount").innerHTML = "";
      });
  }

  function queryMy() {
    var phone = ($("myPhone").value || "").trim();
    if (!phone) {
      try { phone = localStorage.getItem(RUNNER_PHONE_KEY) || ""; $("myPhone").value = phone; } catch (e) {}
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      API.toast("请输入正确的 11 位手机号", "err");
      return;
    }
    $("myResult").innerHTML = '<div class="empty">查询中…</div>';
    refreshSessionBar();
    loadNotices(phone);
    var opts = { noAuth: true };
    if (getRunnerToken()) {
      opts = { headers: runnerHeaders() };
    }
    API.get("/api/public/my", { phone: phone }, opts)
      .then(function (res) {
        var data = res.data || {};
        var runner = data.runner || {};
        var regs = data.registrations || [];
        var letter = readLetter();
        if (letter && letter.phone && letter.phone !== phone) letter = null;
        var unpaidCount = regs.filter(function (r) {
          return (r.payStatus === 0 || r.payStatus === "0") &&
            !(r.status === 2 || r.status === "2" || r.status === 3 || r.status === "3");
        }).length;

        var html =
          (data.privacyHint
            ? '<p class="privacy-banner">' + escapeHtml(data.privacyHint) + "</p>"
            : "") +
          (unpaidCount
            ? '<div class="pay-urge"><strong>你有 ' + unpaidCount +
              " 笔报名待支付</strong>，请尽快完成，以免名额释放。</div>"
            : "") +
          renderConfirmLetter(letter) +
          '<div class="panel" style="margin-bottom:1rem">' +
            "<strong>" + escapeHtml(runner.name || "") + "</strong>" +
            '<span class="muted"> · ' + API.genderText(runner.gender) +
            " · " + escapeHtml(runner.phone || "") +
            (data.verified ? " · 已登录" : " · 脱敏预览") + "</span>" +
          "</div>";
        if (!regs.length) {
          html += '<div class="empty">暂无报名记录</div>';
        } else {
          html += '<div class="result-cards">' + regs.map(function (r) {
            var hidePay = !!(letter && String(letter.registrationId) === String(r.registrationId)
              && (letter.payStatus === 0 || letter.payStatus === "0"));
            return renderResultCard(runner, r, phone, hidePay);
          }).join("") + "</div>";
        }
        $("myResult").innerHTML = html;
        bindCashierStyles($("myResult"));

        var payBtn = $("btnConfirmPay");
        if (payBtn && letter) {
          payBtn.onclick = function () {
            confirmPay(letter.registrationId, phone, selectedChannel("letter"));
          };
        }
        var dlBtn = $("btnDownloadLetter");
        if (dlBtn && letter) {
          dlBtn.onclick = function () { downloadConfirmLetter(letter); };
        }
        var copyBtn = $("btnCopyLetter");
        if (copyBtn && letter) {
          copyBtn.onclick = function () {
            copyText(letterShareText(letter), "确认文案已复制，可发微信");
          };
        }
        var bibBtn = $("btnDownloadBib");
        if (bibBtn && letter) {
          bibBtn.onclick = function () { downloadEBib(letter); };
        }
        var dismiss = $("btnDismissLetter");
        if (dismiss) {
          dismiss.onclick = function () {
            clearLetter();
            var box = $("confirmLetter");
            if (box) box.remove();
          };
        }
        $("myResult").querySelectorAll("[data-share]").forEach(function (btn) {
          btn.addEventListener("click", function () {
            try {
              openShare(JSON.parse(decodeURIComponent(btn.getAttribute("data-share"))));
            } catch (err) {
              API.toast("无法生成分享图", "err");
            }
          });
        });
        $("myResult").querySelectorAll("[data-pay]").forEach(function (btn) {
          btn.addEventListener("click", function () {
            var rid = btn.getAttribute("data-pay");
            confirmPay(rid, phone, selectedChannel("pay" + rid));
          });
        });
        $("myResult").querySelectorAll("[data-cancel]").forEach(function (btn) {
          btn.addEventListener("click", function () {
            if (!confirm("确认退赛？已支付将按规则原路退回报名费（约 3–10 个工作日到账）。")) return;
            API.post("/api/public/cancel", {
              registrationId: Number(btn.getAttribute("data-cancel")),
              phone: phone
            }, { noAuth: true }).then(function (res) {
              var d = res.data || {};
              API.toast(d.message || "退赛已受理", "ok");
              queryMy();
            }).catch(function (e) { API.toast(e.message, "err"); });
          });
        });
        $("myResult").querySelectorAll("[data-letter]").forEach(function (btn) {
          btn.addEventListener("click", function () {
            try {
              downloadConfirmLetter(JSON.parse(decodeURIComponent(btn.getAttribute("data-letter"))));
            } catch (err) {
              API.toast("确认函生成失败", "err");
            }
          });
        });
        $("myResult").querySelectorAll("[data-copy-letter]").forEach(function (btn) {
          btn.addEventListener("click", function () {
            try {
              copyText(
                letterShareText(JSON.parse(decodeURIComponent(btn.getAttribute("data-copy-letter")))),
                "确认文案已复制，可发微信"
              );
            } catch (err) {
              API.toast("复制失败", "err");
            }
          });
        });
        $("myResult").querySelectorAll("[data-bib]").forEach(function (btn) {
          btn.addEventListener("click", function () {
            try {
              downloadEBib(JSON.parse(decodeURIComponent(btn.getAttribute("data-bib"))));
            } catch (err) {
              API.toast("号牌生成失败", "err");
            }
          });
        });
      })
      .catch(function (e) {
        $("myResult").innerHTML = '<div class="empty">' + escapeHtml(e.message) + "</div>";
        API.toast(e.message, "err");
      });
  }

  function renderResultCard(runner, r, phone, hidePayDup) {
    var result = r.result;
    var pending = !result;
    var unpaid = r.payStatus === 0 || r.payStatus === "0";
    var refunded = r.payStatus === 2 || r.payStatus === "2";
    var noBib = r.bibPending || !r.bibNumber;
    var cancelled = r.status === 2 || r.status === "2" || r.status === 3 || r.status === "3";
    var sex = API.genderText(runner.gender);
    var payload = {
      runnerName: runner.name || "",
      eventName: r.eventName || "",
      bib: r.bibNumber || "-",
      gun: result ? (result.chipTimeStr || API.secToClock(result.chipTime)) : "-",
      net: result ? (result.netTimeStr || API.secToClock(result.netTime)) : "-",
      pace: result && result.pace ? result.pace : "-",
      rank: result && result.rank != null ? String(result.rank) : "-",
      gender: sex,
      genderRank: result && result.genderRank != null ? String(result.genderRank) : "-",
      ageRank: result && result.ageRank != null ? String(result.ageRank) : "-",
      ageGroup: result && result.ageGroup ? result.ageGroup : "-"
    };
    var shareAttr = encodeURIComponent(JSON.stringify(payload));
    var statusLine =
      API.fmtDay(r.eventDate) + " · " + sex +
      " · " + API.payText(r.payStatus) + " · " + API.regStatusText(r.status) +
      (r.shirtSize ? " · 尺码 " + r.shirtSize : "") +
      (r.orderNo ? " · 订单 " + r.orderNo : "");
    if (r.payVoucher) statusLine += " · 凭证 " + r.payVoucher;
    if (r.refundNo) {
      statusLine += " · 退款 ¥" + (r.refundAmount != null ? r.refundAmount : "-") +
        " · " + API.refundText(r.refundStatus);
    }

    var cancelBtn = (!cancelled && r.canCancel)
      ? '<button type="button" class="btn btn-danger btn-sm" data-cancel="' +
          escapeHtml(String(r.registrationId || "")) + '">申请退赛退款</button>'
      : "";
    var ruleHint = r.cancelRule
      ? '<p class="cta-hint">' + escapeHtml(r.cancelRule) + "</p>"
      : "";
    var letterObj = {
      registrationId: r.registrationId,
      orderNo: r.orderNo,
      name: runner.name,
      phone: phone,
      eventName: r.eventName,
      eventDate: r.eventDate,
      shirtSize: r.shirtSize,
      payVoucher: r.payVoucher,
      payChannel: r.payChannel,
      bibNumber: r.bibNumber,
      bibHint: r.bibHint
    };
    var letterPayload = encodeURIComponent(JSON.stringify(letterObj));
    var bibPayload = encodeURIComponent(JSON.stringify({
      bibNumber: r.bibNumber,
      name: runner.name,
      eventName: r.eventName,
      eventDate: r.eventDate
    }));
    var letterBtn = (!unpaid && !cancelled)
      ? (
          '<button type="button" class="btn btn-signal btn-sm" data-letter="' + letterPayload + '">保存确认函图片</button>' +
          '<button type="button" class="btn btn-ghost btn-sm" data-copy-letter="' + letterPayload + '">复制确认文案</button>' +
          (!noBib
            ? '<button type="button" class="btn btn-ghost btn-sm" data-bib="' + bibPayload + '">保存电子号牌</button>'
            : "")
        )
      : "";

    var bodyHtml;
    if (cancelled || refunded) {
      bodyHtml =
        '<div class="empty" style="margin:0;text-align:left">' +
          (refunded
            ? ("已退赛 · 退款 ¥" + escapeHtml(String(r.refundAmount != null ? r.refundAmount : "-")) +
               " · 单号 " + escapeHtml(String(r.refundNo || "-")) +
               "（" + escapeHtml(API.refundText(r.refundStatus)) + "，约 3–10 个工作日到账）")
            : "已取消 / 退赛") +
        "</div>" + ruleHint;
    } else if (unpaid && hidePayDup) {
      bodyHtml =
        '<div class="empty" style="margin:0;text-align:left">请在上方确认函完成支付。</div>' +
        '<div class="rc-actions">' + cancelBtn + "</div>" + ruleHint;
    } else if (unpaid) {
      var fee = r.entryFee != null ? "¥" + r.entryFee : "";
      bodyHtml =
        '<div class="pay-urge"><strong>待支付提醒：</strong>名额已占用' +
          (fee ? "，请尽快支付 " + escapeHtml(fee) : "") +
          "，超时未支付可能导致名额释放。</div>" +
        cashierHtml("pay" + (r.registrationId || "")) +
        '<div class="rc-actions">' +
          '<button type="button" class="btn btn-signal btn-sm" data-pay="' +
            escapeHtml(String(r.registrationId || "")) +
          '">立即支付' + (fee ? " " + escapeHtml(fee) : "") + "</button>" + cancelBtn +
        "</div>" + ruleHint;
    } else if (pending) {
      bodyHtml =
        '<div class="empty" style="margin:0 0 .55rem">' +
          escapeHtml(r.resultEta || "成绩尚未录入，预计比赛日结束后 2–24 小时内公布。") +
        "</div>" +
        '<div class="rc-actions">' + letterBtn + cancelBtn + "</div>" + ruleHint;
    } else {
      var splitTimes = "";
      if (result.split5kStr || result.split10kStr || result.split21kStr) {
        splitTimes =
          '<div class="split-row">' +
            '<span class="pill">5K 累计 ' + escapeHtml(result.split5kStr || "-") + "</span>" +
            '<span class="pill">10K 累计 ' + escapeHtml(result.split10kStr || "-") + "</span>" +
            '<span class="pill">半马累计 ' + escapeHtml(result.split21kStr || "-") + "</span>" +
          "</div>";
      }
      var genderRankPill = "性别名次 -";
      if (payload.genderRank !== "-") {
        if (sex === "男") genderRankPill = "男子第 " + payload.genderRank;
        else if (sex === "女") genderRankPill = "女子第 " + payload.genderRank;
        else genderRankPill = "性别名次第 " + payload.genderRank;
      }
      bodyHtml =
        '<div class="rc-times">' +
          '<div><div class="lab">枪声成绩</div><div class="val gun">' + escapeHtml(payload.gun) + "</div></div>" +
          '<div><div class="lab">净成绩</div><div class="val">' + escapeHtml(payload.net) + "</div></div>" +
          '<div><div class="lab">配速</div><div class="val">' + escapeHtml(payload.pace) + "</div></div>" +
        "</div>" +
        renderPaceChart(result) +
        splitTimes +
        '<div class="rc-ranks">' +
          '<span class="pill signal">总名次第 ' + escapeHtml(payload.rank) + "</span>" +
          '<span class="pill">' + escapeHtml(genderRankPill) + "</span>" +
          '<span class="pill">' + escapeHtml(ageGroupLabel(payload.ageGroup)) +
            " 第 " + escapeHtml(payload.ageRank) + "</span>" +
        "</div>" +
        '<p class="age-legend">年龄组：U29≤29岁 · M30=30–39 · M40=40–49 · M50+=50+</p>' +
        '<div class="rc-actions">' +
          '<button type="button" class="btn btn-signal btn-sm" data-share="' + shareAttr + '">下载成绩证明</button>' +
          letterBtn +
          '<a class="btn btn-ghost btn-sm" href="#rank">对照公开榜</a>' +
          cancelBtn +
        "</div>" + ruleHint;
    }

    var bibHintHtml = noBib && !cancelled
      ? '<p class="cta-hint" style="margin:.15rem 0 .85rem">' +
          escapeHtml(r.bibHint || "号码布预计于比赛日前 7–14 天通过站内通知发放。") +
        "</p>"
      : "";

    var bibPreview = noBib
      ? (
          '<div class="bib-preview is-pending" title="号码布待发">' +
            "<small>BIB</small>" +
            "<strong>待发</strong>" +
          "</div>"
        )
      : (
          '<button type="button" class="bib-preview is-clickable" data-bib="' + bibPayload +
            '" title="点击保存电子号牌">' +
            "<small>电子号牌</small>" +
            "<strong>" + escapeHtml(r.bibNumber || "-") + "</strong>" +
            "<em>点按保存</em>" +
          "</button>"
        );

    return (
      '<article class="result-card">' +
        '<div class="rc-top">' +
          '<div class="rc-event">' + escapeHtml(r.eventName || "") +
            '<div class="muted" style="font-size:.85rem;font-weight:400">' + escapeHtml(statusLine) + "</div>" +
          "</div>" +
          bibPreview +
        "</div>" +
        bibHintHtml +
        bodyHtml +
      "</article>"
    );
  }

  function ageGroupLabel(code) {
    if (code === "U29") return "U29（≤29岁）";
    if (code === "M30") return "M30（30–39岁）";
    if (code === "M40") return "M40（40–49岁）";
    if (code === "M50+") return "M50+（≥50岁）";
    return code || "-";
  }

  function openShare(payload) {
    state.sharePayload = payload;
    state.shareText =
      "【发令台完赛成绩】\n" +
      (payload.eventName || "马拉松") + "\n" +
      (payload.runnerName || "-") + " · 号码布 " + (payload.bib || "-") + "\n" +
      "枪声 " + (payload.gun || "-") + " · 净成绩 " + (payload.net || "-") + "\n" +
      "配速 " + (payload.pace || "-") + " · 总名次 #" + (payload.rank || "-") + "\n" +
      "城墙之下 · 枪声之后 · 发令台";
    var canvas = $("shareCanvas");
    var ctx = canvas.getContext("2d");
    var w = 720, h = 960;
    canvas.width = w;
    canvas.height = h;

    var g = ctx.createLinearGradient(0, 0, w, h);
    g.addColorStop(0, "#1a1210");
    g.addColorStop(0.55, "#2a1614");
    g.addColorStop(1, "#120c0a");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, w, h);

    ctx.fillStyle = "#e23b2a";
    ctx.fillRect(0, 0, w, 10);
    ctx.fillStyle = "#f0b429";
    ctx.fillRect(0, 10, w, 4);

    ctx.fillStyle = "#f0b429";
    ctx.font = "600 36px Teko, sans-serif";
    ctx.fillText("GUNSTART  发令台", 48, 78);

    ctx.fillStyle = "#f7f1ec";
    ctx.font = "700 42px Noto Sans SC, sans-serif";
    wrapText(ctx, payload.eventName || "马拉松", 48, 140, w - 96, 48);

    ctx.fillStyle = "#b39a90";
    ctx.font = "500 22px Noto Sans SC, sans-serif";
    ctx.fillText("完赛证书 · FINISHER", 48, 250);

    ctx.fillStyle = "#f7f1ec";
    ctx.font = "700 56px Noto Sans SC, sans-serif";
    ctx.fillText(payload.runnerName || "-", 48, 330);

    ctx.fillStyle = "#f0b429";
    ctx.font = "700 64px Teko, sans-serif";
    ctx.fillText("BIB  " + (payload.bib || "-"), 48, 410);

    drawMetric(ctx, 48, 480, "枪声成绩", payload.gun || "-");
    drawMetric(ctx, 380, 480, "净成绩", payload.net || "-");
    drawMetric(ctx, 48, 600, "配速", payload.pace || "-");
    drawMetric(ctx, 380, 600, "总名次", "#" + (payload.rank || "-"));
    drawMetric(ctx, 48, 720, (payload.gender === "女" ? "女子名次" : "男子名次"), "#" + (payload.genderRank || "-"));
    drawMetric(ctx, 380, 720, ageGroupLabel(payload.ageGroup || "年龄组"), "#" + (payload.ageRank || "-"));

    ctx.fillStyle = "#b39a90";
    ctx.font = "400 18px Noto Sans SC, sans-serif";
    ctx.fillText("城墙之下 · 枪声之后 · 发令台记录你的一程", 48, 880);

    $("shareMask").classList.add("open");
  }

  function drawMetric(ctx, x, y, lab, val) {
    ctx.fillStyle = "#b39a90";
    ctx.font = "500 18px Noto Sans SC, sans-serif";
    ctx.fillText(lab, x, y);
    ctx.fillStyle = "#f7f1ec";
    ctx.font = "700 40px Teko, sans-serif";
    ctx.fillText(String(val), x, y + 48);
  }

  function wrapText(ctx, text, x, y, maxWidth, lineHeight) {
    var chars = String(text).split("");
    var line = "";
    var yy = y;
    for (var i = 0; i < chars.length; i++) {
      var test = line + chars[i];
      if (ctx.measureText(test).width > maxWidth && line) {
        ctx.fillText(line, x, yy);
        line = chars[i];
        yy += lineHeight;
      } else {
        line = test;
      }
    }
    if (line) ctx.fillText(line, x, yy);
  }

  function downloadShare() {
    var canvas = $("shareCanvas");
    var a = document.createElement("a");
    a.href = canvas.toDataURL("image/png");
    a.download = "gunstart-finisher.png";
    a.click();
  }

  function ensureRankEvents() {
    if (state.allEvents && state.allEvents.length) {
      fillRankSelect(state.allEvents);
      return Promise.resolve();
    }
    return API.get("/api/public/events/all", null, { noAuth: true }).then(function (res) {
      state.allEvents = res.data || [];
      fillRankSelect(state.allEvents);
    }).catch(function () {
      fillRankSelect(state.events || []);
    });
  }

  function fillRankSelect(list) {
    var sel = $("rankEvent");
    var cur = sel.value;
    var opts = (list || []).map(function (e) {
      return '<option value="' + e.id + '">' + escapeHtml(e.eventName) +
        "（" + API.statusText(e.status) + "）</option>";
    });
    if (!opts.length) opts.push('<option value="1">赛事 #1</option>');
    sel.innerHTML = opts.join("");
    if (cur) sel.value = cur;
  }

  function pickRank(row) {
    var v = row.overallRank != null ? row.overallRank : row.rank;
    if (v == null && row.RANK != null) v = row.RANK;
    return v;
  }

  var assistantBusy = false;
  var assistantBooted = false;

  function ensureAssistantReady() {
    if (!$("assistantHint")) return;
    $("assistantHint").textContent = "AI 服务 " + API.aiBase() + " · 需主工程与 ai-service 同时运行";
    if (assistantBooted) return;
    assistantBooted = true;
    appendAssistantMsg("bot", "你好，我是发令台赛事助手。可以问我报名中的赛事、剩余名额或报名费。", null, false);
    API.aiHealth()
      .then(function () {
        $("assistantHint").textContent = "助手在线 · " + API.aiBase();
      })
      .catch(function () {
        $("assistantHint").textContent = "助手未启动：请先运行 ai-service（" + API.aiBase() + "）";
      });
  }

  function appendAssistantMsg(role, text, events, grounded) {
    var log = $("assistantLog");
    if (!log) return;
    var wrap = document.createElement("div");
    wrap.className = "assistant-msg " + (role === "user" ? "user" : "bot");
    var roleEl = document.createElement("div");
    roleEl.className = "role";
    roleEl.textContent = role === "user" ? "你" : "助手";
    var bubble = document.createElement("div");
    bubble.className = "bubble";
    bubble.textContent = text || "";
    wrap.appendChild(roleEl);
    wrap.appendChild(bubble);
    if (events && events.length) {
      var refs = document.createElement("div");
      refs.className = "event-refs";
      events.forEach(function (ev) {
        var row = document.createElement("div");
        row.className = "event-ref";
        var name = ev.eventName || ("赛事 #" + ev.id);
        var link = document.createElement("a");
        link.href = "#event/" + ev.id;
        link.textContent = name;
        row.appendChild(link);
        var meta = document.createElement("span");
        meta.className = "muted";
        meta.textContent = [
          API.eventTypeText(ev.eventType),
          API.statusText(ev.status),
          ev.remainQuota != null ? ("余 " + ev.remainQuota + " 名") : "",
          ev.entryFee != null ? ("¥" + ev.entryFee) : ""
        ].filter(Boolean).join(" · ");
        row.appendChild(meta);
        refs.appendChild(row);
      });
      wrap.appendChild(refs);
    }
    if (role === "bot" && grounded) {
      var g = document.createElement("div");
      g.className = "grounded";
      g.textContent = "已引用系统实时赛事数据";
      wrap.appendChild(g);
    }
    log.appendChild(wrap);
    log.scrollTop = log.scrollHeight;
  }

  function askAssistant(question) {
    if (!question || assistantBusy) return;
    ensureAssistantReady();
    assistantBusy = true;
    if ($("assistantInput")) $("assistantInput").value = question;
    if ($("assistantSend")) $("assistantSend").disabled = true;
    appendAssistantMsg("user", question, null, false);
    var thinking = document.createElement("div");
    thinking.className = "assistant-msg bot";
    thinking.id = "assistantThinking";
    thinking.innerHTML = '<div class="role">助手</div><div class="bubble">查询中…</div>';
    $("assistantLog").appendChild(thinking);
    API.aiChat(question)
      .then(function (res) {
        var data = (res && res.data) || {};
        var el = $("assistantThinking");
        if (el) el.remove();
        appendAssistantMsg("bot", data.answer || "没有得到有效回答。", data.events || [], !!data.grounded);
      })
      .catch(function (err) {
        var el = $("assistantThinking");
        if (el) el.remove();
        appendAssistantMsg("bot", (err && err.message) || "助手暂时不可用，请确认 ai-service 已启动。", null, false);
      })
      .then(function () {
        assistantBusy = false;
        if ($("assistantSend")) $("assistantSend").disabled = false;
      });
  }

  function renderFaq() {
    var items = [
      ["怎么报名？", "选赛事 → 填资料 → 支付报名费 → 下载电子确认函。个人中心可随时查看进度。"],
      ["体检要做什么？", "建议赛前 3 个月内完成二级及以上医院体检，重点心电图与血压。组委会可抽查。"],
      ["保险怎么买？", "报名费已含基础意外险。如需加保，请自行向保险公司购买。退赛后退还报名费。"],
      ["退赛退多少？几天到账？", "赛前 7 天可在线退赛，全额原路退回，约 3–10 个工作日到账。逾期请打 029-8888-2026。"],
      ["成绩什么时候出？", "比赛日结束后约 2 小时起陆续公布，预计 24 小时内完整录入。报名中赛事若已有榜单，为成绩样例。"],
      ["如何邀请跑团好友？", "打开赛事详情，点「邀请跑团好友」复制链接发送即可。"]
    ];
    $("faqMount").innerHTML = items.map(function (it) {
      return '<div class="panel guide-card"><h4>' + escapeHtml(it[0]) + "</h4><p>" + escapeHtml(it[1]) + "</p></div>";
    }).join("");
  }

  function loadRank() {
    var eventId = $("rankEvent").value;
    var keyword = ($("rankKeyword").value || "").trim();
    var bibPrefix = ($("rankBibPrefix") && $("rankBibPrefix").value || "").trim().toUpperCase();
    var province = ($("rankProvince") && $("rankProvince").value || "").trim();
    if (!eventId) {
      API.toast("请选择赛事", "err");
      return;
    }
    $("rankMount").innerHTML = '<div class="empty">加载中…</div>';
    API.get("/api/public/rank/" + eventId, {
      keyword: keyword || undefined,
      bibPrefix: bibPrefix || undefined,
      province: province || undefined
    }, { noAuth: true })
      .then(function (res) {
        var list = res.data || [];
        if (!list.length) {
          var tip = [];
          if (bibPrefix) tip.push("号码段 " + bibPrefix);
          if (province) tip.push("所选省份");
          $("rankMount").innerHTML = '<div class="empty">暂无完赛成绩' +
            (tip.length ? "（" + tip.join(" · ") + "无匹配）" : "") + "</div>";
          return;
        }
        var ev = (state.allEvents || state.events || []).find(function (x) {
          return String(x.id) === String(eventId);
        });
        var sampleNote = ev && (ev.status === 0 || ev.status === "0") && list.length
          ? '<div class="sample-banner">' +
              "<strong>成绩样例榜</strong>" +
              "<span>本页展示样例完赛 " + list.length +
              " 人（公开接口上限内）。正式比赛预计完赛约 " +
              (ev.quota ? Math.round(ev.quota * 0.7) : "上万") +
              " 人，以比赛日芯片成绩为准。</span>" +
            "</div>"
          : (list.length
            ? '<p class="cta-hint">本场公开榜当前 ' + list.length + " 人（按筛选条件）。</p>"
            : "");
        var filterNote = (bibPrefix || province)
          ? '<p class="cta-hint">当前筛选：' +
            (bibPrefix ? ("号码段 " + escapeHtml(bibPrefix) + " ") : "") +
            (province ? ("省份 " + escapeHtml(($("rankProvince").selectedOptions[0] || {}).text || province)) : "") +
            " · 共 " + list.length + " 人</p>"
          : "";
        var podium = list.slice(0, 3).map(function (row, idx) {
          var place = pickRank(row);
          if (place == null) place = idx + 1;
          var net = row.netTimeStr || API.secToClock(row.netTime) || "-";
          var bib = row.bibNumber || "-";
          return (
            '<div class="podium-item">' +
              '<div class="place">#' + escapeHtml(String(place)) + "</div>" +
              '<div class="who">' + escapeHtml(row.name || "") + "</div>" +
              '<div class="meta">号码布 ' + escapeHtml(String(bib)) +
                (row.province ? (" · " + escapeHtml(row.province)) : "") +
                " · 净成绩 " + escapeHtml(String(net)) + "</div>" +
            "</div>"
          );
        }).join("");
        $("rankMount").innerHTML =
          sampleNote + filterNote +
          '<div class="podium">' + podium + "</div>" +
          '<div class="table-wrap"><table class="data"><thead><tr>' +
          "<th>总名次</th><th>号码布</th><th>姓名</th><th>省份</th><th>性别</th>" +
          "<th>性别名次</th><th>年龄组</th><th>年龄组名次</th>" +
          "<th>枪声成绩</th><th>净成绩</th><th>配速</th>" +
          "</tr></thead><tbody>" +
          list.map(function (row, idx) {
            var place = pickRank(row);
            if (place == null) place = idx + 1;
            var bib = row.bibNumber || row.bib_number || "-";
            var net = row.netTimeStr || row.net_time_str || API.secToClock(row.netTime || row.net_time) || "-";
            var gun = row.chipTimeStr || row.chip_time_str || API.secToClock(row.chipTime || row.chip_time) || "-";
            var rankClass = String(place) === "1" ? "rank-1" : "";
            var sex = API.genderText(row.gender);
            return (
              "<tr>" +
              '<td class="' + rankClass + '">' + escapeHtml(String(place)) + "</td>" +
              "<td>" + escapeHtml(String(bib)) + "</td>" +
              "<td>" + escapeHtml(row.name || "") + "</td>" +
              "<td>" + escapeHtml(row.province || "-") + "</td>" +
              "<td>" + sex + "</td>" +
              "<td>" + (row.genderRank != null
                ? ((sex === "女" ? "女子第 " : "男子第 ") + row.genderRank)
                : "-") + "</td>" +
              "<td>" + escapeHtml(ageGroupLabel(row.ageGroup || "-")) + "</td>" +
              "<td>" + (row.ageRank != null ? row.ageRank : "-") + "</td>" +
              "<td>" + escapeHtml(String(gun)) + "</td>" +
              "<td>" + escapeHtml(String(net)) + "</td>" +
              "<td>" + escapeHtml(row.pace || "-") + "</td>" +
              "</tr>"
            );
          }).join("") +
          "</tbody></table></div>";
      })
      .catch(function (e) {
        $("rankMount").innerHTML = '<div class="empty">' + escapeHtml(e.message) + "</div>";
        API.toast(e.message, "err");
      });
  }

  function sendOtp() {
    var phone = ($("myPhone").value || "").trim();
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      API.toast("请先填写手机号", "err");
      return;
    }
    API.post("/api/public/otp/send", { phone: phone }, { noAuth: true })
      .then(function (res) {
        var d = res.data || {};
        var code = d.code || d.demoCode;
        $("otpHint").textContent = code
          ? ("验证码已填入输入框，5 分钟内有效。")
          : (d.hint || "验证码已发送，5 分钟内有效");
        if (code) $("myOtp").value = code;
        API.toast("验证码已发送", "ok");
      })
      .catch(function (e) { API.toast(e.message, "err"); });
  }

  function otpLogin() {
    var phone = ($("myPhone").value || "").trim();
    var code = ($("myOtp").value || "").trim();
    API.post("/api/public/otp/login", { phone: phone, code: code }, { noAuth: true })
      .then(function (res) {
        var d = res.data || {};
        setRunnerSession(d.runnerToken, d.phone || phone, d.name);
        refreshSessionBar();
        API.toast("登录成功", "ok");
        queryMy();
      })
      .catch(function (e) { API.toast(e.message, "err"); });
  }

  function fillBirthFromIdCard() {
    var id = ($("suIdCard").value || "").trim();
    if (!/^[1-9]\d{16}[\dXx]$/.test(id)) return;
    var y = id.substring(6, 10);
    var m = id.substring(10, 12);
    var d = id.substring(12, 14);
    var birth = y + "-" + m + "-" + d;
    if (!isNaN(Date.parse(birth))) {
      $("suBirth").value = birth;
    }
  }

  document.addEventListener("DOMContentLoaded", function () {
    $("suIdCard").addEventListener("blur", fillBirthFromIdCard);
    $("suIdCard").addEventListener("input", function () {
      if (($("suIdCard").value || "").trim().length >= 14) fillBirthFromIdCard();
    });
    $("btnMyQuery").addEventListener("click", queryMy);
    $("btnSendOtp").addEventListener("click", sendOtp);
    $("btnOtpLogin").addEventListener("click", otpLogin);
    $("myPhone").addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") queryMy();
    });
    $("btnRankQuery").addEventListener("click", loadRank);
    $("rankKeyword").addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") loadRank();
    });
    if ($("rankBibPrefix")) {
      $("rankBibPrefix").addEventListener("keydown", function (ev) {
        if (ev.key === "Enter") loadRank();
      });
    }
    if ($("assistantForm")) {
      $("assistantForm").addEventListener("submit", function (ev) {
        ev.preventDefault();
        askAssistant(($("assistantInput").value || "").trim());
      });
    }
    if ($("assistantChips")) {
      $("assistantChips").addEventListener("click", function (ev) {
        var btn = ev.target.closest("[data-q]");
        if (!btn) return;
        askAssistant(btn.getAttribute("data-q") || "");
      });
    }
    $("btnSignupSubmit").addEventListener("click", submitSignup);
    $("shareClose").addEventListener("click", function () {
      $("shareMask").classList.remove("open");
    });
    $("shareDownload").addEventListener("click", downloadShare);
    if ($("shareCopyText")) {
      $("shareCopyText").addEventListener("click", function () {
        copyText(state.shareText, "成绩文案已复制");
      });
    }
    $("shareMask").addEventListener("click", function (e) {
      if (e.target === $("shareMask")) $("shareMask").classList.remove("open");
    });
    if ($("inviteCopy")) {
      $("inviteCopy").addEventListener("click", function () {
        copyText(state.inviteText, "邀请文案已复制，可发给跑团");
      });
    }
    if ($("inviteCopyLink")) {
      $("inviteCopyLink").addEventListener("click", function () {
        copyText(state.inviteUrl, "报名链接已复制");
      });
    }
    if ($("inviteClose")) {
      $("inviteClose").addEventListener("click", function () {
        $("inviteMask").classList.remove("open");
      });
    }
    if ($("inviteMask")) {
      $("inviteMask").addEventListener("click", function (e) {
        if (e.target === $("inviteMask")) $("inviteMask").classList.remove("open");
      });
    }
    try {
      var savedPhone = localStorage.getItem(RUNNER_PHONE_KEY);
      if (savedPhone) $("myPhone").value = savedPhone;
    } catch (e) {}
    refreshSessionBar();
    window.addEventListener("hashchange", route);
    route();
  });
})();
