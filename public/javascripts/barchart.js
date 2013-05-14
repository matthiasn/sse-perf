/*global d3*/

var ssePerf = ssePerf || {};

ssePerf.BarChart = function (valueFunction, labelFunction, div, color, textColorInside, textColorOutside, w, h) {
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
                d.height = yScale(valueFunction(d));
                return d.height;
            })
            .attr("fill", color);

        //Update…
        bars.transition().duration(500)
            .attr("x", function (d, i) {
                return i * 32;
            })
            .attr("y", function (d) {
                return h - yScale(valueFunction(d));
            })
            .attr("height", function (d) {
                d.height = yScale(valueFunction(d));
                return d.height;
            });

        //Exit…
        bars.exit().transition().duration(500).attr("x", -xScale.rangeBand()).remove();

        //Enter…
        labels.enter().append("text")
            .text(labelFunction)
            .attr("text-anchor", "middle")
            .attr("x", w)
            .attr("y", function (d) {
                if (d.height < 16.0) { return h - yScale(valueFunction(d)) - 10; }
                else return h - yScale(valueFunction(d)) + 12;                
            })
            .attr("font-family", "Open Sans")
            .attr("font-size", "10px")
            .attr("text-anchor", "middle")
            .attr("fill", function (d) {
                if (d.height < 16.0) { return textColorOutside; }
                else return textColorInside;
            });

        //Update…
        labels.transition().duration(500)
            .text(labelFunction)
            .attr("x", function (d, i) {
                return i * 32 + 15;
            })
            .attr("y", function (d) {
                if (d.height < 16.0) { return h - yScale(valueFunction(d)) - 2; }
                else return h - yScale(valueFunction(d)) + 12;
            })
            .attr("fill", function (d) {
                if (d.height < 16.0) { return textColorOutside; }
                else return textColorInside;
            });

        //Exit…
        labels.exit().transition().duration(500).attr("x", -xScale.rangeBand()).remove();
    };

    return me;
};