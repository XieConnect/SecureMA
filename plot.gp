set autoscale
set key box
set parametric
set term svg size 1920,1200 fsize 10
set object 1 rect from screen 0, 0, 0 to screen 1, 1, 0 behind
set object 1 rect fc  rgb "white"  fillstyle solid 1.0
set out "data/Sub.lnx.svg"
file = 'data/Sub.txt.all.csv'
set datafile separator ','

# plot error of ln(x) in high resolution
set xlabel 'x'
set ylabel 'Error of ln(x)'
#plot file every ::3 using 1:4 with impulses title 'over-estimate',\
#file every ::3 using 1:7 with linespoints title 'optimal'

plot file every ::3 u 1:4 w linespoints title 'over-estimate',\
     "" every ::3 using 1:4:(sprintf("%d",column(1))) with labels title '',\
     "" every ::3 using 1:7 with linespoints title 'optimal',\
     "" every ::3 using 1:7:(sprintf("%d",column(1))) with labels title ''

#!sleep 1000
