set autoscale
set key box
set parametric
set term svg size 1920,1200 fsize 10
set object 1 rect from screen 0, 0, 0 to screen 1, 1, 0 behind
set object 1 rect fc  rgb "white"  fillstyle solid 1.0
set out 'data/Sub.lnx.svg'
file = 'data/Sub.txt.lnx.csv'
set datafile separator ','

# plot error of ln(x) in high resolution
set xlabel 'x'
set ylabel 'Absolute error of ln(x)'

#set yrange [-0.000002 : 0.000001]
set yrange [-0.000005 : 0.000001]

plot file every ::3 u 1:4 w linespoints title 'over-estimate',\
     "" every ::3 using 1:4:(sprintf("%d",column(1))) with labels title '',\
     "" every ::3 using 1:9 with linespoints title 'optimal',\
     "" every ::3 using 1:9:(sprintf("%d",column(1))) with labels title ''

#!sleep 1000
