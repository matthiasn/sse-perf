/*global d3*/

var ssePerf = ssePerf || {};

ssePerf.BarChart = function (valueFunction, labelFunction, div, color, w, h) {
    "use strict";
    var me = {},
        xScale = d3.scale.ordinal().domain(d3.range(ssePerf.metrics.length)).rangeRoundBands([0, w], 0.05),
        yScale = d3.scale.linear().domain([0, d3.max(ssePerf.metrics, valueFunction)]).range([0, h]),
        key = function (d) { return +d.index; },
        svg = d3.select(div).append("svg").attr("width", w).attr("height", h);

    me.reDraw = function () {
        var bars = svg.selectAll("rect").data(ssePerf.metrics, key),
            labels = svg.selectAll("text").data(ssePerf.metrics, key);

        //Update scale domains
        xScale.domain(d3.range(ssePerf.metrics.length));
        yScale.domain([0, d3.max(ssePerf.metrics, valueFunction)]);

        //Enter…
        bars.enter().append("rect").attr("x", w)
            .attr("y", function (d) {
                return h - yScale(valueFunction(d));
            })
            .attr("width", 30)
            .attr("height", function (d) {
                return yScale(valueFunction(d));
            })
            .attr("fill", color);

        //Update…
        bars.transition().duration(500)
            .attr("x", function (d, i) {
                return i * 32;
            })
            .attr("y", function (d) {
                return h - yScale(valueFunction(d));
            });

        //Exit…
        bars.exit().transition().duration(500).attr("x", -xScale.rangeBand()).remove();

        //Enter…
        labels.enter().append("text")
            .text(labelFunction)
            .attr("text-anchor", "middle")
            .attr("x", w)
            .attr("y", function (d) {
                return h - yScale(valueFunction(d));
            })
            .attr("font-family", "sans-serif")
            .attr("font-size", "8px")
            .attr("font-weight", "bold")
            .attr("text-anchor", "middle")
            .attr("fill", "white");

        //Update…
        labels.transition().duration(500)
            .text(labelFunction)
            .attr("x", function (d, i) {
                return i * 32 + 15;
            })
            .attr("y", function (d) {
                return h - yScale(valueFunction(d)) + 14;
            });

        //Exit…
        labels.exit().transition().duration(500).attr("x", -xScale.rangeBand()).remove();
    };

    return me;
};