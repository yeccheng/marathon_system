(function (global) {
  var TOKEN_KEY = "gunstart_token";

  function toast(msg, type) {
    var host = document.getElementById("toastHost");
    if (!host) {
      host = document.createElement("div");
      host.id = "toastHost";
      host.className = "toast-host";
      document.body.appendChild(host);
    }
    var el = document.createElement("div");
    el.className = "toast" + (type === "err" ? " err" : type === "ok" ? " ok" : "");
    el.textContent = msg || "操作完成";
    host.appendChild(el);
    setTimeout(function () {
      el.remove();
    }, 3200);
  }

  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
  }

  function setToken(token) {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  }

  function qs(params) {
    if (!params) return "";
    var parts = [];
    Object.keys(params).forEach(function (k) {
      var v = params[k];
      if (v === undefined || v === null || v === "") return;
      parts.push(encodeURIComponent(k) + "=" + encodeURIComponent(v));
    });
    return parts.length ? "?" + parts.join("&") : "";
  }

  function request(method, url, body, opts) {
    opts = opts || {};
    var headers = Object.assign({}, opts.headers || {});
    if (!opts.noAuth) {
      var token = getToken();
      if (token) headers.Authorization = "Bearer " + token;
    }
    var init = { method: method, headers: headers };
    if (body instanceof FormData) {
      init.body = body;
    } else if (body !== undefined && body !== null) {
      headers["Content-Type"] = "application/json";
      init.body = JSON.stringify(body);
    }
    return fetch(url, init).then(function (res) {
      if (res.status === 401) {
        setToken("");
        var err = new Error("登录已过期，请重新登录");
        err.code = 401;
        throw err;
      }
      return res.json().then(function (json) {
        if (!res.ok) {
          var e = new Error((json && json.msg) || ("HTTP " + res.status));
          e.code = res.status;
          throw e;
        }
        if (json && json.code !== undefined && json.code !== 200) {
          var e2 = new Error(json.msg || "请求失败");
          e2.code = json.code;
          throw e2;
        }
        return json;
      });
    });
  }

  function eventTypeText(t) {
    if (t === 1 || t === "1") return "全程";
    if (t === 2 || t === "2") return "半程";
    if (t === 3 || t === "3") return "迷你";
    return "-";
  }

  function statusText(s) {
    if (s === 0 || s === "0") return "报名中";
    if (s === 1 || s === "1") return "报名截止";
    if (s === 2 || s === "2") return "已结束";
    return "-";
  }

  function genderText(g) {
    if (g === 1 || g === "1" || g === true) return "男";
    if (g === 2 || g === "2") return "女";
    var n = Number(g);
    if (n === 1) return "男";
    if (n === 2) return "女";
    return "-";
  }

  function finishText(f) {
    if (f === 1 || f === "1") return "完赛";
    if (f === 2 || f === "2") return "未完赛";
    if (f === 3 || f === "3") return "DNF/其它";
    return "-";
  }

  function fmtDate(v) {
    if (!v) return "-";
    var s = String(v);
    if (s.indexOf("T") > 0) return s.replace("T", " ").slice(0, 16);
    return s.length > 16 ? s.slice(0, 16) : s;
  }

  function fmtDay(v) {
    if (!v) return "-";
    return String(v).slice(0, 10);
  }

  function secToClock(sec) {
    if (sec === null || sec === undefined || sec === "") return "-";
    var n = Number(sec);
    if (isNaN(n)) return String(sec);
    var h = Math.floor(n / 3600);
    var m = Math.floor((n % 3600) / 60);
    var s = Math.floor(n % 60);
    return (
      String(h).padStart(2, "0") +
      ":" +
      String(m).padStart(2, "0") +
      ":" +
      String(s).padStart(2, "0")
    );
  }

  function payText(p) {
    if (p === 1 || p === "1") return "已支付";
    if (p === 2 || p === "2") return "已退款";
    if (p === 0 || p === "0") return "未支付";
    return "-";
  }

  function channelText(c) {
    if (c === "wechat") return "微信支付";
    if (c === "alipay") return "支付宝";
    if (c === "mock") return "在线支付";
    return c || "-";
  }

  function refundText(s) {
    if (s === 1 || s === "1") return "退款成功";
    if (s === 2 || s === "2") return "退款处理中";
    return "无退款";
  }

  function regStatusText(s) {
    if (s === 1 || s === "1") return "有效";
    if (s === 2 || s === "2") return "已取消";
    if (s === 3 || s === "3") return "退赛";
    if (s === 0 || s === "0") return "已取消";
    return "-";
  }

  function aiBase() {
    try {
      return localStorage.getItem("gunstart_ai_base") || "http://localhost:8082";
    } catch (e) {
      return "http://localhost:8082";
    }
  }

  function aiChat(question) {
    return request("POST", aiBase() + "/api/ai/chat", { question: question }, { noAuth: true });
  }

  function aiHealth() {
    return request("GET", aiBase() + "/api/ai/health", null, { noAuth: true });
  }

  global.API = {
    toast: toast,
    getToken: getToken,
    setToken: setToken,
    qs: qs,
    get: function (url, params, opts) {
      return request("GET", url + qs(params), null, opts);
    },
    post: function (url, body, opts) {
      return request("POST", url, body, opts);
    },
    put: function (url, body, opts) {
      return request("PUT", url, body, opts);
    },
    del: function (url, opts) {
      return request("DELETE", url, null, opts);
    },
    upload: function (url, file) {
      var fd = new FormData();
      fd.append("file", file);
      return request("POST", url, fd);
    },
    aiBase: aiBase,
    aiChat: aiChat,
    aiHealth: aiHealth,
    eventTypeText: eventTypeText,
    statusText: statusText,
    genderText: genderText,
    finishText: finishText,
    fmtDate: fmtDate,
    fmtDay: fmtDay,
    secToClock: secToClock,
    payText: payText,
    channelText: channelText,
    refundText: refundText,
    regStatusText: regStatusText
  };
})(window);
