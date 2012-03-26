#!/usr/bin/env gnuplot
set terminal png size 640,480
set xlabel 'concurrency'
set ylabel 'millis'
set output 'tp99.png'
plot 'data.csv' using 2 with lines title 'tp99 response time'
set output 'mean.png'
plot 'data.csv' using 3 with lines title 'mean response time'
set output 'requests_per_second.png'
plot 'data.csv' using 4 with lines title 'requests/second'
