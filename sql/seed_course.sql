UPDATE marathon_event SET
  aid_stations='[{"km":5,"name":"5K aid","items":"water"},{"km":10,"name":"10K aid","items":"water/gel"},{"km":15,"name":"15K aid","items":"water/banana"},{"km":21.1,"name":"Half","items":"water/medical"},{"km":30,"name":"30K aid","items":"water/gel"},{"km":35,"name":"35K aid","items":"water"},{"km":40,"name":"40K aid","items":"water"}]',
  route_profile='[{"km":0,"elev":400},{"km":5,"elev":405},{"km":10,"elev":412},{"km":15,"elev":420},{"km":21,"elev":430},{"km":25,"elev":425},{"km":30,"elev":435},{"km":35,"elev":450},{"km":40,"elev":445},{"km":42.2,"elev":440}]',
  cancel_rule='比赛日前7天（不含比赛日）可在线退赛并释放名额；此后仅支持现场医务退赛。演示环境退赛不产生真实退款，仅更新状态。'
WHERE id=1;
