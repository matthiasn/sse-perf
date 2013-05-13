/*global $, ssePerf */

$(function () {
    "use strict";
    ssePerf = ssePerf || {};
    ssePerf.metrics = [];

    var feed = new EventSource('/metricsFeed'),
        itemsReceived = 0,
        me = {},
        timer;

    /** from http://stackoverflow.com/questions/2901102/how-to-print-a-number-with-commas-as-thousands-separators-in-javascript */
    function numberWithCommas(x) { return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ","); }

    function updateUI(d) {
        $("#clientsTotal").html(numberWithCommas(d.clients));
        $("#clientsActive").html(numberWithCommas(d.activeClients));
        $("#KBReceived").html((d.bytesReceived / 1024 / d.msSinceLastReset * 1000).toFixed(1));
        $("#MBtotal").html(numberWithCommas((d.bytesReceivedTotal / 1024 / 1024).toFixed(0)));
        $("#MsgTotal").html(numberWithCommas(d.chunksTotal));
        $("#MsgS").html(numberWithCommas(d.msgSec.toFixed(0)));
    }

    function addMetric(item) {
        if (item.activeClients !== 0) {
            itemsReceived += 1;
            item.index = itemsReceived;
            item.chunksTotal = +item.chunksTotal;
            item.chunks = +item.chunks;
            item.msSinceLastReset = +item.msSinceLastReset;
            item.bytesReceivedTotal = +item.bytesReceivedTotal;
            item.bytesReceived = +item.bytesReceived;
            item.activeClients = +item.activeClients;
            item.clients = +item.clients;

            item.kbSec = (item.bytesReceived / 1024 / item.msSinceLastReset * 1000).toFixed(1);
            item.msgSec = item.chunks / item.msSinceLastReset * 1000;
            ssePerf.metrics.push(item);
            if (ssePerf.metrics.length > 30) { ssePerf.metrics.shift(); }
            me.clientsChart.reDraw();
            me.msgChart.reDraw();
            me.mbSecChart.reDraw();
            updateUI(item);
        }
    }

    function handler(msg) {
        var data = JSON.parse(msg.data);
        addMetric(data);
    }
    feed.addEventListener('message', handler, false);

    function addClients(n) {
        $.get("/clients/add?n=" + n + "&url=" + $("#urlField").val(), function (response) {
            console.log(response);
        });
    }

    function removeAll() {
        $.get("/clients/removeAll", function (response) {
            console.log(response);
        });
        setTimeout(function () {
            ssePerf.metrics = [];
            itemsReceived = 0;
        }, 4000);
        clearInterval(timer);
    }

    function ramp() {
        timer = setInterval(function () {
            addClients($("#rampCount").val());
        }, $("#rampInterval").val() * 1000);
    }

    $("#addOneButton").click(function () { addClients(1); });
    $("#addTenButton").click(function () { addClients(10); });
    $("#addFiftyButton").click(function () { addClients(50); });
    $("#removeAllButton").click(function () { removeAll(); });
    $("#rampButton").click(function () { ramp(); });
    $("#stopRampButton").click(function () { clearInterval(timer); });

    /** Chart for showing concurrent clients: value and label are identical */
    function clientsValue(d) { return d.activeClients; }
    me.clientsChart = ssePerf.BarChart(clientsValue, clientsValue, "#clientsChartSvg", "rgb(2, 62, 115)", 960, 150);

    /** Chart for showing cumulative Msg / sec: value and label are different */
    function msgValue(d) { return d.msgSec; }
    function msgLabel(d) { return d.msgSec.toFixed(0); }
    me.msgChart = ssePerf.BarChart(msgValue, msgLabel, "#msgChartSvg", "rgb(3, 140, 127)", 960, 150);

    /** Chart for showing cumulative MB / sec: value and label are different */
    function mbSecValue(d) { return d.kbSec / 1024; }
    function mbSecLabel(d) { return (d.kbSec / 1024).toFixed(1); }
    me.mbSecChart = ssePerf.BarChart(mbSecValue, mbSecLabel, "#mbSecChartSvg", "rgb(242, 154, 46)", 960, 150);
});